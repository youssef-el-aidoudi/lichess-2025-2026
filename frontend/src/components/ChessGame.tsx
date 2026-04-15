import { useEffect, useMemo, useRef, useState } from 'react';
import { RotateCcw, Download, Brain, Circle, Save } from 'lucide-react';
import {
  analyzePosition,
  createPartieJouee,
  getStockfishBestMove,
} from '../services/api';
import { useVoiceAccessibility } from '../hooks/useVoiceAccessibility';
import { useASR } from '../hooks/useASR';
import { VoiceToggle } from './VoiceToggle';
import { MicButton } from './MicButton';
import { parseWithAlternatives, type PieceType } from '../services/chessCommandParser';

interface ChessGameProps {
  mode: 'human' | 'bot';
  timeControl?: {
    minutes: number;
    increment: number;
  };
  engineSettings?: {
    depth: number | null;
    movetime: number | null;
    useDepth: boolean;
  } | null;
}

type Color = 'white' | 'black';

type Square = {
  row: number;
  col: number;
};

type MoveResult = {
  board: string[][];
  moveText: string;
  pgnMove: string;
  uciMove: string;
};

const INITIAL_BOARD: string[][] = [
  ['♜', '♞', '♝', '♛', '♚', '♝', '♞', '♜'],
  ['♟', '♟', '♟', '♟', '♟', '♟', '♟', '♟'],
  ['', '', '', '', '', '', '', ''],
  ['', '', '', '', '', '', '', ''],
  ['', '', '', '', '', '', '', ''],
  ['', '', '', '', '', '', '', ''],
  ['♙', '♙', '♙', '♙', '♙', '♙', '♙', '♙'],
  ['♖', '♘', '♗', '♕', '♔', '♗', '♘', '♖'],
];

const WHITE_PIECES = ['♙', '♖', '♘', '♗', '♕', '♔'];
const BLACK_PIECES = ['♟', '♜', '♞', '♝', '♛', '♚'];

/**
 * Correspondance type de pièce → symbole Unicode par couleur.
 * Utilisé par le résolveur de commandes vocales pour localiser les pièces
 * sur le plateau sans dépendre de l'état interne du composant.
 */
const PIECE_SYMBOLS_BY_TYPE: Record<PieceType, { white: string; black: string }> = {
  pawn:   { white: '\u2659', black: '\u265F' },
  knight: { white: '\u2658', black: '\u265E' },
  bishop: { white: '\u2657', black: '\u265D' },
  rook:   { white: '\u2656', black: '\u265C' },
  queen:  { white: '\u2655', black: '\u265B' },
  king:   { white: '\u2654', black: '\u265A' },
};

function cloneBoard(board: string[][]) {
  return board.map((row) => [...row]);
}

function isWhitePiece(piece: string) {
  return WHITE_PIECES.includes(piece);
}

function isBlackPiece(piece: string) {
  return BLACK_PIECES.includes(piece);
}

function sameColor(pieceA: string, pieceB: string) {
  if (!pieceA || !pieceB) return false;
  return (
    (isWhitePiece(pieceA) && isWhitePiece(pieceB)) ||
    (isBlackPiece(pieceA) && isBlackPiece(pieceB))
  );
}

function oppositeColor(color: Color): Color {
  return color === 'white' ? 'black' : 'white';
}

function formatTime(seconds: number) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

function pieceToNotation(piece: string) {
  const notationMap: Record<string, string> = {
    '♔': 'K',
    '♚': 'K',
    '♕': 'Q',
    '♛': 'Q',
    '♖': 'R',
    '♜': 'R',
    '♗': 'B',
    '♝': 'B',
    '♘': 'N',
    '♞': 'N',
    '♙': '',
    '♟': '',
  };

  return notationMap[piece] || '';
}


function isPathClear(
  board: string[][],
  fromRow: number,
  fromCol: number,
  toRow: number,
  toCol: number
) {
  const rowStep = toRow === fromRow ? 0 : toRow > fromRow ? 1 : -1;
  const colStep = toCol === fromCol ? 0 : toCol > fromCol ? 1 : -1;

  let row = fromRow + rowStep;
  let col = fromCol + colStep;

  while (row !== toRow || col !== toCol) {
    if (board[row][col] !== '') return false;
    row += rowStep;
    col += colStep;
  }

  return true;
}

function findKing(board: string[][], color: Color): Square | null {
  const king = color === 'white' ? '♔' : '♚';

  for (let row = 0; row < 8; row++) {
    for (let col = 0; col < 8; col++) {
      if (board[row][col] === king) {
        return { row, col };
      }
    }
  }

  return null;
}

function canAttackSquare(
  board: string[][],
  fromRow: number,
  fromCol: number,
  toRow: number,
  toCol: number
) {
  const piece = board[fromRow][fromCol];
  if (!piece) return false;

  const rowDiff = toRow - fromRow;
  const colDiff = toCol - fromCol;
  const absRow = Math.abs(rowDiff);
  const absCol = Math.abs(colDiff);

  if (piece === '♙') {
    return rowDiff === -1 && absCol === 1;
  }

  if (piece === '♟') {
    return rowDiff === 1 && absCol === 1;
  }

  if (piece === '♘' || piece === '♞') {
    return (absRow === 2 && absCol === 1) || (absRow === 1 && absCol === 2);
  }

  if (piece === '♗' || piece === '♝') {
    return absRow === absCol && isPathClear(board, fromRow, fromCol, toRow, toCol);
  }

  if (piece === '♖' || piece === '♜') {
    return (rowDiff === 0 || colDiff === 0) && isPathClear(board, fromRow, fromCol, toRow, toCol);
  }

  if (piece === '♕' || piece === '♛') {
    return (
      (rowDiff === 0 || colDiff === 0 || absRow === absCol) &&
      isPathClear(board, fromRow, fromCol, toRow, toCol)
    );
  }

  if (piece === '♔' || piece === '♚') {
    return absRow <= 1 && absCol <= 1 && !(absRow === 0 && absCol === 0);
  }

  return false;
}

