# Requirements Document

## Introduction

Bu doküman, mevcut CarGalleryProject monolitik uygulamasını **Spring Modulith** kullanarak **Modular Monolith** mimarisine dönüştürmek için gerekli gereksinimleri tanımlar. Spring Modulith, Spring Boot 3.x ile gelen resmi modular monolith framework'üdür ve package-based module boundaries, event-driven communication ve test-time verification sağlar.

**Mevcut Durum Analizi:**
- 10+ entity (Car, Rental, User, Payment, DamageReport, PenaltyWaiver, LinkedAccount, WebhookEvent, DamagePhoto)
- 18+ controller, 13+ service domain klasörü
- Flat package structure (`com.akif.*`)
- Event-driven architecture mevcut (ApplicationEventPublisher)
- Cross-cutting concerns dağınık (security, caching, events)

**Hedef:**
- Spring Modulith ile module boundaries
- Package-based application modules
- Event-driven inter-module communication
- Test-time module verification
- Module documentation generation

## Glossary

- **Spring Modulith**: Spring Boot için resmi modular monolith framework'ü
- **Application Module**: Spring Modulith'te bir bounded context'i temsil eden top-level package
- **Module API**: Bir modülün dış dünyaya expose ettiği public sınıflar (top-level package'daki sınıflar)
- **Module Internal**: Modül dışından erişilmemesi gereken sınıflar (sub-package'lardaki sınıflar)
- **ApplicationModuleTest**: Spring Modulith'in module boundary verification test'i
- **@ApplicationModule**: Modül metadata tanımlayan annotation
- **@Externalized**: Event'lerin modül dışına publish edilmesini sağlayan annotation
- **Shared Kernel**: Tüm modüller tarafından kullanılan ortak kod

## Requirements

### Requirement 1: Spring Modulith Integration

**User Story:** As a developer, I want to use Spring Modulith framework, so that I can leverage official Spring support for modular monolith architecture.

#### Acceptance Criteria

1. THE System SHALL include Spring Modulith dependencies in pom.xml (spring-modulith-starter-core, spring-modulith-starter-test)
2. THE System SHALL configure Spring Modulith for the base package `com.akif`
3. WHEN the application starts THEN THE System SHALL detect and register all application modules automatically
4. THE System SHALL use Spring Modulith version compatible with Spring Boot 3.5.x

### Requirement 2: Application Module Structure

**User Story:** As a developer, I want a clear module structure based on Spring Modulith conventions, so that module boundaries are automatically enforced.

#### Acceptance Criteria

1. THE System SHALL organize code into the following application modules: `rental`, `damage`, `car`, `auth`, `currency`, `notification`, `shared`
2. WHEN a module is created THEN THE System SHALL place public API classes in the module's top-level package
3. THE System SHALL place internal implementation classes in sub-packages (e.g., `rental.internal`, `rental.domain`)
4. THE System SHALL create a `package-info.java` file for each module with @ApplicationModule annotation where needed
5. THE System SHALL ensure the `shared` module is accessible by all other modules using @ApplicationModule(allowedDependencies)

### Requirement 3: Module Boundary Enforcement

**User Story:** As a developer, I want module boundaries enforced at test time, so that architectural violations are caught before deployment.

#### Acceptance Criteria

1. THE System SHALL create ApplicationModuleTest class to verify module structure
2. WHEN a class in module A directly accesses module B's internal package THEN THE ApplicationModuleTest SHALL fail
3. THE System SHALL verify that no circular dependencies exist between modules
4. THE System SHALL generate module documentation using Spring Modulith's documentation feature
5. WHEN running `mvn test` THEN THE System SHALL execute all module verification tests

### Requirement 4: Event-Driven Inter-Module Communication

**User Story:** As a developer, I want modules to communicate through domain events, so that modules remain loosely coupled.

#### Acceptance Criteria

1. THE System SHALL use Spring's ApplicationEventPublisher for inter-module events
2. WHEN a module publishes an event THEN THE System SHALL allow other modules to listen via @ApplicationModuleListener or @EventListener
3. THE System SHALL place domain events in the module's top-level package (public API)
4. THE System SHALL ensure events contain only primitive types, DTOs, and value objects (no entities)
5. WHEN an event needs to be processed asynchronously THEN THE System SHALL use @Async annotation
6. THE System SHALL use @Externalized annotation for events that need to be published outside the application (future Kafka/RabbitMQ support)

### Requirement 5: Module API Design

**User Story:** As a developer, I want each module to expose a clean public API, so that inter-module dependencies are explicit and minimal.

#### Acceptance Criteria

1. THE System SHALL expose service interfaces in the module's top-level package as the public API
2. THE System SHALL place service implementations in `{module}.internal` sub-package
3. THE System SHALL place entities in `{module}.domain` sub-package (internal by default)
4. THE System SHALL place repositories in `{module}.repository` sub-package (internal by default)
5. WHEN a module needs data from another module THEN THE System SHALL use the target module's public service interface
6. THE System SHALL place DTOs used in public API in the module's top-level package or `{module}.api` sub-package

### Requirement 6: Shared Module Configuration

**User Story:** As a developer, I want common code centralized in a shared module, so that duplication is avoided and cross-cutting concerns are managed consistently.

#### Acceptance Criteria

1. THE System SHALL place BaseEntity in `shared.domain` package
2. THE System SHALL place common enums (CurrencyType, Role) in `shared.enums` package
3. THE System SHALL place common exceptions (BaseException) in `shared.exception` package
4. THE System SHALL place security configuration in `shared.security` package
5. THE System SHALL place global exception handler in `shared.handler` package
6. THE System SHALL configure shared module as an open module accessible by all other modules
7. WHEN a utility class is used by multiple modules THEN THE System SHALL place the utility class in `shared.util` package

### Requirement 7: Module Testing Strategy

**User Story:** As a developer, I want each module to be independently testable, so that I can verify module behavior in isolation.

#### Acceptance Criteria

1. THE System SHALL create module-specific integration tests using @ApplicationModuleTest
2. WHEN testing a module in isolation THEN THE System SHALL use @MockitoBean for external module dependencies
3. THE System SHALL verify module events are published correctly using Spring Modulith's event publication testing
4. THE System SHALL include module documentation generation in the test phase
5. THE System SHALL ensure all module tests pass before allowing deployment

### Requirement 8: Migration Compatibility

**User Story:** As a developer, I want the migration to be incremental and non-breaking, so that existing functionality continues to work during the transition.

#### Acceptance Criteria

1. THE System SHALL maintain backward compatibility with existing REST API endpoints
2. THE System SHALL preserve existing database schema and Flyway migrations
3. WHEN migrating a module THEN THE System SHALL ensure all existing tests continue to pass
4. THE System SHALL allow gradual migration of modules (not all at once)
5. THE System SHALL document migration steps for each module in MIGRATION.md

### Requirement 9: Cross-Module Entity References

**User Story:** As a developer, I want entity relationships between modules to use ID references instead of JPA relationships, so that modules remain loosely coupled and can be extracted as microservices in the future.

#### Acceptance Criteria

1. THE System SHALL use ID references (Long carId, Long userId) instead of JPA @ManyToOne relationships for cross-module entity associations
2. THE System SHALL denormalize frequently accessed fields (carBrand, carModel, userEmail, userFullName) in entities that reference other modules
3. WHEN a module needs related entity data THEN THE System SHALL call the target module's public service API (e.g., CarService.getCarById())
4. THE System SHALL populate denormalized fields from DTOs during entity creation
5. THE System SHALL maintain foreign key constraints at the database level for referential integrity
6. THE System SHALL create Flyway migration to add denormalized columns to existing tables
7. WHEN publishing domain events THEN THE System SHALL use denormalized data from the entity instead of accessing other module's entities

### Requirement 10: Module Public API Design

**User Story:** As a developer, I want each module to expose a well-defined public API, so that cross-module dependencies are explicit and controlled.

#### Acceptance Criteria

1. THE System SHALL define a public service interface in each module's top-level package (e.g., CarService, AuthService, RentalService)
2. THE System SHALL expose only query methods and essential command methods in the public API
3. THE System SHALL define cross-module DTOs (CarDto, UserDto) with only the fields needed by other modules
4. THE System SHALL place service implementations in the module's internal sub-package
5. WHEN a module needs to change another module's state THEN THE System SHALL provide explicit command methods (e.g., CarService.reserveCar(), CarService.releaseCar())

### Requirement 11: Code Quality & Observability

**User Story:** As a developer, I want improved code quality and observability during the modular migration, so that debugging is easier and code is cleaner before restructuring.

#### Acceptance Criteria

1. THE System SHALL reduce GlobalExceptionHandler from 40+ handlers to 5 essential handlers (BaseException, ValidationException, OAuth2ErrorResponse, MethodArgumentNotValidException, generic fallback)
2. THE System SHALL implement MDC (Mapped Diagnostic Context) logging with correlation ID for request tracing
3. THE System SHALL add a CorrelationIdFilter that generates or extracts correlation ID from request headers
4. THE System SHALL update logback configuration to include correlation ID and userId in log pattern
5. THE System SHALL create DamageMapper using MapStruct for DamageReport entity-to-DTO mappings
6. THE System SHALL update RentalMapper to use denormalized fields after entity refactoring
7. WHEN N+1 query issues are detected THEN THE System SHALL fix them using @EntityGraph or JOIN FETCH
8. THE System SHALL maintain test coverage above 80% for all public API methods

