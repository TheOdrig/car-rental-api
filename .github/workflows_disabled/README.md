# Disabled workflows

Hosted CI is intentionally off for this repository. The gate is local: `scripts/verify.sh` (or `scripts/verify.ps1`) runs the full verification, and the `.githooks/pre-push` hook runs a faster subset before every push. See the root `README.md` for what each runs.

`modulith-verify.yml` is a historical workflow kept for reference only. It targets JDK 17 and the `develop` branch and runs `mvn test -Dtest=ModularityTests` plus a shared-kernel size check, so it no longer matches the project (Java 21, Maven wrapper, `ModularityTests` inside `./mvnw verify`). Do not enable it unchanged.