function isSquareAttacked(board: string[][], row: number, col: number, byColor: Color) {
  for (let r = 0; r < 8; r++) {
    for (let c = 0; c < 8; c++) {
      const piece = board[r][c];
      if (!piece) continue;

      if (byColor === 'white' && !isWhitePiece(piece)) continue;
      if (byColor === 'black' && !isBlackPiece(piece)) continue;

      if (canAttackSquare(board, r, c, row, col)) {
        return true;
      }
    }
  }

  return false;
}

function isKingInCheck(board: string[][], color: Color) {
  const kingPos = findKing(board, color);
  if (!kingPos) return false;

  return isSquareAttacked(board, kingPos.row, kingPos.col, oppositeColor(color));
}

function getOpeningNameFromPgn(pgnMoves: string[]) {
  const joined = pgnMoves.join(' ');

  if (joined.startsWith('e4 c5')) return 'Défense sicilienne';
  if (joined.startsWith('e4 e5 Nf3 Nc6 Bc4')) return 'Partie italienne';
  if (joined.startsWith('e4 e5 Nf3 Nc6 Bb5')) return 'Ouverture espagnole';
  if (joined.startsWith('d4 d5 c4')) return 'Gambit dame';
  if (joined.startsWith('d4 Nf6 c4 g6')) return 'Défense indienne';
  if (joined.startsWith('e4 c6')) return 'Défense Caro-Kann';
  if (joined.startsWith('e4 e6')) return 'Défense française';

  return 'Partie locale';
}

function buildResultText(winner: string | null) {
  if (winner === 'Blancs') return 'Victoire blanc';
  if (winner === 'Noirs') return 'Victoire noir';
  if (winner === 'Nul') return 'Nulle';
  return 'En cours';
}

function buildPgnText(
  pgnMoves: string[],
  mode: 'human' | 'bot',
  winner: string | null
) {
  let formattedMoves = '';

  for (let i = 0; i < pgnMoves.length; i += 2) {
    const number = Math.floor(i / 2) + 1;
    const whiteMove = pgnMoves[i] || '';
    const blackMove = pgnMoves[i + 1] || '';
    formattedMoves += `${number}. ${whiteMove}${blackMove ? ` ${blackMove}` : ''}\n`;
  }

  const result = winner === 'Blancs' ? '1-0' : winner === 'Noirs' ? '0-1' : winner === 'Nul' ? '1/2-1/2' : '*';

  return `[Event "Chess Game"]
[Site "Chess Analytics Platform"]
[Date "${new Date().toISOString().split('T')[0]}"]
[Round "1"]
[White "Joueur"]
[Black "${mode === 'bot' ? 'Stockfish' : 'Joueur 2'}"]
[Result "${result}"]

${formattedMoves}${result}`;
}

