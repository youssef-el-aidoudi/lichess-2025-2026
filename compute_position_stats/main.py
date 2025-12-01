"""Point d'entrée principal du programme MPI en mode push classique"""
from mpi4py import MPI

from database import init_staging_table, aggregate_staging_to_stats, get_connection
from task_manager import prepare_work_distribution, print_final_report
from worker import process_local_batch, collect_results_to_rank0

def main():
    """
    Architecture MPI Push classique.
    Rank 0 distribue les tâches, workers traitent, rank 0 agrège.
    """
    comm = MPI.COMM_WORLD
    rank = comm.Get_rank()
    size = comm.Get_size()

    # Connexion postgres uniquement pour rank 0
    if rank == 0:
        conn = get_connection()
        cursor = conn.cursor()

        print("="*60)
        print("PHASE 1 : INITIALISATION")
        print("="*60)

        init_staging_table(conn, cursor)

        # Préparer et distribuer les tâches
        work_batches = prepare_work_distribution(cursor, size)
        print(f"Distribution de {len(work_batches)} lots aux {size} processus\n")
    else:
        conn = None
        cursor = None
        work_batches = None

    # Scatter : rank 0 envoie un lot à chaque worker
    local_batch = comm.scatter(work_batches, root=0)

    print(f"Rank {rank}: Traitement de {len(local_batch) if local_batch else 0} tâches")

    # PHASE 2 : TRAITEMENT LOCAL
    local_results = process_local_batch(local_batch, rank)

    # PHASE 3 : COLLECTE ET AGGREGATION
    if rank == 0:
        print("\n" + "="*60)
        print("PHASE 3 : COLLECTE ET AGGREGATION")
        print("="*60)

    # Gather : rank 0 récupère tous les résultats
    all_results = comm.gather(local_results, root=0)

    if rank == 0:
        # Insertion des résultats dans postgres
        collect_results_to_rank0(conn, cursor, all_results)

        # Agrégation finale
        print("\nAgrégation des résultats...")
        aggregate_staging_to_stats(conn, cursor)

        # Rapport final
        print_final_report(cursor)

        cursor.close()
        conn.close()

        print("\n" + "="*60)
        print("TRAITEMENT GLOBAL TERMINÉ")
        print("="*60)


if __name__ == "__main__":
    main()