import os
import re
import time
import datetime
import psycopg2
from psycopg2.extras import execute_values
import chess

# =========================
# CONFIGURATION
# =========================

DB_HOST = os.getenv("DB_HOST", "postgres")
DB_PORT = int(os.getenv("DB_PORT", 5432))
DB_NAME = os.getenv("DB_NAME", "chessmate")
DB_USER = os.getenv("DB_USER", "chessmate")
DB_PASSWORD = os.getenv("DB_PASSWORD", "chessmate")

PARTIE_TABLE = "Partie"
POSITION_TABLE = "Position_Stats"

# Nombre de parties chargées par batch depuis la table Partie
PARTIE_BATCH_SIZE = 10_000

# Nombre maximal de positions accumulées en mémoire avant UPSERT
POSITION_FLUSH_THRESHOLD = 100_000

# ============
# PGN CLEANING
# ============

# On pré-compile les regex pour aller vite
# 1) commentaires entre accolades : { ... }
_COMMENT_RE = re.compile(r"\{[^}]*\}")
# 2) NAGs style $1, $2, etc.
_NAG_RE = re.compile(r"\$\d+")
# 3) Tags type [%eval 0.23], [%clk ...] (au cas où ils ne sont pas dans des {})
_BRACKET_TAG_RE = re.compile(r"\[%.*?\]")

def clean_pgn_moves(pgn_moves: str) -> str:
    """
    Nettoie les coups PGN venant de lichess :
      - supprime les commentaires { ... }
      - supprime les NAGs ($1, $2, ...)
      - supprime les tags [%%eval ...], [%%clk ...], etc.
      - normalise les espaces

    On ne touche pas aux numéros de coup, ni aux résultats,
    gérés plus bas dans iter_positions_from_pgn_moves.
    """
    if not pgn_moves:
        return ""

    s = pgn_moves

    # Supprimer commentaires complets { ... }
    s = _COMMENT_RE.sub(" ", s)
    # Supprimer tags [%%eval ...] / [%%clk ...] éventuellement restants
    s = _BRACKET_TAG_RE.sub(" ", s)
    # Supprimer NAGs $1, $2, ...
    s = _NAG_RE.sub(" ", s)

    # Nettoyage des espaces
    s = " ".join(s.split())
    return s


# =========================
# HELPERS
# =========================
import hashlib

def fen_hash(fen: str) -> int:
    """
    Hash stable 64 bits signé basé sur le FEN.
    On prend les 8 premiers octets du SHA-256 et on force sur 63 bits pour rester positif.
    """
    h = hashlib.sha256(fen.encode("utf-8")).digest()
    # 8 premiers octets → entier 64 bits
    val = int.from_bytes(h[:8], byteorder="big", signed=False)
    # on force le bit de signe à 0 pour rester dans BIGINT positif
    val &= (1 << 63) - 1
    return val

def outcome_from_resultat(resultat: int):
    """
    Convertit le champ 'resultat' (int) en (w, b, d) à ajouter par position.
    Convention :
      2 → 1-0 (victoire blanc)
      0 → 0-1 (victoire noir)
      1 → autre (nulle)
    """
    if resultat == 2:
        return 1, 0, 0
    elif resultat == 0:
        return 0, 1, 0
    else:
        return 0, 0, 1


def iter_positions_from_pgn_moves(pgn_moves: str):
    """
    Fonction générative à itérer. yield est un return pour une fonction générative.

    À partir d'une chaîne PGN contenant uniquement la liste de coups (pas les headers),
    génère tous les FEN atteints dans la partie (y compris la position initiale).

    Le pgn à parse doit être  :
    - Une chaîne de coups SAN nettoyée : "1. e4 e5 2. Nf3 Nc6 ... 1-0"
    - On ignore les numéros de coup ('1.', '2.' etc.) et on s'arrête sur '1-0', '0-1', '1/2-1/2', '*'.
    """
    if not pgn_moves:
        return

    cleaned = clean_pgn_moves(pgn_moves)
    if not cleaned:
        return

    board = chess.Board()
    # Position initiale
    yield board

    tokens = cleaned.split()
    for token in tokens:
        # Fin de partie
        if token in ("1-0", "0-1", "1/2-1/2", "*"):
            break

        # Numéros de coup (1., 23... etc.)
        if token.endswith("."):
            continue
        if token == "...":
            continue

        try:
            board.push_san(token)
        except Exception:
            # Si le PGN est corrompu, on arrête la partie ici
            break


        yield board


# =========================
# UPSERT DANS Position_Stats
# =========================

def flush_position_stats(conn, stats_dict):
    """
    Flush d'un dictionnaire :
      hash -> (fen, nb_total, nb_w, nb_b, nb_d)
    dans la table Position_Stats avec UPSERT.
    """
    if not stats_dict:
        return

    rows = []
    now = datetime.datetime.now(datetime.UTC)
    for h, (fen, nb_total, w, b, d) in stats_dict.items():
        rows.append((h, fen, nb_total, w, b, d, now))

    sql = f"""
        INSERT INTO {POSITION_TABLE}
            (hash, fen, nb_total, nb_victoires_blanc, nb_victoires_noir, nb_nulles, updated_at)
        VALUES %s
        ON CONFLICT (hash) DO UPDATE
        SET nb_total          = {POSITION_TABLE}.nb_total          + EXCLUDED.nb_total,
            nb_victoires_blanc = {POSITION_TABLE}.nb_victoires_blanc + EXCLUDED.nb_victoires_blanc,
            nb_victoires_noir  = {POSITION_TABLE}.nb_victoires_noir  + EXCLUDED.nb_victoires_noir,
            nb_nulles          = {POSITION_TABLE}.nb_nulles          + EXCLUDED.nb_nulles,
            updated_at         = NOW();
    """

    with conn.cursor() as cur:
        execute_values(cur, sql, rows)

    print(f"Flushed {len(rows):,} positions vers {POSITION_TABLE}")
    stats_dict.clear()

