# Runs a k6 load test against the locally running services.
# No k6 installation needed — it runs in a throwaway container.
#
#   bash load-test/run.sh [script] [vus] [iterations]
#
#   bash load-test/run.sh                         reserve only, 200 users, 2000 attempts
#   bash load-test/run.sh purchase                reserve + pay, then check the databases for oversells
#   bash load-test/run.sh purchase 500 30000      the same with 500 users and 30000 attempts
#
# Environment variables (SCRIPT, VUS, ITERATIONS, USER_COUNT, HOT_SEAT) still work too.
set -euo pipefail

cd "$(dirname "$0")/.."

# Same settings as docker compose, so the database password matches
if [[ -f .env ]]; then
  set -a
  . ./.env
  set +a
fi

SCRIPT=${1:-${SCRIPT:-reserve}}
export VUS=${2:-${VUS:-}}
export ITERATIONS=${3:-${ITERATIONS:-}}
PG_USER=${POSTGRES_USER:-matchday}

psql_ticket() { docker compose exec -T postgres psql -U "$PG_USER" -d ticketdb -tAq "$@"; }
psql_order()  { docker compose exec -T postgres psql -U "$PG_USER" -d orderdb -tAq "$@"; }

echo "Resetting seats and picking 100 seats of the first match..."
psql_ticket < load-test/seed.sql
psql_ticket -c \
  "SELECT json_agg(id) FROM (
     SELECT s.id FROM seats s JOIN events e ON e.id = s.event_id
     ORDER BY e.starts_at, s.section, s.row_label, s.seat_number
     LIMIT 100
   ) AS first_100" > load-test/seats.json

if [[ $SCRIPT == purchase ]]; then
  echo "Clearing orders..."
  psql_order < load-test/reset-orders.sql
fi

SCRIPTS_DIR="$(pwd -W 2>/dev/null || pwd)/load-test"

MSYS_NO_PATHCONV=1 docker run --rm -i \
  -v "${SCRIPTS_DIR}:/scripts" \
  --add-host host.docker.internal:host-gateway \
  -e HOT_SEAT -e VUS -e ITERATIONS -e USER_COUNT \
  grafana/k6:1.3.0 run "/scripts/${SCRIPT}.js"

[[ $SCRIPT == purchase ]] || exit 0

# ---------------------------------------------------------------------------------------------
# What k6 saw is the clients' view. The databases are the truth: check them directly.

echo
echo "== Invariant check: is any seat sold twice? =="

sold=$(psql_ticket -c "SELECT count(*) FROM seats WHERE status = 'SOLD'")
paid=$(psql_order -c "SELECT count(*) FROM orders WHERE status = 'PAID'")
paid_twice=$(psql_order -c \
  "SELECT count(*) FROM (SELECT seat_id FROM orders WHERE status = 'PAID' GROUP BY seat_id HAVING count(*) > 1) x")

# Same set of seats on both sides: every sold seat has exactly one paid order, and vice versa
sold_ids=$(psql_ticket -c "SELECT id FROM seats WHERE status = 'SOLD' ORDER BY id")
paid_ids=$(psql_order -c "SELECT seat_id FROM orders WHERE status = 'PAID' ORDER BY seat_id")

# Tickets arrive through Kafka, so give the consumer a moment to catch up
tickets=0
for _ in $(seq 1 30); do
  tickets=$(psql_ticket -c "SELECT count(*) FROM tickets")
  [[ $tickets == "$sold" ]] && break
  sleep 1
done

echo "   seats SOLD (ticketdb):        $sold"
echo "   orders PAID (orderdb):        $paid"
echo "   seats with 2+ paid orders:    $paid_twice"
echo "   tickets issued (via Kafka):   $tickets"

status=0
[[ $paid_twice == 0 ]]        || { echo "   FAIL: a seat was paid for twice"; status=1; }
[[ $sold == "$paid" ]]        || { echo "   FAIL: sold seats and paid orders disagree"; status=1; }
[[ $sold_ids == "$paid_ids" ]] || { echo "   FAIL: sold seats are not the seats that were paid for"; status=1; }
[[ $tickets == "$sold" ]]     || { echo "   FAIL: not every sold seat got exactly one ticket"; status=1; }

[[ $status == 0 ]] && echo "   OK: every seat sold at most once, paid exactly once, one ticket each"
exit $status
