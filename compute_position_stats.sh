#!/bin/bash

WORKERS=8

for i in $(seq 0 $((WORKERS-1))); do
  echo "Starting worker $i/$((WORKERS-1))"
  docker compose --profile jobs run -d --rm \
    -e WORKERS=$WORKERS \
    -e WORKER_INDEX=$i \
    compute_position_stats
done

echo "All workers started."