"""Opérations base de données"""

import psycopg

from config import (
    DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD,
    TABLE_POSITION_STAGE, TABLE_POSITION_STATS
)


def get_connection():
    """Crée une connexion PostgreSQL"""
    return psycopg.connect(
        host=DB_HOST,
        port=DB_PORT,
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD
    )



def init_staging_table(conn, cursor):
    """Initialise la table de staging (rank 0 seulement)"""
    cursor.execute(f"""
        CREATE TABLE IF NOT EXISTS {TABLE_POSITION_STAGE} (
            hash BIGINT,
            fen TEXT NOT NULL,
            nb_total BIGINT NOT NULL,
            nb_victoires_blanc BIGINT NOT NULL,
            nb_victoires_noir BIGINT NOT NULL,
            nb_nulles BIGINT NOT NULL
        )
    """)
    cursor.execute(f"TRUNCATE TABLE {TABLE_POSITION_STAGE}")
    conn.commit()
    print("Table de staging initialisée")


def aggregate_staging_to_stats(conn, cursor):
    """Agrège staging vers Position_Stats (rank 0 seulement)"""

    cursor.execute(f"""
        INSERT INTO {TABLE_POSITION_STATS} AS ps
            (hash, fen, nb_total, nb_victoires_blanc, nb_victoires_noir, nb_nulles, updated_at)
        SELECT
            hash,
            MIN(fen) AS fen,
            SUM(nb_total),
            SUM(nb_victoires_blanc),
            SUM(nb_victoires_noir),
            SUM(nb_nulles),
            NOW()
        FROM {TABLE_POSITION_STAGE}
        GROUP BY hash
        ON CONFLICT (hash) DO UPDATE
        SET nb_total = ps.nb_total + EXCLUDED.nb_total,
            nb_victoires_blanc = ps.nb_victoires_blanc + EXCLUDED.nb_victoires_blanc,
            nb_victoires_noir = ps.nb_victoires_noir + EXCLUDED.nb_victoires_noir,
            nb_nulles = ps.nb_nulles + EXCLUDED.nb_nulles,
            updated_at = NOW()
    """)

    conn.commit()
    print("Agrégation staging, Position_Stats terminée")


def flush_positions_to_staging(conn, cursor, positions, rank):
    """
    Flush positions vers staging avec COPY.

    :param conn:
    :param cursor:
    :param positions: Liste de tuples (hash, fen, nb_total, w, b, d)
    :param rank: Rank du worker (pour logging)
    """
    if not positions:
        return

    import io

    buf = io.StringIO()
    for h, fen, total, w, b, d in positions:
        buf.write(f"{h}\t{fen}\t{total}\t{w}\t{b}\t{d}\n")

    buf.seek(0)

    with cursor.copy(f"COPY {TABLE_POSITION_STAGE} (hash, fen, nb_total, w, b, d) FROM STDIN") as copy:
        for h, fen, total, w, b, d in positions:
            copy.write_row((h, fen, total, w, b, d))

    conn.commit()
    buf.close()

    print(f"Worker {rank}: {len(positions):,} positions flushées")