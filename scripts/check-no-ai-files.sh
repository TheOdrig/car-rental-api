#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
bad=$(git ls-files | grep -E '(^|/)(\.kiro|kiro\.steering|\.agent|\.agents|\.claude|\.codex|\.obsidian|specs)/|(^|/)(AGENTS|CLAUDE)\.md$|^docs/|^(CHANGELOG|CONTRIBUTING)\.md$|Zone\.Identifier$' || true)
if [ -n "$bad" ]; then
  echo "check-no-ai-files: tracked files that must stay local-only:" >&2
  echo "$bad" >&2
  exit 1
fi
echo "check-no-ai-files: OK"
