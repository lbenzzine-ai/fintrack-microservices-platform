#!/usr/bin/env bash
# Stop services launched by run-stack.sh. Walks PIDs in reverse dependency
# order so downstream services see upstream go away cleanly.
#
# Usage: ./scripts/stop-stack.sh [java17|java21]
#   With no arg, stops both stacks if state dirs exist.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# reverse of run-stack.sh order
SERVICES_REVERSED=(
  api-gateway
  notification-service
  transaction-service
  account-service
  user-service
  eureka-server
  config-server
)

stop_one() {
  local stack="$1" name="$2"
  local pidfile="$ROOT/.run-stack/$stack/$name.pid"
  [[ -f "$pidfile" ]] || return 0
  local pid
  pid=$(cat "$pidfile")
  if kill -0 "$pid" 2>/dev/null; then
    echo "  stopping $stack/$name (pid $pid)"
    kill "$pid" 2>/dev/null || true
    # wait up to 20s for graceful shutdown
    for _ in $(seq 1 20); do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    if kill -0 "$pid" 2>/dev/null; then
      echo "    force-killing $pid"
      kill -9 "$pid" 2>/dev/null || true
    fi
  fi
  rm -f "$pidfile"
}

stop_stack() {
  local stack="$1"
  [[ -d "$ROOT/.run-stack/$stack" ]] || return 0
  echo "Stopping stack: $stack"
  for name in "${SERVICES_REVERSED[@]}"; do
    stop_one "$stack" "$name"
  done
}

if [[ $# -ge 1 ]]; then
  stop_stack "$1"
else
  stop_stack java17
  stop_stack java21
fi
