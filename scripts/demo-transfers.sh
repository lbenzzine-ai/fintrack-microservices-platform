docker logs fintrack-logstash 2>&1 | grep -i error#!/usr/bin/env bash
# 10-transfer demo.
#
# Registers two users via api-gateway, opens a USD wallet for each, seeds
# the source wallet with a balance directly in MySQL (no public credit
# endpoint), then issues 10 DOMESTIC_TRANSFER calls and reports final
# balances. Use it to generate end-to-end traffic across the stack so
# logs (Kibana), metrics (Grafana / Prometheus) and traces (Zipkin)
# light up.
#
# Usage: ./scripts/demo-transfers.sh

set -euo pipefail

BASE=${BASE:-http://localhost:8080}
SUFFIX=${SUFFIX:-$(date +%s)}
SRC_USER="alice-$SUFFIX"
DST_USER="bob-$SUFFIX"
PASS="DemoPass123!"
SEED_BALANCE=${SEED_BALANCE:-10000}
TRANSFER_AMOUNT=${TRANSFER_AMOUNT:-50}
COUNT=${COUNT:-10}
RESP=$(mktemp)
trap 'rm -f "$RESP"' EXIT

jget() { python3 -c "import sys,json; print(json.load(sys.stdin)['$1'])"; }

post() {
  local path="$1" data="$2" token="${3:-}"
  if [[ -n "$token" ]]; then
    curl -fsS -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $token" -d "$data" "$BASE$path"
  else
    curl -fsS -X POST -H "Content-Type: application/json" -d "$data" "$BASE$path"
  fi
}

echo "==> 10-transfer demo (suffix=$SUFFIX, base=$BASE)"

echo "1/6 Register users"
post /api/v1/auth/register \
  "$(printf '{"username":"%s","email":"%s@demo.local","password":"%s","firstName":"Alice","lastName":"Demo"}' "$SRC_USER" "$SRC_USER" "$PASS")" \
  >/dev/null
post /api/v1/auth/register \
  "$(printf '{"username":"%s","email":"%s@demo.local","password":"%s","firstName":"Bob","lastName":"Demo"}' "$DST_USER" "$DST_USER" "$PASS")" \
  >/dev/null

echo "2/6 Login (acquire JWTs)"
SRC_TOKEN=$(post /api/v1/auth/login "$(printf '{"usernameOrEmail":"%s","password":"%s"}' "$SRC_USER" "$PASS")" | jget accessToken)
DST_TOKEN=$(post /api/v1/auth/login "$(printf '{"usernameOrEmail":"%s","password":"%s"}' "$DST_USER" "$PASS")" | jget accessToken)

echo "3/6 Open wallets (idempotent — saga may have already created them)"
SRC_ACCT=$(post /api/v1/accounts '{"currencyCode":"USD"}' "$SRC_TOKEN" | jget uuid)
DST_ACCT=$(post /api/v1/accounts '{"currencyCode":"USD"}' "$DST_TOKEN" | jget uuid)
echo "   src wallet: $SRC_ACCT"
echo "   dst wallet: $DST_ACCT"

echo "4/6 Seed source balance = $SEED_BALANCE via direct UPDATE on fintrack_accounts"
docker exec -i fintrack-mysql-accounts \
  mysql -uroot -prootpass fintrack_accounts \
  -e "UPDATE accounts SET balance=$SEED_BALANCE WHERE uuid='$SRC_ACCT';" 2>/dev/null
docker exec fintrack-redis redis-cli FLUSHDB >/dev/null

echo "5/6 Issue $COUNT × DOMESTIC_TRANSFER of $TRANSFER_AMOUNT USD"
ok=0; fail=0
for i in $(seq 1 "$COUNT"); do
  payload=$(printf '{"fromAccountUuid":"%s","toAccountUuid":"%s","amount":%s,"currencyCode":"USD","type":"DOMESTIC_TRANSFER","description":"demo-%d"}' \
    "$SRC_ACCT" "$DST_ACCT" "$TRANSFER_AMOUNT" "$i")
  code=$(curl -sS -o "$RESP" -w "%{http_code}" \
    -X POST -H "Content-Type: application/json" \
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
echo "   $SRC_USER balance: $src_bal (started $SEED_BALANCE)"
echo "   $DST_USER balance: $dst_bal (started 0)"

cat <<EOF

Demo done. Where to look:
  Grafana  http://localhost:3000   — service request rates / latencies
  Kibana   http://localhost:5601   — filter on app:transaction-service or app:account-service
  Zipkin   http://localhost:9411   — pick a tx uuid and pull the trace
  Kafdrop  http://localhost:8090   — saga events on fintrack.* topics
EOF
