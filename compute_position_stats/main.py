import time

import psycopg
from mpi4py import MPI


def parse_partie(partie):
    """
    Parse le PGN d'une partie pour récupérer toutes les positions jouées dans le format FEN.
    :param partie: Objet Partie.
    :return:
    Retourne la liste des positions FEN.
    """
    pass


def get_positions_from_parties(parties):
    """
    Récupère les positions depuis toutes les parties.
    :param parties: Liste de Partie.
    :return:
    Liste de position.
    """
    positions = []
    for partie in parties:
        position = parse_partie(partie)
        positions.append(position)
    return positions


def hash_positions():
    """
    Hash le FEN en BIGINT pour la clé primaire de la table Position_Stats.
    :return:
    Hash en BIGINT.
    """
    pass


def flush_positions(positions_hashed):
    """
    Insère toutes les lignes avec un copy_expert.
    :param positions_hashed:
    Objet positions avec le hash inclut.
    """
    pass


def get_parties_from_job():
    """
    Retourne les parties d'un job.
    :return:
    """
    pass


def init_mpi():
    """
    Initialise MPI.
    :return:
    comm, rank ...
    """
    comm = MPI.COMM_WORLD
    rank = comm.Get_rank()

    return comm, rank


def create_new_jobs(cursor, last_id_partie_in_jobs):
    if last_id_partie_in_jobs is None:
        cursor.execute("""
                       SELECT MIN(id), MAX(id)
                       FROM Partie
                       """)
    else:
        cursor.execute("""
                       SELECT MIN(id), MAX(id)
                       FROM Partie
                       WHERE id > %s
                       """, (last_id_partie_in_jobs,))

    min_id, max_id = cursor.fetchone()

    if min_id is not None:
        chunk_size = 1000
        jobs = []
        jobs_created = 0

        for start in range(min_id, max_id + 1, chunk_size):
            end = min(start + chunk_size - 1, max_id)
            jobs.append((start, end, 'pending'))
            jobs_created += 1

        cursor.executemany("""
                           INSERT INTO TachePositionStats (debut_partie_id, fin_partie_id, statut)
                           VALUES (%s, %s, %s)
                           """, jobs)
        print(f"{jobs_created} jobs créés")
    else:
        print("Aucun nouveau jobs.")


def get_tache_position_stats_last_id(cursor):
    cursor.execute("SELECT MAX(fin_partie_id) FROM TachePositionStats;")
    return cursor.fetchone()[0]


def create_jobs():
    """
    Créer les jobs dans TachePositionStats.
    """
    with  get_connection() as conn:
        with conn.cursor() as cursor:
            last_id_partie_in_jobs = get_tache_position_stats_last_id(cursor)
            create_new_jobs(cursor, last_id_partie_in_jobs)


def wait_until_jobs_end():
    """
    Attends que les jobs terminent.
    :return:
    """
    pass


def send_results(positions_hashed):
    """
    Envoi les résultats au rank 0.
    :param positions_hashed:
    :return:
    """
    pass


def print_report():
    pass


def monitor_progress():
    pass


def get_connection():
    return psycopg.connect("dbname=chessmate user=chessmate")


def claim_next_job():
    """
    Récupère le prochain job disponible.

    :return: (job_id, start_id, end_id) ou None
    """
    with get_connection() as conn:
        cursor = conn.cursor()

        cursor.execute("""
                       SELECT id, debut_partie_id, fin_partie_id
                       FROM TachePositionStats
                       WHERE statut = %s
                          OR (statut = %s AND tentative < %s)
                       ORDER BY tentative ASC, tache_id ASC LIMIT 1
                       FOR
                       UPDATE SKIP LOCKED
                       """, ('pending', 'failed', 3))

        result = cursor.fetchone()
        if result is None:
            return None

        job_id, start, end = result

        # Marquer comme running
        cursor.execute("""
                       UPDATE TachePositionStats
                       SET statut      = %s,
                           worker_rank = %s,
                           debut_a     = NOW(),
                           tentative   = tentative + 1
                       WHERE tache_id = %s
                       """, ('running', rank, job_id))

        conn.commit()  # Ou pas si context manager
        return (job_id, start, end)


def all_jobs_done():
    pass


def mark_job_failed(job_id, param):
    pass


def log_error(param):
    pass


def process_all_jobs(rank):
    """
   Boucle principale : les workers récupèrent et traitent les jobs.
   """
    jobs_processed = 0

    while True:
        job = claim_next_job()

        if job is None:
            if all_jobs_done():
                break
            else:
                time.sleep(1)
                continue

        job_id, start_id, end_id = job

        try:
            parties = get_parties_from_job(start_id, end_id)
            positions = get_positions_from_parties(parties)
            positions_hashed = hash_positions(positions)
            flush_positions(positions_hashed)
            mark_job_completed(job_id)
            jobs_processed += 1

        except Exception as e:
            log_error(f"Erreur job {job_id}: {e}")
            mark_job_failed(job_id, str(e))

    print(f"Worker {rank}: {jobs_processed} jobs traités")


def main():
    """
    Architecture MPI de type "pull", les workers vont chercher leurs tâches dans la table TachePositionStats.
    :return:
    """
    comm, rank = init_mpi()

    if rank == 0:
        create_jobs()
        print("Jobs créés, les workers peuvent commencer")

    # Attendre que tout le monde soit ici donc que les jobs soient créés.
    comm.Barrier()

    if rank == 0:
        monitor_progress()
    else:
        process_all_jobs(rank)

    comm.Barrier()

    if rank == 0:
        print_report()


if __name__ == "__main__":
    main()
