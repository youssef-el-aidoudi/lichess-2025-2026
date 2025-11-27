import subprocess

NODES = [
    "qb503660@172.31.18.30",
    "qb503660@172.31.18.31",
    "qb503660@172.31.18.32",
    "qb503660@172.31.18.33",
    "qb503660@172.31.18.34",
    #    "qb503660@172.31.18.35 ",
    "qb503660@172.31.18.36",
    "qb503660@172.31.18.37",
    "qb503660@172.31.18.38",
    "qb503660@172.31.18.39",
    "qb503660@172.31.18.40",
    "qb503660@172.31.18.41",
    "qb503660@172.31.18.42",
    "qb503660@172.31.18.43",
    "qb503660@172.31.18.44",
    "qb503660@172.31.18.45"
]

WORKERS_PER_NODE = 19
GLOBAL_WORKERS = len(NODES) * WORKERS_PER_NODE

procs = []

for m, node in enumerate(NODES):
    for local_idx in range(WORKERS_PER_NODE):
        global_idx = m * WORKERS_PER_NODE + local_idx
    remote_port = 15432 + local_idx  # port DB sur le noeud (mi108-xx)

    cmd = (
        "cd ~/lichess_compute && "
        ". .venv/bin/activate && "  # suppose .env déjà config avec les bons packages sur chaque noeud (/home partagé donc sur un seul ça suffit).
        "MODE=worker "
        f"WORKERS={GLOBAL_WORKERS} WORKER_INDEX={global_idx} "
        f"DB_HOST=localhost DB_PORT={remote_port} "
        "python compute_position_stats.py"
    )

    print(f"Lancement worker {global_idx} sur {node} (remote_port={remote_port})")

    procs.append(
        subprocess.Popen([
            "ssh",
            "-R", f"{remote_port}:localhost:5432",  # tunnel vers Postgres sur préprod
            node,
            cmd,
        ])
    )

for p in procs:
    p.wait()

print("Tous les workers ont terminé.")
