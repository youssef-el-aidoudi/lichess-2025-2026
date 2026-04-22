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
    const baseClass = 'px-3 py-2 rounded-md transition-colors text-sm font-medium';
    if (currentPage === page) {
      return baseClass + ' bg-blue-50 text-blue-600';
    }
    return baseClass + ' text-gray-600 hover:bg-gray-50';
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center gap-8">
              <h1 className="text-xl text-gray-900 font-bold">♟ Chess Analytics</h1>
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
              <span className="text-gray-700 text-sm">👤 {user?.username}</span>
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
