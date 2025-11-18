import os
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
POSITION_FLUSH_THRESHOLD = 200_000


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
    Convention supposée :
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
    À partir d'une chaîne PGN contenant uniquement la liste de coups (pas les headers),
    génère tous les FEN atteints dans la partie (y compris la position initiale).

    Hypothèses sur le champ 'pgn' actuel :
    - Chaîne de coups SAN nettoyée : "1. e4 e5 2. Nf3 Nc6 ... 1-0"
    - On ignore les numéros de coup ('1.', '2.' etc.) et on s'arrête sur '1-0', '0-1', '1/2-1/2', '*'.
    """
    if not pgn_moves:
        return

    board = chess.Board()
    # Position initiale
    yield board

    tokens = pgn_moves.split()
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
    now = datetime.datetime.utcnow()
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

    conn.commit()
    print(f"Flushed {len(rows):,} positions vers {POSITION_TABLE}")
    stats_dict.clear()


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

    conn.close()


if __name__ == "__main__":
    main()
