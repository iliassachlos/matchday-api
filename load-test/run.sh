# Runs the k6 load test against the locally running services.
# No k6 installation needed — it runs in a throwaway container.
set -euo pipefail

cd "$(dirname "$0")/.."

echo "Reseeding: 100 available seats..."
docker compose exec -T postgres psql -U matchday -d ticketdb -q < load-test/seed.sql
docker compose exec -T postgres psql -U matchday -d ticketdb -tAc \
  "SELECT json_agg(id) FROM seats WHERE status = 'AVAILABLE'" > load-test/seats.json

SCRIPTS_DIR="$(pwd -W 2>/dev/null || pwd)/load-test"

MSYS_NO_PATHCONV=1 docker run --rm -i \
  -v "${SCRIPTS_DIR}:/scripts" \
  --add-host host.docker.internal:host-gateway \
  -e HOT_SEAT -e VUS -e ITERATIONS -e USER_COUNT \
  grafana/k6 run /scripts/reserve.js

