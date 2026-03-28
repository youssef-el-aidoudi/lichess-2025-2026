import { useState, useEffect } from 'react';
import { Login } from './components/Login';
import { Dashboard } from './components/Dashboard';
import { PlayPage } from './components/PlayPage';
import { Stats } from './components/Stats';
import { PlayerSearch } from './components/PlayerSearch';
import { HistoryPage } from './components/HistoryPage';
import { PartieDetailPage } from './components/PartieDetailPage';

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentPage, setCurrentPage] = useState('dashboard');
  const [user, setUser] = useState<{ username: string } | null>(null);
  const [selectedPartieId, setSelectedPartieId] = useState<number | null>(null);

  useEffect(() => {
    const token = localStorage.getItem('jwt_token');
    const username = localStorage.getItem('username');

    if (token && username) {
      setIsAuthenticated(true);
      setUser({ username });
    }
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
    setSelectedPartieId(null);
  };

  if (!isAuthenticated) {
    return <Login onLogin={handleLogin} />;
  }

  const navButtonClass = (page: string) => {
    const baseClass = 'px-3 py-2 rounded-md transition-colors';
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
              <h1 className="text-xl text-gray-900">Chess Analytics</h1>

              <div className="flex gap-4">
                <button
                  onClick={() => {
                    setCurrentPage('dashboard');
                    setSelectedPartieId(null);
                  }}
                  className={navButtonClass('dashboard')}
                >
                  Dashboard
                </button>

                <button
                  onClick={() => {
                    setCurrentPage('play');
                    setSelectedPartieId(null);
                  }}
                  className={navButtonClass('play')}
                >
                  Jouer
                </button>

                <button
                  onClick={() => {
                    setCurrentPage('stats');
                    setSelectedPartieId(null);
                  }}
                  className={navButtonClass('stats')}
                >
                  Statistiques
                </button>

                <button
                  onClick={() => {
                    setCurrentPage('history');
                    setSelectedPartieId(null);
                  }}
                  className={navButtonClass('history')}
                >
                  Historique
                </button>

                <button
                  onClick={() => {
                    setCurrentPage('search');
                    setSelectedPartieId(null);
                  }}
                  className={navButtonClass('search')}
                >
                  Recherche
                </button>
              </div>
            </div>

            <div className="flex items-center gap-4">
              <span className="text-gray-700">{user?.username}</span>
              <button
                onClick={handleLogout}
                className="px-4 py-2 bg-red-500 text-white rounded-md hover:bg-red-600 transition-colors"
              >
                Deconnexion
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {currentPage === 'dashboard' && <Dashboard />}
        {currentPage === 'play' && <PlayPage />}
        {currentPage === 'stats' && <Stats />}
        {currentPage === 'search' && <PlayerSearch />}

        {currentPage === 'history' && selectedPartieId === null && (
          <HistoryPage
            onSelectPartie={(partieId) => {
              setSelectedPartieId(partieId);
            }}
            onGoToPlay={() => setCurrentPage('play')}
            onGoToSearch={() => setCurrentPage('search')}
          />
        )}

        {currentPage === 'history' && selectedPartieId !== null && (
          <PartieDetailPage
            partieId={selectedPartieId}
            onBack={() => setSelectedPartieId(null)}
          />
        )}
      </main>
    </div>
  );
}