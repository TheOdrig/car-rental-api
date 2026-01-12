# Git Workflow - Spring Modulith Modular Monolith

## Branch Strategy

```
main
  └── refactor/modular-monolith
```

**Not:** Bu bir refactoring branch'i, feature değil!

## Commit Plan

---

### Pre-Phase 0: Code Quality Quick Wins

```
refactor(handler): cleanup GlobalExceptionHandler

Reduce handlers from 40+ to 5 essential handlers.
Keep: BaseException, ValidationException, OAuth2ErrorResponse, MethodArgumentNotValidException, generic fallback.
Standardize logging: error code + message format.
```

```
feat(logging): add CorrelationIdFilter for MDC logging

Create CorrelationIdFilter with UUID correlation ID.
Add userId, correlationId to MDC.
Check or generate correlation ID from request headers.
```

```
config(logging): add logback-spring.xml configuration

Create src/main/resources/logback-spring.xml.
Add structured logging pattern with %X{correlationId} %X{userId}.
Configure module-based log levels.
```

```
chore: pre-phase quick wins checkpoint

Ensure all tests pass.
GlobalExceptionHandler cleanup + MDC logging complete.
```

---

### Phase 1: Spring Modulith Setup

```
chore(deps): add Spring Modulith dependencies

Add spring-modulith-bom to dependencyManagement.
Add spring-modulith-starter-core dependency.
Add spring-modulith-starter-test dependency (test scope).
```

```
test(arch): add ModularityTests for module verification

Create src/test/java/com/akif/ModularityTests.java.
Add ApplicationModules.verify() test.
Add module documentation generation test.
```

```
docs(arch): create MIGRATION.md with initial violations

Document current module boundary violations.
List required changes for each module.
```

```
chore: phase 1 checkpoint

Spring Modulith dependencies added.
ModularityTests created.
MIGRATION.md documented.

Note: CI pipeline will be added in Phase 7 after modules are created.
```

---

### Phase 2: Shared Module Setup

```
refactor(shared): create shared module package structure

Create com.akif.shared package.
Create sub-packages: domain, enums, exception, security, config, util, handler.
```

```
refactor(shared): move BaseEntity to shared.domain

Move com.akif.shared.domain.BaseEntity to com.akif.shared.domain.
Update all entity imports across the project.
```

```
refactor(shared): move common enums to shared.enums

Move CurrencyType, Role and other shared enums.
Update all imports.
```

```
refactor(shared): move exception classes to shared

Move BaseException and common exceptions to shared.exception.
Move GlobalExceptionHandler (already cleaned!) to shared.handler.
Update all imports.
```

```
refactor(shared): move security classes to shared.security

Move SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter.
Move CustomUserDetailsService.
Move CorrelationIdFilter (from Pre-Phase).
Update all imports.
```

```
refactor(shared): move common config classes to shared.config

Move CorsConfig, AsyncConfig, CacheConfig.
Update all imports.
```

```
refactor(shared): configure shared module as OPEN

Create com.akif.shared.package-info.java.
Add @ApplicationModule(type = Type.OPEN) annotation.
```

```
test(shared): verify shared module structure passes

Run ModularityTests to verify shared module.
Ensure OPEN module is accessible by all.
```

```
chore(shared): verify shared kernel boundaries (STRICT)

Count shared kernel classes: must be ≤ 10.
Verify NO business logic in shared module.
Verify modül-specific enums stayed in their modules (e.g., RentalStatus → rental).
Document shared kernel inventory in MIGRATION.md.

Threshold: hedef ≤ 10, alarm > 15, fail > 20
```

---

### Phase 3: Auth Module Restructure

```
refactor(auth): create auth module package structure

Create com.akif.auth package.
Create sub-packages: domain, internal, repository, web.
```

```
refactor(auth): extract AuthService public interface

Create com.akif.auth.api.AuthService interface (top-level = public).
Define public API methods: getUserById, getUserByUsername, getUserByEmail.
```

