#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
pattern='(^|/)(\.kiro|kiro\.steering|\.agent|\.agents|\.claude|\.codex|\.cursor|\.windsurf|\.obsidian|specs|steering|\.gemini)/|^(notes|prompts)/|(^|/)\.clinerules(/|$)|^\.github/instructions/|(^|/)\.aider[^/]*$|(^|/)(AGENTS|CLAUDE|GEMINI)\.md$|(^|/)\.(cursorrules|windsurfrules)$|(^|/)\.github/copilot-instructions\.md$|Zone\.Identifier$'
bad=$(git ls-files | grep -iE "$pattern" || true)
if [ -n "$bad" ]; then
  echo "check-no-ai-files: tracked files that must stay local-only:" >&2
  echo "$bad" >&2
  exit 1
fi
echo "check-no-ai-files: OK"
