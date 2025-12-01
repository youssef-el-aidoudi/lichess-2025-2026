"""Point d'entrée principal du programme MPI"""
from mpi4py import MPI

from database import init_staging_table, aggregate_staging_to_stats, get_connection
from task_manager import create_all_tasks, print_final_report, wait_for_tasks_creation, wait_for_all_workers_done
from worker import process_all_tasks

def main():
    """
    Architecture MPI Pull avec Task Queue.
    SANS Barrier() pour tolérance aux pannes.
    """
    comm = MPI.COMM_WORLD
    rank = comm.Get_rank()
    with get_connection() as conn:
        with conn.cursor() as cursor:
            if rank == 0:
                print("="*60)
                print("PHASE 1 : INITIALISATION")
                print("="*60)

                init_staging_table(conn, cursor)
                create_all_tasks(conn, cursor)

                print("Initialisation terminée\n")

            if rank != 0:
                # Workers attendent que rank 0 crée les tâches
                wait_for_tasks_creation(cursor, rank)

                # Traiter les tâches
                print(f"Worker {rank}: Démarrage du traitement")
                process_all_tasks(conn, cursor, rank)

                print(f"Worker {rank}: Traitement terminé, arrêt")

            comm.Barrier()

            if rank == 0:
                print("\n" + "="*60)
                print("PHASE 3 : AGGREGATION")
                print("="*60)

                # Agréger staging vers table finale
                print("\nAgrégation des résultats...")
                aggregate_staging_to_stats(conn, cursor)

                # Rapport final
                print_final_report(cursor)

                print("\n" + "="*60)
                print("TRAITEMENT GLOBAL TERMINÉ")
                print("="*60)


if __name__ == "__main__":
    main()