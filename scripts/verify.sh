#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
bash scripts/check-no-ai-files.sh
if [ -f scripts/verify.local.sh ]; then
  bash scripts/verify.local.sh
else
  echo "local checks: none"
fi
bash scripts/check-artifacts.sh
./mvnw -B verify
echo "verify: OK"
