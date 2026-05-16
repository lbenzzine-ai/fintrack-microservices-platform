#!/usr/bin/env bash
# Trigger the optional DataFaker seeders inside the running stack. Each service guards on a
# {fintrack.seed.enabled=true} property — so this script restarts the relevant services with
# that flag flipped, lets the seeder run, then flips it back off.
#
# Usage:
#   ./scripts/seed-data.sh                   # 100k tx, 5k users
#   COUNT_TX=10000 COUNT_USERS=500 ./scripts/seed-data.sh

set -euo pipefail

COUNT_TX=${COUNT_TX:-100000}
COUNT_USERS=${COUNT_USERS:-5000}

echo "▶ Seeding transaction-service with COUNT_TX=$COUNT_TX"
docker compose stop transaction-service >/dev/null
FINTRACK_SEED_ENABLED=true FINTRACK_SEED_COUNT=$COUNT_TX \
  docker compose up -d transaction-service

echo "▶ Seeding user-service with COUNT_USERS=$COUNT_USERS"
docker compose stop user-service >/dev/null
FINTRACK_SEED_ENABLED=true FINTRACK_SEED_COUNT=$COUNT_USERS \
  docker compose up -d user-service

echo "▶ Waiting for seeders to settle…"
sleep 20

echo "▶ Restoring normal config"
docker compose stop transaction-service user-service >/dev/null
docker compose up -d transaction-service user-service

echo "✓ Seed complete. Visit Kafdrop (http://localhost:8090) to inspect the resulting events."
