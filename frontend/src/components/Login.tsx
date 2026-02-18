import { useState } from 'react';
import { LogIn, UserPlus } from 'lucide-react';

interface LoginProps {
  onLogin: (token: string, username: string) => void;
}

export function Login({ onLogin }: LoginProps) {
  const [isSignup, setIsSignup] = useState(false);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const endpoint = isSignup ? '/api/auth/register' : '/api/auth/login';
      const body = isSignup
        ? { email, hash: password, joueur: { pseudo: username } }
        : { email, hash: password };

      const API_URL = import.meta.env.VITE_API_URL || 'https://api.docker.localhost';

      console.log('Sending request to:', `${API_URL}${endpoint}`, 'Body:', body);

      const response = await fetch(`${API_URL}${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
      });

      console.log('Response status:', response.status);

      if (!response.ok) {
        const errorText = await response.text();
        console.error('Error response:', errorText);
        throw new Error(errorText || 'Une erreur est survenue');
      }

      const data = await response.json();
      console.log('Success response:', data);

      if (isSignup) {
        // Après inscription, rediriger vers la connexion
        setIsSignup(false);
        setError('Inscription réussie ! Veuillez vous connecter.');
        setLoading(false);
        return;
      }

      // Connexion réussie, récupération du token
      if (data.token) {
        onLogin(data.token, data.username || email);
      } else {
        throw new Error('Token manquant dans la réponse');
      }

    } catch (err) {
      console.error('Request error:', err);
      if (err instanceof TypeError && err.message.includes('fetch')) {
        setError("Impossible de se connecter au serveur. Vérifiez que le backend est démarré.");
        setError(err instanceof Error ? err.message : 'Une erreur est survenue');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-xl p-8 w-full max-w-md">
        <div className="text-center mb-8">
          <div className="text-4xl mb-2">♟</div>
          <h1 className="text-gray-900">Chess Analytics Platform</h1>
          <p className="text-gray-600 mt-2">
            Analysez vos parties avec Lichess et Stockfish
          </p>
        </div>

        <div className="flex gap-2 mb-6">
          <button
            onClick={() => {
              setIsSignup(false);
              setError('');
            }}
            className={
              !isSignup
                ? 'flex-1 py-2 px-4 rounded-md transition-colors bg-blue-600 text-white'
                : 'flex-1 py-2 px-4 rounded-md transition-colors bg-gray-100 text-gray-700 hover:bg-gray-200'
            }
          >
            <LogIn className="inline-block w-4 h-4 mr-2" />
            Connexion
          </button>
          <button
            onClick={() => {
              setIsSignup(true);
              setError('');
            }}
            className={
              isSignup
                ? 'flex-1 py-2 px-4 rounded-md transition-colors bg-blue-600 text-white'
                : 'flex-1 py-2 px-4 rounded-md transition-colors bg-gray-100 text-gray-700 hover:bg-gray-200'
            }
          >
            <UserPlus className="inline-block w-4 h-4 mr-2" />
            Inscription
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Champ nom d'utilisateur - uniquement pour l'inscription */}
          {isSignup && (
            <div>
              <label className="block text-gray-700 mb-2">
                Nom utilisateur (Pseudo Lichess)
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
            </div>
          )}

          {/* Champ email - pour connexion et inscription */}
          <div>
            <label className="block text-gray-700 mb-2">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>

          {/* Champ mot de passe */}
          <div>
            <label className="block text-gray-700 mb-2">
              Mot de passe
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>

          {error && (
            <div className={`border px-4 py-3 rounded-md ${error.includes('réussie') ? 'bg-green-50 border-green-200 text-green-700' : 'bg-red-50 border-red-200 text-red-700'}`}>
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-3 rounded-md hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Chargement...' : isSignup ? 'Inscrire' : 'Connecter'}
          </button>
        </form>

        <div className="mt-6 text-center text-sm text-gray-600">
          <p>Assurez-vous que le backend est démarré.</p>
        </div>
      </div>
    </div>
  );
}
