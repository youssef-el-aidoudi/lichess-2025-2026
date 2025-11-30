"""Point d'entrée principal du programme MPI"""

from mpi4py import MPI

from database import init_staging_table, aggregate_staging_to_stats
from task_manager import create_all_tasks, print_final_report
from worker import process_all_tasks


def main():
    """Architecture MPI Pull avec Task Queue"""
    comm = MPI.COMM_WORLD
    rank = comm.Get_rank()

    if rank == 0:
        print("=== INITIALISATION ===")
        init_staging_table()
        create_all_tasks()
        print("✓ Prêt pour traitement\n")

    comm.Barrier()

    if rank != 0:
        print(f"Worker {rank} : démarrage")
        process_all_tasks(rank)

    comm.Barrier()

    if rank == 0:
        print("\n=== AGRÉGATION ===")
        aggregate_staging_to_stats()
        print_final_report()


if __name__ == "__main__":
    main()