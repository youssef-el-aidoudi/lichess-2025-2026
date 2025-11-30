"""Logique de traitement des workers MPI"""

import time

from task_manager import (
    claim_next_task,
    mark_task_completed,
    mark_task_failed,
    all_tasks_done,
    get_parties_from_task
)
from pgn_parser import get_positions_from_parties
from database import flush_positions_to_staging


def process_all_tasks(cursor, rank):
    """
    Boucle principale : les workers récupèrent et traitent les tâches.

    :param cursor:
    :param rank: Rank du worker MPI
    """
    tasks_processed = 0

    while True:
        task = claim_next_task(cursor, rank)

        if task is None:
            if all_tasks_done(cursor):
                break
            time.sleep(1)
            continue

        task_id, start_id, end_id = task

        try:
            parties = get_parties_from_task(cursor, start_id, end_id, rank)

            positions = get_positions_from_parties(parties)

            flush_positions_to_staging(cursor, positions, rank)

            mark_task_completed(cursor, task_id, rank)
            tasks_processed += 1

        except Exception as e:
            print(f"Worker {rank} - Erreur tâche {task_id}: {e}")
            mark_task_failed(cursor, task_id, str(e), rank)

    print(f"Worker {rank}: {tasks_processed} tâches traitées")