import { useState, useEffect } from 'react';
import { RotateCcw, Download, Brain, Circle } from 'lucide-react';

interface ChessGameProps {
  mode: 'human' | 'bot';
  timeControl?: {
    minutes: number;
    increment: number;
  };
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

export function ChessGame({ mode, timeControl }: ChessGameProps) {
  const [board, setBoard] = useState(INITIAL_BOARD);
  const [selectedSquare, setSelectedSquare] = useState<{ row: number; col: number } | null>(null);
  const [moveHistory, setMoveHistory] = useState<string[]>([]);
  const [pgnMoves, setPgnMoves] = useState<string[]>([]);
  const [currentTurn, setCurrentTurn] = useState<'white' | 'black'>('white');
  const [gameStatus, setGameStatus] = useState<string>('En cours');
  const [thinking, setThinking] = useState(false);
  const [winner, setWinner] = useState<string | null>(null);
  
  const [whiteTime, setWhiteTime] = useState((timeControl?.minutes || 5) * 60);
  const [blackTime, setBlackTime] = useState((timeControl?.minutes || 5) * 60);
  const [isTimerActive, setIsTimerActive] = useState(true);

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
      // Capture en diagonale
      if (absColDiff === 1 && rowDiff === -1 && (targetPiece !== '' || isAttackCheck)) {
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
      // Capture en diagonale
      if (absColDiff === 1 && rowDiff === 1 && (targetPiece !== '' || isAttackCheck)) {
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
      if (absRowDiff <= 1 && absColDiff <= 1) {
        return true;
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
    // Si la partie est terminée, ne rien faire
    if (winner) return;

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
      
      // Créer la notation PGN standard
      const pieceNotation = pieceToNotation(movingPiece);
      const fromSquare = `${files[selectedSquare.col]}${8 - selectedSquare.row}`;
      const toSquare = `${files[col]}${8 - row}`;
      
      newBoard[row][col] = movingPiece;
      newBoard[selectedSquare.row][selectedSquare.col] = '';
      
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
      if (capturedPiece) {
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
      
      setBoard(newBoard);
      setMoveHistory([...moveHistory, move]);
      setPgnMoves([...pgnMoves, pgnMove]);
      setSelectedSquare(null);
      
      // Ajouter l'incrément de temps après le coup
      if (currentTurn === 'white') {
        setWhiteTime(prev => prev + (timeControl?.increment || 0));
      } else {
        setBlackTime(prev => prev + (timeControl?.increment || 0));
      }
      
      if (!willBeGameOver) {
        setCurrentTurn(currentTurn === 'white' ? 'black' : 'white');

        // Si mode bot et que c'est au tour des noirs
        if (mode === 'bot' && currentTurn === 'white') {
          setTimeout(() => makeBotMove(newBoard), 1000);
        }
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

  const makeBotMove = async (currentBoard: string[][]) => {
    setThinking(true);
    
    try {
      // Simuler l'appel à l'API Stockfish
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      // Trouver toutes les pièces noires
      const blackPieces: { row: number; col: number; piece: string }[] = [];
      currentBoard.forEach((row, rowIndex) => {
        row.forEach((piece, colIndex) => {
          if (isBlackPiece(piece)) {
            blackPieces.push({ row: rowIndex, col: colIndex, piece });
          }
        });
      });

      if (blackPieces.length > 0) {
        // Chercher tous les coups légaux possibles
        const legalMoves: { fromRow: number; fromCol: number; toRow: number; toCol: number }[] = [];
        
        for (const piece of blackPieces) {
          // Tester toutes les cases de l'échiquier comme destination
          for (let toRow = 0; toRow < 8; toRow++) {
            for (let toCol = 0; toCol < 8; toCol++) {
              // Vérifier si le coup est valide
              if (isValidMove(currentBoard, piece.row, piece.col, toRow, toCol)) {
                // Simuler le coup pour vérifier qu'il ne met pas le roi en échec
                const testBoard = currentBoard.map(r => [...r]);
                testBoard[toRow][toCol] = testBoard[piece.row][piece.col];
                testBoard[piece.row][piece.col] = '';
                
                // Vérifier que le roi noir n'est pas en échec après ce coup
                if (!isKingInCheck(testBoard, false)) {
                  legalMoves.push({
                    fromRow: piece.row,
                    fromCol: piece.col,
                    toRow,
                    toCol
                  });
                }
              }
            }
          }
        }

        if (legalMoves.length > 0) {
          // Choisir un coup légal au hasard
          const move = legalMoves[Math.floor(Math.random() * legalMoves.length)];
          const newBoard = currentBoard.map(r => [...r]);
          const files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
          const movingPiece = newBoard[move.fromRow][move.fromCol];
          const capturedPiece = newBoard[move.toRow][move.toCol];
          
          let moveStr = `${movingPiece} ${files[move.fromCol]}${8 - move.fromRow} → ${files[move.toCol]}${8 - move.toRow}`;
          
          if (capturedPiece) {
            moveStr += ` (capture ${capturedPiece})`;
          }
          moveStr += ' (Bot)';
          
          // Créer la notation PGN pour le coup du bot
          const pieceNotation = pieceToNotation(movingPiece);
          const toSquare = `${files[move.toCol]}${8 - move.toRow}`;
          
          newBoard[move.toRow][move.toCol] = movingPiece;
          newBoard[move.fromRow][move.fromCol] = '';
          
          // Vérifier si on met le roi blanc en échec
          const whiteInCheck = isKingInCheck(newBoard, true);
          
          // Construire le coup PGN du bot
          let pgnMove = '';
          if (capturedPiece) {
            if (pieceNotation === '') {
              pgnMove = `${files[move.fromCol]}x${toSquare}`;
            } else {
              pgnMove = `${pieceNotation}x${toSquare}`;
            }
          } else {
            pgnMove = `${pieceNotation}${toSquare}`;
          }
          
          // Vérifier si la partie est terminée
          const gameOver = checkGameOver(newBoard, false);
          
          if (gameOver) {
            pgnMove += '#';
            moveStr += ' (échec et mat)';
          } else if (whiteInCheck) {
            pgnMove += '+';
            moveStr += ' (échec)';
            setGameStatus('Échec !');
          }
          
          setBoard(newBoard);
          setMoveHistory(prev => [...prev, moveStr]);
          setPgnMoves(prev => [...prev, pgnMove]);
          
          // Ajouter l'incrément de temps pour le bot
          setBlackTime(prev => prev + (timeControl?.increment || 0));
          
          if (!gameOver) {
            setCurrentTurn('white');
          }
        }
      }
    } catch (error) {
      console.error('Erreur lors du calcul du coup du bot:', error);
    } finally {
      setThinking(false);
    }
  };

  const resetGame = () => {
    setBoard(INITIAL_BOARD);
    setMoveHistory([]);
    setPgnMoves([]); // Réinitialiser les coups en notation PGN
    setSelectedSquare(null);
    setCurrentTurn('white');
    setGameStatus('En cours');
    setWinner(null);
    setWhiteTime((timeControl?.minutes || 5) * 60);
    setBlackTime((timeControl?.minutes || 5) * 60);
    setIsTimerActive(true);
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
    const token = localStorage.getItem('jwt_token');
    alert('Analyse de la position en cours...\n(Connecter l"API Stockfish Java)');
  };

  return (
    <div className="space-y-6">
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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Échiquier */}
        <div className="lg:col-span-2">
          <div className="bg-gradient-to-br from-slate-50 to-slate-100 rounded-2xl shadow-xl p-8 border border-slate-200">
            {/* Indicateurs de joueurs */}
            <div className="flex justify-between items-center mb-6">
              <div className={`flex items-center gap-3 px-5 py-3 rounded-xl transition-all duration-300 ${
                currentTurn === 'white' && !winner
                  ? 'bg-white shadow-lg ring-2 ring-blue-500 scale-105'
                  : 'bg-white/50 shadow'
              }`}>
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-slate-100 to-slate-300 border-2 border-slate-400 flex items-center justify-center">
                  <Circle className="w-4 h-4 text-slate-700 fill-white" />
                </div>
                <div>
                  <div className="text-sm text-gray-600">Blancs</div>
                  <div className={`text-lg font-mono ${
                    whiteTime < 30 ? 'text-red-600 animate-pulse' : 'text-gray-900'
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

              <div className={`flex items-center gap-3 px-5 py-3 rounded-xl transition-all duration-300 ${
                currentTurn === 'black' && !winner
                  ? 'bg-slate-800 shadow-lg ring-2 ring-purple-500 scale-105'
                  : 'bg-slate-800/50 shadow'
              }`}>
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-slate-700 to-slate-900 border-2 border-slate-600 flex items-center justify-center">
                  <Circle className="w-4 h-4 text-slate-300 fill-slate-800" />
                </div>
                <div>
                  <div className="text-sm text-slate-200">Noirs</div>
                  <div className={`text-lg font-mono ${
                    blackTime < 30 ? 'text-red-400 animate-pulse' : 'text-slate-100'
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
                              className={`w-16 h-16 sm:w-20 sm:h-20 flex items-center justify-center text-5xl sm:text-6xl transition-all duration-200 relative ${
                                isLight 
                                  ? 'bg-gradient-to-br from-amber-50 to-amber-100' 
                                  : 'bg-gradient-to-br from-amber-600 to-amber-800'
                              } ${
                                isSelected 
                                  ? 'ring-4 ring-blue-500 ring-inset scale-95 shadow-inner' 
                                  : ''
                              } ${
                                !winner && !thinking ? 'hover:brightness-110 active:scale-95' : 'cursor-default'
                              } ${
                                thinking ? 'opacity-60' : ''
                              }`}
                            >
                              {piece && (
                                <span className={`drop-shadow-2xl transition-transform filter ${
                                  isSelected ? 'scale-110' : ''
                                }`} style={{ 
                                  filter: 'drop-shadow(0 4px 6px rgba(0, 0, 0, 0.3))',
                                  textShadow: '0 2px 4px rgba(0, 0, 0, 0.2)'
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

            {/* Statut de la partie */}
            <div className="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className={`px-6 py-3 rounded-xl transition-all duration-300 ${
                winner 
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
                      className={`px-4 py-3 rounded-xl text-sm transition-all duration-200 ${
                        index === moveHistory.length - 1
                          ? 'bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 shadow-sm'
                          : 'bg-slate-50 hover:bg-slate-100'
                      }`}
                    >
                      <div className="flex items-start gap-2">
                        <span className={`mt-0.5 px-2 py-0.5 rounded text-xs ${
                          index % 2 === 0 
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