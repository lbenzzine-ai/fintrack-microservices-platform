#!/usr/bin/env bash
# Launch a FinTrack stack (java17 or java21) on the host.
# Sources .env so KAFKA_BOOTSTRAP_SERVERS=localhost:29092 (and other vars)
# reach the JVMs — config-server serves placeholders unresolved, so each
# Spring Boot service must resolve them from its own environment.
#
# Usage: ./scripts/run-stack.sh <java17|java21>

set -euo pipefail

STACK="${1:-}"
if [[ "$STACK" != "java17" && "$STACK" != "java21" ]]; then
  echo "usage: $0 <java17|java21>" >&2
  exit 2
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STACK_DIR="$ROOT/$STACK"
STATE_DIR="$ROOT/.run-stack/$STACK"
LOG_DIR="$ROOT/logs/$STACK"
ENV_FILE="$ROOT/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "missing $ENV_FILE — copy .env.example and fill it in" >&2
  exit 1
fi

mkdir -p "$STATE_DIR" "$LOG_DIR"

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

echo "Loaded env from $ENV_FILE"
echo "  KAFKA_BOOTSTRAP_SERVERS=$KAFKA_BOOTSTRAP_SERVERS"
echo "  EUREKA_SERVER_URL=$EUREKA_SERVER_URL"
echo

# service: port (probed via /actuator/health)
SERVICES=(
  "config-server:8888"
  "eureka-server:8761"
  "user-service:8081"
  "account-service:8082"
  "transaction-service:8083"
  "notification-service:8084"
  "api-gateway:8080"
)

wait_healthy() {
  local name="$1" port="$2" timeout="${3:-120}" elapsed=0
  printf "  waiting for %s on :%s " "$name" "$port"
  while (( elapsed < timeout )); do
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "http://localhost:$port/actuator/health" || echo "000")
    if [[ "$code" == "200" ]]; then
      echo " UP (${elapsed}s)"
      return 0
    fi
    printf "."
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo " TIMEOUT after ${timeout}s" >&2
  return 1
}

already_up() {
  local port="$1" code
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 1 "http://localhost:$port/actuator/health" || echo "000")
  [[ "$code" == "200" ]]
}

start_service() {
  local name="$1" port="$2"
  local pidfile="$STATE_DIR/$name.pid"
  local logfile="$LOG_DIR/$name.out"

  if already_up "$port"; then
    echo "  $name already healthy on :$port — skipping"
    return 0
  fi

  if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
    echo "  $name pid $(cat "$pidfile") already running — waiting"
  else
    echo "  starting $name (log: $logfile)"
    (
      cd "$STACK_DIR/$name"
      nohup mvn -q spring-boot:run >> "$logfile" 2>&1 &
      echo $! > "$pidfile"
    )
  fi

  wait_healthy "$name" "$port" 180
}

echo "Starting stack: $STACK"
for entry in "${SERVICES[@]}"; do
  IFS=':' read -r name port <<< "$entry"
  start_service "$name" "$port"
done

echo
echo "All services up. PIDs in $STATE_DIR, logs in $LOG_DIR"
