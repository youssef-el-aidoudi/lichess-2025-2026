import { useState, useEffect } from 'react';
import { Login } from './components/Login';
import { Dashboard } from './components/Dashboard';
import { PlayPage } from './components/PlayPage';
import { PlayPage3D } from './components/PlayPage3D';
import { Stats } from './components/Stats';
import { FriendsPage } from './components/FriendsPage';
import { MultiplayerGame } from './components/MultiplayerGame';
import { PlayerSearch } from './components/PlayerSearch';
import CheatAnalysis from './components/CheatAnalysis';
import { CheatHistory } from './components/CheatHistory';
import { Users, ShieldAlert } from 'lucide-react';

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [user, setUser] = useState<{ username: string } | null>(null);
  // For challenge from friends page
  const [challengeGameId, setChallengeGameId] = useState<string | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('jwt_token');
    const username = localStorage.getItem('username');
    if (token && username) {
      setIsAuthenticated(true);
      setUser({ username });
    }

    const handleAuthExpired = () => {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('username');
      setIsAuthenticated(false);
      setUser(null);
      setCurrentPage('dashboard');
    };

    window.addEventListener('auth:expired', handleAuthExpired);
    return () => window.removeEventListener('auth:expired', handleAuthExpired);
  }, []);

  const handleLogin = (token: string, username: string) => {
    localStorage.setItem('jwt_token', token);
    localStorage.setItem('username', username);
    setIsAuthenticated(true);
    setUser({ username });
  };

  const handleLogout = () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('username');
    setIsAuthenticated(false);
    setUser(null);
    setCurrentPage('dashboard');
  };

  const handleChallengeFriend = (gameId: string) => {
    setChallengeGameId(gameId);
    setCurrentPage('challenge');
  };

  if (!isAuthenticated) {
    return <Login onLogin={handleLogin} />;
  }

  const navButtonClass = (page: string) => {
    const baseClass = 'px-3 py-2 rounded-md transition-colors text-sm font-medium flex items-center gap-2';
    if (currentPage === page) {
      return baseClass + ' bg-cyan-500/20 text-cyan-400 shadow-sm shadow-cyan-500/20 border border-cyan-500/30';
    }
    return baseClass + ' text-slate-400 hover:bg-slate-800 hover:text-slate-200';
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 selection:bg-cyan-500/30">
      <nav className="bg-slate-900 shadow-lg border-b border-white/5 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center gap-8">
              <h1 className="text-xl text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-indigo-400 font-bold tracking-tight">♟ Chess Analytics</h1>
              <div className="flex gap-2">
                <button
                  onClick={() => setCurrentPage('dashboard')}
                  className={navButtonClass('dashboard')}
                >
                  Dashboard
                </button>
                <button
                  onClick={() => setCurrentPage('play')}
                  className={navButtonClass('play')}
                >
                  Jouer (2D)
                </button>
                <button
                  onClick={() => setCurrentPage('play3d')}
                  className={navButtonClass('play3d')}
                >
                  Échecs 3D
                </button>
                <button
                  onClick={() => setCurrentPage('friends')}
                  className={navButtonClass('friends')}
                >
                  <span className="flex items-center gap-1">
                    <Users className="w-4 h-4" />
                    Amis
                  </span>
                </button>
                <button
                  onClick={() => setCurrentPage('stats')}
                  className={navButtonClass('stats')}
                >
                  Statistiques
                </button>
                <button
                  onClick={() => setCurrentPage('search')}
                  className={navButtonClass('search')}
                >
                  Recherche
                </button>
                <button
                  onClick={() => setCurrentPage('cheat')}
                  className={navButtonClass('cheat')}
                >
                  <span className="flex items-center gap-1">
                    <ShieldAlert className="w-4 h-4" />
                    Détection triche
                  </span>
                </button>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-slate-300 font-medium text-sm">👤 {user?.username}</span>
              <button
                onClick={handleLogout}
                className="px-4 py-2 bg-red-500 text-white rounded-md hover:bg-red-600 transition-colors text-sm"
              >
                Déconnexion
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {currentPage === 'dashboard' && <Dashboard onNavigate={(page) => setCurrentPage(page)} />}
        {currentPage === 'play' && <PlayPage username={user?.username || 'anonymous'} />}
        {currentPage === 'play3d' && <PlayPage3D />}
        {currentPage === 'friends' && (
          <FriendsPage
            username={user?.username || 'anonymous'}
            onChallengeFriend={(gameId) => handleChallengeFriend(gameId)}
          />
        )}
        {currentPage === 'stats' && <Stats />}
        {currentPage === 'search' && <PlayerSearch />}
        {currentPage === 'cheat' && <CheatAnalysis onNavigate={(page) => setCurrentPage(page)} />}
        {currentPage === 'cheatHistory' && <CheatHistory />}
        {currentPage === 'challenge' && challengeGameId && (
          <MultiplayerGame
            gameId={challengeGameId}
            username={user?.username || 'anonymous'}
            onBack={() => { setChallengeGameId(null); setCurrentPage('friends'); }}
            fromChallenge={true}
          />
        )}
      </main>
    </div>
  );
}
