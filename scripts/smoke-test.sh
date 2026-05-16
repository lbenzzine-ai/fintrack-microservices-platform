#!/usr/bin/env bash
# Quick "is everything alive?" check for a freshly-started stack.
set -euo pipefail

BASE=${BASE:-http://localhost}

check() {
  local label="$1" url="$2"
  if curl -fsS --max-time 5 "$url" >/dev/null; then
    printf "  %-28s %s\n" "$label" "OK"
  else
    printf "  %-28s %s\n" "$label" "FAIL ($url)"
    return 1
  fi
}

fail=0
echo "FinTrack smoke-test:"

check "config-server health"        "$BASE:8888/actuator/health"        || fail=1
check "eureka-server health"        "$BASE:8761/actuator/health"        || fail=1
check "api-gateway health"          "$BASE:8080/actuator/health"        || fail=1
check "user-service health"         "$BASE:8081/actuator/health"        || fail=1
check "account-service health"      "$BASE:8082/actuator/health"        || fail=1
check "transaction-service health"  "$BASE:8083/actuator/health"        || fail=1
check "notification-service health" "$BASE:8084/actuator/health"        || fail=1
check "kafdrop ui"                  "$BASE:8090/"                       || fail=1
check "prometheus ui"               "$BASE:9090/-/healthy"              || fail=1
check "grafana ui"                  "$BASE:3000/login"                  || fail=1
check "zipkin ui"                   "$BASE:9411/health"                 || fail=1
check "kibana status"               "$BASE:5601/api/status"             || fail=1

if [[ $fail -ne 0 ]]; then
  echo "Some checks failed. Re-run after docker-compose has fully come up."
  exit 1
fi
echo "All services responsive."
