#!/usr/bin/env bash
# ============================================================
# FinTrack — 10-transfer end-to-end demo
# ============================================================
#
# Registers two demo users via the API Gateway, opens a USD
# wallet for each, seeds both wallets with a starting balance,
# then issues 10 DOMESTIC_TRANSFER calls and reports final
# balances. Use it to generate live traffic across the stack
# so logs (Kibana), metrics (Grafana/Prometheus) and traces
# (Zipkin) light up.
#
# Usage:
#   ./scripts/demo-transfers.sh
#
# Options (env vars):
#   BASE             API base URL  (default: http://localhost:8080)
#   SUFFIX           Username suffix (default: unix timestamp)
#   SEED_BALANCE     Alice starting balance (default: 10000)
#   BOB_BALANCE      Bob starting balance   (default: 5000)
#   TRANSFER_AMOUNT  Amount per transfer    (default: 50)
#   COUNT            Number of transfers    (default: 10)
#
# Examples:
#   BASE=https://fintrack.ddns.net ./scripts/demo-transfers.sh
#   TRANSFER_AMOUNT=500 COUNT=5 ./scripts/demo-transfers.sh
#
# Prerequisites:
#   - Docker Compose stack running (java21)
#   - curl, python3 available on PATH
# ============================================================
set -euo pipefail

BASE=${BASE:-http://localhost:8080}
SUFFIX=${SUFFIX:-$(date +%s)}
SRC_USER="alice-$SUFFIX"
DST_USER="bob-$SUFFIX"
PASS="DemoPass123!"
SEED_BALANCE=${SEED_BALANCE:-10000}
BOB_BALANCE=${BOB_BALANCE:-5000}
TRANSFER_AMOUNT=${TRANSFER_AMOUNT:-50}
COUNT=${COUNT:-10}

RESP=$(mktemp)
trap 'rm -f "$RESP"' EXIT

jget() { python3 -c "import sys,json; print(json.load(sys.stdin)['$1'])"; }

post() {
  local path="$1" data="$2" token="${3:-}"
  if [[ -n "$token" ]]; then
    curl -fsS -X POST \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $token" \
      -d "$data" "$BASE$path"
  else
    curl -fsS -X POST \
      -H "Content-Type: application/json" \
      -d "$data" "$BASE$path"
  fi
}

echo "==> FinTrack 10-transfer demo (suffix=$SUFFIX, base=$BASE)"
echo ""

echo "1/6 Register users (alice + bob)"
post /api/v1/auth/register \
  "$(printf '{"username":"%s","email":"%s@demo.local","password":"%s","firstName":"Alice","lastName":"Demo"}' \
    "$SRC_USER" "$SRC_USER" "$PASS")" >/dev/null
post /api/v1/auth/register \
  "$(printf '{"username":"%s","email":"%s@demo.local","password":"%s","firstName":"Bob","lastName":"Demo"}' \
    "$DST_USER" "$DST_USER" "$PASS")" >/dev/null

echo "2/6 Login — acquire JWTs"
SRC_TOKEN=$(post /api/v1/auth/login \
  "$(printf '{"usernameOrEmail":"%s","password":"%s"}' "$SRC_USER" "$PASS")" | jget accessToken)
DST_TOKEN=$(post /api/v1/auth/login \
  "$(printf '{"usernameOrEmail":"%s","password":"%s"}' "$DST_USER" "$PASS")" | jget accessToken)

echo "3/6 Open USD wallets"
SRC_ACCT=$(post /api/v1/accounts '{"currencyCode":"USD"}' "$SRC_TOKEN" | jget uuid)
DST_ACCT=$(post /api/v1/accounts '{"currencyCode":"USD"}' "$DST_TOKEN" | jget uuid)
echo "   alice wallet : $SRC_ACCT"
echo "   bob wallet   : $DST_ACCT"

echo "4/6 Seed balances — alice=\$$SEED_BALANCE, bob=\$$BOB_BALANCE"
docker exec -i fintrack-mysql-accounts \
  mysql -uroot -prootpass fintrack_accounts \
  -e "UPDATE accounts SET balance=$SEED_BALANCE WHERE uuid='$SRC_ACCT';
      UPDATE accounts SET balance=$BOB_BALANCE WHERE uuid='$DST_ACCT';" 2>/dev/null
docker exec fintrack-redis redis-cli FLUSHDB >/dev/null

echo "5/6 Issue $COUNT x DOMESTIC_TRANSFER of \$$TRANSFER_AMOUNT USD"
ok=0; fail=0
for i in $(seq 1 "$COUNT"); do
  payload=$(printf \
    '{"fromAccountUuid":"%s","toAccountUuid":"%s","amount":%s,"currencyCode":"USD","type":"DOMESTIC_TRANSFER","description":"demo-%d"}' \
    "$SRC_ACCT" "$DST_ACCT" "$TRANSFER_AMOUNT" "$i")
  code=$(curl -sS -o "$RESP" -w "%{http_code}" \
    -X POST \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $SRC_TOKEN" \
    -d "$payload" "$BASE/api/v1/transactions")
  if [[ "$code" =~ ^2 ]]; then
    tx=$(jget uuid <"$RESP" 2>/dev/null || echo "?")
    printf "   #%2d  HTTP %s  tx=%s\n" "$i" "$code" "$tx"
    ok=$((ok+1))
  else
    printf "   #%2d  HTTP %s  FAIL  %s\n" "$i" "$code" "$(tr -d '\n' <"$RESP")"
    fail=$((fail+1))
  fi
done
echo "   result: $ok ok / $fail failed"

echo "6/6 Wait 5s for sagas to settle, then read balances"
sleep 5
docker exec fintrack-redis redis-cli FLUSHDB >/dev/null
src_bal=$(curl -fsS -H "Authorization: Bearer $SRC_TOKEN" "$BASE/api/v1/accounts/$SRC_ACCT/balance" | jget balance)
dst_bal=$(curl -fsS -H "Authorization: Bearer $DST_TOKEN" "$BASE/api/v1/accounts/$DST_ACCT/balance" | jget balance)
echo ""
echo "   $SRC_USER balance: \$$src_bal (started \$$SEED_BALANCE)"
echo "   $DST_USER balance: \$$dst_bal (started \$$BOB_BALANCE)"
echo ""
cat <<SUMMARY
Demo done. Where to look:
  Grafana  http://localhost:3000              — request rates / latencies
  Kibana   http://localhost:5601              — filter on app:transaction-service
  Zipkin   http://localhost:9411              — paste a tx uuid and pull the trace
  Kafdrop  http://localhost:8090              — saga events on fintrack.* topics
  Swagger  http://localhost:8080/swagger-ui.html — explore the full API
SUMMARY