def debug_parse_single_partie(conn, partie_id: int):
    """
    Debug : charge une seule partie, log :
      - PGN brut
      - PGN nettoyé
      - tokens
      - chaque coup joué avec FEN
    """
    with conn.cursor() as cur:
        cur.execute(
            f"SELECT id, resultat, pgn FROM {PARTIE_TABLE} WHERE id = %s",
            (partie_id,)
        )
        row = cur.fetchone()

    if row is None:
        print(f"[DEBUG] Aucune partie avec id={partie_id}")
        return

    pid, resultat, pgn_moves = row
    print("=" * 80)
    print(f"[DEBUG] Partie id={pid}, resultat={resultat}")
    print("-" * 80)
    print("[DEBUG] PGN BRUT :")
    print(pgn_moves)
    print("-" * 80)

    cleaned = clean_pgn_moves(pgn_moves or "")
    print("[DEBUG] PGN APRES NETTOYAGE :")
    print(cleaned)
    print("-" * 80)

    tokens = cleaned.split()
    print("[DEBUG] TOKENS :")
    for i, tok in enumerate(tokens):
        print(f"  #{i:03d}: {tok!r}")
    print("-" * 80)

    board = chess.Board()
    print("[DEBUG] Position initiale FEN :")
    print(board.fen())
    print("-" * 80)

    for i, token in enumerate(tokens):
        print(f"[DEBUG] Traitement token #{i:03d}: {token!r}")

        # Fin de partie
        if token in ("1-0", "0-1", "1/2-1/2", "*"):
            print(f"  -> Token est un résultat de partie, arrêt du parsing.")
            break

        # Numéros de coups
        if token.endswith("."):
            print("  -> Token est un numéro de coup, on skip.")
            continue
        if token == "...":
            print("  -> Token est '...', on skip.")
            continue

        try:
            move = board.parse_san(token)
            board.push(move)
            print("  -> Coup accepté")
            print("     FEN :", board.fen())
        except Exception as e:
            print("  -> ERREUR parse_san / push_san !")
            print(f"     Exception: {e}")
            print("     FEN courante avant ce coup :", board.fen())
            print("     On STOP ici pour analyse.")
            break

    print("=" * 80)


# =========================
# TRAITEMENT PRINCIPAL
# =========================

def main():
    conn = psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD,
    )
    conn.autocommit = False

    # --- MODE DEBUG : une seule partie, on log tout et on s'arrête ---
    DEBUG_PARTIE_ID=None
    if DEBUG_PARTIE_ID is not None:
        try:
            pid = int(DEBUG_PARTIE_ID)
        except ValueError:
            print(f"[DEBUG] DEBUG_PARTIE_ID invalide: {DEBUG_PARTIE_ID!r}")
            conn.close()
            return

        debug_parse_single_partie(conn, pid)
        conn.close()
        return

    # Dictionnaire : hash → (fen, nb_total, nb_w, nb_b, nb_d)
    position_stats = {}

    total_parties = 0
    total_positions = 0

    global_start = time.time()

    with conn.cursor(name="parties_cursor") as cur:
        # Cursor côté serveur pour streamer les lignes
        cur.itersize = PARTIE_BATCH_SIZE
        cur.execute(f"SELECT id, resultat, pgn FROM {PARTIE_TABLE}")

        for row in cur:
            partie_id, resultat, pgn_moves = row
            total_parties += 1

            w_inc, b_inc, d_inc = outcome_from_resultat(resultat)

            try:
                for board in iter_positions_from_pgn_moves(pgn_moves):
                    fen = board.fen()
                    h = fen_hash(fen)

                    if h not in position_stats:
                        position_stats[h] = [fen, 0, 0, 0, 0]

                    entry = position_stats[h]
                    entry[1] += 1  # nb_total
                    entry[2] += w_inc  # nb_victoires_blanc
                    entry[3] += b_inc  # nb_victoires_noir
                    entry[4] += d_inc  # nb_nulles

                    total_positions += 1

            except Exception as e:
                # Si une partie est foireuse, on log et on continue
                print(f"[WARN] Partie id={partie_id} ignorée (erreur parsing PGN) : {e}")

            # Flush périodique des positions
            if len(position_stats) >= POSITION_FLUSH_THRESHOLD:
                flush_position_stats(conn, position_stats)

            # Petit log périodique
            if total_parties % 1_000 == 0:
                elapsed = time.time() - global_start
                print(
                    f"{total_parties:,} parties traitées, "
                    f"{total_positions:,} positions vues, "
                    f"{elapsed:.1f}s écoulées"
                )

    # Flush final
    flush_position_stats(conn, position_stats)

    total_time = time.time() - global_start
    print("\n=== Terminé ===")
    print(f"Parties traitées : {total_parties:,}")
    print(f"Positions totales : {total_positions:,}")
    print(f"Durée totale : {total_time:.1f} s")

    conn.commit()
    conn.close()


if __name__ == "__main__":
    main()
