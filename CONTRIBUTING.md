# Contributing

Thank you for considering a contribution to the Car Rental API. This page covers how to set up, what the project expects of a change, and how to verify it.

## Set up

You need Java 21 and Docker (Compose v2). Maven is provided by the wrapper (`./mvnw`).

```bash
git clone https://github.com/TheOdrig/car-rental-api.git
cd car-rental-api
docker compose up --build
```

This starts PostgreSQL 18 and the API with the `dev` profile, loads sample data and needs no `.env` file. The API is on `http://localhost:8082/api/v1`, Swagger UI on `http://localhost:8082/swagger-ui/index.html` and health on `http://localhost:8081/actuator/health`. To run from Maven instead, start only the database and point the application at it:

```bash
docker compose up -d db
DATABASE_URL=jdbc:postgresql://localhost:5433/car_rental SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

The [README](README.md) describes the stack and architecture. The [environment template](.env.example) lists the supported variables.

## What a change should respect

- **Module boundaries.** The code is a Spring Modulith application. A module may call another only through the other module's `api` package and only if its `package-info.java` declares the dependency. `ModularityTests` fails on a violation.
- **Schema changes use Flyway migrations.** Add a new `V<n>__description.sql`; never edit an applied migration. Hibernate runs with `ddl-auto=validate`.
- **Public API changes update the contract.** After an intentional change to an endpoint, DTO or error code, run `scripts/update-contract.sh` and commit the regenerated `openapi/openapi.json` and `openapi/endpoint-inventory.txt`. New rate-limited endpoints need an entry in `RateLimitEndpointClassifier`, and a new error code needs an `ErrorCode` entry.
- **Tests come with behaviour changes.** Prefer a test that fails first. Tests run against a real PostgreSQL started by Testcontainers, so Docker must be running.
- **No comments in source.** `NoSourceCommentsTest` (Java) and `NoConfigCommentsTest` (SQL, properties, XML and HTML resources, scripts, hooks, Dockerfile, Compose) fail on new ones; express intent through names and tests. `.env.example` is the one place with documented lines.
- **Time and money.** Use the injected `Clock`, never `now()` without one, and round amounts with the currency's decimal places.
- **Secrets and payments.** Never commit secrets or tokens. Stripe is used in test mode only; the `prod` profile rejects live keys.
- **Local-only files.** Keep local notes and planning files out of tracked changes.

## Verify

```bash
bash scripts/verify.sh
```

On Windows use `scripts/verify.ps1`. This is the full gate: the tracked-file guard, the OpenAPI artifact check, and `./mvnw -B verify` with the coverage floors. It takes several minutes. For a quicker loop run a single class, for example `./mvnw -B -o test -Dtest=ModularityTests`. `scripts/setup-hooks.sh` enables a pre-push hook that runs a faster subset. There is no hosted CI, so the local gate is the check.

## Commit messages

Commits follow [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <description>`.

| Type | Use for |
|------|---------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `refactor` | A change that neither fixes a bug nor adds a feature |
| `perf` | A performance improvement |
| `test` | Adding or correcting tests |
| `docs` | Documentation only |
| `build`, `chore` | Build, tooling and maintenance |

The scope is usually a module, for example `fix(payment): bound refund lock wait`. Mark a breaking change to the HTTP contract with `!`, for example `fix(rental)!: reject cancel of in-use rentals`, and include the regenerated `openapi/` files in the same commit.

## Pull requests

Open a pull request against `main` with a description of what changed and why, and confirm that `scripts/verify.sh` passes locally.

## Reporting issues

For a bug, include the version or commit, your environment, the steps to reproduce, the expected and the actual behaviour, and relevant log lines (without secrets). For a feature, describe the problem it solves and the approach you have in mind. For a security problem, follow the [private reporting policy](SECURITY.md).
