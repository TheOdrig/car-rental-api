#!/usr/bin/env bash
cd "$(git rev-parse --show-toplevel)"
git config core.hooksPath .githooks
chmod +x .githooks/pre-push scripts/*.sh 2>/dev/null || true
echo "hooks enabled: core.hooksPath=.githooks"
