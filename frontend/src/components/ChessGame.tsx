import { useState, useEffect, useRef } from 'react';
import { RotateCcw, Download, Brain, Circle } from 'lucide-react';
import { getStockfishBestMove, analyzePosition } from '../services/api';

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

interface EngineSettings {
  depth: number | null;
  movetime: number | null;
}

const INITIAL_BOARD = [
  ['♜', '♞', '♝', '♛', '♚', '♝', '♞', '♜'],
  ['♟', '♟', '♟', '♟', '♟', '♟', '♟', '♟'],
  ['', '', '', '', '', '', '', ''],
  ['', '', '', '', '', '', '', ''],
  ['', '', '', '', '', '', '', ''],
  ['', '', '', '', '', '', '', ''],
  ['♙', '♙', '♙', '♙', '♙', '♙', '♙', '♙'],
  ['♖', '♘', '♗', '♕', '♔', '♗', '♘', '♖'],
];

export function ChessGame({ mode, timeControl, engineSettings: passedEngineSettings }: ChessGameProps) {
  const [board, setBoard] = useState(INITIAL_BOARD);
  const [selectedSquare, setSelectedSquare] = useState<{ row: number; col: number } | null>(null);
  const [moveHistory, setMoveHistory] = useState<string[]>([]);
  const [pgnMoves, setPgnMoves] = useState<string[]>([]);
  const [currentTurn, setCurrentTurn] = useState<'white' | 'black'>('white');
  const [gameStatus, setGameStatus] = useState<string>('En cours');
  const [backendStatus, setBackendStatus] = useState<'checking' | 'connected' | 'disconnected'>('checking');
  // Silence unused variable warning until used in UI
  useEffect(() => { if (backendStatus) { } }, [backendStatus]);
  const [thinking, setThinking] = useState(false);
  const [winner, setWinner] = useState<string | null>(null);

  const [whiteTime, setWhiteTime] = useState((timeControl?.minutes || 5) * 60);
  const [blackTime, setBlackTime] = useState((timeControl?.minutes || 5) * 60);
  const [isTimerActive, setIsTimerActive] = useState(true);

  // Track pieces that have moved for castling validation
  const [movedPieces, setMovedPieces] = useState<Set<string>>(new Set());

  // Track the last pawn double move for en passant
  const [lastEnPassantFile, setLastEnPassantFile] = useState<number | null>(null);

  // Track UCI moves for Stockfish (more reliable than parsing moveHistory)
  const uciMovesRef = useRef<string[]>([]);

  const [engineSettings] = useState<EngineSettings>(() => {
    if (passedEngineSettings) {
      if (passedEngineSettings.useDepth) {
        return {
          depth: passedEngineSettings.depth,
          movetime: null,
        };
      } else {
        return {
          depth: null,
          movetime: passedEngineSettings.movetime,
        };
      }
    }
    // Default: use depth mode
    return {
      depth: 15,
      movetime: null,
    };
  });

  const [promotionState, setPromotionState] = useState<{
    row: number;
    col: number;
    isWhite: boolean;
    fromRow: number;
    fromCol: number;
  } | null>(null);

  // Guard against concurrent bot moves
  const botMoveInProgress = useRef(false);

  // Timer
  useEffect(() => {
    if (!isTimerActive || winner || thinking) return;

    const interval = setInterval(() => {
      if (currentTurn === 'white') {
        setWhiteTime((prev) => {
          if (prev <= 1) {
            setWinner('Noirs');
            setGameStatus('Temps écoulé ! Les noirs ont gagné !');
            setIsTimerActive(false);
            return 0;
          }
          return prev - 1;
        });
      } else {
        setBlackTime((prev) => {
          if (prev <= 1) {
            setWinner('Blancs');
            setGameStatus('Temps écoulé ! Les blancs ont gagné !');
            setIsTimerActive(false);
            return 0;
          }
          return prev - 1;
        });
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [currentTurn, isTimerActive, winner, thinking]);

  // Trigger bot move when it's the bot's turn
  useEffect(() => {
    if (mode !== 'bot' || currentTurn !== 'white' || thinking || winner) {
      return;
    }

    // Don't do anything - bot moves are triggered by currentTurn changing to 'black' below
  }, [currentTurn, mode, thinking, winner]);

  // Formater le temps en mm:ss
  const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const isWhitePiece = (piece: string) => {
    return ['♙', '♖', '♘', '♗', '♕', '♔'].includes(piece);
  };

  const isBlackPiece = (piece: string) => {
    return ['♟', '♜', '♞', '♝', '♛', '♚'].includes(piece);
  };

  const pieceToNotation = (piece: string): string => {
    const notationMap: { [key: string]: string } = {
      '♔': 'K', '♚': 'K',
      '♕': 'Q', '♛': 'Q',
      '♖': 'R', '♜': 'R',
      '♗': 'B', '♝': 'B',
      '♘': 'N', '♞': 'N',
      '♙': '', '♟': '', // Les pions n'ont pas de lettre
    };
    return notationMap[piece] || '';
  };

  const findKing = (boardState: string[][], isWhite: boolean): { row: number; col: number } | null => {
    const kingPiece = isWhite ? '♔' : '♚';
    for (let row = 0; row < 8; row++) {
      for (let col = 0; col < 8; col++) {
        if (boardState[row][col] === kingPiece) {
          return { row, col };
        }
      }
    }
    return null;
  };

  const isSquareAttacked = (boardState: string[][], row: number, col: number, byWhite: boolean): boolean => {
    // Vérifier si une case est attaquée par un joueur
    for (let r = 0; r < 8; r++) {
      for (let c = 0; c < 8; c++) {
        const piece = boardState[r][c];
        if (piece === '') continue;

        // Vérifier si la pièce appartient à l'attaquant
        const isPieceWhite = isWhitePiece(piece);
        if (isPieceWhite !== byWhite) continue;

        // Vérifier si cette pièce peut légalement attaquer la case cible
        if (isValidMove(boardState, r, c, row, col, true)) {
          return true;
        }
      }
    }
    return false;
  };

  const isPathClear = (boardState: string[][], fromRow: number, fromCol: number, toRow: number, toCol: number): boolean => {
    const rowDir = toRow > fromRow ? 1 : toRow < fromRow ? -1 : 0;
    const colDir = toCol > fromCol ? 1 : toCol < fromCol ? -1 : 0;

    let currentRow = fromRow + rowDir;
    let currentCol = fromCol + colDir;

    while (currentRow !== toRow || currentCol !== toCol) {
      if (boardState[currentRow][currentCol] !== '') {
        return false; // Obstacle trouvé
      }
      currentRow += rowDir;
      currentCol += colDir;
    }

    return true;
  };

  // Check backend connectivity
  const checkBackendStatus = async () => {
    try {
      setBackendStatus('checking');
      const API_URL = import.meta.env.VITE_API_URL || 'https://api.docker.localhost';
      const response = await fetch(`${API_URL}/api/chess/bestmove?moves=&mode=depth&color=black&depth=1&movetime=100`, {
        method: 'GET',
        signal: AbortSignal.timeout(3000),
      });

      if (response.ok) {
        setBackendStatus('connected');
        if (gameStatus.includes('❌ Erreur')) {
          setGameStatus('En cours'); // Clear error status when backend reconnects
        }
      } else {
        setBackendStatus('disconnected');
      }
    } catch (error) {
      console.log('Backend check failed:', error);
      setBackendStatus('disconnected');
    }
  };

  // Check backend status on component mount and periodically
  useEffect(() => {
    checkBackendStatus();
    const interval = setInterval(checkBackendStatus, 30000); // Check every 30 seconds instead of 10
    return () => clearInterval(interval);
  }, []);

  const isValidMove = (boardState: string[][], fromRow: number, fromCol: number, toRow: number, toCol: number, isAttackCheck: boolean = false): boolean => {
    const piece = boardState[fromRow][fromCol];
    const targetPiece = boardState[toRow][toCol];
    const rowDiff = toRow - fromRow;
    const colDiff = toCol - fromCol;
    const absRowDiff = Math.abs(rowDiff);
    const absColDiff = Math.abs(colDiff);

    // Pion blanc (♙)
    if (piece === '♙') {
      // Avance simple
      if (colDiff === 0 && rowDiff === -1 && targetPiece === '') {
        return true;
      }
      // Avance double depuis la position initiale
      if (colDiff === 0 && rowDiff === -2 && fromRow === 6 && targetPiece === '' && boardState[fromRow - 1][fromCol] === '') {
        return true;
      }
      // Capture en diagonale (must capture an enemy piece)
      if (absColDiff === 1 && rowDiff === -1 && targetPiece !== '' && !isWhitePiece(targetPiece)) {
        return true;
      }
      // En passant: capture diagonale sur case vide
      if (absColDiff === 1 && rowDiff === -1 && targetPiece === '' && fromRow === 3 && lastEnPassantFile === toCol) {
        return true;
      }
      return false;
    }

    // Pion noir (♟)
    if (piece === '♟') {
      // Avance simple
      if (colDiff === 0 && rowDiff === 1 && targetPiece === '') {
        return true;
      }
      // Avance double depuis la position initiale
      if (colDiff === 0 && rowDiff === 2 && fromRow === 1 && targetPiece === '' && boardState[fromRow + 1][fromCol] === '') {
        return true;
      }
      // Capture en diagonale (must capture an enemy piece)
      if (absColDiff === 1 && rowDiff === 1 && targetPiece !== '' && isWhitePiece(targetPiece)) {
        return true;
      }
      // En passant: capture diagonale sur case vide
      if (absColDiff === 1 && rowDiff === 1 && targetPiece === '' && fromRow === 4 && lastEnPassantFile === toCol) {
        return true;
      }
      return false;
    }

    // Tour (♖, ♜)
    if (piece === '♖' || piece === '♜') {
      if ((rowDiff === 0 || colDiff === 0) && isPathClear(boardState, fromRow, fromCol, toRow, toCol)) {
        return true;
      }
      return false;
    }

    // Cavalier (♘, ♞)
    if (piece === '♘' || piece === '♞') {
      if ((absRowDiff === 2 && absColDiff === 1) || (absRowDiff === 1 && absColDiff === 2)) {
        return true;
      }
      return false;
    }

    // Fou (♗, ♝)
    if (piece === '♗' || piece === '♝') {
      if (absRowDiff === absColDiff && absRowDiff > 0 && isPathClear(boardState, fromRow, fromCol, toRow, toCol)) {
        return true;
      }
      return false;
    }

    // Reine (♕, ♛)
    if (piece === '♕' || piece === '♛') {
      if ((rowDiff === 0 || colDiff === 0 || absRowDiff === absColDiff) && isPathClear(boardState, fromRow, fromCol, toRow, toCol)) {
        return true;
      }
      return false;
    }

    // Roi (♔, ♚)
    if (piece === '♔' || piece === '♚') {
      // Mouvement normal du roi (1 case dans n'importe quelle direction)
      if (absRowDiff <= 1 && absColDiff <= 1 && !(rowDiff === 0 && colDiff === 0)) {
        return true;
      }

      // Castling (grand roque et petit roque)
      if (rowDiff === 0 && absColDiff === 2 && !isAttackCheck) {
        const isWhiteKing = piece === '♔';
        const kingRow = isWhiteKing ? 7 : 0;
        const rookCol = colDiff > 0 ? 7 : 0; // Petit roque = col droite (7), Grand roque = col gauche (0)

        // Vérifier que le roi est en position initiale
        if (fromRow === kingRow && fromCol === 4) {
          // Vérifier que la tour est en place
          const rook = boardState[kingRow][rookCol];
          const isWhiteRook = rook === '♖';
          const isBlackRook = rook === '♜';

          if ((isWhiteKing && isWhiteRook) || (!isWhiteKing && isBlackRook)) {
            // Vérifier que le chemin est dégagé
            if (isPathClear(boardState, fromRow, fromCol, kingRow, rookCol)) {
              // Vérifier que ni le roi ni la tour n'ont bougé
              const kingPieceId = isWhiteKing ? 'K' : 'k';
              const rookPieceId = isWhiteKing ? `R${rookCol}` : `r${rookCol}`;

              const kingHasMoved = movedPieces.has(kingPieceId);
              const rookHasMoved = movedPieces.has(rookPieceId);

              return !kingHasMoved && !rookHasMoved;
            }
          }
        }
      }

      return false;
    }

    return false;
  };

  const isKingInCheck = (boardState: string[][], isWhiteKing: boolean): boolean => {
    const kingPos = findKing(boardState, isWhiteKing);
    if (!kingPos) return false;

    // Vérifier si le roi est attaqué par l'adversaire
    return isSquareAttacked(boardState, kingPos.row, kingPos.col, !isWhiteKing);
  };

  // Vérifier si le joueur a des coups légaux disponibles
  const hasLegalMoves = (boardState: string[][], isWhite: boolean): boolean => {
    for (let fromRow = 0; fromRow < 8; fromRow++) {
      for (let fromCol = 0; fromCol < 8; fromCol++) {
        const piece = boardState[fromRow][fromCol];
        if (piece === '') continue;

        // Vérifier si la pièce appartient au joueur
        const isPieceWhite = isWhitePiece(piece);
        if (isPieceWhite !== isWhite) continue;

        // Tester tous les coups possibles pour cette pièce
        for (let toRow = 0; toRow < 8; toRow++) {
          for (let toCol = 0; toCol < 8; toCol++) {
            // Vérifier que ce n'est pas la case source
            if (toRow === fromRow && toCol === fromCol) continue;

            // Vérifier qu'on n'essaie pas de capturer une pièce alliée
            const targetPiece = boardState[toRow][toCol];
            if (targetPiece !== '' && isWhitePiece(targetPiece) === isWhite) continue;

            if (isValidMove(boardState, fromRow, fromCol, toRow, toCol)) {
              // Simuler le coup
              const testBoard = boardState.map(r => [...r]);
              testBoard[toRow][toCol] = testBoard[fromRow][fromCol];
              testBoard[fromRow][fromCol] = '';

              // Si le roi n'est pas en échec après ce coup, c'est un coup légal
              if (!isKingInCheck(testBoard, isWhite)) {
                return true;
              }
            }
          }
        }
      }
    }
    return false;
  };

  const checkGameOver = (boardState: string[][], nextPlayerIsWhite: boolean) => {
    // Vérifier si le joueur suivant a des coups légaux
    const canMove = hasLegalMoves(boardState, nextPlayerIsWhite);

    if (!canMove) {
      // Vérifier si le roi est en échec
      const inCheck = isKingInCheck(boardState, nextPlayerIsWhite);

      if (inCheck) {
        // Échec et mat
        if (nextPlayerIsWhite) {
          setGameStatus('Échec et mat ! Les noirs ont gagné !');
          setWinner('Noirs');
          setIsTimerActive(false);
        } else {
          setGameStatus('Échec et mat ! Les blancs ont gagné !');
          setWinner('Blancs');
          setIsTimerActive(false);
        }
        return true;
      } else {
        // Pat (nulle)
        setGameStatus('Pat ! Match nul');
        setWinner('Nul');
        setIsTimerActive(false);
        return true;
      }
    }

    return false;
  };

  const handleSquareClick = (row: number, col: number) => {
    // Si la partie est terminée ou en cours de réflexion, ne rien faire
    if (winner || thinking) return;

    const piece = board[row][col];

    // Si aucune pièce n'est sélectionnée
    if (!selectedSquare) {
      // Vérifier si la pièce appartient au joueur actuel
      if (currentTurn === 'white' && isWhitePiece(piece)) {
        setSelectedSquare({ row, col });
      } else if (currentTurn === 'black' && isBlackPiece(piece)) {
        setSelectedSquare({ row, col });
      }
    } else {
      // Si on clique sur la même case, désélectionner
      if (selectedSquare.row === row && selectedSquare.col === col) {
        setSelectedSquare(null);
        return;
      }

      // Si on clique sur une de nos propres pièces, la sélectionner à la place
      const clickedPiece = board[row][col];
      if (currentTurn === 'white' && isWhitePiece(clickedPiece)) {
        setSelectedSquare({ row, col });
        return;
      }
      if (currentTurn === 'black' && isBlackPiece(clickedPiece)) {
        setSelectedSquare({ row, col });
        return;
      }

      // Déplacer la pièce
      const newBoard = board.map(r => [...r]);
      const movingPiece = newBoard[selectedSquare.row][selectedSquare.col];
      const capturedPiece = newBoard[row][col];

      // Vérifier si le coup est valide selon les règles de la pièce
      if (!isValidMove(board, selectedSquare.row, selectedSquare.col, row, col)) {
        setSelectedSquare(null);
        return;
      }

      // Enregistrer le coup pour l'affichage
      const files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
      let move = `${movingPiece} ${files[selectedSquare.col]}${8 - selectedSquare.row} → ${files[col]}${8 - row}`;

      if (capturedPiece) {
        move += ` (capture ${capturedPiece})`;
      }

      // Appliquer le coup et vérifier le castling
      newBoard[row][col] = movingPiece;
      newBoard[selectedSquare.row][selectedSquare.col] = '';

      // Gérer la capture en passant (pion capture en diagonale sur case vide)
      if ((movingPiece === '♙' || movingPiece === '♟') && !capturedPiece && selectedSquare.col !== col) {
        // C'est une capture en diagonale sur case vide - enlever le pion capturé
        const capturedPawnRow = selectedSquare.row; // Le pion capturé est sur la même ligne que la source
        newBoard[capturedPawnRow][col] = '';
        const capturedPawn = movingPiece === '♙' ? '♟' : '♙';
        move = `${movingPiece} ${files[selectedSquare.col]}${8 - selectedSquare.row} → ${files[col]}${8 - row} (en passant ${capturedPawn})`;
      }

      // Gérer le castling (déplacer aussi la tour)
      if ((movingPiece === '♔' || movingPiece === '♚') && Math.abs(col - selectedSquare.col) === 2) {
        const kingRow = selectedSquare.row;
        if (col > selectedSquare.col) {
          // Petit roque (roi se déplace vers la droite)
          const rookCol = 7;
          const rook = newBoard[kingRow][rookCol];
          newBoard[kingRow][5] = rook; // La tour se déplace en col 5
          newBoard[kingRow][rookCol] = '';
          move = `${movingPiece} ${files[selectedSquare.col]}${8 - kingRow} → ${files[col]}${8 - kingRow} (petit roque)`;
        } else {
          // Grand roque (roi se déplace vers la gauche)
          const rookCol = 0;
          const rook = newBoard[kingRow][rookCol];
          newBoard[kingRow][3] = rook; // La tour se déplace en col 3
          newBoard[kingRow][rookCol] = '';
          move = `${movingPiece} ${files[selectedSquare.col]}${8 - kingRow} → ${files[col]}${8 - kingRow} (grand roque)`;
        }
      }

      // Créer la notation PGN standard
      const pieceNotation = pieceToNotation(movingPiece);
      // const fromSquare = `${files[selectedSquare.col]}${8 - selectedSquare.row}`;
      const toSquare = `${files[col]}${8 - row}`;

      // Check for pawn promotion (white pawns reach row 0, black pawns reach row 7)
      if ((movingPiece === '♙' && row === 0) || (movingPiece === '♟' && row === 7)) {
        // Promotion needed - update board with pawn position first, then show promotion dialog
        setBoard(newBoard);
        setPromotionState({
          row,
          col,
          isWhite: movingPiece === '♙',
          fromRow: selectedSquare.row,
          fromCol: selectedSquare.col
        });
        // Store the move info for after promotion
        setSelectedSquare(null);
        return;
      }

      // Vérifier si le roi actuel serait en échec après le coup (coup illégal)
      const isWhiteTurn = currentTurn === 'white';
      if (isKingInCheck(newBoard, isWhiteTurn)) {
        // Coup illégal - on reste en échec ou on se met en échec
        return;
      }

      // Vérifier si on met l'adversaire en échec
      const opponentInCheck = isKingInCheck(newBoard, !isWhiteTurn);

      // Vérifier si la partie est terminée après le coup
      const willBeGameOver = checkGameOver(newBoard, !isWhiteTurn);

      // Construire le coup en notation PGN
      let pgnMove = '';

      // Check for castling
      if ((movingPiece === '♔' || movingPiece === '♚') && Math.abs(col - selectedSquare.col) === 2) {
        // Castling notation in PGN
        if (col > selectedSquare.col) {
          pgnMove = 'O-O'; // King-side castling (petit roque)
        } else {
          pgnMove = 'O-O-O'; // Queen-side castling (grand roque)
        }
      } else if (capturedPiece) {
        // Capture
        if (pieceNotation === '') {
          // Pion qui capture
          pgnMove = `${files[selectedSquare.col]}x${toSquare}`;
        } else {
          pgnMove = `${pieceNotation}x${toSquare}`;
        }
      } else {
        // Déplacement simple
        pgnMove = `${pieceNotation}${toSquare}`;
      }

      // Ajouter # pour échec et mat, ou + pour échec
      if (willBeGameOver) {
        pgnMove += '#';
        move += ' (échec et mat)';
      } else if (opponentInCheck) {
        pgnMove += '+';
        move += ' (échec)';
        setGameStatus('Échec !');
      }
      // Check for pawn double move (for en passant on next move)
      const isPawnDoubleMoveWhite = movingPiece === '♙' && selectedSquare.row === 6 && row === 4;
      const isPawnDoubleMoveBlack = movingPiece === '♟' && selectedSquare.row === 1 && row === 3;

      // Track UCI move for Stockfish
      const fromFile = String.fromCharCode('a'.charCodeAt(0) + selectedSquare.col);
      const fromRank = 8 - selectedSquare.row;
      const toFile = String.fromCharCode('a'.charCodeAt(0) + col);
      const toRank = 8 - row;
      const uciMove = `${fromFile}${fromRank}${toFile}${toRank}`;
      uciMovesRef.current = [...uciMovesRef.current, uciMove];
      console.log('=== PLAYER MOVE TRACKING ===');
      console.log('Player move:', `${movingPiece} ${files[selectedSquare.col]}${8 - selectedSquare.row} → ${files[col]}${8 - row}`);
      console.log('Player UCI move:', uciMove);
      console.log('All UCI moves:', uciMovesRef.current.join(' '));
      console.log('Updated board will be sent to Stockfish');
      console.log('================================');

      const updatedMoveHistory = [...moveHistory, move];
      setBoard(newBoard);
      setMoveHistory(updatedMoveHistory);
      setPgnMoves([...pgnMoves, pgnMove]);
      setSelectedSquare(null);

      // Track pieces that have moved (for castling validation)
      const newMovedPieces = new Set(movedPieces);
      if (movingPiece === '♔' || movingPiece === '♚') {
        newMovedPieces.add(movingPiece === '♔' ? 'K' : 'k');
      } else if (movingPiece === '♖' || movingPiece === '♜') {
        newMovedPieces.add(movingPiece === '♖' ? `R${selectedSquare.col}` : `r${selectedSquare.col}`);
      }
      setMovedPieces(newMovedPieces);

      // Update en passant file if pawn double move
      if (isPawnDoubleMoveWhite || isPawnDoubleMoveBlack) {
        setLastEnPassantFile(col);
      } else {
        setLastEnPassantFile(null);
      }

      // Ajouter l'incrément de temps après le coup
      if (currentTurn === 'white') {
        setWhiteTime(prev => prev + (timeControl?.increment || 0));
      } else {
        setBlackTime(prev => prev + (timeControl?.increment || 0));
      }

      if (!willBeGameOver) {
        setCurrentTurn(currentTurn === 'white' ? 'black' : 'white');
        // Bot move will be triggered by useEffect when turn changes
      } else {
        // Game is over - disable all interaction
        setIsTimerActive(false);
        setSelectedSquare(null);
      }
    }
  };

  // Calculer les coups légaux pour la pièce sélectionnée
  const getLegalMoves = (): { row: number; col: number }[] => {
    if (!selectedSquare) return [];

    const legalMoves: { row: number; col: number }[] = [];

    for (let row = 0; row < 8; row++) {
      for (let col = 0; col < 8; col++) {
        if (isValidMove(board, selectedSquare.row, selectedSquare.col, row, col)) {
          // Vérifier que ce coup ne mettrait pas notre roi en échec
          const testBoard = board.map(r => [...r]);
          testBoard[row][col] = testBoard[selectedSquare.row][selectedSquare.col];
          testBoard[selectedSquare.row][selectedSquare.col] = '';

          const isWhiteTurn = currentTurn === 'white';
          if (!isKingInCheck(testBoard, isWhiteTurn)) {
            legalMoves.push({ row, col });
          }
        }
      }
    }

    return legalMoves;
  };

  const legalMoves = getLegalMoves();

  const handlePromotion = (piece: string) => {
    if (!promotionState) return;

    const newBoard = board.map((r: string[]) => [...r]);
    newBoard[promotionState.row][promotionState.col] = piece;

    const files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
    const toSquare = `${files[promotionState.col]}${8 - promotionState.row}`;
    const fromSquare = `${files[promotionState.fromCol]}${8 - promotionState.fromRow}`;

    // Build descriptive move string
    const moveStr = `Promotion: ${fromSquare} → ${toSquare} = ${piece}`;

    // Build PGN notation for promotion (e.g., e8=Q)
    const promotedPieceNotation: { [key: string]: string } = {
      '♕': 'Q', '♛': 'Q',
      '♖': 'R', '♜': 'R',
      '♗': 'B', '♝': 'B',
      '♘': 'N', '♞': 'N',
    };
    const pgnMove = `${toSquare}=${promotedPieceNotation[piece] || 'Q'}`;

    // Track UCI move for Stockfish (with promotion piece)
    const uciPromotion = promotedPieceNotation[piece]?.toLowerCase() || 'q';
    const uciMove = `${fromSquare}${toSquare}${uciPromotion}`;
    uciMovesRef.current = [...uciMovesRef.current, uciMove];
    console.log('=== PROMOTION MOVE TRACKING ===');
    console.log('Promotion move:', `${fromSquare}${toSquare}=${promotedPieceNotation[piece] || 'Q'}`);
    console.log('Promotion UCI move:', uciMove);
    console.log('All UCI moves:', uciMovesRef.current.join(' '));
    console.log('===================================');

    const isWhiteTurn = currentTurn === 'white';
    // const opponentInCheck = isKingInCheck(newBoard, !isWhiteTurn);
    const gameOver = checkGameOver(newBoard, !isWhiteTurn);

    // Update board and move history
    setBoard(newBoard);
    setMoveHistory([...moveHistory, moveStr]);
    setPgnMoves([...pgnMoves, pgnMove]);

    // Add increment time
    if (currentTurn === 'white') {
      setWhiteTime((prev: number) => prev + (timeControl?.increment || 0));
    } else {
      setBlackTime((prev: number) => prev + (timeControl?.increment || 0));
    }

    setPromotionState(null);

    if (!gameOver) {
      setCurrentTurn(currentTurn === 'white' ? 'black' : 'white');
      // Bot move will be triggered by useEffect when turn changes
    } else {
      setIsTimerActive(false);
    }
  };

  // Trigger bot move when it's the bot's turn (after makeBotMove is defined)
  useEffect(() => {
    console.log('=== BOT USEEFFECT TRIGGERED ===');
    console.log('Mode:', mode, 'Current turn:', currentTurn, 'Thinking:', thinking, 'Winner:', winner);
    console.log('Bot move in progress:', botMoveInProgress.current);
    console.log('Current UCI moves when bot turn starts:', uciMovesRef.current.join(' '));

    // Early exit conditions - must be bot mode, black's turn, not thinking, no winner, and bot not in progress
    if (mode !== 'bot' || currentTurn !== 'black' || thinking || winner || botMoveInProgress.current) {
      console.log('Bot move skipped due to conditions');
      return;
    }

    console.log('✅ Starting bot move process...');

    // Set bot move in progress immediately to prevent race conditions
    botMoveInProgress.current = true;
    setThinking(true);

    const makeBotMove = async () => {
      // Validate engine settings before making a move
      if ((engineSettings.depth === null || engineSettings.depth === undefined) &&
        (engineSettings.movetime === null || engineSettings.movetime === undefined)) {
        console.warn('Engine settings not configured');
        setThinking(false);
        botMoveInProgress.current = false;
        return;
      }

      try {
        // Use the UCI moves ref directly (more reliable than parsing moveHistory)
        const uciMoves = [...uciMovesRef.current];
        const files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];

        console.log('Sending UCI moves to Stockfish:', uciMoves.join(' '));

        // Use movetime if set, otherwise use depth
        const mode = engineSettings.movetime && engineSettings.movetime > 0 ? 'movetime' : 'depth';
        const depthValue = engineSettings.depth || 15;
        const movetimeValue = engineSettings.movetime || 1000;

        // Appeler Stockfish via l'API backend
        const bestMove = await getStockfishBestMove(
          uciMoves.join(' '),
          mode,
          'black',
          depthValue,
          movetimeValue
        );

        // Parser le coup retourné par Stockfish (format: "e7e5" ou "e7e8q" pour promotion)
        if (bestMove && bestMove.length >= 4) {
          const fromCol = bestMove.charCodeAt(0) - 'a'.charCodeAt(0);
          const fromRow = 8 - parseInt(bestMove[1]);
          const toCol = bestMove.charCodeAt(2) - 'a'.charCodeAt(0);
          const toRow = 8 - parseInt(bestMove[3]);

          // Check for promotion piece (5th character: q, r, b, n)
          let promotionPiece: string | null = null;
          if (bestMove.length === 5) {
            const promoPiece = bestMove[4].toLowerCase();
            // Map UCI promotion chars to chess pieces (black pieces for bot)
            const promoMap: { [key: string]: string } = {
              'q': '♛', // Queen
              'r': '♜', // Rook
              'b': '♝', // Bishop
              'n': '♞'  // Knight
            };
            promotionPiece = promoMap[promoPiece] || '♛'; // Default to queen
          }

          // Use the current board state directly - this should match what was sent to Stockfish
          const currentBoard = [...board.map((r: string[]) => [...r])];

          // Enhanced debugging - log current board state and UCI moves
          const fromSquare = bestMove.substring(0, 2);
          const toSquare = bestMove.substring(2, 4);
          const piece = currentBoard[fromRow]?.[fromCol];
          const targetPiece = currentBoard[toRow]?.[toCol];

          console.log('=== BOT MOVE ANALYSIS ===');
          console.log('UCI moves sent to Stockfish:', uciMovesRef.current.join(' '));
          console.log('Stockfish returned move:', bestMove);
          console.log(`Parsed move: ${fromSquare} → ${toSquare} (fromRow: ${fromRow}, fromCol: ${fromCol}, toRow: ${toRow}, toCol: ${toCol})`);
          console.log('Piece at source position:', piece);
          console.log('Piece at target position:', targetPiece);
          console.log('Current board state:');
          console.log(currentBoard.map((row: string[]) =>
            row.map((cell: string) => cell || '.').join(' ')
          ).join('\n'));

          // Check if the coordinates are valid first
          if (fromRow < 0 || fromRow >= 8 || fromCol < 0 || fromCol >= 8 ||
            toRow < 0 || toRow >= 8 || toCol < 0 || toCol >= 8) {
            console.error('❌ Invalid coordinates:', { fromRow, fromCol, toRow, toCol });
            setThinking(false);
            botMoveInProgress.current = false;
            return;
          }

          // Check if there's actually a piece at the source
          if (!piece || piece === '') {
            console.error('❌ No piece at source position');
            setThinking(false);
            botMoveInProgress.current = false;
            return;
          }

          // Check if it's a black piece (bot should only move black pieces)
          if (isWhitePiece(piece)) {
            console.error('❌ Bot trying to move white piece');
            setThinking(false);
            botMoveInProgress.current = false;
            return;
          }

          // Valider le coup
          if (!isValidMove(currentBoard, fromRow, fromCol, toRow, toCol)) {
            console.error('❌ Invalid move returned by Stockfish:', bestMove);
            console.error('Move validation failed for:', { fromRow, fromCol, toRow, toCol, piece, targetPiece });
            console.error('Expected move format: UCI notation (e.g., e7e5)');
            setThinking(false);
            botMoveInProgress.current = false;
            return;
          }

          console.log('✅ Move validation passed');

          const newBoard = currentBoard;
          const movingPiece = newBoard[fromRow][fromCol];
          const capturedPiece = newBoard[toRow][toCol];

          // Appliquer le coup
          newBoard[toRow][toCol] = movingPiece;
          newBoard[fromRow][fromCol] = '';

          // Gérer la capture en passant
          if ((movingPiece === '♙' || movingPiece === '♟') && !capturedPiece && fromCol !== toCol) {
            // C'est une capture en diagonale sur case vide - enlever le pion capturé
            const capturedPawnRow = fromRow; // Le pion capturé est sur la même ligne que la source
            newBoard[capturedPawnRow][toCol] = '';
          }

          // Gérer le castling (déplacer aussi la tour)
          if ((movingPiece === '♔' || movingPiece === '♚') && Math.abs(toCol - fromCol) === 2) {
            const kingRow = fromRow;
            if (toCol > fromCol) {
              // Petit roque (roi se déplace vers la droite)
              const rookCol = 7;
              const rook = newBoard[kingRow][rookCol];
              newBoard[kingRow][5] = rook;
              newBoard[kingRow][rookCol] = '';
            } else {
              // Grand roque (roi se déplace vers la gauche)
              const rookCol = 0;
              const rook = newBoard[kingRow][rookCol];
              newBoard[kingRow][3] = rook;
              newBoard[kingRow][rookCol] = '';
            }
          }

          // Handle black pawn promotion
          if (movingPiece === '♟' && toRow === 7) {
            // Use the promotion piece from Stockfish move, or default to queen
            newBoard[toRow][toCol] = promotionPiece || '♛';
          }
          // Handle white pawn promotion (for completeness, though bot plays black)
          if (movingPiece === '♙' && toRow === 0) {
            // Map to white pieces for white promotion
            const whitePromoMap: { [key: string]: string } = {
              '♛': '♕', '♜': '♖', '♝': '♗', '♞': '♘'
            };
            newBoard[toRow][toCol] = promotionPiece ? (whitePromoMap[promotionPiece] || '♕') : '♕';
          }

          // Construire la notation descriptive
          let moveStr = `${movingPiece} ${files[fromCol]}${8 - fromRow} → ${files[toCol]}${8 - toRow}`;
          if (capturedPiece) {
            moveStr += ` (capture ${capturedPiece})`;
          }
          moveStr += ' (Stockfish)';

          // Construire la notation PGN
          const pieceNotation = pieceToNotation(movingPiece);
          const toSquareNotation = `${files[toCol]}${8 - toRow}`;

          let pgnMove = '';
          if (capturedPiece) {
            if (pieceNotation === '') {
              pgnMove = `${files[fromCol]}x${toSquareNotation}`;
            } else {
              pgnMove = `${pieceNotation}x${toSquareNotation}`;
            }
          } else {
            pgnMove = `${pieceNotation}${toSquareNotation}`;
          }

          // Vérifier si le roi blanc est en échec
          const whiteInCheck = isKingInCheck(newBoard, true);
          const gameOver = checkGameOver(newBoard, true);  // Check if white (next player) has legal moves

          if (gameOver) {
            pgnMove += '#';
            moveStr += ' (échec et mat)';
          } else if (whiteInCheck) {
            pgnMove += '+';
            moveStr += ' (échec)';
            setGameStatus('Échec !');
          }

          // Track UCI move for Stockfish (use the original bestMove which may include promotion)
          uciMovesRef.current = [...uciMovesRef.current, bestMove];
          console.log('=== BOT MOVE TRACKING ===');
          console.log('Bot UCI move added:', bestMove);
          console.log('Complete UCI game:', uciMovesRef.current.join(' '));
          console.log('==========================');

          setBoard(newBoard);
          setMoveHistory((prev: string[]) => [...prev, moveStr]);
          setPgnMoves((prev: string[]) => [...prev, pgnMove]);

          // Track pieces that have moved (for castling validation)
          const newMovedPieces = new Set(movedPieces);
          if (movingPiece === '♔' || movingPiece === '♚') {
            newMovedPieces.add(movingPiece === '♔' ? 'K' : 'k');
          } else if (movingPiece === '♖' || movingPiece === '♜') {
            newMovedPieces.add(movingPiece === '♖' ? `R${fromCol}` : `r${fromCol}`);
          }
          setMovedPieces(newMovedPieces);

          // Update en passant file if pawn double move
          const isPawnDoubleMoveBlack = movingPiece === '♟' && fromRow === 1 && toRow === 3;
          if (isPawnDoubleMoveBlack) {
            setLastEnPassantFile(toCol);
          } else {
            setLastEnPassantFile(null);
          }

          // Ajouter l'incrément de temps
          setBlackTime((prev: number) => prev + (timeControl?.increment || 0));

          if (!gameOver) {
            setCurrentTurn('white');
          } else {
            // Game is over - disable timer and interaction
            setIsTimerActive(false);
            setSelectedSquare(null);
          }
        }
      } catch (error) {
        console.error('❌ Bot move failed:', error);

        const errorMessage = error instanceof Error ? error.message : String(error);

        if (errorMessage.includes('Backend server not responding')) {
          setGameStatus('❌ Erreur: Le serveur backend n\'est pas disponible. Veuillez le redémarrer.');
        } else if (errorMessage.includes('Failed to fetch')) {
          setGameStatus('❌ Erreur: Connexion au serveur interrompue.');
        } else {
          setGameStatus(`❌ Erreur Stockfish: ${errorMessage}`);
        }

        // Don't show alert for network errors, just update game status
        if (!errorMessage.includes('Backend server not responding') && !errorMessage.includes('Failed to fetch')) {
          alert('Erreur Stockfish: ' + errorMessage);
        }
      } finally {
        setThinking(false);
        botMoveInProgress.current = false;
      }
    };

    // Execute bot move with a small delay to allow state to settle
    const timer = setTimeout(() => {
      makeBotMove();
    }, 100);

    return () => clearTimeout(timer);
  }, [currentTurn, mode, winner]); // Removed thinking and engineSettings to prevent loops

  const resetGame = () => {
    setBoard(INITIAL_BOARD);
    setMoveHistory([]);
    setPgnMoves([]);
    uciMovesRef.current = []; // Reset UCI moves for Stockfish
    setSelectedSquare(null);
    setCurrentTurn('white');
    setGameStatus('En cours');
    setWinner(null);
    setWhiteTime((timeControl?.minutes || 5) * 60);
    setBlackTime((timeControl?.minutes || 5) * 60);
    setIsTimerActive(true);
    setPromotionState(null);
    setMovedPieces(new Set());
    setLastEnPassantFile(null);
  };

  const exportPGN = () => {
    // Formater les coups par paires (blancs, noirs)
    let formattedMoves = '';
    for (let i = 0; i < pgnMoves.length; i += 2) {
      const moveNumber = Math.floor(i / 2) + 1;
      const whiteMove = pgnMoves[i];
      const blackMove = pgnMoves[i + 1];

      if (blackMove) {
        formattedMoves += `${moveNumber}. ${whiteMove} ${blackMove}\n`;
      } else {
        formattedMoves += `${moveNumber}. ${whiteMove}\n`;
      }
    }

    // Ajouter le résultat
    let result = '1/2-1/2'; // Match nul par défaut
    if (winner === 'Blancs') result = '1-0';
    if (winner === 'Noirs') result = '0-1';

    const pgn = `[Event "Chess Game"]
[Site "Chess Analytics Platform"]
[Date "${new Date().toISOString().split('T')[0]}"]
[Round "1"]
[White "Joueur"]
[Black "${mode === 'bot' ? 'Stockfish Bot' : 'Joueur 2'}"]
[Result "${result}"]

${formattedMoves}${result}`;

    const blob = new Blob([pgn], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `chess-game-${Date.now()}.pgn`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const analyzeWithStockfish = async () => {
    try {
      // Obtenir l'historique des coups en notation UCI
      // const files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
      const uciMoves: string[] = [];

      moveHistory.forEach((move: string) => {
        const match = move.match(/([a-h])(\d) → ([a-h])(\d)/);
        if (match) {
          const fromSquare = match[1] + match[2];
          const toSquare = match[3] + match[4];
          uciMoves.push(fromSquare + toSquare);
        }
      });

      // Appeler l'analyse Stockfish
      const result = await analyzePosition(uciMoves.join(' '), 20);

      alert(
        `Analyse Stockfish complétée!\n\n` +
        `Meilleur coup: ${result.bestmove}\n` +
        `Position: ${moveHistory.length} coups joués`
      );
    } catch (error) {
      console.error('Erreur analyse Stockfish:', error);
      alert('Erreur lors de l\'analyse: ' + (error instanceof Error ? error.message : String(error)));
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-gray-900 mb-2">
            {mode === 'bot' ? 'Jouer contre Stockfish' : 'Jouer aux échecs'}
          </h2>
          <p className="text-gray-600">
            {mode === 'bot'
              ? 'Affrontez le moteur d\'échecs Stockfish'
              : 'Jouez une partie libre'}
          </p>
        </div>
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Échiquier */}
        <div className="lg:col-span-2">
          <div className="bg-gradient-to-br from-slate-50 to-slate-100 rounded-2xl shadow-xl p-8 border border-slate-200">
            {/* Indicateurs de joueurs */}
            <div className="flex justify-center items-center mb-6">
              <div className={`flex items-center gap-3 px-5 py-3 rounded-xl transition-all duration-300 ${currentTurn === 'white' && !winner
                ? 'bg-white shadow-lg ring-2 ring-blue-500 scale-105'
                : 'bg-white/50 shadow'
                }`}>
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-slate-100 to-slate-300 border-2 border-slate-400 flex items-center justify-center">
                  <Circle className="w-4 h-4 text-slate-700 fill-white" />
                </div>
                <div>
                  <div className="text-sm text-gray-600">Blancs</div>
                  <div className={`text-lg font-mono ${whiteTime < 30 ? 'text-red-600 animate-pulse' : 'text-gray-900'
                    }`}>
                    {formatTime(whiteTime)}
                  </div>
                </div>
                {currentTurn === 'white' && !winner && (
                  <div className="ml-2">
                    <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse" />
                  </div>
                )}
              </div>

              <div className={`flex items-center gap-3 px-5 py-3 rounded-xl transition-all duration-300 ${currentTurn === 'black' && !winner
                ? 'bg-slate-800 shadow-lg ring-2 ring-purple-500 scale-105'
                : 'bg-slate-800/50 shadow'
                }`}>
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-slate-700 to-slate-900 border-2 border-slate-600 flex items-center justify-center">
                  <Circle className="w-4 h-4 text-slate-300 fill-slate-800" />
                </div>
                <div>
                  <div className="text-sm text-slate-200">Noirs</div>
                  <div className={`text-lg font-mono ${blackTime < 30 ? 'text-red-400 animate-pulse' : 'text-slate-100'
                    }`}>
                    {formatTime(blackTime)}
                  </div>
                </div>
                {currentTurn === 'black' && !winner && (
                  <div className="ml-2">
                    <div className="w-2 h-2 bg-purple-500 rounded-full animate-pulse" />
                  </div>
                )}
              </div>
            </div>

            <div className="max-w-2xl mx-auto">
              {/* Échiquier avec coordonnées */}
              <div className="inline-block">
                {/* Échiquier */}
                <div className="flex">
                  {/* Colonne de chiffres à gauche */}
                  <div className="flex flex-col justify-around pr-2">
                    {[8, 7, 6, 5, 4, 3, 2, 1].map((num) => (
                      <div key={num} className="h-16 sm:h-20 flex items-center justify-center text-slate-600 font-mono text-sm">
                        {num}
                      </div>
                    ))}
                  </div>

                  {/* Échiquier principal */}
                  <div className="border-8 border-slate-800 rounded-2xl overflow-hidden shadow-2xl">
                    {board.map((row, rowIndex) => (
                      <div key={rowIndex} className="flex">
                        {row.map((piece, colIndex) => {
                          const isLight = (rowIndex + colIndex) % 2 === 0;
                          const isSelected = selectedSquare?.row === rowIndex && selectedSquare?.col === colIndex;
                          const isLegalMove = legalMoves.some(move => move.row === rowIndex && move.col === colIndex);

                          return (
                            <button
                              key={`${rowIndex}-${colIndex}`}
                              onClick={() => handleSquareClick(rowIndex, colIndex)}
                              disabled={thinking}
                              className={`w-16 h-16 sm:w-20 sm:h-20 flex items-center justify-center text-5xl sm:text-6xl transition-all duration-200 relative ${isLight
                                ? 'bg-gradient-to-br from-amber-50 to-amber-100'
                                : 'bg-gradient-to-br from-amber-600 to-amber-800'
                                } ${isSelected
                                  ? 'ring-4 ring-blue-500 ring-inset scale-95 shadow-inner'
                                  : ''
                                } ${!winner && !thinking ? 'hover:brightness-110 active:scale-95' : 'cursor-default'
                                } ${thinking ? 'opacity-60' : ''
                                }`}
                            >
                              {piece && (
                                <span className={`drop-shadow-2xl transition-transform filter ${isSelected ? 'scale-110' : ''
                                  }`} style={{
                                    filter: 'drop-shadow(0 4px 6px rgba(0, 0, 0, 0.3))',
                                    textShadow: '0 2px 4px rgba(0, 0, 0, 0.2)',
                                    color: isBlackPiece(piece) ? '#1f2937' : '#f9fafb'
                                  }}>
                                  {piece}
                                </span>
                              )}
                              {isLegalMove && (
                                <div className={`absolute inset-0 flex items-center justify-center pointer-events-none`}>
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

                {/* Ligne de lettres en bas */}
                <div className="flex pl-8">
                  {['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'].map((letter) => (
                    <div key={letter} className="w-16 sm:w-20 flex items-center justify-center pt-2 text-slate-600 font-mono text-sm">
                      {letter}
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Promotion Dialog */}
            {promotionState && (
              <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                <div className="bg-white rounded-2xl shadow-2xl p-8 border border-slate-200">
                  <h3 className="text-gray-900 mb-6 text-center text-lg font-semibold">
                    Choisissez la promotion
                  </h3>
                  <div className="flex gap-4 justify-center">
                    {promotionState.isWhite ? (
                      <>
                        <button
                          onClick={() => handlePromotion('♕')}
                          className="w-16 h-16 bg-amber-100 rounded-lg hover:bg-amber-200 transition-all text-4xl shadow-md"
                        >
                          ♕
                        </button>
                        <button
                          onClick={() => handlePromotion('♖')}
                          className="w-16 h-16 bg-amber-100 rounded-lg hover:bg-amber-200 transition-all text-4xl shadow-md"
                        >
                          ♖
                        </button>
                        <button
                          onClick={() => handlePromotion('♗')}
                          className="w-16 h-16 bg-amber-100 rounded-lg hover:bg-amber-200 transition-all text-4xl shadow-md"
                        >
                          ♗
                        </button>
                        <button
                          onClick={() => handlePromotion('♘')}
                          className="w-16 h-16 bg-amber-100 rounded-lg hover:bg-amber-200 transition-all text-4xl shadow-md"
                        >
                          ♘
                        </button>
                      </>
                    ) : (
                      <>
                        <button
                          onClick={() => handlePromotion('♛')}
                          className="w-16 h-16 bg-slate-200 rounded-lg hover:bg-slate-300 transition-all text-4xl shadow-md"
                        >
                          ♛
                        </button>
                        <button
                          onClick={() => handlePromotion('♜')}
                          className="w-16 h-16 bg-slate-200 rounded-lg hover:bg-slate-300 transition-all text-4xl shadow-md"
                        >
                          ♜
                        </button>
                        <button
                          onClick={() => handlePromotion('♝')}
                          className="w-16 h-16 bg-slate-200 rounded-lg hover:bg-slate-300 transition-all text-4xl shadow-md"
                        >
                          ♝
                        </button>
                        <button
                          onClick={() => handlePromotion('♞')}
                          className="w-16 h-16 bg-slate-200 rounded-lg hover:bg-slate-300 transition-all text-4xl shadow-md"
                        >
                          ♞
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* Statut de la partie */}
            <div className="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className={`px-6 py-3 rounded-xl transition-all duration-300 ${winner
                ? 'bg-gradient-to-r from-yellow-400 to-yellow-500 text-yellow-900 shadow-lg ring-2 ring-yellow-600 scale-105'
                : gameStatus === 'Échec !'
                  ? 'bg-gradient-to-r from-red-500 to-red-600 text-white shadow-lg animate-pulse'
                  : 'bg-gradient-to-r from-green-500 to-green-600 text-white shadow-md'
                }`}>
                <div className="flex items-center gap-2">
                  <div className={`w-2 h-2 rounded-full ${winner ? 'bg-yellow-900' : 'bg-white'}`} />
                  <span>{gameStatus}</span>
                </div>
              </div>

              {mode === 'bot' && (engineSettings.depth === null && engineSettings.movetime === null) && !thinking && (
                <div className="flex items-center gap-3 px-5 py-3 bg-amber-100 rounded-xl shadow-md border border-amber-300">
                  <span className="text-amber-800">⚠️ Configurez la profondeur ou le temps</span>
                </div>
              )}

              {thinking && (
                <div className="flex items-center gap-3 px-5 py-3 bg-purple-100 rounded-xl shadow-md border border-purple-200">
                  <Brain className="w-5 h-5 text-purple-600 animate-pulse" />
                  <span className="text-purple-800">Stockfish réfléchit...</span>
                </div>
              )}
            </div>

            {/* Boutons d'action */}
            <div className="mt-6 flex flex-wrap gap-3">
              <button
                onClick={resetGame}
                className="flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-blue-600 to-blue-700 text-white rounded-xl hover:from-blue-700 hover:to-blue-800 transition-all duration-200 shadow-md hover:shadow-lg active:scale-95"
              >
                <RotateCcw className="w-5 h-5" />
                Nouvelle partie
              </button>
              <button
                onClick={exportPGN}
                disabled={moveHistory.length === 0}
                className="flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-slate-600 to-slate-700 text-white rounded-xl hover:from-slate-700 hover:to-slate-800 transition-all duration-200 shadow-md hover:shadow-lg active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Download className="w-5 h-5" />
                Exporter PGN
              </button>
              <button
                onClick={analyzeWithStockfish}
                className="flex items-center gap-2 px-5 py-3 bg-gradient-to-r from-purple-600 to-purple-700 text-white rounded-xl hover:from-purple-700 hover:to-purple-800 transition-all duration-200 shadow-md hover:shadow-lg active:scale-95"
              >
                <Brain className="w-5 h-5" />
                Analyser
              </button>
            </div>
          </div>
        </div>

        {/* Historique des coups */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-2xl shadow-xl p-6 border border-slate-200">
            <h3 className="text-gray-900 mb-4 flex items-center gap-2">
              <div className="w-1 h-6 bg-gradient-to-b from-blue-500 to-purple-500 rounded-full" />
              Historique des coups
            </h3>
            <div className="space-y-2 max-h-[600px] overflow-y-auto pr-2 custom-scrollbar">
              {moveHistory.length === 0 ? (
                <div className="text-center py-12">
                  <Circle className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                  <p className="text-gray-500">
                    Aucun coup joué
                  </p>
                </div>
              ) : (
                <div className="space-y-2">
                  {moveHistory.map((move, index) => (
                    <div
                      key={index}
                      className={`px-4 py-3 rounded-xl text-sm transition-all duration-200 ${index === moveHistory.length - 1
                        ? 'bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 shadow-sm'
                        : 'bg-slate-50 hover:bg-slate-100'
                        }`}
                    >
                      <div className="flex items-start gap-2">
                        <span className={`mt-0.5 px-2 py-0.5 rounded text-xs ${index % 2 === 0
                          ? 'bg-blue-100 text-blue-700'
                          : 'bg-purple-100 text-purple-700'
                          }`}>
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