```
refactor(auth): move User and LinkedAccount to auth.domain

Move entities to com.akif.auth.domain.
Update all imports.
```

```
refactor(auth): move auth repositories to auth.repository

Move UserRepository, LinkedAccountRepository.
Update all imports.
```

```
refactor(auth): move auth services to auth.internal

Move AuthServiceImpl to com.akif.auth.internal.
Move OAuth2 services to auth.internal.oauth2.
Implement AuthService interface.
```

```
refactor(auth): move auth controllers to auth.web

Move AuthController, OAuth2Controller.
Update all imports.
```

```
refactor(auth): create auth DTOs in public API

Create UserDto record in com.akif.auth (top-level).
Move/create auth request/response DTOs.
```

```
refactor(auth): configure auth module dependencies

Create com.akif.auth.package-info.java.
Add @ApplicationModule(allowedDependencies = {"shared"}).
```

```
test(auth): verify auth module structure passes

Run ModularityTests for auth module.
Ensure no boundary violations.
```

---

### Phase 4: Currency Module Restructure

```
refactor(currency): create currency module structure

Create com.akif.currency package.
Create sub-packages: internal, web.
```

```
refactor(currency): extract CurrencyService public interface

Create com.akif.currency.CurrencyService interface.
Define convert, getExchangeRate, getAllRates methods.
```

```
refactor(currency): move currency implementations to internal

Move CurrencyConversionServiceImpl, ExchangeRateCacheServiceImpl.
Move ExchangeRateClientImpl to currency.internal.
Move configs to currency.internal.config.
```

```
refactor(currency): move currency controller and DTOs

Move CurrencyController to currency.web.
Move/create ExchangeRateDto, ConversionResultDto in com.akif.currency.
```

```
refactor(currency): configure currency module dependencies

Create com.akif.currency.package-info.java.
Add @ApplicationModule(allowedDependencies = {"shared"}).
```

---

### Phase 5: Car Module Restructure

```
refactor(car): create car module package structure

Create com.akif.car package.
Create sub-packages: domain, internal, repository, web, mapper.
```

```
refactor(car): extract CarService public interface

Create com.akif.car.api.CarService interface.
Define: getCarById, searchCars, isCarAvailable, reserveCar, releaseCar, markAsMaintenance.
```

```
refactor(car): move Car entity to car.domain

Move Car entity to com.akif.car.domain.
Update all imports.
```

```
refactor(car): move car repository to car.repository

Move CarRepository.
Update all imports.
```

```
refactor(car): move car services to car.internal

Move CarServiceImpl to car.internal.
Move AvailabilityService, SimilarCarService to internal.availability.
Move PricingService to internal.pricing.
```

```
refactor(car): move car controllers to car.web

Move CarController, CarSearchController, CarStatisticsController.
Move CarBusinessController, AvailabilitySearchController, PricingController.
```

```
refactor(car): create car DTOs in public API

Create CarDto record in com.akif.car (cross-module DTO).
Move/create CarSearchCriteria, CarResponseDto.
```

```
refactor(car): move CarMapper to car.mapper

Move existing CarMapper to com.akif.car.mapper.
Update mappings for new package structure.
```

```
refactor(car): move car-related enums

Move CarStatusType to com.akif.car.domain.enums.
```

```
refactor(car): configure car module dependencies

Create com.akif.car.package-info.java.
Add @ApplicationModule(allowedDependencies = {"currency", "shared"}).
```

```
test(car): verify car module structure passes

Run ModularityTests for car module.
Ensure no boundary violations.
```

---

### Phase 6: Notification Module Restructure

```
refactor(notification): create notification module structure

Create com.akif.notification package.
Create sub-packages: internal, listener.
```

```
refactor(notification): extract NotificationService interface

Create com.akif.notification.NotificationService interface.
Define sendEmail, sendRentalConfirmation methods.
```

