#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
bash scripts/verify.sh
if [ "${NIGHTLY_DEPENDENCY_CHECK:-0}" = "1" ]; then
  bash scripts/dependency-check.sh
fi
echo "nightly: OK"
