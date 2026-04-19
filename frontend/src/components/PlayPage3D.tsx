import { useState } from 'react';
import { GameSetup } from './GameSetup';
import { ChessGame3D } from './ChessGame3D';

interface GameSettings {
  opponent: 'human' | 'bot';
  timeMinutes: number;
  increment: number;
  engineSettings?: {
    depth: number | null;
    movetime: number | null;
    useDepth: boolean;
  };
}

export function PlayPage3D() {
  const [gameStarted, setGameStarted] = useState(false);
  const [gameSettings, setGameSettings] = useState<GameSettings | null>(null);

  const handleStartGame = (settings: GameSettings) => {
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
      <ChessGame3D
        mode={gameSettings?.opponent || 'bot'}
        timeControl={{
          minutes: gameSettings?.timeMinutes || 5,
          increment: gameSettings?.increment || 0,
        }}
        engineSettings={gameSettings?.engineSettings}
        onExit={handleBackToSetup}
      />
    </div>
  );
}