```
refactor(notification): move email services to internal

Move EmailNotificationService, EmailTemplateService.
Move MockEmailSender, SendGridEmailSender.
Move EmailProperties to internal.config.
```

```
refactor(notification): move event listeners

Move EmailEventListener to notification.listener.
```

```
refactor(notification): configure notification module

Create com.akif.notification.package-info.java.
Add @ApplicationModule(allowedDependencies = {"shared"}).
Note: Listens to events from rental/damage but doesn't depend on them directly.
```

---

### Phase 7: Mid-Project Verification

```
test(arch): mid-project modularity verification

Run ModularityTests for shared, auth, currency, car, notification modules.
Document any remaining violations.
```

```
ci(modulith): add GitHub Actions workflow for module verification (KRİTİK - MOVED FROM PHASE 1)

Create .github/workflows/modulith-verify.yml.
Add mvn test -Dtest=ModularityTests step.
Add shared kernel size check (fail if > 20 classes).
Run on push/PR to main and develop branches.
Test: Create a test PR that violates module boundaries → CI should fail.

Note: Added in Phase 7 because now we have real modules to verify!
Without this, module boundaries are NOT enforced!
```

```
test: fix broken tests from import changes

Update all test imports for new package structure.
Ensure all tests pass.
```

```
chore(arch): mid-project health check (anti-pattern tarama)

Verify shared kernel ≤ 10 classes.
Verify all modules have package-info.java.
Verify no circular dependencies (ModularityTests output).
Grep for cross-module @Autowired Repository injections.
Document findings in MIGRATION.md.
```

```
chore: squash merge to main (mid-project checkpoint)

Squash merge refactor/modular-monolith to main.
Create new branch for Phase 8+.
```

---

### Phase 8: Rental Module - Structure

```
refactor(rental): create rental module package structure

Create com.akif.rental package.
Create sub-packages: domain, internal, repository, web, mapper.
```

```
refactor(rental): extract RentalService public interface

Create com.akif.rental.api.RentalService interface.
Define: getRentalById, isRentalActive, hasActiveRentalForCar.
Note: Command methods stay internal, called from controller.
```

```
refactor(rental): move rental entities to rental.domain

Move Rental, Payment, PenaltyWaiver entities.
```

```
refactor(rental): move rental repositories

Move RentalRepository, PaymentRepository, PenaltyWaiverRepository.
All to com.akif.rental.repository.
```

```
refactor(rental): move rental services to internal

Move RentalServiceImpl to rental.internal.
Move PenaltyCalculationService, PenaltyPaymentService to internal.penalty.
Move LateReturnDetectionService to internal.detection.
Move LateReturnReportService to internal.report.
```

```
refactor(rental): move rental controllers to web

Move RentalController, LateReturnController, PenaltyWaiverController.
All to com.akif.rental.web.
```

```
refactor(rental): move payment gateway to internal

Move IPaymentGateway, StripePaymentGateway to internal.gateway.
Move StripeWebhookHandler, WebhookEvent to internal.webhook.
Move StripeWebhookController to rental.web.
```

```
refactor(rental): create rental DTOs in public API

Create RentalSummaryDto record in com.akif.rental (cross-module DTO).
Move/create RentalDto, RentalRequestDto.
```

```
refactor(rental): move rental events to public API

Move RentalConfirmedEvent, RentalCancelledEvent, PaymentCapturedEvent.
Move PenaltySummaryEvent, GracePeriodWarningEvent, LateReturnNotificationEvent.
All to com.akif.rental (top-level = public).
```

```
refactor(rental): move rental-related enums

Move RentalStatus, PaymentStatus, LateReturnStatus to rental.domain.enums.
```

```
refactor(rental): move rental schedulers to internal

Move LateReturnScheduler, ReminderScheduler, ReconciliationScheduler.
All to rental.internal.scheduler.
```

---

### Phase 9: Rental Module - Entity Refactoring

