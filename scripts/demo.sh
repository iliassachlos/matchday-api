# Demo different scenarios against the running services, to show how they behave under various conditions.

set -uo pipefail

cd "$(dirname "$0")/.."

USERS=${USER_SERVICE_URL:-http://localhost:8080}
TICKETS=${TICKET_SERVICE_URL:-http://localhost:8081}
ORDERS=${ORDER_SERVICE_URL:-http://localhost:8082}
PG_USER=${POSTGRES_USER:-matchday}
CROWD_SIZE=${CROWD_SIZE:-1000}

BOLD=$'\e[1m'; GREEN=$'\e[32m'; RED=$'\e[31m'; YELLOW=$'\e[33m'; DIM=$'\e[2m'; RESET=$'\e[0m'

step() { printf '\n%s== %s ==%s\n' "$BOLD" "$1" "$RESET"; }
info() { printf '   %s\n' "$1"; }
ok()   { printf '   %s✔ %s%s\n' "$GREEN" "$1" "$RESET"; }
warn() { printf '   %s⚠ %s%s\n' "$YELLOW" "$1" "$RESET"; SCENARIO_RESULT=known; }
fail() { printf '   %s✘ %s%s\n' "$RED" "$1" "$RESET"; SCENARIO_RESULT=failed; }
note() { printf '   %s%s%s\n' "$DIM" "$1" "$RESET"; }

# ---------------------------------------------------------------------------------------------
# HTTP helpers

# call METHOD URL TOKEN [JSON_BODY] -> sets STATUS and BODY
call() {
    local method=$1 url=$2 token=$3 body=${4:-}
    local args=(-s -X "$method" "$url" -w $'\n%{http_code}')
    [[ -n $token ]] && args+=(-H "Authorization: Bearer $token")
    [[ -n $body ]] && args+=(-H "Content-Type: application/json" -d "$body")

    local response
    response=$(curl "${args[@]}")
    STATUS=${response##*$'\n'}
    BODY=${response%$'\n'*}
}

# Tiny JSON readers, so the demo needs nothing beyond curl
field()  { grep -o "\"$1\":\"[^\"]*\"" <<<"$2" | head -1 | cut -d'"' -f4; }
number() { grep -o "\"$1\":[0-9]*" <<<"$2" | head -1 | cut -d: -f2; }

expect() {
    local wanted=$1 label=$2
    if [[ $STATUS == "$wanted" ]]; then
        ok "$label (HTTP $STATUS)"
    else
        fail "$label: expected HTTP $wanted, got $STATUS: $BODY"
        return 1
    fi
}

expect_field() {
    local name=$1 wanted=$2 label=$3 actual
    actual=$(field "$name" "$BODY")
    if [[ $actual == "$wanted" ]]; then
        ok "$label"
    else
        fail "$label: expected $name=$wanted, got '$actual': $BODY"
        return 1
    fi
}

reserve() { call POST "$TICKETS/api/v1/seats/$1/reserve" "$2"; }
buy()     { call POST "$ORDERS/api/v1/orders" "$2" "{\"seatId\":\"$1\",\"paymentMethod\":\"$3\"}"; }

# race SEAT TOKEN... -> fires every reservation at once, sets RACE_CODES in token order
race() {
    local seat=$1; shift
    local dir i=0
    dir=$(mktemp -d)
    for token in "$@"; do
        curl -s -o /dev/null -w '%{http_code}' -X POST "$TICKETS/api/v1/seats/$seat/reserve" \
            -H "Authorization: Bearer $token" >"$dir/$i" &
        i=$((i + 1))
    done
    wait
    RACE_CODES=()
    for ((j = 0; j < i; j++)); do RACE_CODES+=("$(<"$dir/$j")"); done
    rm -rf "$dir"
}

count() {
    local wanted=$1 n=0; shift
    for code in "$@"; do [[ $code == "$wanted" ]] && n=$((n + 1)); done
    echo "$n"
}

# Hands out a different available seat to every scenario
next_seat() {
    SEAT=${FREE_SEATS[$NEXT_SEAT]:-}
    NEXT_SEAT=$((NEXT_SEAT + 1))
    if [[ -z $SEAT ]]; then fail "No available seats left in this match"; return 1; fi
}

# ---------------------------------------------------------------------------------------------
# Setup, shared by every scenario

register_and_login() {
    local name=$1 email
    email="$1-$(date +%s%N)@matchday.test"
    call POST "$USERS/api/v1/auth/register" "" \
        "{\"email\":\"$email\",\"password\":\"demo-password\",\"displayName\":\"$name\"}"
    expect 201 "$name registered" || exit 1
    call POST "$USERS/api/v1/auth/login" "" "{\"email\":\"$email\",\"password\":\"demo-password\"}"
    expect 200 "$name logged in" || exit 1
    TOKEN=$(field accessToken "$BODY")
}

setup() {
    step "Setup"
    for url in "$USERS" "$TICKETS" "$ORDERS"; do
        if curl -s "$url/actuator/health" | grep -q '"status":"UP"'; then
            ok "$url is up"
        else
            fail "$url is not running"; exit 1
        fi
    done

    register_and_login maria;   MARIA=$TOKEN
    register_and_login giorgos; GIORGOS=$TOKEN

    call GET "$TICKETS/api/v1/events" "$MARIA"
    expect 200 "Match list" || exit 1
    EVENT=$(field id "$BODY")
    SEATS_BEFORE=$(number availableSeats "$BODY")
    info "Match: $(field name "$BODY") @ $(field venueName "$BODY"), $SEATS_BEFORE seats available"

    call GET "$TICKETS/api/v1/events/$EVENT/seats" "$MARIA"
    mapfile -t FREE_SEATS < <(grep -o '"id":"[^"]*"[^}]*"status":"AVAILABLE"' <<<"$BODY" | cut -d'"' -f4 | head -50)
    NEXT_SEAT=0
}

# ---------------------------------------------------------------------------------------------
# Scenarios

scenario_good_path() {
    next_seat || return
    reserve "$SEAT" "$MARIA"
    expect 200 "Maria holds the seat until $(field reservedUntil "$BODY")" || return
    buy "$SEAT" "$MARIA" pm_card_ok
    expect 201 "Order placed" || return
    expect_field status PAID "PAID: charged $(number amountCents "$BODY") cents, then ticket-service sold the seat"
}

scenario_two_fans() {
    next_seat || return
    race "$SEAT" "$MARIA" "$GIORGOS"
    info "maria: HTTP ${RACE_CODES[0]}    giorgos: HTTP ${RACE_CODES[1]}"
    if [[ $(count 200 "${RACE_CODES[@]}") != 1 || $(count 409 "${RACE_CODES[@]}") != 1 ]]; then
        fail "Expected exactly one 200 and one 409"; return 1
    fi
    ok "Exactly one winner: the database made them wait in line for the seat"

    local winner=$MARIA
    [[ ${RACE_CODES[0]} == 409 ]] && winner=$GIORGOS

    buy "$SEAT" "$winner" pm_card_ok
    expect_field status PAID "The winner pays: PAID"
}

scenario_crowd() {
    next_seat || return
    local half=$((CROWD_SIZE / 2)) dir started elapsed
    dir=$(mktemp -d)

    # One curl process per fan, each sending its half of the requests all at once (parallel mode),
    # instead of starting a separate program per request, which would be too slow to be "at once"
    for ((i = 0; i < half; i++)); do printf 'url = "%s"\n' "$TICKETS/api/v1/seats/$SEAT/reserve"; done >"$dir/urls"

    started=$(date +%s%N)
    for who in MARIA GIORGOS; do
        curl -s -Z --parallel-immediate --parallel-max "$half" -X POST \
            -H "Authorization: Bearer ${!who}" -K "$dir/urls" -w '\n%{http_code}\n' >"$dir/$who" &
    done
    wait
    elapsed=$(( ($(date +%s%N) - started) / 1000000 ))

    mapfile -t RACE_CODES < <(grep -hxE '[0-9]{3}' "$dir/MARIA" "$dir/GIORGOS")
    rm -rf "$dir"

    local won lost
    won=$(count 200 "${RACE_CODES[@]}"); lost=$(count 409 "${RACE_CODES[@]}")
    info "$((half * 2)) requests for one seat in ${elapsed} ms: $won got it, $lost were refused"
    if [[ $won == 1 && $lost == $((half * 2 - 1)) ]]; then
        ok "Exactly one winner out of $((half * 2)), everyone else got a clean 409"
    else
        fail "Expected 1 winner and $((half * 2 - 1)) refusals; other answers: $(printf '%s\n' "${RACE_CODES[@]}" | grep -vxE '200|409' | sort | uniq -c | tr '\n' ' ')"
        return 1
    fi
}

scenario_declined_card() {
    next_seat || return
    reserve "$SEAT" "$MARIA"
    expect 200 "Maria holds a seat" || return
    buy "$SEAT" "$MARIA" pm_card_declined
    expect 201 "Order placed" || return
    expect_field status FAILED "FAILED: card declined, nothing charged" || return
    buy "$SEAT" "$MARIA" pm_card_ok
    expect_field status PAID "Her hold survived, so a second card works: PAID"
}

scenario_expired_hold() {
    next_seat || return
    reserve "$SEAT" "$GIORGOS"
    expect 200 "Giorgos holds a seat" || return
    note "Simulating 10 minutes passing before the release job runs..."
    if ! docker compose exec -T postgres psql -U "$PG_USER" -d ticketdb -q \
        -c "UPDATE seats SET reserved_until = now() - interval '1 second' WHERE id = '$SEAT';" >/dev/null 2>&1; then
        fail "Could not reach Postgres through docker compose"; return 1
    fi
    buy "$SEAT" "$GIORGOS" pm_card_ok
    expect 201 "Order placed" || return
    case $(field status "$BODY") in
        REFUNDED) ok "REFUNDED: charged, ticket-service refused the expired hold, money returned" ;;
        *) note "Got $(field status "$BODY"): the release job freed the seat first. Run this scenario again to see the refund" ;;
    esac
}

scenario_someone_elses_hold() {
    next_seat || return
    reserve "$SEAT" "$GIORGOS"
    expect 200 "Giorgos holds a seat" || return
    buy "$SEAT" "$MARIA" pm_card_ok
    if [[ $STATUS == 409 ]]; then
        ok "Maria is refused before any charge (HTTP 409)"
    elif [[ $(field status "$BODY") == REFUNDED ]]; then
        warn "Known weakness: Maria was charged, then refunded. The seat is safe, but she should be refused before paying"
        note "Why: order-service only checks the seat is RESERVED, not that the caller is the one holding it"
    else
        fail "Unexpected: HTTP $STATUS $BODY"; return 1
    fi
}

scenario_no_token() {
    call GET "$ORDERS/api/v1/orders/me" ""
    expect 401 "No token: order-service refuses" || return
    call POST "$TICKETS/api/v1/seats/${FREE_SEATS[0]}/reserve" ""
    expect 401 "No token: ticket-service refuses"
}

scenario_forged_token() {
    note "Anyone can read a JWT's payload:"
    note "$(cut -d. -f2 <<<"$MARIA" | tr '_-' '/+' | base64 -d 2>/dev/null)"
    local header signature forged
    IFS=. read -r header _ signature <<<"$MARIA"
    forged="$header.$(cut -d. -f2 <<<"$GIORGOS").$signature"
    call GET "$ORDERS/api/v1/orders/me" "$forged"
    expect 401 "...but Giorgos's identity under Maria's signature is rejected"
}

scenario_bad_input() {
    call POST "$ORDERS/api/v1/orders" "$MARIA" '{"paymentMethod":"pm_card_ok"}'
    expect 400 "Order without a seatId" || return
    call GET "$TICKETS/api/v1/seats/not-a-uuid" "$MARIA"
    expect 400 "Seat id that is not a UUID" || return
    if grep -q 'at com\.\|Exception' <<<"$BODY"; then
        fail "The error leaks internals: $BODY"; return 1
    fi
    ok "Errors come back as a short, readable message, with no internal details exposed"
}

SCENARIOS=(
    "good_path|Good path: reserve, pay, PAID"
    "two_fans|Two fans click the same seat at the same moment"
    "crowd|$CROWD_SIZE simultaneous requests for one seat"
    "declined_card|Card declined, then retried with a good card"
    "expired_hold|Paid after the hold expired: automatic refund"
    "someone_elses_hold|Paying for a seat someone else is holding"
    "no_token|No token"
    "forged_token|Forged token (someone else's identity)"
    "bad_input|Bad input gets a clean 400"
)

# ---------------------------------------------------------------------------------------------
# Menu and runner

pick_scenarios() {
    local choices=("$@")
    if ((${#choices[@]} == 0)); then
        printf '\n%sScenarios%s\n' "$BOLD" "$RESET"
        for i in "${!SCENARIOS[@]}"; do printf '  %d) %s\n' $((i + 1)) "${SCENARIOS[$i]#*|}"; done
        printf '  a) all\n'
        read -rp $'\nPick (e.g. "1 3 5" or "a"): ' -a choices
    fi

    SELECTED=()
    for choice in "${choices[@]}"; do
        if [[ $choice == a || $choice == all ]]; then
            SELECTED=("${!SCENARIOS[@]}"); return
        elif [[ $choice =~ ^[0-9]+$ ]] && ((choice >= 1 && choice <= ${#SCENARIOS[@]})); then
            SELECTED+=($((choice - 1)))
        else
            printf 'Unknown scenario: %s\n' "$choice"; exit 2
        fi
    done
}

pick_scenarios "$@"
setup

PASSED=0; FAILED=0; KNOWN=0
for index in "${SELECTED[@]}"; do
    entry=${SCENARIOS[$index]}
    step "$((index + 1)). ${entry#*|}"
    SCENARIO_RESULT=passed
    "scenario_${entry%%|*}" || SCENARIO_RESULT=failed
    case $SCENARIO_RESULT in
        passed) PASSED=$((PASSED + 1)) ;;
        known)  KNOWN=$((KNOWN + 1)) ;;
        *)      FAILED=$((FAILED + 1)) ;;
    esac
done

step "Summary"
call GET "$TICKETS/api/v1/events" "$MARIA"
info "Seats available in this match: $SEATS_BEFORE -> $(number availableSeats "$BODY")"
info "${GREEN}$PASSED passed${RESET}, ${YELLOW}$KNOWN known issue(s)${RESET}, ${RED}$FAILED failed${RESET}"
((FAILED == 0))
