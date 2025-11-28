import os
import subprocess
import sys

# =====================
# Configuration cluster
# =====================

NODES = [
    "qb503660@172.31.18.30",
    "qb503660@172.31.18.31",
    "qb503660@172.31.18.32",
    "qb503660@172.31.18.33",
    "qb503660@172.31.18.34",
    # "qb503660@172.31.18.35",
    "qb503660@172.31.18.36",
    "qb503660@172.31.18.37",
    "qb503660@172.31.18.38",
    "qb503660@172.31.18.39",
    "qb503660@172.31.18.40",
    "qb503660@172.31.18.41",
    "qb503660@172.31.18.42",
    "qb503660@172.31.18.43",
    "qb503660@172.31.18.44",
    "qb503660@172.31.18.45",
]

WORKERS_PER_NODE = 19
GLOBAL_WORKERS = len(NODES) * WORKERS_PER_NODE

PROJECT_DIR = os.path.expanduser("./compute_position_stats")
SCRIPT_NAME = "main.py"


# =====================
# Helpers locaux
# =====================

def run_local_mode(mode: str):
    """Lance compute_position_stats.py en local (sur préprod) avec MODE=..."""
    env = os.environ.copy()
    env["MODE"] = mode

    print(f"\n[ORCH] Lancement local : MODE={mode}\n", flush=True)

    # On suppose qu'on est sur préprod, dans le même projet
    result = subprocess.run(
        ["python3", SCRIPT_NAME],
        cwd=PROJECT_DIR,
        env=env,
    )
    if result.returncode != 0:
        print(f"[ORCH] ERREUR: compute_position_stats.py MODE={mode} a échoué "
              f"(code={result.returncode})")
        sys.exit(result.returncode)


def run_workers():
    """Lance tous les workers sur tous les nœuds via ssh -R."""
    procs = []

    print(f"[ORCH] Lancement des workers distribués...")
    print(f"[ORCH] NODES={len(NODES)}, WORKERS_PER_NODE={WORKERS_PER_NODE}, "
          f"GLOBAL_WORKERS={GLOBAL_WORKERS}\n")

    for m, node in enumerate(NODES):
        for local_idx in range(WORKERS_PER_NODE):
            global_idx = m * WORKERS_PER_NODE + local_idx
            remote_port = 15432 + local_idx  # port DB local au noeud (redirigé vers préprod)

            cmd = (
                f"cd {PROJECT_DIR} && "
                ". .venv/bin/activate && "
                "mkdir -p logs && "
                "MODE=worker "
                f"WORKERS={GLOBAL_WORKERS} WORKER_INDEX={global_idx} "
                f"DB_HOST=localhost DB_PORT={remote_port} "
                f"python {SCRIPT_NAME} > logs/worker_{global_idx}.log 2>&1"
            )

            print(f"[ORCH] Lancement worker {global_idx} sur {node} "
                  f"(remote_port={remote_port})", flush=True)

            procs.append(
                subprocess.Popen(
                    [
                        "ssh",
                        "-R",
                        f"{remote_port}:localhost:5432",  # tunnel vers Postgres sur préprod
                        node,
                        cmd,
                    ]
                )
            )

    print(f"\n[ORCH] {len(procs)} workers lancés, attente de leur fin...\n", flush=True)

    # Attendre la fin de tous les workers
    for i, p in enumerate(procs, start=1):
        ret = p.wait()
        print(f"[ORCH] Worker process {i}/{len(procs)} terminé (code={ret})", flush=True)

    print("[ORCH] Tous les workers SSH sont terminés.\n", flush=True)


def main():
    # 1) PREPARE (local sur préprod)
    run_local_mode("prepare")

    # 2) WORKERS (distants via ssh)
    run_workers()

    # 3) AGGREGATE (local sur préprod)
    run_local_mode("aggregate")

    print("[ORCH] Pipeline complet terminé (prepare -> workers -> aggregate).")


if __name__ == "__main__":
    main()
