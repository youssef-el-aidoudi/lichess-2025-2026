import { getAllPartiesJouees } from '../services/api';
import { useEffect, useMemo, useState } from 'react';

type JoueurMini = {
  pseudonyme?: string;
};

type OuvertureMini = {
  libelle?: string;
};

type Partie = {
  id: number;
  title?: string;
  dateHeureUTC?: string;
  typeResultat?: string;
  joueurBlanc?: JoueurMini;
  joueurNoir?: JoueurMini;
  ouverture?: OuvertureMini;
};

type HistoryPageProps = {
  onSelectPartie?: (partieId: number) => void;
  onGoToSearch?: () => void;
  onGoToPlay?: () => void;
};

const DEMO_PARTIES: Partie[] = [
  {
    id: 101,
    title: 'Magnus vs Hikaru - Blitz',
    dateHeureUTC: '2026-03-20T18:30:00',
    typeResultat: 'Victoire blanc',
    joueurBlanc: { pseudonyme: 'Magnus' },
    joueurNoir: { pseudonyme: 'Hikaru' },
    ouverture: { libelle: 'Défense sicilienne' },
  },
  {
    id: 102,
    title: 'Youssef vs Stockfish',
    dateHeureUTC: '2026-03-22T20:10:00',
    typeResultat: 'Victoire noir',
    joueurBlanc: { pseudonyme: 'Youssef' },
    joueurNoir: { pseudonyme: 'Stockfish' },
    ouverture: { libelle: 'Partie italienne' },
  },
  {
    id: 103,
    title: 'Partie analysée Lichess',
    dateHeureUTC: '2026-03-25T14:45:00',
    typeResultat: 'Nulle',
    joueurBlanc: { pseudonyme: 'AlphaPlayer' },
    joueurNoir: { pseudonyme: 'BetaPlayer' },
    ouverture: { libelle: 'Ouverture espagnole' },
  },
];

