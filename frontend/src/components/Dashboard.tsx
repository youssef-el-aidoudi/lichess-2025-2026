import { TrendingUp, Target, Brain, Clock } from 'lucide-react';

export function Dashboard() {
  // Données mockées - à remplacer par des appels à votre API Java
  const stats = {
    totalGames: 156,
    winRate: 58.3,
    averageAccuracy: 87.5,
    lastAnalyzed: '2 heures',
  };

  const recentGames = [
    { id: 1, opponent: 'Magnus2024', result: 'Victoire', accuracy: 92, date: '2024-11-25' },
    { id: 2, opponent: 'ChessMaster', result: 'Défaite', accuracy: 84, date: '2024-11-24' },
    { id: 3, opponent: 'Stockfish_L5', result: 'Victoire', accuracy: 89, date: '2024-11-24' },
    { id: 4, opponent: 'Rookie123', result: 'Victoire', accuracy: 95, date: '2024-11-23' },
  ];

  const bestMoves = [
    { position: 'Début de partie', move: 'e4', probability: 92, count: 45 },
    { position: 'Milieu de partie', move: 'Nf3', probability: 87, count: 38 },
    { position: 'Finale', move: 'Qg4+', probability: 94, count: 28 },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-gray-900 mb-4">Tableau de bord</h2>
        <p className="text-gray-600">
          Bienvenue sur votre plateforme d'analyse d'échecs
        </p>
      </div>

      {/* Statistiques principales */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-2">
            <div className="text-gray-600">Parties totales</div>
            <Target className="w-5 h-5 text-blue-600" />
          </div>
          <div className="text-3xl text-gray-900">{stats.totalGames}</div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-2">
            <div className="text-gray-600">Taux de victoire</div>
            <TrendingUp className="w-5 h-5 text-green-600" />
          </div>
          <div className="text-3xl text-gray-900">{stats.winRate}%</div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-2">
            <div className="text-gray-600">Précision moyenne</div>
            <Brain className="w-5 h-5 text-purple-600" />
          </div>
          <div className="text-3xl text-gray-900">{stats.averageAccuracy}%</div>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-2">
            <div className="text-gray-600">Dernière analyse</div>
            <Clock className="w-5 h-5 text-orange-600" />
          </div>
          <div className="text-2xl text-gray-900">{stats.lastAnalyzed}</div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Parties récentes */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-gray-900 mb-4">Parties récentes</h3>
          <div className="space-y-3">
            {recentGames.map((game) => (
              <div
                key={game.id}
                className="flex items-center justify-between p-3 bg-gray-50 rounded-md hover:bg-gray-100 transition-colors"
              >
                <div className="flex-1">
                  <div className="text-gray-900">{game.opponent}</div>
                  <div className="text-sm text-gray-600">{game.date}</div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="text-sm text-gray-600">
                    Précision: {game.accuracy}%
                  </div>
                  <div
                    className={`px-3 py-1 rounded-full text-sm ${
                      game.result === 'Victoire'
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {game.result}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Meilleurs coups */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-gray-900 mb-4">Meilleurs coups par probabilité</h3>
          <div className="space-y-4">
            {bestMoves.map((move, index) => (
              <div key={index} className="space-y-2">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-gray-900">{move.position}</div>
                    <div className="text-sm text-gray-600">
                      Coup: <span className="font-mono">{move.move}</span> • {move.count} parties
                    </div>
                  </div>
                  <div className="text-blue-600">{move.probability}%</div>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-blue-600 h-2 rounded-full transition-all"
                    style={{ width: `${move.probability}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Actions rapides */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h3 className="text-gray-900 mb-2">Actions rapides</h3>
        <p className="text-gray-600 mb-4">
          Importez des parties depuis Lichess ou lancez une nouvelle analyse
        </p>
        <div className="flex gap-3">
          <button className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors">
            Importer depuis Lichess
          </button>
          <button className="px-4 py-2 bg-white text-blue-600 border border-blue-600 rounded-md hover:bg-blue-50 transition-colors">
            Analyser une partie
          </button>
        </div>
      </div>
    </div>
  );
}
