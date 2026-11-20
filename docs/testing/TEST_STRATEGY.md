# Test strategy

How the Car Rental API is tested: the layers, the infrastructure, the gates that `./mvnw verify` enforces, and where the critical behaviours are covered.

## Principles

- Tests are deterministic. Time comes from an injected `Clock` and tests that need to move time use `MutableClock`.
- Tests run against the real schema: PostgreSQL 18 started by Testcontainers, with Flyway running the real migrations and Hibernate validating the result. The `btree_gist` exclusion constraint and the partial indexes are therefore exercised, which an in-memory database could not do. Docker must be running.
- Behaviour that other modules rely on is tested through the module's `api` types; modules are mocked at that boundary, not at repositories.

## Layers

| Layer | Location | What it covers |
|-------|----------|----------------|
| Unit | `src/test/java/com/akif/<module>/unit` | Services and domain logic with Mockito, no Spring context |
| Integration | `<module>/integration` | Controllers, security, repositories and the event registry in a Spring context on PostgreSQL |
| End to end | `<module>/e2e`, `e2e/edgecases` | Whole flows through MockMvc: rental lifecycle, payment flow, damage, penalties, cancellation and refund |
| Contract and architecture | `contract`, `ModularityTests`, `migration`, `shared` | Public contract, module boundaries, configuration and safety rules |

Test classes are named `<Class>Test` (unit), `<Class>IntegrationTest`, `<Feature>E2ETest` and `<Class>AuthorizationTest`. Method names describe the behaviour and `@DisplayName` states it in English.

## Gates in `./mvnw verify`

| Test | Guards |
|------|--------|
| `ModularityTests` | The exact module set, module boundaries and declared dependencies; writes the Modulith documentation |
| [ArchitectureRulesTest](../../src/test/java/com/akif/ArchitectureRulesTest.java) | ArchUnit prevents web-to-repository and domain-to-internal dependencies, JPA entities in API packages, and `ResponseEntity` use outside web code and the shared exception handler |
| [PaymentModuleTest](../../src/test/java/com/akif/payment/PaymentModuleTest.java), [RentalModuleTest](../../src/test/java/com/akif/rental/RentalModuleTest.java) | Spring Modulith bootstraps each module with its direct dependencies and `shared` |
| `OpenApiContractTest`, `EndpointInventoryTest` | The [OpenAPI contract](../../openapi/openapi.json) and [endpoint inventory](../../openapi/endpoint-inventory.txt) match the application, and the security chain enforces the inventory |
| `ProblemDetailContractTest`, `PageResponseContractTest` | Error and page response shapes |
| `ApiAccessPolicyTest`, `CatalogueSearchAccessPolicyTest` | Path and role rules |
| `ValidationFieldMappingTest` | ArchUnit rule: `@AssertTrue` validation helpers report a field |
| `LogSafetyTest`, `SecretRedactionTest` | No secrets, URLs, presigned values, e-mail addresses or typed login names in logs, and credential records redact themselves |
| `LogbackProfileTest` | `prod` logs at `INFO`; `DEBUG` and `TRACE` loggers sit only inside non-prod profiles |
| `NoSourceCommentsTest`, `NoConfigCommentsTest` | No comments in Java, SQL, properties, XML and HTML resources, scripts, hooks, Dockerfile and Compose |
| `ManagementEndpointsTest` | Actuator on the management port, rejected requests on unexposed paths |
| `FlywaySeedPolicyTest` | Seed data loads in `dev` only |
| `EnvExampleCompletenessTest` | `.env.example` lists every variable the properties reference |
| `ProdConfigurationValidatorTest`, `ProdProfileWiringTest` | Production validation and the real `prod` bean wiring |

After an intentional API change, run `scripts/update-contract.sh`, review the diff of `openapi/` and commit it with the change.

### Coverage gates

The JaCoCo gates are in [pom.xml](../../pom.xml) and fail `verify` when coverage drops below them:

| Scope | Counter | Minimum |
|-------|---------|---------|
| Whole bundle | Line | 80% (`jacoco.line.minimum`) |
| Whole bundle | Branch | 63% (`jacoco.branch.minimum`) |
| `com.akif.payment` | Line | 82% |
| `com.akif.rental` | Line | 88% |
| `com.akif.auth` | Line | 70% |

