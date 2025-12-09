import { useState } from 'react';
import { GameSetup } from './GameSetup';
import { ChessGame } from './ChessGame';
import { ArrowLeft } from 'lucide-react';

export function PlayPage() {
  const [gameStarted, setGameStarted] = useState(false);
  const [gameSettings, setGameSettings] = useState(null);

  const handleStartGame = (settings) => {
    setGameSettings(settings);
    setGameStarted(true);
  };

  const handleBackToSetup = () => {
    setGameStarted(false);
    setGameSettings(null);
  };

  if (!gameStarted) {
    return <GameSetup onStartGame={handleStartGame} />;
  }

  return (
    <div className="space-y-4">
      <button
        onClick={handleBackToSetup}
        className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        Retour
      </button>

      <ChessGame 
        mode={gameSettings?.opponent || 'bot'} 
        timeControl={{
          minutes: gameSettings?.timeMinutes || 5,
          increment: gameSettings?.increment || 0,
        }}
        engineSettings={gameSettings?.engineSettings}
      />
    </div>
  );
}