```
feat(db): add V12__rental_denormalization.sql migration

Add columns: car_brand, car_model, car_license_plate, user_email, user_full_name.
Populate from existing data (UPDATE ... FROM car, users).
Keep foreign keys for referential integrity.
```

```
refactor(rental)!: refactor Rental entity for cross-module isolation

Remove @ManyToOne Car and User relationships.
Keep carId, userId as Long columns (foreign keys remain).
Add denormalized fields: carBrand, carModel, carLicensePlate, userEmail, userFullName.

BREAKING CHANGE: Rental entity no longer has JPA relationships to Car/User.
```

```
refactor(rental): update RentalMapper for denormalized fields

Move RentalMapper to com.akif.rental.mapper.
Update mappings: rental.getCarBrand() instead of rental.getCar().getBrand().
Add mapping for denormalized fields.
```

```
refactor(rental): update RentalServiceImpl cross-module dependencies

Replace CarRepository with CarService (public API).
Replace UserRepository with AuthService (public API).
Populate denormalized fields from DTOs during rental creation.
```

```
refactor(rental): update rental state change methods

Update confirmRental: use CarService.reserveCar().
Update returnRental: use CarService.releaseCar().
Update cancelRental: use CarService.releaseCar().
```

```
refactor(rental): update rental events to use denormalized data

RentalConfirmedEvent uses rental.getCarBrand(), rental.getUserEmail().
RentalCancelledEvent, PaymentCapturedEvent use denormalized fields.
Remove entity access from all events.
```

```
refactor(rental): configure rental module dependencies

Create com.akif.rental.package-info.java.
Add @ApplicationModule(allowedDependencies = {"car", "auth", "currency", "shared"}).
Note: notification is NOT a dependency (rental publishes events, notification listens).
```

```
test(rental): verify rental module structure passes

Run Flyway migration.
Ensure all rental tests pass.
```

---

### Phase 10: Damage Module Restructure

```
refactor(damage): create damage module package structure

Create com.akif.damage package.
Create sub-packages: domain, internal, repository, web, mapper.
```

```
refactor(damage): extract DamageService public interface

Create com.akif.damage.api.DamageService interface.
Define: getDamageReportById, getDamageReportsByRentalId, hasPendingDamageReports.
```

```
refactor(damage): move damage entities to damage.domain

Move DamageReport, DamagePhoto entities.
```

```
refactor(damage): move damage repositories

Move DamageReportRepository, DamagePhotoRepository.
All to com.akif.damage.repository.
```

```
refactor(damage): move damage services to internal

Move DamageReportServiceImpl, DamageAssessmentServiceImpl.
Move DamageDisputeServiceImpl, DamageHistoryServiceImpl.
Move DamageChargeServiceImpl.
Move FileUploadService implementations to internal.storage.
```

```
refactor(damage): move damage controllers to web

Move DamageReportController, DamageAssessmentController.
Move DamageDisputeController, DamageHistoryController.
```

```
feat(damage): create DamageMapper using MapStruct

Create com.akif.damage.mapper.DamageMapper interface.
Map DamageReport → DamageReportDto.
Map DamagePhoto → DamagePhotoDto.
Replace manual mappings in services.
```

```
refactor(damage): create damage DTOs in public API

Create DamageReportDto record in com.akif.damage (cross-module DTO).
Move/create other damage DTOs.
```

```
refactor(damage): move damage events to public API

Move DamageReportedEvent, DamageAssessedEvent, etc.
All to com.akif.damage (top-level = public).
```

```
refactor(damage): move damage-related enums

Move DamageStatus, DamageSeverity, DamageCategory to damage.domain.enums.
```

```
refactor(damage): update DamageServiceImpl cross-module dependencies

Replace RentalRepository with RentalService (public API).
Replace CarRepository with CarService (public API).
Use CarService.markAsMaintenance() for MAJOR damage.
```

