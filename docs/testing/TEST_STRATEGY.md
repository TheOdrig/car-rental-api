# Test Strategy

## Table of Contents
1. [Test Philosophy](#1-test-philosophy)
2. [Test Pyramid](#2-test-pyramid)
3. [Coverage Targets](#3-coverage-targets)
4. [Testing Tools & Frameworks](#4-testing-tools--frameworks)
5. [Test Directory Structure](#5-test-directory-structure)
6. [Naming Conventions](#6-naming-conventions)
7. [Module Coverage Matrix (Baseline)](#7-module-coverage-matrix-baseline)
8. [Test Data Management](#8-test-data-management)
9. [Modularity Testing](#9-modularity-testing)
10. [CI/CD Integration](#10-cicd-integration)
11. [New Module Checklist](#11-new-module-checklist)

## 1. Test Philosophy
This project is built on the **"Test-Early, Test-Often"** principle. Every piece of code must be tested to verify business logic and prevent regressions during future changes.

**Core Principles:**
- **Reliability:** Tests must be deterministic. "Flaky" tests are not tolerated.
- **Speed:** Unit tests should run in milliseconds. Integration and E2E tests should complete within a reasonable timeframe.
- **Independence:** Tests should be isolated and should not depend on the state left by other tests.

## 2. Test Pyramid
The project follows the classic **Test Pyramid** model for test distribution:

| Test Type | Target Ratio | Description |
| :--- | :--- | :--- |
| **Unit Tests** | **70%** | Tests business logic in isolation using Mockito/JUnit. |
| **Integration Tests** | **20%** | Tests intra-module components and Database/API integrations. |
| **E2E Tests** | **10%** | Tests critical business flows (e.g., Rental lifecycle) from end to end. |

## 3. Coverage Targets
To ensure project sustainability, the following minimum coverage targets are established:

| Scope | Target | Description |
| :--- | :--- | :--- |
| **Overall Instruction** | **80%** | Minimum overall target for the entire project. |
| **Critical Paths** | **95%** | Key business flows in Rental, Payment, and Auth modules. |
| **Branch Coverage** | **80%** | Coverage ratio for logical branches (if/else). |
| **New Code** | **80%** | Mandatory minimum for any newly added code. |

> **Note:** As of January 2026, our Branch Coverage is **55.17%**. This is identified as the primary technical debt and must be prioritized for improvement to reach the 80% target.

## 4. Testing Tools & Frameworks
The following tools and frameworks are used for testing in this project:

| Tool | Version | Purpose |
| :--- | :--- | :--- |
| **JUnit 5** | 5.10.x | Core testing framework for writing and running tests. |
| **Mockito** | 5.x | Mocking framework for creating test doubles and verifying interactions. |
| **AssertJ** | 3.x | Fluent assertion library for readable test assertions. |
| **Spring Boot Test** | 3.5.6 | Integration testing support with `@SpringBootTest`. |
| **H2 Database** | 2.x | In-memory database for fast integration tests. |
| **Awaitility** | 4.2.0 | Asynchronous testing support for event-driven flows. |
| **Spring Modulith Test** | 1.3.0 | Module boundary verification and architecture testing. |
| **Spring Security Test** | 6.x | Security context testing and authentication mocking. |
| **JaCoCo** | 0.8.12 | Code coverage measurement and reporting. |

## 5. Test Directory Structure
Tests are organized following the Spring Modulith architecture with clear separation by test type:

```
src/test/java/com/akif/
├── ModularityTests.java          # Spring Modulith module boundary verification
├── {module}/                     # Module-specific tests (structure varies by module)
│   ├── unit/                     # Pure unit tests with mocks (all modules)
│   ├── integration/              # @SpringBootTest integration tests (all modules)
│   ├── e2e/                      # Full flow E2E tests (most modules)
│   ├── scheduler/                # Scheduler tests (rental module only)
│   └── webhook/                  # Webhook tests (payment module only)
├── e2e/
│   ├── infrastructure/           # Shared E2E test utilities
│   │   ├── E2ETestBase.java      # Base class for all E2E tests
│   │   ├── TestFixtures.java     # Static test data constants
│   │   ├── TestDataBuilder.java  # Fluent builder for test entities
│   │   └── TestEventCaptor.java  # Captures async events for verification
│   └── edgecases/                # Cross-cutting edge case tests
└── shared/                       # Shared test utilities
```

> **Note:** Not all modules have the same subdirectory structure. The `unit/` and `integration/` directories are standard across all modules, while `e2e/`, `scheduler/`, and `webhook/` are module-specific.

### Module List
The project contains **9 business modules** plus shared infrastructure:

| Module | Description |
| :--- | :--- |
| `auth` | Authentication and authorization (JWT, OAuth2) |
| `car` | Car inventory and availability management |
| `currency` | Multi-currency support and exchange rates |
| `damage` | Damage reporting and assessment |
| `dashboard` | Admin dashboard and analytics |
| `notification` | Email and notification services |
| `payment` | Stripe payment processing |
| `rental` | Core rental lifecycle management |
| `shared` | Cross-cutting concerns and utilities |

## 6. Naming Conventions
Consistent naming ensures readability and maintainability across the test codebase.

### Test Class Naming
| Test Type | Pattern | Example |
| :--- | :--- | :--- |
| Unit Test | `{ClassName}Test` | `RentalServiceTest` |
| Integration Test | `{ClassName}IntegrationTest` | `RentalControllerIntegrationTest` |
| E2E Test | `{Feature}E2ETest` | `RentalLifecycleE2ETest` |
| Authorization Test | `{ClassName}AuthorizationTest` | `CarControllerAuthorizationTest` |

### Test Method Naming
Use the `should{Action}When{Condition}` pattern:

```java
@Test
@DisplayName("Should throw exception when rental dates overlap")
void shouldThrowExceptionWhenRentalDatesOverlap() {
    // test implementation
}
```

### @DisplayName Usage
- Always write in **English**
- Be **descriptive** and explain the expected behavior
- Use **business terminology** where appropriate

## 7. Module Coverage Matrix (Baseline - January 2026)
The following table shows the current test coverage status measured by JaCoCo:

### Overall Project Metrics
| Metric | Current | Target | Gap | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Instruction Coverage** | 74.68% | 80% | -5.32% | ❌ |
| **Branch Coverage** | 55.17% | 80% | -24.83% | 🚨 |
| **Line Coverage** | 75.45% | 80% | -4.55% | ❌ |
| **Method Coverage** | 78.74% | 85% | -6.26% | ⚠️ |
| **Missed Classes** | 20 | 0 | +20 | ❌ |
| **Covered Classes** | 248 | — | — | — |

### Critical Findings
1. **Branch Coverage is the #1 priority.** At 55.17%, nearly half of all logical branches are untested.
2. **20 classes have zero coverage.** These need immediate attention or removal if unused.
3. **Critical modules (rental, payment, auth)** require focused testing to reach the 95% critical path target.

### Priority Actions
- [ ] Identify and address the 20 missed classes
- [ ] Add edge case tests to improve branch coverage
- [ ] Focus on error handling paths in Payment and Rental modules

## 8. Test Data Management
Consistent and maintainable test data is crucial for reliable tests. This project uses two main patterns for test data creation.

### TestFixtures Pattern
`TestFixtures` provides static constants for commonly used test values. Located at `e2e/infrastructure/TestFixtures.java`:

```java
// Example usage
import static com.akif.e2e.infrastructure.TestFixtures.*;

@Test
void shouldCreateRentalWithDefaultDates() {
    RentalRequest request = new RentalRequest(carId, FUTURE_START, FUTURE_END, "Test");
    // FUTURE_START = LocalDate.now().plusDays(1)
    // FUTURE_END = LocalDate.now().plusDays(5)
}
```

**Available Constants:**
| Category | Constants |
| :--- | :--- |
| Pricing | `BASE_PRICE`, `DEFAULT_CURRENCY` |
| Dates | `FUTURE_START`, `FUTURE_END`, `EARLY_BOOKING_START/END`, `LONG_DURATION_START/END` |
| Users | `TEST_USER_USERNAME`, `TEST_USER_EMAIL`, `TEST_ADMIN_USERNAME` |
| Cars | `TEST_CAR_BRAND`, `TEST_CAR_MODEL`, `TEST_LICENSE_PLATE_PREFIX` |

### TestDataBuilder Pattern
`TestDataBuilder` provides fluent methods for creating domain entities. Located at `e2e/infrastructure/TestDataBuilder.java`:

```java
// Example: Create a test user
User user = TestDataBuilder.createTestUser();
User admin = TestDataBuilder.createTestAdmin();

// Example: Create an available car
Car car = TestDataBuilder.createAvailableCar();

// Example: Create a rental with specific status
Rental rental = TestDataBuilder.createConfirmedRental(car, user);
```

### @Transactional for Test Isolation
All E2E tests extend `E2ETestBase` which is annotated with `@Transactional`. This ensures:
- Each test runs in its own transaction
- All database changes are **rolled back** after each test
- Tests are **isolated** and do not affect each other

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // <-- Automatic rollback after each test
public abstract class E2ETestBase {
    // ...
}
```

### H2 Test Database Configuration
The test profile uses an in-memory H2 database configured in `application-test.properties`:

```properties
# In-memory H2 database
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver

# Schema created automatically (no Flyway)
spring.jpa.hibernate.ddl-auto=create
spring.flyway.enabled=false
```

**Key Points:**
- `DB_CLOSE_DELAY=-1`: Keeps database alive between connections within same test
- `ddl-auto=create`: Hibernate creates schema from entities
- `flyway.enabled=false`: Production migrations are skipped in tests

## 9. Modularity Testing
Spring Modulith enforces module boundaries at test time. This prevents architectural erosion by failing tests when boundaries are violated.

### ModularityTests Class
Located at `src/test/java/com/akif/ModularityTests.java`:

```java
class ModularityTests {
    private final ApplicationModules modules = ApplicationModules.of(CarGalleryProjectApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();  // Fails if module boundaries are violated
    }

    @Test
    void createsModuleDocumentation() {
        new Documenter(modules)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml();
    }
}
```

### What `modules.verify()` Checks
| Check | Description |
| :--- | :--- |
| **Module Boundaries** | Cross-module access only via public APIs (`api/` package) |
| **Circular Dependencies** | No A→B→A dependency cycles |
| **Internal Access** | `internal/` packages are not accessible from other modules |
| **Repository Encapsulation** | Repositories stay within their owning module |

### package-info.java for Module Dependencies
Each module declares its allowed dependencies in `package-info.java`:

```java
// src/main/java/com/akif/rental/package-info.java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"car", "auth", "currency", "payment", "shared"}
)
package com.akif.rental;
```

**When to Update:**
- Adding a new module dependency
- Refactoring cross-module calls
- Exposing new public APIs

## 10. CI/CD Integration
Tests are integrated into the CI/CD pipeline via GitHub Actions.

### Workflow: `modulith-verify.yml`
**Triggers:** Push to `main`, `develop`, `refactor/**` branches and PRs to `main` or `develop`.

**Steps:**
1. **Verify Module Structure** - Runs `ModularityTests`
2. **Check Shared Kernel Size** - Counts classes in `com.akif.shared`
3. **Upload Test Results** - Stores test reports as artifacts

### Shared Kernel Thresholds
| Status | Threshold | Action |
| :--- | :--- | :--- |
| ✅ Target | ≤ 10 classes | All good |
| ⚠️ Warning | > 15 classes | Review needed |
| ❌ Critical | > 25 classes | Build fails |

### Maven Commands for Local Testing

```bash
# Run all tests
mvn test

# Run only ModularityTests
mvn test -Dtest=ModularityTests

# Run tests with coverage report
mvn test jacoco:report

# Run specific module tests
mvn test -Dtest="com.akif.rental.**"
```

### JaCoCo Coverage Reports
After running `mvn test`, JaCoCo generates reports in:

| Format | Location |
| :--- | :--- |
| HTML | `target/site/jacoco/index.html` |
| XML | `target/site/jacoco/jacoco.xml` |
| CSV | `target/site/jacoco/jacoco.csv` |

### Troubleshooting CI Failures

| Failure | Cause | Solution |
| :--- | :--- | :--- |
| ModularityTests fails | Module boundary violation | Check for cross-module internal access |
| Shared kernel size exceeds 25 | Too many shared classes | Refactor classes to appropriate modules |
| Tests timeout | Slow E2E tests | Check for unnecessary `@SpringBootTest` usage |
| H2 schema error | Entity mismatch | Verify entity annotations match H2 dialect |

## 11. New Module Checklist
When adding a new module to the project, use this checklist to ensure proper test coverage:

### Pre-Implementation
- [ ] Create `package-info.java` with `@ApplicationModule` annotation
- [ ] Define allowed dependencies in `allowedDependencies` attribute
- [ ] Create test directory structure: `{module}/unit/`, `{module}/integration/`

### Unit Tests
- [ ] Create `{ServiceName}Test.java` in `unit/` package
- [ ] Test all public methods with happy path scenarios
- [ ] Test validation and business rule violations
- [ ] Mock all external dependencies (repositories, other services)
- [ ] Achieve **minimum 80%** coverage for service classes

### Integration Tests
- [ ] Create `{ControllerName}IntegrationTest.java` in `integration/` package
- [ ] Test all API endpoints with valid requests
- [ ] Test authentication and authorization requirements
- [ ] Test error responses (400, 401, 403, 404, 500)
- [ ] Use `@ActiveProfiles("test")` for H2 database

### Modularity Verification
- [ ] Run `mvn test -Dtest=ModularityTests` locally
- [ ] Verify no module boundary violations
- [ ] Confirm public APIs are in `api/` package
- [ ] Confirm internal classes are in `internal/` package

### Coverage Verification
- [ ] Run `mvn test jacoco:report`
- [ ] Check coverage in `target/site/jacoco/index.html`
- [ ] Verify instruction coverage ≥ 80%
- [ ] Verify branch coverage ≥ 70% (target 80%)

### Documentation
- [ ] Add module to the Module List in this document
- [ ] Update test count if significantly changed

---

**Last Updated:** January 2026