Reports are written to `target/site/jacoco/`.

## Test infrastructure

| Piece | Purpose |
|-------|---------|
| `TestcontainersConfiguration` | One static `postgres:18-alpine` container per JVM, exposed through a `JdbcConnectionDetails` bean |
| `E2ETestBase` | Base class of the end-to-end tests: `@SpringBootTest` with the `test` profile, MockMvc and a `MutableClock`. It is not transactional; `TestDatabase.truncateAll` empties every `car_rental` table (except Flyway history and `installation_identity`) before and after each test |
| `TestDataBuilder`, `TestFixtures` | Entity builders and shared constants |
| `TestEventCaptor` | Captures application events for assertions |
| `FakeStripeServer` | A local stand-in for the Stripe HTTP API, used to test the real gateway and signed webhooks |

Rules that matter:

- Do not expose the container as a `@ServiceConnection` bean whose destroy method does nothing. Boot closes `Startable` beans when a context shuts down, so the first cached context to close would stop the container for all others, and the rest hang for about 30 s waiting for the event registry.
- Surefire and Failsafe run with `-Duser.timezone=UTC`.
- A test that needs real commits (concurrency, constraints) cannot rely on rollback and must clean up after itself; the end-to-end base already truncates.
- A test that installs a `SecurityContext` must clear it, or it leaks into the next test on the same thread.

## Critical behaviours and where they are tested

Class-level references, which stay valid as methods change. Open the class to see the individual scenarios.

| Behaviour | Tests |
|-----------|-------|
| Rental lifecycle (request, confirm, pickup, return) | `RentalServiceTest`, `RentalLifecycleE2ETest` |
| Cancellation and refunds, early return, `IN_USE` cannot be cancelled | `CancellationRefundE2ETest`, `RentalServiceTest` |
| No double booking, competing requests, overlap constraint | [Database exclusion constraint](../../src/test/java/com/akif/rental/integration/RentalOverlapConstraintTest.java), `BookingIntegrityIntegrationTest`, [competing requests](../../src/test/java/com/akif/e2e/edgecases/DuplicateBookingAndConfirmationE2ETest.java), `DateOverlapE2ETest` |
| Open request cap and request expiry | `OpenRequestCapConcurrencyTest`, `RentalRequestExpirySchedulerTest` |
| Payment flow, payment window, unpaid expiry | `PaymentFlowE2ETest`, `JpyRentalFlowE2ETest`, `PaymentCommittedTransactionsIntegrationTest` |
| Stripe gateway, idempotency keys, reconciliation | `StripePaymentGatewayTest`, `IdempotencyKeyGeneratorTest`, `PaymentReconciliationServiceTest` |
| Webhook signature, replay, refunds | `StripeWebhookSignedIntegrationTest`, `StripeWebhookControllerIntegrationTest` |
| Registration, login, account state | `AuthControllerIntegrationTest`, `AccountStatusIntegrationTest`, `AuthServiceTest` |
| Refresh token rotation and reuse | `RefreshTokenIntegrationTest` |
| Bearer authentication and authorization | `BearerAuthenticationIntegrationTest`, `AuthorizationE2ETest`, `RentalControllerAuthorizationTest` |
| OAuth2 | `OAuth2ControllerIntegrationTest`, `OAuth2ErrorHandlingTest` |
| Rate limiting and client address | `RateLimitIntegrationTest`, `RateLimitServiceTest`, `ClientIpResolverTest`, `RateLimitEndpointClassifierTest` |
| Late-return detection, penalties, waivers | `LateReturnDetectionServiceImplTest`, `PenaltyCalculationServiceImplTest`, `PenaltyWaiverServiceImplTest`, `PenaltyWaiverConcurrencyTest`, `LateReturnPenaltyE2ETest` |
| Damage reports, assessment, disputes, currency totals | `DamageManagementE2ETest`, `DamageDisputeServiceTest`, `DamageStatisticsCurrencyIntegrationTest` |
| Error handling | `ErrorHandlingE2ETest`, `GlobalExceptionHandlerTest`, `ProblemDetailContractTest` |
| Event delivery | [Publication registry](../../src/test/java/com/akif/notification/integration/EventPublicationRegistryIntegrationTest.java), `EmailEventListenerIntegrationTest` |

