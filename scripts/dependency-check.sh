#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
if [ -z "${NVD_API_KEY:-}" ]; then
  echo "dependency-check: NVD_API_KEY is not set; the first run downloads the whole NVD database and is very slow without a key" >&2
fi
./mvnw -B -Pdependency-check -DskipTests org.owasp:dependency-check-maven:12.2.2:check
echo "dependency-check: OK"