export function HistoryPage({
  onSelectPartie,
  onGoToSearch,
  onGoToPlay,
}: HistoryPageProps) {
  const [parties, setParties] = useState<Partie[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [usingDemo, setUsingDemo] = useState(false);
  const [sortOrder, setSortOrder] = useState<'recent' | 'oldest'>('recent');
  const [resultFilter, setResultFilter] = useState<'all' | 'blanc' | 'noir' | 'nulle'>('all');

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

useEffect(() => {
  const fetchParties = async () => {
    try {
      setLoading(true);
      setError('');
      setUsingDemo(false);

      const token = localStorage.getItem('jwt_token');

      const response = await fetch(`${API_URL}/parties`, {
        headers: token
          ? {
              Authorization: `Bearer ${token}`,
            }
          : {},
      });

      let apiParties: Partie[] = [];
      if (response.ok) {
        const data = await response.json();
        apiParties = Array.isArray(data) ? data : [];
      }

      let partiesJouees: Partie[] = [];
      try {
        const played = await getAllPartiesJouees();
        partiesJouees = Array.isArray(played)
          ? played.map((p: any) => ({
              id: -p.id,
              title: p.titre,
              dateHeureUTC: p.dateHeure,
              typeResultat: p.resultat,
              joueurBlanc: { pseudonyme: p.joueurBlanc },
              joueurNoir: { pseudonyme: p.joueurNoir },
              ouverture: { libelle: p.ouverture || 'Partie locale' },
            }))
          : [];
      } catch (e) {
        console.error('Erreur parties jouées:', e);
      }

      const merged = [...partiesJouees, ...apiParties];

      if (merged.length > 0) {
        setParties(merged);
        setUsingDemo(false);
      } else {
        setParties(DEMO_PARTIES);
        setUsingDemo(true);
      }
    } catch (err) {
      console.error(err);
      setParties(DEMO_PARTIES);
      setUsingDemo(true);
      setError('');
    } finally {
      setLoading(false);
    }
  };

  fetchParties();
}, [API_URL]);

  const filteredParties = useMemo(() => {
    const term = search.trim().toLowerCase();

    let result = [...parties];

    if (term) {
      result = result.filter((partie) => {
        const title = partie.title?.toLowerCase() || '';
        const white = partie.joueurBlanc?.pseudonyme?.toLowerCase() || '';
        const black = partie.joueurNoir?.pseudonyme?.toLowerCase() || '';
        const opening = partie.ouverture?.libelle?.toLowerCase() || '';
        const matchResult = partie.typeResultat?.toLowerCase() || '';

        return (
          title.includes(term) ||
          white.includes(term) ||
          black.includes(term) ||
          opening.includes(term) ||
          matchResult.includes(term)
        );
      });
    }

    if (resultFilter !== 'all') {
      result = result.filter((partie) => {
        const value = (partie.typeResultat || '').toLowerCase();

        if (resultFilter === 'blanc') {
          return value.includes('blanc') || value.includes('white');
        }

        if (resultFilter === 'noir') {
          return value.includes('noir') || value.includes('black');
        }

        if (resultFilter === 'nulle') {
          return value.includes('nulle') || value.includes('nul') || value.includes('draw');
        }

        return true;
      });
    }

    result.sort((a, b) => {
      const da = a.dateHeureUTC ? new Date(a.dateHeureUTC).getTime() : 0;
      const db = b.dateHeureUTC ? new Date(b.dateHeureUTC).getTime() : 0;

      return sortOrder === 'recent' ? db - da : da - db;
    });

    return result;
  }, [parties, search, resultFilter, sortOrder]);

  const formatDate = (value?: string) => {
    if (!value) return 'Date inconnue';

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;

    return date.toLocaleString('fr-FR');
  };

  const getResultBadgeClass = (result?: string) => {
    const value = (result || '').toLowerCase();

    if (value.includes('white') || value.includes('blanc') || value.includes('victoire blanc')) {
      return 'bg-green-100 text-green-700';
    }

    if (value.includes('black') || value.includes('noir') || value.includes('victoire noir')) {
      return 'bg-blue-100 text-blue-700';
    }

    if (value.includes('draw') || value.includes('nul') || value.includes('nulle')) {
      return 'bg-gray-100 text-gray-700';
    }

    return 'bg-orange-100 text-orange-700';
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
        <div>
          <h2 className="text-3xl font-semibold text-gray-900">Historique des parties</h2>
          <p className="mt-2 text-gray-600">
            Consultez les parties enregistrées et accédez à leur analyse détaillée.
          </p>
        </div>

        <div>
          <span
            className={`inline-flex px-3 py-1 rounded-full text-sm font-medium ${
              usingDemo ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-700'
            }`}
          >
            Source : {usingDemo ? 'Démonstration' : 'API'}
          </span>
        </div>
      </div>

      {usingDemo && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          Mode démonstration activé : affichage de parties d’exemple pour présenter la fonctionnalité.
        </div>
      )}

      <div className="bg-white border rounded-xl p-4 shadow-sm space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Rechercher une partie
          </label>
          <input
            type="text"
            placeholder="Titre, joueur, ouverture, résultat..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-4 py-3 outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Filtrer par résultat
            </label>
            <select
              value={resultFilter}
              onChange={(e) => setResultFilter(e.target.value as 'all' | 'blanc' | 'noir' | 'nulle')}
              className="w-full rounded-lg border border-gray-300 px-4 py-3 outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">Tous</option>
              <option value="blanc">Victoire blanc</option>
              <option value="noir">Victoire noir</option>
              <option value="nulle">Nulle</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Trier par date
            </label>
            <select
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value as 'recent' | 'oldest')}
              className="w-full rounded-lg border border-gray-300 px-4 py-3 outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="recent">Plus récentes d’abord</option>
              <option value="oldest">Plus anciennes d’abord</option>
            </select>
          </div>
        </div>
      </div>

      <div className="bg-white border rounded-xl shadow-sm">
        <div className="p-4 border-b">
          <p className="text-sm text-gray-500">
            {loading ? 'Chargement...' : `${filteredParties.length} partie(s) trouvée(s)`}
          </p>
        </div>

        {loading && <div className="p-6 text-gray-600">Chargement des parties...</div>}

        {!loading && error && <div className="p-6 text-red-600">{error}</div>}

        {!loading && !error && filteredParties.length === 0 && (
          <div className="p-8 text-center">
            <h3 className="text-xl font-semibold text-gray-800">
              Aucune partie enregistrée pour le moment
            </h3>
            <p className="mt-2 text-gray-600">
              Lancez une nouvelle partie ou recherchez des parties Lichess pour commencer.
            </p>

            <div className="mt-6 flex flex-col sm:flex-row gap-3 justify-center">
              <button
                onClick={onGoToPlay}
                className="px-5 py-3 rounded-lg bg-blue-600 text-white hover:bg-blue-700 transition-colors"
              >
                Nouvelle partie
              </button>

              <button
                onClick={onGoToSearch}
                className="px-5 py-3 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
              >
                Rechercher sur Lichess
              </button>
            </div>
          </div>
        )}

        {!loading && !error && filteredParties.length > 0 && (
          <div className="divide-y">
            {filteredParties.map((partie) => (
              <div
                key={partie.id}
                className="p-5 flex flex-col gap-4 md:flex-row md:items-center md:justify-between hover:bg-gray-50 transition-colors"
              >
                <div className="space-y-1">
                  <h3 className="text-lg font-semibold text-gray-900">
                    {partie.title || `Partie #${partie.id}`}
                  </h3>

                  <p className="text-sm text-gray-600">
                    {partie.joueurBlanc?.pseudonyme || 'Blanc inconnu'} vs{' '}
                    {partie.joueurNoir?.pseudonyme || 'Noir inconnu'}
                  </p>

                  <p className="text-sm text-gray-500">{formatDate(partie.dateHeureUTC)}</p>

                  <p className="text-sm text-gray-500">
                    Ouverture : {partie.ouverture?.libelle || 'Non renseignée'}
                  </p>
                </div>

                <div className="flex flex-col items-start gap-3 md:items-end">
                  <span
                    className={`px-3 py-1 rounded-full text-sm font-medium ${getResultBadgeClass(
                      partie.typeResultat
                    )}`}
                  >
                    {partie.typeResultat || 'Résultat inconnu'}
                  </span>

                  <button
                    onClick={() => onSelectPartie?.(partie.id)}
                    className="px-4 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700 transition-colors"
                  >
                    Voir détails
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}