#!/bin/bash
set -e

echo "Checking we are on dev branch..."
CURRENT=$(git branch --show-current)
if [ "$CURRENT" != "dev" ]; then
  echo "❌ Must be on dev branch. Currently on: $CURRENT"
  exit 1
fi

mkdir -p /tmp/pit-reports/account-service /tmp/pit-reports/transaction-service \
         /tmp/pit-reports/user-service /tmp/pit-reports/notification-service \
         /tmp/pit-reports/api-gateway

for svc in account-service transaction-service user-service notification-service api-gateway; do
  latest=$(ls -d java17/$svc/target/pit-reports/*/ 2>/dev/null | tail -1)
  if [ -n "$latest" ] && [ -f "${latest}mutations.xml" ]; then
    cp "${latest}mutations.xml" /tmp/pit-reports/$svc/mutations.xml
    echo "  ✅ $svc"
  else
    echo "  ⚠️  $svc — skipped"
  fi
done

git fetch origin
git checkout gh-pages

for svc in account-service transaction-service user-service notification-service api-gateway; do
  mkdir -p dev/pit/$svc
  [ -f "/tmp/pit-reports/$svc/mutations.xml" ] && \
    cp /tmp/pit-reports/$svc/mutations.xml dev/pit/$svc/mutations.xml
done

git add dev/pit/
git diff --cached --quiet && echo "Nothing to commit" || \
  git commit -m "docs: seed PIT mutation XML reports for dashboard"
git push origin gh-pages
git checkout dev

echo "Done! https://lbenzzine-ai.github.io/fintrack-microservices-platform/dev/pit/index.html"