```
refactor(damage): configure damage module dependencies

Create com.akif.damage.package-info.java.
Add @ApplicationModule(allowedDependencies = {"rental", "car", "shared"}).
```

```
test(damage): verify damage module structure passes

Ensure all damage tests pass.
```

---

### Phase 11: Performance & Quality

```
perf(db): fix N+1 queries in rental and damage modules

Identify N+1 queries.
Add @EntityGraph where needed.
Add JOIN FETCH to critical queries.
```

```
perf(cache): optimize cache strategy for new module structure

Review cache hit rates.
Optimize cache keys for new module structure.
Update @CacheEvict for cross-module operations.
```

```
test: increase test coverage for public APIs

Add missing unit tests for new public APIs.
Update integration tests for new package structure.
Target: >80% coverage on public APIs.
```

```
test: fix broken tests from entity refactoring

Update all test imports for new package structure.
Update @MockitoBean paths for moved classes.
Fix any test failures from entity refactoring.
```

---

### Phase 12: Final Verification & Documentation

```
test(arch): final modularity verification

Run full ApplicationModules.verify().
Ensure no circular dependencies.
Fix any remaining violations.
```

```
docs(arch): generate module documentation

Run Documenter to generate PlantUML diagrams.
Add generated docs to docs/architecture/.
```

```
test(arch): verify success criteria (KRİTİK)

Verify: modules.verify() → PASS
Verify: All existing tests → PASS
Verify: Circular dependency → 0
Verify: Shared kernel size → ≤ 10 classes
Verify: Each module dependency count → ≤ 3
Verify: Entity leak → 0 (manual review of event records)
Document results in MIGRATION.md "Success Criteria" section.
```

```
ci(modulith): finalize CI/CD pipeline

Verify .github/workflows/modulith-verify.yml runs on every PR.
Ensure shared kernel size check fails if > 20 classes.
Test: Create PR with module boundary violation → CI should fail.
```

```
docs(arch): update MIGRATION.md with completed steps

Document completed migration steps.
Document breaking changes (Rental entity refactoring).
Document rollback strategy.
Add "Success Criteria Results" section.
Add "Lessons Learned" section.
```

```
docs(arch): create Architecture Decision Records

ADR-001: Why Spring Modulith over Maven multi-module.
ADR-002: Cross-module entity strategy (ID reference + denormalization).
ADR-003: Event-driven inter-module communication.
ADR-004: Shared kernel boundaries and rules.
```

```
docs(readme): add Spring Modulith architecture section

Add module dependency diagram (Mermaid).
List all modules with responsibilities.
Explain public API vs internal packages.
Add "Module Health Metrics" section.
```

```
docs(readme): add developer guide for modules

Document how to add a new module.
Document how to add cross-module dependencies.
Document how to run ModularityTests.
Document shared kernel rules (what NOT to add).
Document anti-patterns to avoid.
```

```
docs(plan): mark Modular Monolith as completed

Update RENTAL_MVP_PLAN.md.
Mark Faz 2.5: Modular Monolith & Architecture Refactoring as completed.
Update progress summary.
```

```
chore(arch): setup post-migration monitoring

Create scripts/modulith-health-check.sh for weekly checks.
Define 3-month and 6-month review dates.
Add monitoring instructions to MIGRATION.md.
```

```
chore: final checkpoint - merge to main

Ensure ALL tests pass.
Verify all Success Criteria met.
Final commit: complete modular monolith migration.
Merge to main.
```

---

## Merge & Rollback

```bash
# Merge to main
git checkout main
git merge refactor/modular-monolith

# Rollback single commit
git revert <commit-hash>

# Rollback entire refactoring
git revert --no-commit HEAD~<number-of-commits>..HEAD
git commit -m "revert(arch): rollback modular monolith refactoring"
```

## Testing

```bash
# Run modularity tests only
mvn test -Dtest="ModularityTests"

# Run all tests
mvn test

# Run specific module tests
mvn test -Dtest="*Auth*"
mvn test -Dtest="*Rental*"
mvn test -Dtest="*Car*"
```

