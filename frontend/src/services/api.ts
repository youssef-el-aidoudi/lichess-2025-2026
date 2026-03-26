const API_URL = import.meta.env.VITE_API_URL || 'https://api.docker.localhost';

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

// Recherche les parties d'un joueur sur Lichess via l'API proxy backend
export const searchPlayerGames = async (
    pseudo: string,
    options?: { debut?: number; fin?: number; max?: number }
): Promise<string> => {
    const params = new URLSearchParams();
    if (options?.debut) params.append('debut', options.debut.toString());
    if (options?.fin) params.append('fin', options.fin.toString());
    if (options?.max) params.append('max', options.max.toString());

    const query = params.toString() ? `?${params.toString()}` : '';
    const response = await fetch(
        `${API_URL}/api/parties/${encodeURIComponent(pseudo)}${query}`,
        {
            method: 'GET',
            signal: AbortSignal.timeout(15000),
        }
    );

    if (!response.ok) {
        throw new Error(`Erreur API: ${response.status} ${response.statusText}`);
    }

    return response.text();
};

// Recherche une partie par son ID sur Lichess
export const searchGameById = async (id: string): Promise<string> => {
    const response = await fetch(`${API_URL}/api/partie/${encodeURIComponent(id)}`, {
        method: 'GET',
        signal: AbortSignal.timeout(10000),
    });

    if (!response.ok) {
        throw new Error(`Erreur API: ${response.status} ${response.statusText}`);
    }

    return response.text();
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

        console.log('API Call:', `${API_URL}/api/chess/bestmove?${params}`);

        const response = await fetch(`${API_URL}/api/chess/bestmove?${params}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
            signal: AbortSignal.timeout(10000), // 10 second timeout
        });

        if (!response.ok) {
            console.error('API Response Error:', response.status, response.statusText);
            throw new Error(`Stockfish API Error: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();
        console.log('API Response:', data);
        return data.bestmove || 'e2e4'; // Return bestmove from JSON response
    } catch (error) {
        console.error('Stockfish API Error:', error);

        if (error instanceof TypeError && error.message.includes('Failed to fetch')) {
            throw new Error('Backend server not responding. Please ensure the backend is running on port 8080.');
        }

        throw error;
    }
};

// Stockfish - Évaluer une position (retourne le score en centipawns)
export const evaluatePosition = async (
    moves: string,
    depth: number = 15
): Promise<{ score: number; isMate: boolean; bestMove: string }> => {
    try {
        const params = new URLSearchParams({
            moves: moves || '',
            depth: depth.toString(),
        });

        const response = await fetch(`${API_URL}/api/chess/evaluate?${params}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
            signal: AbortSignal.timeout(10000),
        });

        if (!response.ok) {
            throw new Error(`Evaluate API Error: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();
        return {
            score: data.score || 0,
            isMate: data.isMate || false,
            bestMove: data.bestMove || 'e2e4',
        };
    } catch (error) {
        console.error('Evaluate API Error:', error);
        throw error;
    }
};

// Stockfish - Analyser une position (wrapper legacy)
export const analyzePosition = async (
    moves: string,
    depth: number = 20
): Promise<{ bestmove: string; evaluation: number }> => {
    try {
        const result = await evaluatePosition(moves, depth);
        return {
            bestmove: result.bestMove,
            evaluation: result.score,
        };
    } catch (error) {
        console.error('Erreur analyse:', error);
        throw error;
    }
};
