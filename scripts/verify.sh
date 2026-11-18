#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
bash scripts/check-no-ai-files.sh
PY=$(command -v python3 || command -v python || true)
LINT="$HOME/.claude/skills/repo-docs-wiki/scripts/lint_docs_wiki.py"
if [ -d docs ] && [ -f "$LINT" ] && [ -n "$PY" ]; then "$PY" -I "$LINT" . || true; else echo "verify: docs lint skipped"; fi
for artifact in openapi/openapi.json openapi/endpoint-inventory.txt; do
  if [ ! -f "$artifact" ]; then
    echo "verify: missing $artifact; run scripts/update-contract.sh and commit the result" >&2
    exit 1
  fi
done
./mvnw -B verify
echo "verify: OK"
