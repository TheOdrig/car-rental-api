# Developer guide

How the Car Rental API is organized into Spring Modulith modules, the rules that keep the boundaries intact, and how to add a module or a cross-module dependency.

## Modules

The application class `CarRentalApplication` is in `com.akif`; every direct subpackage is a module. `ModularityTests` asserts that exactly these modules exist and that `modules.verify()` passes.

| Module | Responsibility | May use (declared in `package-info.java`) |
|--------|----------------|-------------------------------------------|
| `auth` | Users, login, refresh tokens, OAuth2, profile and avatar, admin user management | `shared` |
| `car` | Catalogue, availability search, fleet status, pricing strategies | `shared`, `currency::api` |
| `currency` | Exchange rates and conversion | `shared` |
| `payment` | Payments, Stripe gateway, webhooks, refunds, reconciliation | `shared` |
| `rental` | Rental lifecycle, penalties, late-return detection, payment window | `shared`, `auth::api`, `car::api`, `currency::api`, `payment::api` |
| `damage` | Damage reports, assessment, disputes, charges | `shared`, `auth::api`, `car::api`, `rental::api`, `payment::api` |
| `dashboard` | Admin metrics, alerts, quick actions | `shared`, `car::api`, `damage::api`, `payment::api`, `rental::api` |
| `notification` | E-mail sending, triggered by events | `shared`, `auth::api`, `damage::api`, `payment::api`, `rental::api` |
| `shared` | Cross-cutting code: security, error handling, configuration, base entity, file storage | open module |

The [repository overview](../../README.md#architecture) shows selected dependencies.

## Module layout

```
module/
├── api/            public: service interfaces, DTOs, events, exceptions other modules need
├── domain/         entities and enums that stay inside the module
├── internal/       service implementations, repositories, mappers, internal DTOs, config, schedulers
├── web/            REST controllers
└── package-info.java
```

| Package | Visible to other modules |
|---------|--------------------------|
| `api/` | Yes. It has a `package-info.java` with `@NamedInterface("api")`, which is what an `allowedDependencies = "car::api"` entry refers to |
| `domain/`, `internal/`, `web/` | No |

`notification` has no `api` or `web` package: it is driven by events and exposes only `EmailNotificationService`.

## Rules

1. **Cross-module calls go through `api/`.** Inject the other module's interface (for example `CarCatalog`, `UserLookup`, `PaymentService`), never its repository, entity, `internal` or `web` classes. Verified by `modules.verify()`.
2. **Declare dependencies.** A module may only use the modules listed in its `allowedDependencies`. Verified.
3. **No cycles.** When a port would create a cycle, place it in the module that the other already depends on: the booking-overlap port `CarBookingCalendar` lives in `car.api` and is implemented by `rental`, because `rental` depends on `car`. Verified.
4. **No JPA relations across modules.** Entities reference other modules by id and keep the few display fields they need as copies (a rental stores car brand, model and plate and the user's e-mail, username and name). See [ADR-002](adr/ADR-002-cross-module-entity-strategy.md).
5. **Events carry data, not entities.** Events are immutable records in the publisher's `api` package.
6. **Match the entity lifecycle to its module.** `BaseEntity` provides auditing and soft deletion where used. [RefreshToken](../../src/main/java/com/akif/auth/domain/RefreshToken.java) has its own timestamps and revocation state.
7. **Schema changes only through Flyway migrations.** Hibernate runs with `ddl-auto=validate`; never edit an applied migration.
8. **Time comes from the injected `Clock`.** Do not call `now()` without one.
9. **The shared kernel stays cross-cutting.** Business logic and domain enums belong to a module. See [ADR-001](adr/ADR-001-spring-modulith-over-maven-multimodule.md).
10. **No comments in source.** `NoSourceCommentsTest` fails on new ones.

## Events

Modules that must not call each other synchronously communicate with application events. Publish with `ApplicationEventPublisher`; listen with `@ApplicationModuleListener`, which runs after the publisher's transaction commits, asynchronously and in its own transaction.

Spring Modulith's JPA event publication registry (table `event_publication`, migration `V6`) records each delivery until the listener completes. Completed entries are deleted (`spring.modulith.events.completion-mode=delete`) and outstanding ones are republished on restart (`spring.modulith.events.republish-outstanding-events-on-restart=true`); `EventPublicationRegistryIntegrationTest` covers this. A listener that must not be retried from the registry uses a plain `@TransactionalEventListener` instead: the password-reset e-mail does this because its event carries a secret link.

```java
@Component
class EmailEventListener {

    @ApplicationModuleListener
    public void handlePaymentRetryRequested(PaymentRetryRequestedEvent event) {
        emailNotificationService.sendPaymentRetryRequest(event);
    }
}
```

The listeners are in `EmailEventListener` (notification), `DashboardEventListener` (dashboard) and `PaymentCancelledListener` (rental).

## Adding a module

1. Create `com.akif.<module>` with `api`, `domain`, `internal` and `web` packages.
2. Add `package-info.java`:

   ```java
   @org.springframework.modulith.ApplicationModule(
       allowedDependencies = {"shared"}
   )
   package com.akif.newmodule;
   ```

3. Add `@NamedInterface("api")` in `api/package-info.java`.
4. Add the module name to `EXPECTED_MODULES` in `ModularityTests`.
5. Run `./mvnw -B -o test -Dtest=ModularityTests`.

## Adding a cross-module dependency

1. Add the other module's named interface to `allowedDependencies`, for example `"car::api"`.
2. Inject the public interface:

   ```java
   private final CarCatalog carCatalog;
   ```

3. Run `ModularityTests`. If it reports a cycle, move the port as described in rule 3.

## Paging, sorting and caching conventions

- A handler that takes a `Pageable` is guarded by `PageableGuardInterceptor`: `size` 1..100, `page` >= 0, integers only. List the sortable properties with `@AllowedSort({...})` on the handler; without it the endpoint accepts no client `sort`.
- End every paged query with an `id` tie-breaker: `Pageables.stable(pageable)` for derived queries, `, x.id` in `@Query` text.
- Never mutate an object returned from a `@Cacheable` method; build a copy (records with `toBuilder`) and include the sort in paged cache keys.
- Per-request work that is the same for every row (fleet occupancy, booked ids) is computed once and passed down; add a Hibernate statistics assertion (`hibernate.generate_statistics`) when a new list endpoint touches several tables.
- JSON bodies are capped at `request.max-json-body-bytes` (1 MiB); see [ADR-012](adr/ADR-012-request-bounds-and-query-hygiene.md).

## Verification commands

```bash
./mvnw -B -o test -Dtest=ModularityTests
./mvnw -B -o test -Dtest='OpenApiContractTest,EndpointInventoryTest'
./mvnw -B clean verify
```

`ModularityTests` also writes the Modulith documentation (PlantUML and AsciiDoc) to `target/spring-modulith-docs`. After an intentional API change, run `scripts/update-contract.sh` and commit the regenerated `openapi/` files. See the [test strategy](../testing/TEST_STRATEGY.md) for the test layout and gates.
