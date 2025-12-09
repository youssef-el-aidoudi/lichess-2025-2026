import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell } from 'recharts';
import { Download, TrendingUp, Activity } from 'lucide-react';

export function Stats() {
  // Données temporaire fake
  const performanceData = [
    { month: 'Jan', victoires: 12, defaites: 8, nuls: 3 },
    { month: 'Fév', victoires: 15, defaites: 6, nuls: 4 },
    { month: 'Mar', victoires: 18, defaites: 7, nuls: 2 },
    { month: 'Avr', victoires: 14, defaites: 9, nuls: 5 },
    { month: 'Mai', victoires: 20, defaites: 5, nuls: 3 },
    { month: 'Jun', victoires: 22, defaites: 4, nuls: 4 },
  ];

  const accuracyOverTime = [
    { game: '1-10', accuracy: 82 },
    { game: '11-20', accuracy: 84 },
    { game: '21-30', accuracy: 86 },
    { game: '31-40', accuracy: 85 },
    { game: '41-50', accuracy: 88 },
    { game: '51-60', accuracy: 90 },
  ];

  const openingStats = [
    { name: 'Sicilienne', value: 45, winRate: 62 },
    { name: 'Espagnole', value: 30, winRate: 58 },
    { name: 'Française', value: 15, winRate: 54 },
    { name: 'Italienne', value: 10, winRate: 70 },
  ];

  const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'];

  const bestMovesProbability = [
    { move: 'e4', count: 45, avgAccuracy: 92, avgTime: '0.5s', probability: 94 },
    { move: 'd4', count: 38, avgAccuracy: 89, avgTime: '0.6s', probability: 89 },
    { move: 'Nf3', count: 42, avgAccuracy: 91, avgTime: '0.4s', probability: 91 },
    { move: 'c4', count: 28, avgAccuracy: 87, avgTime: '0.7s', probability: 86 },
    { move: 'g3', count: 15, avgAccuracy: 85, avgTime: '0.8s', probability: 82 },
  ];

  const exportStats = () => {
    // Exporter les statistiques en CSV ou JSON
    const data = {
      performance: performanceData,
      accuracy: accuracyOverTime,
      openings: openingStats,
      bestMoves: bestMovesProbability,
    };
    
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `chess-stats-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-gray-900 mb-2">Statistiques & Analyses ETL</h2>
          <p className="text-gray-600">
            Visualisez vos performances et les données extraites de Lichess
          </p>
        </div>
        <button
          onClick={exportStats}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
        >
          <Download className="w-4 h-4" />
          Exporter les stats
        </button>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg shadow p-6 text-white">
          <div className="flex items-center gap-3 mb-2">
            <TrendingUp className="w-6 h-6" />
            <div>Taux de victoire</div>
          </div>
          <div className="text-3xl">64.2%</div>
          <div className="text-sm mt-1 opacity-90">+5.3% ce mois</div>
        </div>

        <div className="bg-gradient-to-br from-green-500 to-green-600 rounded-lg shadow p-6 text-white">
          <div className="flex items-center gap-3 mb-2">
            <Activity className="w-6 h-6" />
            <div>Précision moyenne</div>
          </div>
          <div className="text-3xl">87.5%</div>
          <div className="text-sm mt-1 opacity-90">+2.1% ce mois</div>
        </div>

        <div className="bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg shadow p-6 text-white">
          <div className="flex items-center gap-3 mb-2">
            <Activity className="w-6 h-6" />
            <div>Parties analysées</div>
          </div>
          <div className="text-3xl">1,245</div>
          <div className="text-sm mt-1 opacity-90">Lichess ETL</div>
        </div>
      </div>

      {/* Graphiques */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Performance par mois */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-gray-900 mb-4">Performance mensuelle</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={performanceData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="victoires" fill="#10b981" />
              <Bar dataKey="defaites" fill="#ef4444" />
              <Bar dataKey="nuls" fill="#94a3b8" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Précision au fil du temps */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-gray-900 mb-4">Évolution de la précision</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={accuracyOverTime}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="game" />
              <YAxis domain={[75, 95]} />
              <Tooltip />
              <Legend />
              <Line 
                type="monotone" 
                dataKey="accuracy" 
                stroke="#3b82f6" 
                strokeWidth={3}
                name="Précision (%)"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Ouvertures et probabilités */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Répartition des ouvertures */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-gray-900 mb-4">Répartition des ouvertures</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={openingStats}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, value }) => `${name}: ${value}%`}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
              >
                {openingStats.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
          <div className="mt-4 space-y-2">
            {openingStats.map((opening, index) => (
              <div key={index} className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-2">
                  <div 
                    className="w-3 h-3 rounded-full" 
                    style={{ backgroundColor: COLORS[index % COLORS.length] }}
                  />
                  <span className="text-gray-700">{opening.name}</span>
                </div>
                <span className="text-gray-600">{opening.winRate}% victoires</span>
              </div>
            ))}
          </div>
        </div>

        {/* Meilleurs coups avec probabilités */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-gray-900 mb-4">Analyse des meilleurs coups (Stockfish)</h3>
          <div className="space-y-4">
            {bestMovesProbability.map((move, index) => (
              <div key={index} className="border-b pb-4 last:border-b-0">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                      <span className="font-mono">{move.move}</span>
                    </div>
                    <div>
                      <div className="text-gray-900">{move.count} parties</div>
                      <div className="text-sm text-gray-600">
                        Précision: {move.avgAccuracy}% • Temps: {move.avgTime}
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-blue-600">{move.probability}%</div>
                    <div className="text-xs text-gray-600">probabilité</div>
                  </div>
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

      {/* Informations ETL */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
        <h3 className="text-gray-900 mb-2">Configuration ETL Lichess</h3>
        <p className="text-gray-600 mb-4">
          Les données sont automatiquement extraites de Lichess, traitées par votre backend Java, 
          et analysées avec Stockfish pour calculer les probabilités des meilleurs coups.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
          <div className="bg-white p-3 rounded-md">
            <div className="text-gray-600">Dernière extraction</div>
            <div className="text-gray-900">Il y a 2 heures</div>
          </div>
          <div className="bg-white p-3 rounded-md">
            <div className="text-gray-600">Parties dans la base</div>
            <div className="text-gray-900">1,245 parties</div>
          </div>
          <div className="bg-white p-3 rounded-md">
            <div className="text-gray-600">Prochain ETL</div>
            <div className="text-gray-900">Dans 4 heures</div>
          </div>
        </div>
      </div>
    </div>
  );
}
