# Requirements Document

## Introduction

Bu spec, Car Rental API projesinin test dokümantasyonunu oluşturmayı hedefler. Proje şu anda 800+ test içermesine rağmen, test stratejisi, kritik senaryolar ve coverage hedefleri dokümante edilmemiştir. Bu dokümantasyon, yeni geliştiricilerin test yaklaşımını anlamasını ve tutarlı test yazmasını sağlayacaktır.

## Glossary

- **Test_Strategy_Document**: Test piramidi, coverage hedefleri, test araçları ve test türlerini tanımlayan ana dokümantasyon dosyası
- **Critical_Scenarios_Document**: Happy path, edge case ve error senaryolarını listeleyen ve her senaryo için test referansı içeren dokümantasyon dosyası
- **Test_Pyramid**: Unit (%70), Integration (%20), E2E (%10) oranlarını tanımlayan test dağılım modeli
- **Coverage_Target**: Minimum %80 genel coverage, kritik path'ler için %95 coverage hedefi
- **Module**: Spring Modulith mimarisindeki 9 modülden biri (auth, car, currency, damage, dashboard, notification, payment, rental, shared)
- **Critical_Path**: Rental lifecycle, payment processing, authentication gibi iş açısından kritik akışlar

## Requirements

### Requirement 1: Test Strategy Document

**User Story:** As a developer, I want a comprehensive test strategy document, so that I can understand the testing approach and write consistent tests.

#### Acceptance Criteria

1. THE Test_Strategy_Document SHALL define the test pyramid with specific percentages (Unit %70, Integration %20, E2E %10)
2. THE Test_Strategy_Document SHALL specify minimum coverage targets (overall %80, critical paths %95)
3. THE Test_Strategy_Document SHALL list all testing tools and frameworks used (JUnit 5, Mockito, AssertJ, Spring Boot Test, H2, Awaitility)
4. THE Test_Strategy_Document SHALL describe the test directory structure for each module (unit/, integration/, e2e/)
5. THE Test_Strategy_Document SHALL define naming conventions for test classes and methods
6. THE Test_Strategy_Document SHALL explain when to use unit tests vs integration tests vs E2E tests
7. THE Test_Strategy_Document SHALL reference the existing steering files (java-spring-boot-test-standards.md)
8. WHEN a new module is created, THE Test_Strategy_Document SHALL provide a checklist for required test coverage

### Requirement 2: Critical Scenarios Document

**User Story:** As a developer, I want a document listing all critical test scenarios, so that I can ensure important business flows are properly tested.

#### Acceptance Criteria

1. THE Critical_Scenarios_Document SHALL categorize scenarios into Happy Path, Edge Cases, and Error Handling sections
2. THE Critical_Scenarios_Document SHALL document the complete Rental Lifecycle scenarios (Request → Confirm → Pickup → Return)
3. THE Critical_Scenarios_Document SHALL document Payment Processing scenarios (Authorization, Capture, Refund, Webhook handling)
4. THE Critical_Scenarios_Document SHALL document Authentication scenarios (JWT, OAuth2, Token refresh)
5. THE Critical_Scenarios_Document SHALL document Late Return and Penalty scenarios
6. THE Critical_Scenarios_Document SHALL document Damage Management scenarios
7. WHEN documenting a scenario, THE Critical_Scenarios_Document SHALL include the test file reference that covers it
8. THE Critical_Scenarios_Document SHALL identify any gaps where scenarios lack test coverage

### Requirement 3: Module Coverage Matrix

**User Story:** As a team lead, I want to see test coverage per module, so that I can identify undertested areas.

#### Acceptance Criteria

1. THE Test_Strategy_Document SHALL include a module coverage matrix showing current coverage per module
2. THE Test_Strategy_Document SHALL identify critical modules requiring higher coverage (rental, payment, auth)
3. WHEN coverage falls below target, THE Test_Strategy_Document SHALL flag the module as needing attention
4. THE Test_Strategy_Document SHALL distinguish between unit test coverage and integration test coverage per module

### Requirement 4: Test Data Management

**User Story:** As a developer, I want guidelines for test data management, so that I can write maintainable tests.

#### Acceptance Criteria

1. THE Test_Strategy_Document SHALL document the TestFixtures and TestDataBuilder patterns used in E2E tests
2. THE Test_Strategy_Document SHALL explain when to use @Transactional for test isolation
3. THE Test_Strategy_Document SHALL describe the test database configuration (H2 for tests)
4. THE Test_Strategy_Document SHALL provide examples of creating test users, cars, and rentals

### Requirement 5: Modularity Testing

**User Story:** As an architect, I want documentation on modularity testing, so that module boundaries are enforced.

#### Acceptance Criteria

1. THE Test_Strategy_Document SHALL explain the ModularityTests class and its purpose
2. THE Test_Strategy_Document SHALL document how Spring Modulith verifies module boundaries
3. THE Test_Strategy_Document SHALL describe the consequences of modularity test failures
4. WHEN adding cross-module dependencies, THE Test_Strategy_Document SHALL explain how to update package-info.java

### Requirement 6: CI/CD Test Integration

**User Story:** As a DevOps engineer, I want documentation on how tests run in CI/CD, so that I can maintain the pipeline.

#### Acceptance Criteria

1. THE Test_Strategy_Document SHALL document the Maven commands for running different test types
2. THE Test_Strategy_Document SHALL explain JaCoCo coverage report generation
3. THE Test_Strategy_Document SHALL describe test execution order and parallelization settings
4. IF tests fail in CI, THEN THE Test_Strategy_Document SHALL provide troubleshooting guidance