export function ChessGame({
  mode,
  timeControl,
  engineSettings: passedEngineSettings,
}: ChessGameProps) {
  const [board, setBoard] = useState<string[][]>(cloneBoard(INITIAL_BOARD));
  const [selectedSquare, setSelectedSquare] = useState<Square | null>(null);
  const [moveHistory, setMoveHistory] = useState<string[]>([]);
  const [pgnMoves, setPgnMoves] = useState<string[]>([]);
  const [currentTurn, setCurrentTurn] = useState<Color>('white');
  const [gameStatus, setGameStatus] = useState('En cours');
  const [winner, setWinner] = useState<string | null>(null);
  const [thinking, setThinking] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState('');
  const [savedGameId, setSavedGameId] = useState<number | null>(null);

  const [whiteTime, setWhiteTime] = useState((timeControl?.minutes || 5) * 60);
  const [blackTime, setBlackTime] = useState((timeControl?.minutes || 5) * 60);

  const uciMovesRef = useRef<string[]>([]);
  const botMoveInProgress = useRef(false);

  // ── Module d'accessibilité vocale (Mode 1 — Suivi vocal) ─────────────────
  // Persistance de la préférence utilisateur dans localStorage
  const [voiceEnabled, setVoiceEnabled] = useState<boolean>(() => {
    try {
      return localStorage.getItem('chess_voice_enabled') === 'true';
    } catch {
      return false;
    }
  });

  const { isSupported: voiceSupported, liveText, speak } = useVoiceAccessibility({
    isEnabled: voiceEnabled,
    moveHistory,
    gameStatus,
    winner,
  });

  const handleVoiceToggle = (enabled: boolean) => {
    setVoiceEnabled(enabled);
    try {
      localStorage.setItem('chess_voice_enabled', String(enabled));
    } catch {
      // localStorage peut être indisponible en navigation privée
    }
  };

  // ── Module d'accessibilité vocale (Mode 2 — Saisie vocale des coups) ──────

  /** Texte de feedback affiché sous le bouton micro. */
  const [micFeedback, setMicFeedback] = useState('');

  /**
   * Résolveur de coup vocal — v2 (robuste)
   *
   * Cas gérés :
   * 1. Case complète "e4" → recherche directe
   * 2. Case partielle "pion 4" → cherche le pion du joueur courant qui peut aller sur rang 4
   * 3. Case partielle "pion e" → cherche le pion sur la colonne e ou qui peut y aller
   * 4. Ambiguïté → si 2+ candidats, retourne 'ambiguous'
   * 5. Meilleur candidat si 2+ pions sur même rang → prend le plus avancé
   */
  const resolveVoiceMove = (
    intent: import('../services/chessCommandParser').ChessIntent,
  ):
    | { ok: true; fromRow: number; fromCol: number; toRow: number; toCol: number }
    | { ok: false; error: 'illegal' | 'ambiguous' | 'partial' } => {

    if (!intent) return { ok: false, error: 'illegal' };

    // ── Roque ────────────────────────────────────────────────────────────
    if (intent.type === 'castle-short' || intent.type === 'castle-long') {
      const kingRow = currentTurn === 'white' ? 7 : 0;
      const toCol   = intent.type === 'castle-short' ? 6 : 2;
      if (isLegalMove(board, kingRow, 4, kingRow, toCol)) {
        return { ok: true, fromRow: kingRow, fromCol: 4, toRow: kingRow, toCol };
      }
      return { ok: false, error: 'illegal' };
    }

    if (intent.type !== 'move') return { ok: false, error: 'illegal' };

    const pieceSymbol = PIECE_SYMBOLS_BY_TYPE[intent.piece]?.[currentTurn];
    if (!pieceSymbol) return { ok: false, error: 'illegal' };

    // ── Cas case partielle : rang ou colonne manquant ─────────────────────
    const hasPartial = intent.toSquare.includes('?');
    const partialFile = intent.partialFile;  // 'e', 'd', etc. ou undefined
    const partialRank = intent.partialRank;  // '4', '3', etc. ou undefined

    if (hasPartial) {
      // Collecte tous les candidats pouvant jouer
      const allCandidates: { fromRow: number; fromCol: number; toRow: number; toCol: number }[] = [];

      for (let fromR = 0; fromR < 8; fromR++) {
        for (let fromC = 0; fromC < 8; fromC++) {
          if (board[fromR][fromC] !== pieceSymbol) continue;

          for (let toR = 0; toR < 8; toR++) {
            for (let toC = 0; toC < 8; toC++) {
              // Filtre par rang partiel
              if (partialRank !== undefined) {
                const expectedRank = 8 - parseInt(partialRank, 10);
                if (toR !== expectedRank) continue;
              }
              // Filtre par colonne partielle
              if (partialFile !== undefined) {
                const expectedCol = partialFile.charCodeAt(0) - 'a'.charCodeAt(0);
                if (toC !== expectedCol) continue;
              }
              if (isLegalMove(board, fromR, fromC, toR, toC)) {
                allCandidates.push({ fromRow: fromR, fromCol: fromC, toRow: toR, toCol: toC });
              }
            }
          }
        }
      }

      if (allCandidates.length === 0) return { ok: false, error: 'illegal' };

      // Si un unique candidat, on le joue directement
      if (allCandidates.length === 1) {
        return { ok: true, ...allCandidates[0] };
      }

      // Plusieurs candidats : pour les pions, on préfère le plus avancé (rangée la plus haute pour blanc)
      if (intent.piece === 'pawn') {
        const sorted = [...allCandidates].sort((a, b) =>
          currentTurn === 'white'
            ? a.fromRow - b.fromRow   // blanc : rang fromRow bas = plus avancé
            : b.fromRow - a.fromRow   // noir : rang fromRow haut = plus avancé
        );
        // Si candidat unique le plus avancé (ex: pion en e4 peut aller en e5 et e6, pas e3)
        // On prend le coup le plus court (avance d'une case de préférence)
        const oneStep = sorted.filter(c => Math.abs(c.toRow - c.fromRow) === 1);
        if (oneStep.length === 1) return { ok: true, ...oneStep[0] };
        // Sinon prend le premier (le plus avancé)
        const best = sorted[0];
        const sameCandidates = sorted.filter(c => c.fromRow === best.fromRow && c.fromCol === best.fromCol);
        if (sameCandidates.length === 1 || (partialRank && allCandidates.length <= 2)) {
          return { ok: true, ...sorted[0] };
        }
      }

      return { ok: false, error: 'ambiguous' };
    }

    // ── Cas case complète ─────────────────────────────────────────────────
    const fileChar = intent.toSquare[0];
    const rankChar = intent.toSquare[1];
    const toCol = fileChar.charCodeAt(0) - 'a'.charCodeAt(0);
    const toRow = 8 - parseInt(rankChar, 10);

    if (toCol < 0 || toCol > 7 || toRow < 0 || toRow > 7) {
      return { ok: false, error: 'illegal' };
    }

    const candidates: { fromRow: number; fromCol: number }[] = [];
    for (let r = 0; r < 8; r++) {
      for (let c = 0; c < 8; c++) {
        if (board[r][c] === pieceSymbol && isLegalMove(board, r, c, toRow, toCol)) {
          candidates.push({ fromRow: r, fromCol: c });
        }
      }
    }

    if (candidates.length === 1) {
      return { ok: true, ...candidates[0], toRow, toCol };
    }

    // Ambiguïté : si la pièce par défaut est 'pawn' (commande genre "e4"),
    // essayer les autres pièces du joueur sur cette case pour lever l'ambiguïté
    if (candidates.length > 1 && intent.piece === 'pawn') {
      // L'ASR a dit "e4" sans préciser : on préfère le pion unique
      const uniquePawnCandidates = candidates; // déjà filtrés sur pieceSymbol
      if (uniquePawnCandidates.length > 0) {
        // Prend le pion le plus proche de la destination (avance d'une case)
        const oneStep = uniquePawnCandidates.filter(c => Math.abs(toRow - c.fromRow) === 1);
        if (oneStep.length === 1) return { ok: true, ...oneStep[0], toRow, toCol };
        // Prend le pion sur la même colonne
        const sameFile = uniquePawnCandidates.filter(c => c.fromCol === toCol);
        if (sameFile.length === 1) return { ok: true, ...sameFile[0], toRow, toCol };
      }
      return { ok: false, error: 'ambiguous' };
    }

    if (candidates.length > 1) return { ok: false, error: 'ambiguous' };
    return { ok: false, error: 'illegal' };
  };

  /**
   * Gestionnaire principal d'une commande vocale — v3 (diagnostic avancé)
   *
   * Appelé par useASR avec primary, alternatives[], confidence.
   * Tente plusieurs stratégies avant d'échouer.
   * Affiche un feedback riche pour diagnostiquer les erreurs.
   */
  const handleVoiceCommand = (primary: string, alternatives: string[], _confidence?: number) => {
    // ── Gardes de sécurité ─────────────────────────────────────────────────
    if (winner) {
      setMicFeedback('⚠ Partie terminée.');
      speak('La partie est terminée.');
      return;
    }
    if (thinking) {
      setMicFeedback('⚠ Stockfish réfléchit…');
      speak('Attendez la réponse de Stockfish.');
      return;
    }
    // En mode bot, seul le tour blanc (joueur humain) peut jouer vocalement
    if (mode === 'bot' && currentTurn !== 'white') {
      setMicFeedback("⚠ Ce n'est pas votre tour.");
      speak("Ce n'est pas votre tour de jouer.");
      return;
    }

    // ── Log complet ASR pour diagnostic ───────────────────────────────────
    console.info('[Voice] Transcription ASR :', primary);
    if (alternatives.length > 0) console.info('[Voice] Alternatives ASR :', alternatives);

    // ── Parsing multi-stratégie ────────────────────────────────────────────
    const parsed = parseWithAlternatives(primary, alternatives);

    if (!parsed) {
      // Afficher ce qui a réellement été entendu pour aider le diagnostic
      const allHeard = [primary, ...alternatives].filter(Boolean).join(' | ');
      const heard = primary || alternatives[0] || '(rien)';
      console.warn('[Voice] Parsing échoué. ASR a entendu :', allHeard);
      setMicFeedback(`❌ Entendu: « ${heard} » — non compris`);
      if (voiceEnabled) speak('Non compris. Répétez en disant par exemple : e quatre, ou cavalier f trois.');
      return;
    }

    const { intent, transcript: usedTranscript, confidence } = parsed;

    // ── Log de l'intention décodée ──────────────────────────────────────────
    console.info('[Voice] Intention parsée :', intent, '(depuis « ' + usedTranscript + '»)');

    // ── Résolution du coup sur le plateau ─────────────────────────────────
    const resolution = resolveVoiceMove(intent);

    if (!resolution.ok) {
      // Construire un message utile qui dit EXACTEMENT ce qui était demandé
      const intentDesc = intent.type === 'move'
        ? `${intent.piece} → ${intent.toSquare}`
        : intent.type;
      console.warn('[Voice] Résolution échouée :', resolution.error, 'pour intent :', intentDesc);

      switch (resolution.error) {
        case 'ambiguous':
          setMicFeedback(`⚠ Ambigu : plusieurs pièces peuvent aller là. Précisez (ex: cavalier f3 au lieu de f3)`);
          if (voiceEnabled) speak('Ambiguïté. Précisez quelle pièce vous voulez jouer.');
          break;
        case 'partial':
          setMicFeedback(`⚠ Case incomplète — dites pièce ET case (ex: pion e4, cavalier f3)`);
          if (voiceEnabled) speak('Case incomplète. Dites la pièce et la case, par exemple pion e quatre.');
          break;
        default: {
          // Essayer de donner un message utile selon ce qui était demandé
          const sq = intent.type === 'move' ? intent.toSquare : '?';
          setMicFeedback(`❌ « ${usedTranscript} » → coup illégal (case ${sq} impossible pour ce tour)`);
          if (voiceEnabled) speak('Coup impossible. Vérifiez que ce coup est légal dans la position actuelle.');
        }
      }
      return;
    }

    const { fromRow, fromCol, toRow, toCol } = resolution;

    // ── Exécution réelle du coup ───────────────────────────────────────────
    const moveResult = applyMoveOnBoard(board, fromRow, fromCol, toRow, toCol);
    const nextTurn: Color = currentTurn === 'white' ? 'black' : 'white';

    setBoard(moveResult.board);
    setMoveHistory((prev) => [...prev, moveResult.moveText]);
    setPgnMoves((prev) => [...prev, moveResult.pgnMove]);
    setSelectedSquare(null);
    uciMovesRef.current = [...uciMovesRef.current, moveResult.uciMove];

    if (currentTurn === 'white') {
      setWhiteTime((prev) => prev + (timeControl?.increment ?? 0));
    } else {
      setBlackTime((prev) => prev + (timeControl?.increment ?? 0));
    }

    setCurrentTurn(nextTurn);
    finalizePositionAfterMove(moveResult.board, nextTurn);

    // Feedback enrichi selon la confiance
    const confidenceLabel = confidence === 'low' ? ' (interprété)' : '';
    const altLabel = usedTranscript !== primary ? ` (alt: « ${usedTranscript} »)` : '';
    console.info('[Voice] Coup joué :', moveResult.moveText);
    setMicFeedback(`✅ ${moveResult.moveText}${altLabel}${confidenceLabel}`);

    if (voiceEnabled) speak(`Coup joué : ${moveResult.moveText}`);

    setTimeout(() => setMicFeedback(''), 5000);
  };

  const { isListening, isSupported: asrSupported, startListening, stopListening, interimText: _interimText } = useASR({
    // useASR envoie (primary, alternatives, confidence) — on ignore confidence ici
    onResult: (primary, alternatives) => handleVoiceCommand(primary, alternatives),
    onInterim: (text) => {
      if (text.trim()) setMicFeedback(`🎤 « ${text} »…`);
    },
    onError: (err) => {
      setMicFeedback(`❌ ${err}`);
      if (voiceEnabled) speak(err);
    },
    lang: 'fr-FR',
  });

  const handleMicToggle = () => {
    if (isListening) {
      stopListening();
    } else {
      setMicFeedback('');
      startListening();
    }
  };
  // ─────────────────────────────────────────────────────────────────────────

  const engineSettings = useMemo(() => {
    if (!passedEngineSettings) {
      return {
        depth: 15,
        movetime: null as number | null,
      };
    }

    if (passedEngineSettings.useDepth) {
      return {
        depth: passedEngineSettings.depth ?? 15,
        movetime: null as number | null,
      };
    }

    return {
      depth: null as number | null,
      movetime: passedEngineSettings.movetime ?? 1000,
    };
  }, [passedEngineSettings]);

  const isTimerActive = !winner && !thinking;

  useEffect(() => {
    if (!isTimerActive) return;

    const interval = setInterval(() => {
      if (currentTurn === 'white') {
        setWhiteTime((prev) => {
          if (prev <= 1) {
            setWinner('Noirs');
            setGameStatus('Temps écoulé ! Les noirs ont gagné !');
            return 0;
          }
          return prev - 1;
        });
      } else {
        setBlackTime((prev) => {
          if (prev <= 1) {
            setWinner('Blancs');
            setGameStatus('Temps écoulé ! Les blancs ont gagné !');
            return 0;
          }
          return prev - 1;
        });
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [currentTurn, isTimerActive]);

  const isPseudoLegalMove = (
    currentBoard: string[][],
    fromRow: number,
    fromCol: number,
    toRow: number,
    toCol: number
  ) => {
    const piece = currentBoard[fromRow][fromCol];
    const target = currentBoard[toRow][toCol];

    if (!piece) return false;
    if (fromRow === toRow && fromCol === toCol) return false;
    if (target && sameColor(piece, target)) return false;

    const rowDiff = toRow - fromRow;
    const colDiff = toCol - fromCol;
    const absRow = Math.abs(rowDiff);
    const absCol = Math.abs(colDiff);

    if (piece === '♙') {
      if (colDiff === 0 && rowDiff === -1 && target === '') return true;
      if (fromRow === 6 && colDiff === 0 && rowDiff === -2 && target === '' && currentBoard[5][fromCol] === '') return true;
      if (absCol === 1 && rowDiff === -1 && target !== '' && isBlackPiece(target)) return true;
      return false;
    }

    if (piece === '♟') {
      if (colDiff === 0 && rowDiff === 1 && target === '') return true;
      if (fromRow === 1 && colDiff === 0 && rowDiff === 2 && target === '' && currentBoard[2][fromCol] === '') return true;
      if (absCol === 1 && rowDiff === 1 && target !== '' && isWhitePiece(target)) return true;
      return false;
    }

    if (piece === '♘' || piece === '♞') {
      return (absRow === 2 && absCol === 1) || (absRow === 1 && absCol === 2);
    }

    if (piece === '♗' || piece === '♝') {
      return absRow === absCol && isPathClear(currentBoard, fromRow, fromCol, toRow, toCol);
    }

    if (piece === '♖' || piece === '♜') {
      return (rowDiff === 0 || colDiff === 0) && isPathClear(currentBoard, fromRow, fromCol, toRow, toCol);
    }

    if (piece === '♕' || piece === '♛') {
      return (
        (rowDiff === 0 || colDiff === 0 || absRow === absCol) &&
        isPathClear(currentBoard, fromRow, fromCol, toRow, toCol)
      );
    }

    if (piece === '♔') {
      if (absRow <= 1 && absCol <= 1) return true;

      if (fromRow === 7 && fromCol === 4 && rowDiff === 0 && absCol === 2) {
        if (toCol === 6 && currentBoard[7][5] === '' && currentBoard[7][6] === '' && currentBoard[7][7] === '♖') {
          return true;
        }
        if (toCol === 2 && currentBoard[7][1] === '' && currentBoard[7][2] === '' && currentBoard[7][3] === '' && currentBoard[7][0] === '♖') {
          return true;
        }
      }

      return false;
    }

    if (piece === '♚') {
      if (absRow <= 1 && absCol <= 1) return true;

      if (fromRow === 0 && fromCol === 4 && rowDiff === 0 && absCol === 2) {
        if (toCol === 6 && currentBoard[0][5] === '' && currentBoard[0][6] === '' && currentBoard[0][7] === '♜') {
          return true;
        }
        if (toCol === 2 && currentBoard[0][1] === '' && currentBoard[0][2] === '' && currentBoard[0][3] === '' && currentBoard[0][0] === '♜') {
          return true;
        }
      }

      return false;
    }

    return false;
  };

  const applyMoveOnBoard = (
    currentBoard: string[][],
    fromRow: number,
    fromCol: number,
    toRow: number,
    toCol: number,
    options?: { promotion?: string; stockfish?: boolean }
  ): MoveResult => {
    const files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
    const newBoard = cloneBoard(currentBoard);
    const movingPiece = newBoard[fromRow][fromCol];
    const capturedPiece = newBoard[toRow][toCol];
    const fromText = `${files[fromCol]}${8 - fromRow}`;
    const toText = `${files[toCol]}${8 - toRow}`;

    let moveText = `${movingPiece} ${fromText} → ${toText}`;
    let pgnMove = '';
    let finalPiece = movingPiece;

    if ((movingPiece === '♔' || movingPiece === '♚') && Math.abs(toCol - fromCol) === 2) {
      if (toCol === 6) {
        newBoard[toRow][toCol] = movingPiece;
        newBoard[fromRow][fromCol] = '';
        newBoard[toRow][5] = newBoard[toRow][7];
        newBoard[toRow][7] = '';
        moveText = `${movingPiece} ${fromText} → ${toText} (petit roque)`;
        pgnMove = 'O-O';
      } else {
        newBoard[toRow][toCol] = movingPiece;
        newBoard[fromRow][fromCol] = '';
        newBoard[toRow][3] = newBoard[toRow][0];
        newBoard[toRow][0] = '';
        moveText = `${movingPiece} ${fromText} → ${toText} (grand roque)`;
        pgnMove = 'O-O-O';
      }

      return {
        board: newBoard,
        moveText: options?.stockfish ? `${moveText} (Stockfish)` : moveText,
        pgnMove,
        uciMove: `${fromText}${toText}`,
      };
    }

    newBoard[fromRow][fromCol] = '';

    if ((movingPiece === '♙' && toRow === 0) || (movingPiece === '♟' && toRow === 7)) {
      finalPiece =
        options?.promotion ||
        (movingPiece === '♙' ? '♕' : '♛');
    }

    newBoard[toRow][toCol] = finalPiece;

    if (capturedPiece) {
      moveText += ` (capture ${capturedPiece})`;
    }

    if (options?.promotion) {
      moveText += ` (promotion ${options.promotion})`;
    }

    const pieceNotation = pieceToNotation(movingPiece);

    if (capturedPiece) {
      if (pieceNotation === '') {
        pgnMove = `${files[fromCol]}x${toText}`;
      } else {
        pgnMove = `${pieceNotation}x${toText}`;
      }
    } else {
      pgnMove = `${pieceNotation}${toText}`;
    }

    if (options?.promotion) {
      const promotionLetter = pieceToNotation(options.promotion);
      pgnMove += `=${promotionLetter || 'Q'}`;
    }

    return {
      board: newBoard,
      moveText: options?.stockfish ? `${moveText} (Stockfish)` : moveText,
      pgnMove,
      uciMove: `${fromText}${toText}${options?.promotion ? pieceToNotation(options.promotion).toLowerCase() : ''}`,
    };
  };

  const isLegalMove = (
    currentBoard: string[][],
    fromRow: number,
    fromCol: number,
    toRow: number,
    toCol: number
  ) => {
    const piece = currentBoard[fromRow][fromCol];
    if (!piece) return false;

    if (!isPseudoLegalMove(currentBoard, fromRow, fromCol, toRow, toCol)) {
      return false;
    }

    const color: Color = isWhitePiece(piece) ? 'white' : 'black';
    const simulated = applyMoveOnBoard(currentBoard, fromRow, fromCol, toRow, toCol);
    return !isKingInCheck(simulated.board, color);
  };

  const legalMoves = useMemo(() => {
    if (!selectedSquare) return [];

    const piece = board[selectedSquare.row][selectedSquare.col];
    if (!piece) return [];

    const color: Color = isWhitePiece(piece) ? 'white' : 'black';
    if (color !== currentTurn) return [];

    const moves: Square[] = [];

    for (let row = 0; row < 8; row++) {
      for (let col = 0; col < 8; col++) {
        if (isLegalMove(board, selectedSquare.row, selectedSquare.col, row, col)) {
          moves.push({ row, col });
        }
      }
    }

    return moves;
  }, [board, selectedSquare, currentTurn]);

  const hasAnyLegalMove = (currentBoard: string[][], color: Color) => {
    for (let fromRow = 0; fromRow < 8; fromRow++) {
      for (let fromCol = 0; fromCol < 8; fromCol++) {
        const piece = currentBoard[fromRow][fromCol];
        if (!piece) continue;

        if (color === 'white' && !isWhitePiece(piece)) continue;
        if (color === 'black' && !isBlackPiece(piece)) continue;

        for (let toRow = 0; toRow < 8; toRow++) {
          for (let toCol = 0; toCol < 8; toCol++) {
            if (isLegalMove(currentBoard, fromRow, fromCol, toRow, toCol)) {
              return true;
            }
          }
        }
      }
    }

    return false;
  };

  const finalizePositionAfterMove = (newBoard: string[][], nextTurn: Color) => {
    const inCheck = isKingInCheck(newBoard, nextTurn);
    const canStillMove = hasAnyLegalMove(newBoard, nextTurn);

    if (!canStillMove && inCheck) {
      if (nextTurn === 'white') {
        setWinner('Noirs');
        setGameStatus('Échec et mat ! Les noirs ont gagné !');
      } else {
        setWinner('Blancs');
        setGameStatus('Échec et mat ! Les blancs ont gagné !');
      }
      return;
    }

    if (!canStillMove && !inCheck) {
      setWinner('Nul');
      setGameStatus('Pat ! Match nul');
      return;
    }

    if (inCheck) {
      setGameStatus('Échec !');
    } else {
      setGameStatus('En cours');
    }
  };

  const saveGameToHistory = async (forcedWinner?: string | null) => {
    if (saving) return;
    if (savedGameId) return;
    if (pgnMoves.length === 0) return;

    try {
      setSaving(true);
      setSaveMessage('');

      const finalWinner = forcedWinner ?? winner;
      const openingName = getOpeningNameFromPgn(pgnMoves);
      const pgn = buildPgnText(pgnMoves, mode, finalWinner);

      const payload = {
        titre: `Joueur vs ${mode === 'bot' ? 'Stockfish' : 'Joueur 2'} - ${new Date().toLocaleString('fr-FR')}`,
        joueurBlanc: 'Joueur',
        joueurNoir: mode === 'bot' ? 'Stockfish' : 'Joueur 2',
        resultat: buildResultText(finalWinner),
        variant: `${timeControl?.minutes || 5}+${timeControl?.increment || 0}`,
        ouverture: openingName,
        source: mode === 'bot' ? 'STOCKFISH' : 'LOCAL',
        vainqueur:
          finalWinner === 'Blancs'
            ? 'Joueur'
            : finalWinner === 'Noirs'
            ? mode === 'bot'
              ? 'Stockfish'
              : 'Joueur 2'
            : 'Nul',
        nombreCoups: pgnMoves.length,
        pgn,
        resumeAnalyse: `Partie ${mode === 'bot' ? 'contre Stockfish' : 'locale'} sauvegardée automatiquement.`,
      };

      const saved = await createPartieJouee(payload);
      if (saved?.id) {
        setSavedGameId(saved.id);
      }

      setSaveMessage('Partie enregistrée dans l’historique.');
      setTimeout(() => setSaveMessage(''), 3000);
    } catch (error) {
      console.error('Erreur sauvegarde historique :', error);
      setSaveMessage("Impossible d'enregistrer la partie.");
      setTimeout(() => setSaveMessage(''), 3000);
    } finally {
      setSaving(false);
    }
  };

  useEffect(() => {
    if (winner && !savedGameId) {
      saveGameToHistory(winner);
    }
  }, [winner, savedGameId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSquareClick = (row: number, col: number) => {
    if (winner || thinking) return;

    const piece = board[row][col];

    if (!selectedSquare) {
      if (currentTurn === 'white' && isWhitePiece(piece)) {
        setSelectedSquare({ row, col });
      } else if (currentTurn === 'black' && isBlackPiece(piece)) {
        setSelectedSquare({ row, col });
      }
      return;
    }

    if (selectedSquare.row === row && selectedSquare.col === col) {
      setSelectedSquare(null);
      return;
    }

    if (
      currentTurn === 'white' &&
      isWhitePiece(piece)
    ) {
      setSelectedSquare({ row, col });
      return;
    }

    if (
      currentTurn === 'black' &&
      isBlackPiece(piece)
    ) {
      setSelectedSquare({ row, col });
      return;
    }

    if (!isLegalMove(board, selectedSquare.row, selectedSquare.col, row, col)) {
      setSelectedSquare(null);
      return;
    }

    const movingPiece = board[selectedSquare.row][selectedSquare.col];
    const moveResult = applyMoveOnBoard(
      board,
      selectedSquare.row,
      selectedSquare.col,
      row,
      col
    );

    const nextTurn = currentTurn === 'white' ? 'black' : 'white';

    setBoard(moveResult.board);
    setMoveHistory((prev) => [...prev, moveResult.moveText]);
    setPgnMoves((prev) => [...prev, moveResult.pgnMove]);
    setSelectedSquare(null);

    if (movingPiece === '♙' || movingPiece === '♟' || movingPiece === '♘' || movingPiece === '♞' || movingPiece === '♗' || movingPiece === '♝' || movingPiece === '♖' || movingPiece === '♜' || movingPiece === '♕' || movingPiece === '♛' || movingPiece === '♔' || movingPiece === '♚') {
      uciMovesRef.current = [...uciMovesRef.current, moveResult.uciMove];
    }

    if (currentTurn === 'white') {
      setWhiteTime((prev) => prev + (timeControl?.increment || 0));
    } else {
      setBlackTime((prev) => prev + (timeControl?.increment || 0));
    }

    setCurrentTurn(nextTurn);
    finalizePositionAfterMove(moveResult.board, nextTurn);
  };

  useEffect(() => {
    if (mode !== 'bot') return;
    if (currentTurn !== 'black') return;
    if (winner) return;
    if (thinking) return;
    if (botMoveInProgress.current) return;

    botMoveInProgress.current = true;
    setThinking(true);

    const timer = setTimeout(async () => {
      try {
        const modeValue =
          engineSettings.movetime && engineSettings.movetime > 0 ? 'movetime' : 'depth';

        const depthValue = engineSettings.depth ?? 15;
        const movetimeValue = engineSettings.movetime ?? 1000;

        const bestMove = await getStockfishBestMove(
          uciMovesRef.current.join(' '),
          modeValue,
          'black',
          depthValue,
          movetimeValue
        );

        if (!bestMove || bestMove.length < 4) {
          throw new Error('Coup Stockfish invalide');
        }

        const fromCol = bestMove.charCodeAt(0) - 'a'.charCodeAt(0);
        const fromRow = 8 - Number(bestMove[1]);
        const toCol = bestMove.charCodeAt(2) - 'a'.charCodeAt(0);
        const toRow = 8 - Number(bestMove[3]);

        let promotionPiece: string | undefined;

        if (bestMove.length >= 5) {
          const promo = bestMove[4].toLowerCase();
          if (promo === 'q') promotionPiece = '♛';
          if (promo === 'r') promotionPiece = '♜';
          if (promo === 'b') promotionPiece = '♝';
          if (promo === 'n') promotionPiece = '♞';
        }

        if (!isLegalMove(board, fromRow, fromCol, toRow, toCol)) {
          throw new Error('Stockfish a renvoyé un coup illégal');
        }

        const moveResult = applyMoveOnBoard(board, fromRow, fromCol, toRow, toCol, {
          promotion: promotionPiece,
          stockfish: true,
        });

        setBoard(moveResult.board);
        setMoveHistory((prev) => [...prev, moveResult.moveText]);
        setPgnMoves((prev) => [...prev, moveResult.pgnMove]);
        uciMovesRef.current = [...uciMovesRef.current, bestMove];
        setCurrentTurn('white');
        setBlackTime((prev) => prev + (timeControl?.increment || 0));

        finalizePositionAfterMove(moveResult.board, 'white');
      } catch (error) {
        console.error('Erreur Stockfish :', error);
        setGameStatus("Erreur Stockfish : impossible de jouer le coup.");
      } finally {
        setThinking(false);
        botMoveInProgress.current = false;
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [mode, currentTurn, winner, board, engineSettings, thinking, timeControl]);

  const resetGame = () => {
    setBoard(cloneBoard(INITIAL_BOARD));
    setSelectedSquare(null);
    setMoveHistory([]);
    setPgnMoves([]);
    setCurrentTurn('white');
    setGameStatus('En cours');
    setWinner(null);
    setThinking(false);
    setSaving(false);
    setSaveMessage('');
    setSavedGameId(null);
    uciMovesRef.current = [];
    botMoveInProgress.current = false;
    setWhiteTime((timeControl?.minutes || 5) * 60);
    setBlackTime((timeControl?.minutes || 5) * 60);
  };

  const exportPGN = () => {
    const pgn = buildPgnText(pgnMoves, mode, winner);

    const blob = new Blob([pgn], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `chess-game-${Date.now()}.pgn`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const analyzeWithStockfish = async () => {
    try {
      const result = await analyzePosition(uciMovesRef.current.join(' '), 12);

      alert(
        `Analyse terminée\n\nÉvaluation: ${result.evaluation}\nMeilleur coup: ${result.bestmove}`
      );
    } catch (error) {
      console.error(error);
      alert("Analyse indisponible pour l’instant.");
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-gray-900 mb-2 text-2xl font-semibold">
            {mode === 'bot' ? 'Jouer contre Stockfish' : 'Jouer aux échecs'}
          </h2>
          <p className="text-gray-600">
            {mode === 'bot' ? "Affrontez Stockfish" : 'Partie locale entre deux joueurs'}
          </p>
        </div>
      </div>

      {saveMessage && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">
          {saveMessage}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <div className="bg-gradient-to-br from-slate-50 to-slate-100 rounded-2xl shadow-xl p-8 border border-slate-200">
            <div className="flex justify-center items-center mb-6 gap-6">
              <div className="flex items-center gap-3 px-5 py-3 rounded-xl bg-white shadow">
                <Circle className="w-4 h-4 text-slate-700 fill-white" />
                <div>
                  <div className="text-sm text-gray-600">Blancs</div>
                  <div className="text-lg font-mono text-gray-900">
                    {formatTime(whiteTime)}
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-3 px-5 py-3 rounded-xl bg-slate-800 shadow">
                <Circle className="w-4 h-4 text-slate-300 fill-slate-800" />
                <div>
                  <div className="text-sm text-slate-200">Noirs</div>
                  <div className="text-lg font-mono text-slate-100">
                    {formatTime(blackTime)}
                  </div>
                </div>
              </div>
            </div>

            <div className="max-w-2xl mx-auto">
              <div className="inline-block">
                <div className="flex">
                  <div className="flex flex-col justify-around pr-2">
                    {[8, 7, 6, 5, 4, 3, 2, 1].map((num) => (
                      <div
                        key={num}
                        className="h-16 sm:h-20 flex items-center justify-center text-slate-600 font-mono text-sm"
                      >
                        {num}
                      </div>
                    ))}
                  </div>

                  <div className="border-8 border-slate-800 rounded-2xl overflow-hidden shadow-2xl">
                    {board.map((row, rowIndex) => (
                      <div key={rowIndex} className="flex">
                        {row.map((piece, colIndex) => {
                          const isLight = (rowIndex + colIndex) % 2 === 0;
                          const isSelected =
                            selectedSquare?.row === rowIndex &&
                            selectedSquare?.col === colIndex;
                          const isLegalMove = legalMoves.some(
                            (move) => move.row === rowIndex && move.col === colIndex
                          );

                          return (
                            <button
                              key={`${rowIndex}-${colIndex}`}
                              onClick={() => handleSquareClick(rowIndex, colIndex)}
                              disabled={thinking || !!winner}
                              className={`w-16 h-16 sm:w-20 sm:h-20 flex items-center justify-center text-5xl sm:text-6xl transition-all duration-200 relative ${
                                isLight
                                  ? 'bg-gradient-to-br from-amber-50 to-amber-100'
                                  : 'bg-gradient-to-br from-amber-600 to-amber-800'
                              } ${isSelected ? 'ring-4 ring-blue-500 ring-inset' : ''}`}
                            >
                              {piece && <span>{piece}</span>}

                              {isLegalMove && (
                                <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                                  {board[rowIndex][colIndex] === '' ? (
                                    <div className="w-4 h-4 bg-green-500/60 rounded-full shadow-lg" />
                                  ) : (
                                    <div className="absolute inset-0 bg-red-500/30 rounded-sm ring-2 ring-red-500/60 ring-inset" />
                                  )}
                                </div>
                              )}
                            </button>
                          );
                        })}
                      </div>
                    ))}
                  </div>
                </div>

                <div className="flex pl-8">
                  {['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'].map((letter) => (
                    <div
                      key={letter}
                      className="w-16 sm:w-20 flex items-center justify-center pt-2 text-slate-600 font-mono text-sm"
                    >
                      {letter}
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div
                className={`px-6 py-3 rounded-xl shadow-md ${
                  winner
                    ? 'bg-gradient-to-r from-yellow-400 to-yellow-500 text-yellow-900'
                    : gameStatus === 'Échec !'
                    ? 'bg-gradient-to-r from-red-500 to-red-600 text-white'
                    : 'bg-gradient-to-r from-green-500 to-green-600 text-white'
                }`}
              >
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-white" />
                  <span>{thinking ? 'Stockfish réfléchit...' : gameStatus}</span>
                </div>
              </div>
            </div>

            <div className="mt-6 flex flex-wrap gap-3">
              <button
                onClick={resetGame}
                className="flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-blue-600 to-blue-700 text-white rounded-xl hover:from-blue-700 hover:to-blue-800 transition-all duration-200 shadow-md"
              >
                <RotateCcw className="w-5 h-5" />
                Nouvelle partie
              </button>

              <button
                onClick={exportPGN}
                disabled={moveHistory.length === 0}
                className="flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-slate-600 to-slate-700 text-white rounded-xl hover:from-slate-700 hover:to-slate-800 transition-all duration-200 shadow-md disabled:opacity-50"
              >
                <Download className="w-5 h-5" />
                Exporter PGN
              </button>

              <button
                onClick={analyzeWithStockfish}
                disabled={moveHistory.length === 0}
                className="flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-purple-600 to-purple-700 text-white rounded-xl hover:from-purple-700 hover:to-purple-800 transition-all duration-200 shadow-md disabled:opacity-50"
              >
                <Brain className="w-5 h-5" />
                Analyser
              </button>

              <button
                onClick={() => saveGameToHistory(winner)}
                disabled={moveHistory.length === 0 || saving}
                className="flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-emerald-600 to-emerald-700 text-white rounded-xl hover:from-emerald-700 hover:to-emerald-800 transition-all duration-200 shadow-md disabled:opacity-50"
              >
                <Save className="w-5 h-5" />
                Sauvegarder
              </button>

              {/* ── Module vocal Mode 1 — Suivi vocal ── */}
              <VoiceToggle
                enabled={voiceEnabled}
                onToggle={handleVoiceToggle}
                isSupported={voiceSupported}
              />

              {/* ── Module vocal Mode 2 — Saisie vocale des coups ── */}
              <MicButton
                isListening={isListening}
                onToggle={handleMicToggle}
                isSupported={asrSupported}
                feedback={micFeedback}
                disabled={!!winner || thinking || (mode === 'bot' && currentTurn === 'black')}
              />
            </div>

            {/* ── Zone aria-live pour les lecteurs d'écran système ── */}
            {/* Visuellement cachée, mais lue par NVDA, VoiceOver, etc. */}
            <div
              aria-live="polite"
              aria-atomic="true"
              style={{
                position: 'absolute',
                width: '1px',
                height: '1px',
                padding: 0,
                margin: '-1px',
                overflow: 'hidden',
                clip: 'rect(0,0,0,0)',
                whiteSpace: 'nowrap',
                borderWidth: 0,
              }}
            >
              {liveText}
            </div>
          </div>
        </div>

        <div className="lg:col-span-1">
          <div className="bg-white rounded-2xl shadow-xl p-6 border border-slate-200">
            <h3 className="text-gray-900 mb-4 flex items-center gap-2 text-lg font-semibold">
              <div className="w-1 h-6 bg-gradient-to-b from-blue-500 to-purple-500 rounded-full" />
              Historique des coups
            </h3>

            <div className="space-y-2 max-h-[600px] overflow-y-auto pr-2">
              {moveHistory.length === 0 ? (
                <div className="text-center py-12">
                  <Circle className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                  <p className="text-gray-500">Aucun coup joué</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {moveHistory.map((move, index) => (
                    <div
                      key={index}
                      className={`px-4 py-3 rounded-xl text-sm transition-all duration-200 ${
                        index === moveHistory.length - 1
                          ? 'bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 shadow-sm'
                          : 'bg-slate-50 hover:bg-slate-100'
                      }`}
                    >
                      <div className="flex items-start gap-2">
                        <span
                          className={`mt-0.5 px-2 py-0.5 rounded text-xs ${
                            index % 2 === 0
                              ? 'bg-blue-100 text-blue-700'
                              : 'bg-purple-100 text-purple-700'
                          }`}
                        >
                          {Math.floor(index / 2) + 1}.{index % 2 === 0 ? '' : '..'}
                        </span>
                        <span className="text-gray-900 flex-1">{move}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="mt-6 pt-4 border-t border-slate-200">
              <div className="flex items-center justify-between px-3 py-2 bg-gradient-to-r from-slate-50 to-slate-100 rounded-lg">
                <span className="text-sm text-slate-600">Total coups:</span>
                <span className="px-3 py-1 bg-white rounded-md shadow-sm text-slate-900">
                  {moveHistory.length}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}