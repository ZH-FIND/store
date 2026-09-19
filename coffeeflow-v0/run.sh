#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
docker compose -f "$ROOT/docker-compose.yml" up --build -d --wait
echo "CoffeeFlow V0: http://localhost:${FRONTEND_PORT:-4173}"