## Phase Summary

| Phase | Description | Key Commits |
|-------|-------------|-------------|
| 0 | Pre-Phase: Quick Wins | GlobalExceptionHandler cleanup, MDC logging |
| 1 | Spring Modulith Setup | Dependencies, ModularityTests |
| 2 | Shared Module | BaseEntity, security, exceptions, **Boundary Check** |
| 3 | Auth Module | User, OAuth2, AuthService |
| 4 | Currency Module | CurrencyService |
| 5 | Car Module | CarService, CarMapper |
| 6 | Notification Module | EmailService |
| 7 | Mid-Project Verification | **CI Pipeline**, **Health Check**, Squash merge to main |
| 8 | Rental Module - Structure | Package structure |
| 9 | Rental Module - Entity | V12 migration, cross-module isolation |
| 10 | Damage Module | DamageService, DamageMapper |
| 11 | Performance & Quality | N+1 fix, cache, tests |
| 12 | Documentation + Verification | ADRs, README, **Success Criteria**, **CI Finalize** |

## Key Components

| Component | Description |
|-----------|-------------|
| ModularityTests | Spring Modulith module verification |
| package-info.java | Module config with @ApplicationModule |
| shared | OPEN module: BaseEntity, security, exceptions |
| auth | User, LinkedAccount, OAuth2 |
| currency | Exchange rates, conversion |
| car | Car entity, availability, pricing |
| notification | Email services, event listeners |
| rental | Rental, Payment, penalties, schedulers |
| damage | DamageReport, disputes, storage |

## Scope Reference

| Scope | Usage |
|-------|-------|
| `deps` | Dependency changes (pom.xml) |
| `arch` | Architecture tests, documentation |
| `shared` | Shared module changes |
| `auth` | Auth module changes |
| `currency` | Currency module changes |
| `car` | Car module changes |
| `notification` | Notification module changes |
| `rental` | Rental module changes |
| `damage` | Damage module changes |
| `db` | Database migrations |
| `handler` | Exception handler changes |
| `logging` | Logging configuration |
| `cache` | Cache configuration |
| `readme` | README documentation |
| `plan` | RENTAL_MVP_PLAN.md updates |

## Notes

- Branch adı `refactor/` prefix'i ile başlıyor (feature değil!)
- Çoğu commit `refactor` type (yeni özellik yok, kod taşıma)
- Her modül sonunda `test` commit'i var (verification)
- Phase 7'de mid-project squash merge to main
- BREAKING CHANGE sadece Rental entity refactoring'de (Phase 9)
- Flyway migration V12 (V10, V11 damage tables için kullanıldı)
- Her commit sonunda proje MUTLAKA compile olmalı

---

## Critical Commits (Atlanamaz!)

| Commit | Neden Kritik |
|--------|-------------|
| `ci(modulith): add GitHub Actions workflow` | CI olmadan modül sınırları kontrol edilmez |
| `chore(shared): verify shared kernel boundaries` | Shared şişerse her şey çöker |
| `chore(arch): mid-project health check` | Anti-pattern'leri erken yakala |
| `test(arch): verify success criteria` | Başarıyı ölçmeden tamamlandı deme |
| `ci(modulith): finalize CI/CD pipeline` | Her PR kontrol edilmeli |

## Post-Migration Monitoring

```bash
# Haftalık (Her Cuma)
mvn test -Dtest=ModularityTests
find . -path "*/shared/*" -name "*.java" | wc -l  # Must be ≤ 10

# 3 ay sonra kontrol
# - modules.verify() hala geçiyor mu?
# - Shared kernel hala ≤ 10 class mı?
# - Yeni feature eklemek hızlandı mı?

# 6 ay sonra kontrol
# - Yeni feature için 1 modül değişikliği yeterli mi?
# - Bug fix için 1 modül değişikliği yeterli mi?
```
