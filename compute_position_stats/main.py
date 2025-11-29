def get_all_parties():
    """
    Récupère les parties depuis Postgres.
    :return:
    Les parties.
    """
    conn = get_conn()
    cursor = get_cursor()

    try:
        request = ""
        parties = cursor.execute(request)
    except Exception:
        ""
    finally:
        cursor.close()
        conn.close()

    return parties


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
    world, rank ...
    """
    pass


def create_jobs(parties):
    """
    Créer les jobs directement en SQL pour ne rien charger en mémoire (TABLE TO TABLE).
    :param parties:
    :return:
    """
    pass


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


def main():
    world, rank = init_mpi()

    if rank == 0:
        create_jobs(parties)
        wait_until_jobs_end()
        flush_positions()
    else:
        parties = get_parties_from_job()
        positions = get_positions_from_parties(parties)
        positions_hashed = hash_positions()
        send_results(positions_hashed)

if __name__ == "__main__":
    main()