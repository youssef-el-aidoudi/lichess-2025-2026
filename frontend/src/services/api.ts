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