## Local gate

The project uses a local gate through [verify.sh](../../scripts/verify.sh) or [verify.ps1](../../scripts/verify.ps1). Start Docker before running it.

1. Check tracked paths for local-only and planning files.
2. `scripts/verify.local.sh` or `.ps1`, when a contributor has added one.
3. `scripts/check-artifacts.sh`: `openapi/openapi.json` and `openapi/endpoint-inventory.txt` exist.
4. `./mvnw -B verify`: the whole suite and the coverage gates.

`scripts/setup-hooks.sh` enables `.githooks/pre-push`. When the pushed range touches `src/`, `pom.xml`, `scripts/`, the Dockerfile, the Compose file, `.mvn/` or `openapi/`, it runs the full `scripts/verify.sh`; otherwise it runs steps 1 and 3 plus `LogSafetyTest`, `ModularityTests`, `OpenApiContractTest` and `EndpointInventoryTest`. The hook checks the working tree, not the pushed commit.

`scripts/nightly.sh` (or `.ps1`) runs the full gate and, when `NIGHTLY_DEPENDENCY_CHECK=1`, the dependency scan. It is meant for a local scheduled task. It covers this repository only; the web repository has its own gate and browser tests.

```bash
./mvnw -B verify
./mvnw -B -o test -Dtest=ModularityTests
./mvnw -B -o test -Dtest='OpenApiContractTest,EndpointInventoryTest'
```

| Failure | Cause | Fix |
|---------|-------|-----|
| `ModularityTests` fails | A module boundary or dependency violation | Use the other module's `api` types, or declare the dependency |
| `OpenApiContractTest` fails | An endpoint or DTO changed | Run `scripts/update-contract.sh`, review and commit the diff |
| Tests hang for about 30 s at exit | A cached context outlived the shared container | See the infrastructure rules above |
| Container start errors | Docker is not running | Start Docker |
| A test passes alone and fails in a suite | State leaked between tests, often a `SecurityContext` or a cache | Clear it in the test; the end-to-end base clears caches and the context |

## Checklist for a new module

- `package-info.java` with `@ApplicationModule` and its allowed dependencies, and the module name in `EXPECTED_MODULES` of `ModularityTests`.
- Unit tests for the services and integration tests for the controllers, including the 401, 403, 404 and validation cases.
- If the module adds endpoints: the refreshed `openapi/` files, an access rule in `ApiAccessPolicy`, and a rate-limit rule when the endpoint is expensive or abusable.
- `./mvnw -B verify` green with the coverage gates met.

## Deterministic tests

- **Deterministic calendar**: `MutableClock.pinTo(Instant)` replaces the wall clock for one test (`E2ETestBase` resets it before and after every test); tests that assert prices or refund amounts pin the clock to a fixed date. A pinned clock expires access tokens when advanced, so generate tokens after `advance`. `create_time` is set by JPA auditing from the system clock; set it through SQL when a rule reads the booking date.
- **External service stubs**: `TestExchangeRateInitializer` (registered in `META-INF/spring.factories`) points `exchange-rate.api.base-url` at `FakeExchangeRateServer`, a JVM-wide local stub; `ExchangeRateStubIntegrationTest` fails if a context talks to anything else. `CurrencyConversionE2ETest` replaces the client with a mock, so its fallback assertions never depended on the network. The stub payment gateway keeps intent state per idempotency key across tests; tests that change gateway state directly (`PaymentRepairE2ETest`) advance the payment id sequence first so their keys are unique.
- **Waiting**: `Thread.sleep` is replaced by Awaitility (`during()` for "never happens", `await()` for the truncate retry in `TestDatabase`).
- **Money paths covered end to end**: `PenaltyCollectionE2ETest` (return without scheduler flag, unknown charge outcome confirmed later, waiver of paid and unpaid penalties, declined charge alert, no second charge), `NoShowReleaseE2ETest`, `PaymentRepairE2ETest`, `PaymentRevenueIntegrationTest` (per kind, currency, capture time, refunds), `PaymentCapturedAtMigrationTest`, `CancellationRefundE2ETest` (exact refund amounts including the 30-day discount case) and the damage lifecycle in `DamageManagementE2ETest`.
