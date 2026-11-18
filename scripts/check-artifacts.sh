#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
for artifact in openapi/openapi.json openapi/endpoint-inventory.txt; do
  if [ ! -f "$artifact" ]; then
    echo "verify: missing $artifact; run scripts/update-contract.sh and commit the result" >&2
    exit 1
  fi
done
echo "check-artifacts: OK"
