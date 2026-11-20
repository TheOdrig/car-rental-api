# ADR-001: Spring Modulith over Maven multi-module

**Status:** Accepted


## Context

The backend needs explicit boundaries between business areas while building and running as one Spring Boot application.

## Decision

Use Spring Modulith in a single Maven artifact. Domain modules declare their dependencies and expose named `api` interfaces for cross-module use. Keep `shared` open for cross-cutting code such as security, UTC helpers, infrastructure and error handling. Domain rules and domain-specific contracts belong to their owning modules. [Shared module declaration](../../../src/main/java/com/akif/shared/package-info.java) · [Developer guide](../DEVELOPER_GUIDE.md)

## Consequences

`ModularityTests` asserts the expected module set, calls `modules.verify()`, and generates module documentation. Both the full local gate and the fast pre-push path run it. Module boundaries are checked by the test suite while the application ships as one artifact. [ModularityTests](../../../src/test/java/com/akif/ModularityTests.java) · [Verification](../../testing/TEST_STRATEGY.md)
