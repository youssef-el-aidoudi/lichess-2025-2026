import { useState } from 'react';
import { Clock, User, Bot, Zap, Timer, Crown, Play, Settings } from 'lucide-react';

interface TimePreset {
  label: string;
  minutes: number;
  increment: number;
}

interface GameSetupProps {
  onStartGame: (settings: any) => void;
}

const TIME_PRESETS: { [key: string]: TimePreset[] } = {
  bullet: [
    { label: '1+0', minutes: 1, increment: 0 },
    { label: '1+1', minutes: 1, increment: 1 },
    { label: '2+1', minutes: 2, increment: 1 },
  ],
  blitz: [
    { label: '3+0', minutes: 3, increment: 0 },
    { label: '3+2', minutes: 3, increment: 2 },
    { label: '5+0', minutes: 5, increment: 0 },
    { label: '5+3', minutes: 5, increment: 3 },
  ],
  rapid: [
    { label: '10+0', minutes: 10, increment: 0 },
    { label: '10+5', minutes: 10, increment: 5 },
    { label: '15+10', minutes: 15, increment: 10 },
  ],
  classical: [
    { label: '30+0', minutes: 30, increment: 0 },
    { label: '30+20', minutes: 30, increment: 20 },
    { label: '60+0', minutes: 60, increment: 0 },
  ],
};

