#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
./mvnw -B test -Dtest='OpenApiContractTest,EndpointInventoryTest' -Dopenapi.update=true -Dinventory.update=true
git diff --stat -- openapi
