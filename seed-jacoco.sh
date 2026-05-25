#!/bin/bash
set -e

REPO=/Users/latifbenzzine/fintrack

echo "Checking we are on dev branch..."
CURRENT=$(git branch --show-current)
if [ "$CURRENT" != "dev" ]; then
  echo "❌ Must be on dev branch."
  exit 1
fi

git fetch origin
git checkout gh-pages
git pull origin gh-pages --rebase

# Copy per-service jacoco XML and HTML reports
for svc in account-service transaction-service user-service notification-service api-gateway; do
  mkdir -p dev/jacoco/$svc
  cp -r $REPO/java17/$svc/target/site/jacoco/. dev/jacoco/$svc/
  echo "  ✅ $svc"
done

git add dev/jacoco/
git diff --cached --quiet && echo "Nothing to commit" || \
  git commit -m "docs: seed JaCoCo per-service reports"
git push origin gh-pages
git checkout dev

echo "Done!"
echo "https://lbenzzine-ai.github.io/fintrack-microservices-platform/dev/jacoco/index.html"