export function GameSetup({ onStartGame }: GameSetupProps) {
  const [timeControl, setTimeControl] = useState<string>('blitz');
  const [selectedPreset, setSelectedPreset] = useState<TimePreset>(TIME_PRESETS.blitz[1]);
  const [opponent, setOpponent] = useState<string>('bot');
  const [depth, setDepth] = useState<number | null>(null);
  const [movetime, setMovetime] = useState<number | null>(null);
  const [useDepth, setUseDepth] = useState<boolean>(true);

  const handleTimeControlChange = (control: string) => {
    setTimeControl(control);
    setSelectedPreset(TIME_PRESETS[control][0]);
  };

  const handleStartGame = () => {
    // Validate engine settings for bot mode
    if (opponent === 'bot') {
      if (useDepth && depth === null) {
        alert('Veuillez sélectionner une profondeur');
        return;
      }
      if (!useDepth && movetime === null) {
        alert('Veuillez sélectionner un temps');
        return;
      }
    }

    onStartGame({
      timeControl,
      timeMinutes: selectedPreset.minutes,
      increment: selectedPreset.increment,
      opponent,
      engineSettings: opponent === 'bot' ? {
        depth: useDepth ? depth : null,
        movetime: !useDepth ? movetime : null,
        useDepth,
      } : null,
    });
  };

  const getTimeControlClass = (control: string): string => {
    const baseClass = 'p-4 rounded-xl border-2 transition-all duration-200';
    if (timeControl === control) {
      return baseClass + ' border-blue-500 bg-blue-50 shadow-lg scale-105';
    }
    return baseClass + ' border-slate-200 bg-white hover:border-blue-300 hover:shadow-md';
  };

  const getPresetClass = (preset: TimePreset): string => {
    const baseClass = 'p-4 rounded-xl border-2 transition-all duration-200';
    if (selectedPreset.label === preset.label) {
      return baseClass + ' border-purple-500 bg-purple-50 shadow-lg scale-105';
    }
    return baseClass + ' border-slate-200 bg-white hover:border-purple-300 hover:shadow-md';
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-gradient-to-br from-white to-slate-50 rounded-2xl shadow-2xl p-8 border border-slate-200">
        <div className="text-center mb-8">
          <h2 className="text-gray-900 mb-2">Nouvelle partie</h2>
          <p className="text-gray-600">Configurez votre partie</p>
        </div>

        <div className="mb-8">
          <label className="flex items-center gap-2 text-gray-900 mb-4">
            <Clock className="w-5 h-5 text-blue-600" />
            Cadence de jeu
          </label>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <button onClick={() => handleTimeControlChange('bullet')} className={getTimeControlClass('bullet')}>
              <Zap className={timeControl === 'bullet' ? 'w-6 h-6 mx-auto mb-2 text-blue-600' : 'w-6 h-6 mx-auto mb-2 text-slate-400'} />
              <div className={timeControl === 'bullet' ? 'text-blue-900' : 'text-gray-700'}>Bullet</div>
              <div className="text-xs text-gray-500 mt-1">moins 3 min</div>
            </button>

            <button onClick={() => handleTimeControlChange('blitz')} className={getTimeControlClass('blitz')}>
              <Timer className={timeControl === 'blitz' ? 'w-6 h-6 mx-auto mb-2 text-blue-600' : 'w-6 h-6 mx-auto mb-2 text-slate-400'} />
              <div className={timeControl === 'blitz' ? 'text-blue-900' : 'text-gray-700'}>Blitz</div>
              <div className="text-xs text-gray-500 mt-1">3-10 min</div>
            </button>

            <button onClick={() => handleTimeControlChange('rapid')} className={getTimeControlClass('rapid')}>
              <Clock className={timeControl === 'rapid' ? 'w-6 h-6 mx-auto mb-2 text-blue-600' : 'w-6 h-6 mx-auto mb-2 text-slate-400'} />
              <div className={timeControl === 'rapid' ? 'text-blue-900' : 'text-gray-700'}>Rapide</div>
              <div className="text-xs text-gray-500 mt-1">10-20 min</div>
            </button>

            <button onClick={() => handleTimeControlChange('classical')} className={getTimeControlClass('classical')}>
              <Crown className={timeControl === 'classical' ? 'w-6 h-6 mx-auto mb-2 text-blue-600' : 'w-6 h-6 mx-auto mb-2 text-slate-400'} />
              <div className={timeControl === 'classical' ? 'text-blue-900' : 'text-gray-700'}>Classique</div>
              <div className="text-xs text-gray-500 mt-1">plus 20 min</div>
            </button>
          </div>
        </div>

        <div className="mb-8">
          <label className="text-gray-900 mb-4 block">Temps de jeu</label>
          <div className="grid grid-cols-3 gap-3">
            {TIME_PRESETS[timeControl].map((preset) => (
              <button
                key={preset.label}
                onClick={() => setSelectedPreset(preset)}
                className={getPresetClass(preset)}
              >
                <div className={selectedPreset.label === preset.label ? 'text-2xl mb-1 text-purple-900' : 'text-2xl mb-1 text-gray-700'}>
                  {preset.label}
                </div>
                <div className="text-xs text-gray-500">
                  {preset.minutes} min {preset.increment > 0 ? '+ ' + preset.increment + 's' : ''}
                </div>
              </button>
            ))}
          </div>
        </div>

        <div className="mb-8">
          <label className="text-gray-900 mb-4 block">Adversaire</label>
          <div className="grid grid-cols-2 gap-4">
            <button onClick={() => setOpponent('bot')} className={opponent === 'bot' ? 'p-6 rounded-xl border-2 transition-all duration-200 border-green-500 bg-green-50 shadow-lg scale-105' : 'p-6 rounded-xl border-2 transition-all duration-200 border-slate-200 bg-white hover:border-green-300 hover:shadow-md'}>
              <Bot className={opponent === 'bot' ? 'w-8 h-8 mx-auto mb-3 text-green-600' : 'w-8 h-8 mx-auto mb-3 text-slate-400'} />
              <div className={opponent === 'bot' ? 'mb-1 text-green-900' : 'mb-1 text-gray-700'}>
                Bot Stockfish
              </div>
              <div className="text-sm text-gray-500">Affrontez le moteur</div>
            </button>

            <button onClick={() => setOpponent('human')} className={opponent === 'human' ? 'p-6 rounded-xl border-2 transition-all duration-200 border-orange-500 bg-orange-50 shadow-lg scale-105' : 'p-6 rounded-xl border-2 transition-all duration-200 border-slate-200 bg-white hover:border-orange-300 hover:shadow-md'}>
              <User className={opponent === 'human' ? 'w-8 h-8 mx-auto mb-3 text-orange-600' : 'w-8 h-8 mx-auto mb-3 text-slate-400'} />
              <div className={opponent === 'human' ? 'mb-1 text-orange-900' : 'mb-1 text-gray-700'}>
                Humain
              </div>
              <div className="text-sm text-gray-500">Mode 2 joueurs</div>
            </button>
          </div>
        </div>

        {opponent === 'bot' && (
          <div className="mb-8 p-6 bg-blue-50 rounded-xl border-2 border-blue-200">
            <label className="flex items-center gap-2 text-gray-900 mb-4">
              <Settings className="w-5 h-5 text-blue-600" />
              Paramètres du moteur Stockfish
            </label>

            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-3">Mode de calcul</label>
              <div className="flex gap-4">
                <button
                  onClick={() => setUseDepth(true)}
                  className={`flex-1 p-3 rounded-lg border-2 transition-all ${
                    useDepth
                      ? 'border-blue-500 bg-blue-100 text-blue-900'
                      : 'border-slate-200 bg-white text-gray-700 hover:border-blue-300'
                  }`}
                >
                  Profondeur
                </button>
                <button
                  onClick={() => setUseDepth(false)}
                  className={`flex-1 p-3 rounded-lg border-2 transition-all ${
                    !useDepth
                      ? 'border-purple-500 bg-purple-100 text-purple-900'
                      : 'border-slate-200 bg-white text-gray-700 hover:border-purple-300'
                  }`}
                >
                  Temps
                </button>
              </div>
            </div>

            {useDepth ? (
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">Profondeur (coups d'avance)</label>
                <div className="grid grid-cols-5 gap-2">
                  {[5, 10, 15, 20, 25].map((d) => (
                    <button
                      key={d}
                      onClick={() => setDepth(d)}
                      className={`p-2 rounded-lg border-2 transition-all text-sm font-medium ${
                        depth === d
                          ? 'border-blue-500 bg-blue-100 text-blue-900'
                          : 'border-slate-200 bg-white text-gray-700 hover:border-blue-300'
                      }`}
                    >
                      {d}
                    </button>
                  ))}
                </div>
                <div className="mt-3">
                  <label className="text-xs text-gray-600">Ou valeur personnalisée (1-30):</label>
                  <input
                    type="number"
                    min="1"
                    max="30"
                    placeholder="Ex: 18"
                    className="mt-1 px-3 py-2 border border-slate-300 rounded-lg w-24 text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    onChange={(e) => {
                      const val = e.target.value === '' ? null : Math.max(1, Math.min(30, parseInt(e.target.value) || depth || 15));
                      setDepth(val);
                    }}
                  />
                </div>
                {depth && <p className="mt-2 text-sm text-gray-600">Profondeur sélectionnée: <span className="font-bold">{depth}</span></p>}
              </div>
            ) : (
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">Temps de réflexion (millisecondes)</label>
                <div className="grid grid-cols-4 gap-2">
                  {[500, 1000, 2000, 3000, 5000].map((t) => (
                    <button
                      key={t}
                      onClick={() => setMovetime(t)}
                      className={`p-2 rounded-lg border-2 transition-all text-sm font-medium ${
                        movetime === t
                          ? 'border-purple-500 bg-purple-100 text-purple-900'
                          : 'border-slate-200 bg-white text-gray-700 hover:border-purple-300'
                      }`}
                    >
                      {t}ms
                    </button>
                  ))}
                </div>
                {movetime && <p className="mt-2 text-sm text-gray-600">Temps sélectionné: <span className="font-bold">{movetime}ms</span></p>}
              </div>
            )}

            {(useDepth && !depth) && (
              <p className="text-red-600 text-sm mt-3">⚠️ Veuillez sélectionner une profondeur</p>
            )}
            {(!useDepth && !movetime) && (
              <p className="text-red-600 text-sm mt-3">⚠️ Veuillez sélectionner un temps</p>
            )}
          </div>
        )}

        <div className="border-t border-slate-200 pt-6">
          <div className="bg-slate-50 rounded-xl p-4 mb-6">
            <div className="text-sm text-gray-600 mb-2">Resume de la partie</div>
            <div className="flex flex-wrap gap-3">
              <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded-lg text-sm">
                {timeControl.charAt(0).toUpperCase() + timeControl.slice(1)}
              </span>
              <span className="px-3 py-1 bg-purple-100 text-purple-800 rounded-lg text-sm">
                {selectedPreset.label}
              </span>
              <span className={opponent === 'bot' ? 'px-3 py-1 rounded-lg text-sm bg-green-100 text-green-800' : 'px-3 py-1 rounded-lg text-sm bg-orange-100 text-orange-800'}>
                vs {opponent === 'bot' ? 'Bot' : 'Humain'}
              </span>
            </div>
          </div>

          <button
            onClick={handleStartGame}
            disabled={opponent === 'bot' && ((useDepth && !depth) || (!useDepth && !movetime))}
            className={`w-full py-4 rounded-xl transition-all duration-200 shadow-lg flex items-center justify-center gap-3 text-white font-semibold ${
              opponent === 'bot' && ((useDepth && !depth) || (!useDepth && !movetime))
                ? 'bg-gray-400 cursor-not-allowed opacity-50'
                : 'bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 hover:shadow-xl'
            }`}
          >
            <Play className="w-6 h-6" />
            <span className="text-lg">Commencer</span>
          </button>
        </div>
      </div>
    </div>
  );
}
