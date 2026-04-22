import { TrendingUp, Target, Brain, Clock } from "lucide-react";
interface DashboardProps {
  onNavigate?: (page: string) => void;
}

export function Dashboard({ onNavigate }: DashboardProps) {

  const stats = {
    totalGames: 156,
    winRate: 58.3,
    averageAccuracy: 87.5,
    lastAnalyzed: "2 heures",
  };

  const recentGames = [
    { id: 1, opponent: "Magnus2024", result: "Victoire", accuracy: 92, date: "2024-11-25" },
    { id: 2, opponent: "ChessMaster", result: "Défaite", accuracy: 84, date: "2024-11-24" },
    { id: 3, opponent: "Stockfish_L5", result: "Victoire", accuracy: 89, date: "2024-11-24" },
    { id: 4, opponent: "Rookie123", result: "Victoire", accuracy: 95, date: "2024-11-23" },
  ];

  const bestMoves = [
    { position: "Début de partie", move: "e4", probability: 92, count: 45 },
    { position: "Milieu de partie", move: "Nf3", probability: 87, count: 38 },
    { position: "Finale", move: "Qg4+", probability: 94, count: 28 },
  ];

  return (
    <div className="max-w-6xl mx-auto px-6 py-8 space-y-6 animate-in fade-in duration-700">
      <div className="relative">
        <h2 className="text-4xl font-black text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-indigo-400 mb-2 tracking-tight">Tableau de bord</h2>
        <p className="text-slate-400 font-medium">
          Bienvenue sur votre plateforme d'analyse algorithmique
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-panel rounded-2xl p-6 group hover:translate-y-[-2px] hover:shadow-cyan-500/10 transition-all duration-300">
          <div className="flex items-center justify-between mb-2">
            <div className="text-slate-400 text-sm font-semibold uppercase tracking-wider">Parties totales</div>
            <Target className="w-5 h-5 text-cyan-400 group-hover:scale-110 transition-transform" />
          </div>
          <div className="text-4xl font-black text-slate-100">{stats.totalGames}</div>
        </div>

        <div className="glass-panel rounded-2xl p-6 group hover:translate-y-[-2px] hover:shadow-indigo-500/10 transition-all duration-300">
          <div className="flex items-center justify-between mb-2">
            <div className="text-slate-400 text-sm font-semibold uppercase tracking-wider">Taux de victoire</div>
            <TrendingUp className="w-5 h-5 text-indigo-400 group-hover:scale-110 transition-transform" />
          </div>
          <div className="text-4xl font-black text-slate-100">{stats.winRate}%</div>
        </div>

        <div className="glass-panel rounded-2xl p-6 group hover:translate-y-[-2px] hover:shadow-fuchsia-500/10 transition-all duration-300">
          <div className="flex items-center justify-between mb-2">
            <div className="text-slate-400 text-sm font-semibold uppercase tracking-wider">Précision moyenne</div>
            <Brain className="w-5 h-5 text-fuchsia-400 group-hover:scale-110 transition-transform" />
          </div>
          <div className="text-4xl font-black text-slate-100">{stats.averageAccuracy}%</div>
        </div>

        <div className="glass-panel rounded-2xl p-6 group hover:translate-y-[-2px] hover:shadow-amber-500/10 transition-all duration-300">
          <div className="flex items-center justify-between mb-2">
            <div className="text-slate-400 text-sm font-semibold uppercase tracking-wider">Dernière analyse</div>
            <Clock className="w-5 h-5 text-amber-400 group-hover:scale-110 transition-transform" />
          </div>
          <div className="text-3xl font-black text-slate-100">{stats.lastAnalyzed}</div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="glass-panel rounded-2xl p-6">
          <h3 className="text-white mb-6 text-xl font-bold flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-cyan-400"></span> Parties récentes
          </h3>
          <div className="space-y-3">
            {recentGames.map((game) => (
              <div
                key={game.id}
                className="flex items-center justify-between p-4 bg-slate-800/50 rounded-xl hover:bg-slate-800 border border-transparent hover:border-slate-700 transition-all"
              >
                <div className="flex-1">
                  <div className="text-slate-100 font-semibold">{game.opponent}</div>
                  <div className="text-xs text-slate-500">{game.date}</div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="text-sm font-medium text-slate-300">
                    Précision: <span className="text-cyan-400">{game.accuracy}%</span>
                  </div>
                  <div
                    className={`px-3 py-1.5 rounded-lg text-xs font-bold uppercase tracking-wider ${
                      game.result === "Victoire"
                        ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
                        : "bg-rose-500/10 text-rose-400 border border-rose-500/20"
                    }`}
                  >
                    {game.result}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-panel rounded-2xl p-6">
          <h3 className="text-white mb-6 text-xl font-bold flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-indigo-400"></span> Meilleurs coups par probabilité
          </h3>
          <div className="space-y-6">
            {bestMoves.map((move, index) => (
              <div key={index} className="space-y-2">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-slate-200 font-medium">{move.position}</div>
                    <div className="text-xs text-slate-500 mt-1">
                      Coup: <span className="font-mono bg-slate-800 px-1.5 py-0.5 rounded text-cyan-300 border border-slate-700">{move.move}</span> • {move.count} parties
                    </div>
                  </div>
                  <div className="text-xl font-black text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-indigo-400">{move.probability}%</div>
                </div>
                <div className="w-full bg-slate-800 rounded-full h-2.5 shadow-inner">
                  <div
                    className="h-2.5 rounded-full transition-all duration-1000 bg-gradient-to-r from-cyan-500 to-indigo-500 relative"
                    style={{ width: `${move.probability}%` }}
                  >
                    <div className="absolute top-0 right-0 bottom-0 w-4 bg-white/20 blur-[2px] rounded-full"></div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="relative overflow-hidden rounded-2xl p-8 border border-white/10 bg-slate-900/40 backdrop-blur-md">
        <div className="absolute top-0 right-0 w-64 h-64 bg-cyan-500/10 rounded-full blur-3xl -mr-20 -mt-20"></div>
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl -ml-20 -mb-20"></div>
        
        <div className="relative z-10">
          <h3 className="text-white mb-2 text-2xl font-bold tracking-tight">Scanner d'Analyse</h3>
          <p className="text-slate-400 mb-6 max-w-2xl">
            Branchez votre base de données Lichess pour initier une analyse algorithmique PGN avec détection d'anomalies.
          </p>
          <div className="flex gap-4 flex-wrap">
            <button className="px-6 py-3 bg-white text-slate-900 font-bold rounded-xl hover:bg-cyan-50 transition-colors shadow-[0_0_20px_rgba(255,255,255,0.1)]">
              Importer depuis Lichess
            </button>

            <button
              onClick={() => onNavigate?.("cheat")}
              className="px-6 py-3 bg-transparent text-cyan-400 font-bold border-2 border-cyan-500/30 rounded-xl hover:bg-cyan-500/10 hover:border-cyan-400 transition-all shadow-[0_0_15px_rgba(6,182,212,0.1)]"
            >
              Lancer Détection Triche
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}