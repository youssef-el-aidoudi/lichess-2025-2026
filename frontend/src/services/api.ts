const API_URL = 'http://localhost:8080';

// Helper pour récupérer le token JWT du localStorage
const getAuthToken = (): string | null => {
    return localStorage.getItem('token');
};

// Helper pour les requêtes authentifiées
const fetchWithAuth = async (endpoint: string, options: RequestInit = {}) => {
    const token = getAuthToken();

    const headers = {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
        ...options.headers,
    };

    const response = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers,
    });

    if (!response.ok) {
        throw new Error(`API Error: ${response.status} ${response.statusText}`);
    }

    return response.json();
};

// Récupère un joueur par son pseudo
export const getJoueurByPseudo = async (pseudo: string) => {
    return fetchWithAuth(`/joueurs/search?pseudo=${encodeURIComponent(pseudo)}`);
};

// Récupère toutes les parties
export const getAllParties = async () => {
    return fetchWithAuth('/parties');
};

// Récupère une partie par son ID
export const getPartieById = async (id: number) => {
    return fetchWithAuth(`/parties/${id}`);
};

// Récupère les joueurs d'une partie
export const getPartieJoueurs = async (id: number) => {
    return fetchWithAuth(`/parties/${id}/joueurs`);
};

// Récupère un joueur par son ID
export const getJoueurById = async (id: number) => {
    return fetchWithAuth(`/joueurs/${id}`);
};

// Stockfish - Obtenir le meilleur coup
export const getStockfishBestMove = async (
    moves: string,
    mode: 'depth' | 'movetime' = 'depth',
    color: 'white' | 'black' = 'black',
    depth: number = 15,
    movetime: number = 1000
): Promise<string> => {
    try {
        const params = new URLSearchParams({
            moves: moves || '', // Handle empty moves
            mode,
            color,
            depth: depth.toString(),
            movetime: movetime.toString(),
        });

        console.log('🔗 API Call:', `${API_URL}/api/chess/bestmove?${params}`);

        const response = await fetch(`${API_URL}/api/chess/bestmove?${params}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
            signal: AbortSignal.timeout(10000), // 10 second timeout
        });

        if (!response.ok) {
            console.error('❌ API Response Error:', response.status, response.statusText);
            throw new Error(`Stockfish API Error: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();
        console.log('✅ API Response:', data);
        return data.bestmove || 'e2e4'; // Return bestmove from JSON response
    } catch (error) {
        console.error('❌ Stockfish API Error:', error);
        
        if (error instanceof TypeError && error.message.includes('Failed to fetch')) {
            throw new Error('Backend server not responding. Please ensure the backend is running on port 8080.');
        }
        
        throw error;
    }
};

// Stockfish - Analyser une position
export const analyzePosition = async (
    moves: string,
    depth: number = 20
): Promise<{ bestmove: string; evaluation: number }> => {
    try {
        const bestMove = await getStockfishBestMove(moves, 'depth', 'black', depth, 3000);
        return {
            bestmove: bestMove,
            evaluation: 0, // Backend ne retourne que bestmove pour maintenant
        };
    } catch (error) {
        console.error('Erreur analyse:', error);
        throw error;
    }
};
