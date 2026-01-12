# Java Spring Boot - Main Standards

> Follow these rules when implementing new features.

## 📑 TABLE OF CONTENTS

| # | Section | Description |
|---|---------|-------------|
| 1 | [⚡ Quick Start](#-quick-start-30-seconds) | Essential basics in 30 seconds |
| 2 | [🗺️ Navigation](#️-navigation) | What I'm doing → Where to look |
| 3 | [🚫 Common Mistakes](#-common-mistakes) | Top 10 error list |
| 4 | [🔴 Critical Rules](#-critical-rules) | Summary table |
| 5 | [📦 DTO Rules](#-dto-rules) | Pure record pattern |
| 6 | [⚠️ Entity Rules](#️-entity-rules) | BaseEntity, soft delete |
| 7 | [📁 Module Structure](#-module-structure) | Package organization |
| 8 | [🔐 Visibility Rules](#-visibility-rules-important) | public class requirement |
| 9 | [🔗 Cross-Module Communication](#-cross-module-communication) | Public API usage |
| 10 | [📝 Layer Templates](#-layer-templates) | Service, Controller, Entity templates |
| 11 | [🔄 MapStruct Mapper](#-mapstruct-mapper-rules) | Mapper patterns |
| 12 | [🔄 @Transactional Rules](#-transactional-rules) | When to use/not use |
| 13 | [⚠️ Exception Handling](#️-exception-handling-rules) | Catch strategy |
| 14 | [📝 Logging Rules](#-logging-rules) | Log levels |
| 15 | [✅ Validation Rules](#-validation-rules) | Bean validation |
| 16 | [🔒 Null Handling](#-null-handling-rules) | Optional usage |
| 17 | [📦 Cache Rules](#-cache-rules) | Cache eviction |

---

## ⚡ QUICK START (30 SECONDS)

**Writing a new class:**
```java
@Service
@RequiredArgsConstructor                    // 1. DI
@Transactional(readOnly = true)             // 2. Only if DB operations exist
public class YourServiceImpl implements YourService {  // 3. public class!
    private final YourRepository repository; // 4. private final
}
```

**Calling another module:**
```java
private final AuthService authService;      // ✅ Public API interface
// private final UserRepository userRepo;   // ❌ NEVER!
```

**Can't solve the issue?** Use the navigation below or Ctrl+F to search.

---

## 🗺️ NAVIGATION

| I'm doing | Go to → |
|-----------|---------|
| Writing new Service/Controller | **LAYER TEMPLATES** |
| Writing a Mapper | **MAPSTRUCT MAPPER RULES** |
| Calling another module | **CROSS-MODULE COMMUNICATION** |
| Exception handling | **EXCEPTION HANDLING RULES** |
| Adding @Transactional | **@TRANSACTIONAL RULES** |
| Adding cache | **CACHE RULES** |
| Adding logs | **LOGGING RULES** |
| Adding validation | **VALIDATION RULES** |
| Writing tests | → `java-spring-boot-test-standards.md` |

---

## 🚫 COMMON MISTAKES

| # | Mistake | Fix |
|---|---------|-----|
| 1 | `@Autowired` field injection | `@RequiredArgsConstructor` + `private final` |
| 2 | Injecting another module's Repository | Use Public API Service interface |
| 3 | `findAll().stream().filter()` | Write indexed query method |
| 4 | `catch(Exception)` and suppress | Throw exception → GlobalExceptionHandler |
| 5 | Using `var` | Explicit type: `String name` |
| 6 | Writing `createdAt` | BaseEntity field name: `createTime` |
| 7 | `package-private` class | `public class` (for test access) |
| 8 | Single `@CacheEvict` | Use `@Caching` for multi-evict |
| 9 | `@Data` on Entity | Use `@Getter/@Setter` |
| 10 | Returning `null` | Use `orElseThrow()` or `Optional` |

---

## 🔴 CRITICAL RULES

| Rule | ✅ CORRECT | ❌ WRONG |
|------|-----------|----------|
| DI | `@RequiredArgsConstructor` + `private final` | `@Autowired` field injection |
| Entity | `@Getter/@Setter` | `@Data` (equals/hashCode issue) |
| Entity Extend | `extends BaseEntity` + use it | Duplicate field (createdAt vs createTime) |
| DTO | Pure record (data carrier only) | `of()` method with calculations |
| Local Variable | `String name` (explicit type) | `var name` |
| Delete | `entity.softDelete()` + save | `repository.delete()` |
| Return | Return DTO | Return Entity |
| Query | Indexed query method | `findAll().stream().filter()` |
| Cache | `@Caching` multi-evict | Single `@CacheEvict` |
| Import | `import` + short name (`LocalDateTime`) | Fully qualified (`java.time.LocalDateTime`) |
| **Visibility** | **`public class` + `public` methods** | **Package-private (test access error)** |
| **@Transactional** | **Use when own DB operation exists** | **Adding to delegate-only service** |
| **Exception Handling** | **Throw exception → GlobalExceptionHandler** | **`catch(Exception)` suppress and return 200** |

---

## 📦 DTO RULES

### DTO = Data Transfer Object = DATA CARRIER ONLY

```java
// ✅ CORRECT - Pure Record
public record FleetStatusDto(
    int totalCars,
    int rentedCars,
    BigDecimal occupancyRate,
    LocalDateTime generatedAt
) {}  // NO methods
```

### ❌ WRONG - Business Logic in DTO
```java
public record FleetStatusDto(...) {
    public static FleetStatusDto of(int total, int rented) {
        BigDecimal occupancy = calculateOccupancy(total, rented);  // ❌ Business logic!
        return new FleetStatusDto(..., occupancy, LocalDateTime.now());  // ❌ Timestamp!
    }
}
```

### ✅ CORRECT - Calculation in Service
```java
// In Service
public FleetStatusDto getFleetStatus() {
    int total = carService.countTotal();
    int rented = carService.countRented();
    BigDecimal occupancy = calculateOccupancy(total, rented);  // ✅ Calculation here
    return new FleetStatusDto(total, rented, occupancy, LocalDateTime.now());  // ✅
}
```

### Acceptable DTO Methods
```java
// ✅ OK - Named constructor (semantic clarity, NO calculation)
public static Result success() { return new Result(true, null); }
public static Result failure(String msg) { return new Result(false, msg); }
```

---

## ⚠️ ENTITY RULES

### BaseEntity Fields (DO NOT Duplicate!)

```java
// Fields ALREADY in BaseEntity:
public abstract class BaseEntity {
    private Long id;
    private LocalDateTime createTime;     // ← USE THIS (NOT createdAt!)
    private LocalDateTime updateTime;
    private Long version;
    private Boolean isDeleted;
    private String createdBy;
    private String updatedBy;
}
```

### ❌ WRONG - Duplicate Field
```java
public class Alert extends BaseEntity {
    private LocalDateTime createdAt;  // ❌ createTime EXISTS in BaseEntity!
}
```

### ✅ CORRECT - Use BaseEntity
```java
public class Alert extends BaseEntity {
    // NO createdAt - use BaseEntity's createTime
    // In DTO mapper: @Mapping(target = "createdAt", source = "createTime")
}
```

### ⚠️ @PageableDefault in CONTROLLER

**CRITICAL:** When using `@PageableDefault(sort = ...)`, use **entity field name**, NOT DTO field name!

```java
// ❌ WRONG - Entity has createTime, not createdAt!
@PageableDefault(size = 10, sort = "createdAt") Pageable pageable

// ✅ CORRECT - Entity field name must be used
@PageableDefault(size = 10, sort = "createTime") Pageable pageable
```

> **Why?** Hibernate/JPA queries use **entity field names**. DTO may have different names
> but Pageable goes directly to entity. Wrong name throws:
> `PathElementException: Could not resolve attribute 'createdAt' of 'Rental'`

### 🏗️ Spring Modulith & @EntityGraph

**This project uses Spring Modulith architecture:**
- Cross-module entity relations (`@OneToMany`, `@ManyToOne`) are **avoided**
- Instead, use **ID references** (`userId`, `carId`) + denormalized data
- This eliminates most N+1 query problems at the architecture level

```java
// ❌ WRONG - Cross-module entity relation
@ManyToOne(fetch = FetchType.LAZY)
private User user;

// ✅ CORRECT - ID reference + denormalized data
@Column(name = "user_id", nullable = false)
private Long userId;

@Column(name = "user_email", nullable = false)
private String userEmail;  // Denormalized for display
```

**@EntityGraph Usage:**
```java
// Empty @EntityGraph - prepared for future intra-module relations (e.g., images, features)
// Currently no lazy-loaded associations exist in Car entity
@EntityGraph(attributePaths = {})
Page<Car> findByIsDeletedFalse(Pageable pageable);

// Future: If intra-module relations are added
@EntityGraph(attributePaths = {"images", "features"})
Page<Car> findByIsDeletedFalse(Pageable pageable);
```

> **Rule:** Only use `@EntityGraph` for **intra-module** relations. Cross-module data is fetched via Public API services.

---

## 📁 MODULE STRUCTURE

> 📖 For detailed explanation see: `docs/architecture/DEVELOPER_GUIDE.md`

```
com.akif.{module}/
├── package-info.java             # @ApplicationModule + allowedDependencies
│
├── api/                          # ✅ PUBLIC (@NamedInterface)
│   ├── package-info.java         # @NamedInterface("api")
│   ├── {Module}Service.java      # Public API interface
│   ├── {Module}Dto.java          # Cross-module DTO
│   ├── {Module}Response.java     # Response DTO
│   ├── {Module}Event.java        # Domain event
│   └── Enums (if shared)         # Public enums
│
├── domain/                       # ❌ INTERNAL
│   ├── enums/                    # Internal enums
│   └── model/
│       └── {Entity}.java
│
├── internal/                     # ❌ INTERNAL
│   ├── service/
│   │   └── {Module}ServiceImpl.java
│   ├── repository/
│   │   └── {Entity}Repository.java
│   ├── dto/
│   │   └── request/              # Request DTOs
│   ├── mapper/
│   │   └── {Entity}Mapper.java
│   ├── exception/
│   └── config/
│
└── web/                          # ❌ INTERNAL
    └── {Module}Controller.java
```

---

## 🔐 VISIBILITY RULES (IMPORTANT!)

### Why Public?

Classes in `internal/` package must be **accessible from unit tests**. In Java, package-private classes cannot be accessed from different packages!

```java
// ❌ WRONG - Cannot access from test!
class DashboardServiceImpl implements DashboardService { ... }

// ✅ CORRECT - Accessible from test
public class DashboardServiceImpl implements DashboardService { ... }
```

### Test Package vs Main Package

```
src/main/java/com/akif/dashboard/internal/service/
└── DashboardServiceImpl.java   // Package: com.akif.dashboard.internal.service

src/test/java/com/akif/dashboard/unit/
└── DashboardServiceImplTest.java  // Package: com.akif.dashboard.unit ← DIFFERENT PACKAGE!
```

**Different packages = package-private inaccessible!**

### Rule Summary

| File | Visibility | Reason |
|------|------------|--------|
| `api/*Service.java` | `public interface` | Cross-module API |
| `internal/*ServiceImpl.java` | `public class` | Unit test access |
| `internal/*ServiceImpl` methods | `public` (from interface) | Mock access |
| Helper classes | `package-private` OK | If test not needed |
| Repository | `interface` (default public) | Spring Data |

---

## 🎯 LEARNING MODULE PUBLIC APIs

> ⚠️ **IMPORTANT:** This document does NOT maintain hardcoded API list - stale data risk!

To learn a module's public API, open the **ACTUAL source file**:

```
src/main/java/com/akif/{module}/api/{Module}Service.java
```

**Examples:**
- `com.akif.auth.api.AuthService` → Auth module API
- `com.akif.car.api.CarService` → Car module API
- `com.akif.rental.api.RentalService` → Rental module API
- `com.akif.payment.api.PaymentService` → Payment module API

**Rule:** ALWAYS read the actual interface file instead of documentation!

---

## 🔗 CROSS-MODULE COMMUNICATION

### ✅ CORRECT - Use Public API
```java
@Service
@RequiredArgsConstructor
class DamageServiceImpl implements DamageService {
    // ✅ Inject public API services
    private final AuthService authService;
    private final CarService carService;
    private final RentalService rentalService;
    private final PaymentService paymentService;
    
    // ✅ Internal repository (own module's)
    private final DamageReportRepository damageReportRepository;
    
    public void createDamageReport(DamageRequest request, String username) {
        // ✅ Public API call
        UserDto user = authService.getUserByUsername(username);
        RentalSummaryDto rental = rentalService.getRentalSummaryById(request.rentalId());
        
        // Business logic...
        
        // ✅ Update status via public API
        carService.markAsMaintenance(rental.carId());
        rentalService.incrementDamageReportCount(rental.id());
    }
}
```

### ❌ WRONG - Never Do This
```java
// ❌ Accessing another module's repository
private final UserRepository userRepository;  // WRONG!

// ❌ Accessing another module's internal class
private final CarServiceImpl carServiceImpl;  // WRONG!

// ❌ Accessing another module's entity
import com.akif.auth.domain.User;  // WRONG in unit test!
```

---

## 📝 LAYER TEMPLATES

### Service Interface (api/ package)
```java
// api/YourService.java
public interface YourService {
    YourResponse create(YourRequest request, String username);
    YourResponse getById(Long id);
    YourDto getByIdForOtherModules(Long id);  // Cross-module DTO
}
```

### Service Implementation (internal/ package)
```java
// internal/service/YourServiceImpl.java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class YourServiceImpl implements YourService {  // ⚠️ PUBLIC class! (for test access)
    
    private final YourRepository repository;
    private final YourMapper mapper;
    private final AuthService authService;  // Cross-module API
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    public YourResponse getById(Long id) {
        YourEntity entity = repository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new YourNotFoundException(id));
        return mapper.toResponse(entity);
    }
    
    @Override
    @Transactional
    public YourResponse create(YourRequest request, String username) {
        UserDto user = authService.getUserByUsername(username);
        
        YourEntity entity = mapper.toEntity(request);
        entity.setUserId(user.id());
        entity.setUserEmail(user.email());
        
        YourEntity saved = repository.save(entity);
        
        eventPublisher.publishEvent(new YourCreatedEvent(
            saved.getId(),
            user.email(),
            // ... denormalized data
        ));
        
        return mapper.toResponse(saved);
    }
}
```

### Controller (web/ package)
```java
// web/YourController.java
@RestController
@RequestMapping("/api/yours")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Your Management")
class YourController {
    
    private final YourService service;
    
    @GetMapping("/{id}")
    @Operation(summary = "Get by ID")
    public ResponseEntity<YourResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }
    
    @PostMapping
    @Operation(summary = "Create new")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<YourResponse> create(
            @Valid @RequestBody YourRequest request,
            @AuthenticationPrincipal UserDetails user) {
        YourResponse response = service.create(request, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

### Repository (internal/ package)
```java
// internal/repository/YourRepository.java
@Repository
interface YourRepository extends JpaRepository<YourEntity, Long> {
    
    Optional<YourEntity> findByIdAndIsDeletedFalse(Long id);
    
    List<YourEntity> findByUserIdAndIsDeletedFalse(Long userId);
    
    // ✅ CORRECT - Indexed query
    Optional<YourEntity> findByTransactionIdAndIsDeletedFalse(String transactionId);
    
    // ❌ WRONG - NEVER use findAll().stream().filter()!
}
```

### Entity (domain/ package)
```java
// domain/model/YourEntity.java
@Entity
@Table(name = "your_entities",
    indexes = {
        @Index(name = "idx_your_user", columnList = "user_id"),
        @Index(name = "idx_your_transaction", columnList = "transaction_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class YourEntity extends BaseEntity {
    
    // ✅ ID reference (NOT Entity reference)
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    // ✅ Denormalized data
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private YourStatus status;
    
    public void softDelete() {
        this.isDeleted = true;
    }
}
```

### DTO (Record)
```java
// api/YourDto.java (for cross-module)
public record YourDto(
    Long id,
    Long userId,
    String userEmail,
    YourStatus status,
    LocalDateTime createdAt
) {}

// internal/dto/request/YourRequest.java (for HTTP request)
public record YourRequest(
    @NotNull Long targetId,
    @NotBlank String description,
    @NotNull YourType type
) {}
```

### Event (api/ package)
```java
// api/YourCreatedEvent.java
@Getter
@RequiredArgsConstructor
public class YourCreatedEvent {
    private final Long entityId;
    private final String userEmail;
    private final String description;
    private final LocalDateTime createdAt;
    
    // ❌ DO NOT put Entity reference
    // private final YourEntity entity;  // WRONG!
}
```

---

## 📦 package-info.java Files

### Module Root
```java
// com/akif/yourmodule/package-info.java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"auth::api", "car::api", "payment::api", "shared"}
    // Syntax: "moduleName::interfaceName" for named interface access
    // Use "moduleName" alone to access entire module (not recommended)
)
package com.akif.yourmodule;
```

### api/ Package
```java
// com/akif/yourmodule/api/package-info.java
@org.springframework.modulith.NamedInterface("api")
package com.akif.yourmodule.api;
```

---

## 🔄 MAPSTRUCT MAPPER RULES

### Basic Mapper Structure
```java
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,  // ← Constructor injection for testability
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CarMapper {
    
    @Mapping(target = "createdAt", source = "createTime")  // BaseEntity mapping!
    CarDto toDto(Car entity);
    
    @Mapping(target = "id", ignore = true)
    Car toEntity(CreateCarRequest request);
    
    List<CarDto> toDtoList(List<Car> entities);  // Automatic
}
```

### ⚠️ CRITICAL: BaseEntity Field Mapping

| Entity Field | DTO Field | Mapping Required? |
|--------------|-----------|-------------------|
| `createTime` | `createdAt` | ✅ YES - `@Mapping` |
| `updateTime` | `updatedAt` | ✅ YES - `@Mapping` |
| `id`, `status` | `id`, `status` | ❌ NO - Same name |

```java
// ❌ WRONG - No mapping, returns null!
CarDto toDto(Car entity);

// ✅ CORRECT
@Mapping(target = "createdAt", source = "createTime")
CarDto toDto(Car entity);
```

### ❌ WRONG - Manual Mapping
```java
// DO NOT USE - MapStruct exists, don't write manual mapping!
public CarDto toDto(Car entity) {
    return new CarDto(entity.getId(), entity.getBrand(), ...);
}
```

---

## ✅ NEW FEATURE CHECKLIST

### Module Structure
- [ ] Public interface and DTOs in `api/` package
- [ ] `@NamedInterface("api")` in `api/package-info.java`
- [ ] Implementation in `internal/` package
- [ ] Entity in `domain/model/` package
- [ ] Controller in `web/` package

### Service Layer
- [ ] Public interface in `api/` package
- [ ] Implementation in `internal/` package
- [ ] `@Transactional(readOnly = true)` at class level
- [ ] `@Transactional` on write methods
- [ ] Public API used for cross-module
- [ ] ServiceImpl class is **public class** (for test access)
- [ ] All public API methods are **public** (for mock access)

### Entity Layer
- [ ] `@Getter/@Setter` used (NOT `@Data`)
- [ ] `isDeleted` soft delete field exists
- [ ] ID reference to other modules (NOT entity reference)
- [ ] Denormalized data exists (email, name, etc.)
- [ ] `@Index` for frequently queried fields

### Query Performance
- [ ] NO `findAll().stream().filter()`
- [ ] Indexed query methods used
- [ ] `AndIsDeletedFalse` suffix exists

### Cache (if applicable)
- [ ] `@Caching` multi-evict on status-changing methods
- [ ] All related caches are evicted

### @Transactional
- [ ] Class-level `@Transactional(readOnly = true)` for services with own repository
- [ ] `@Transactional` on write methods
- [ ] NO `@Transactional` on delegate-only services

---

## 🔄 @TRANSACTIONAL RULES

### When to USE?

```java
// ✅ CORRECT - Has own DB operation
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // ← class level, read-only default
public class RentalServiceImpl implements RentalService {
    
    private final RentalRepository rentalRepository;  // ← Own repository
    
    @Override
    public RentalResponse getById(Long id) {
        // ✅ Uses repository → @Transactional required
        return rentalRepository.findByIdAndIsDeletedFalse(id)...
    }
    
    @Override
    @Transactional  // ← Write method, readOnly = false
    public RentalResponse confirmRental(Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)...
        rental.setStatus(RentalStatus.CONFIRMED);
        rentalRepository.save(rental);  // ✅ DB write
        return mapper.toResponse(rental);
    }
}
```

### When NOT to USE?

```java
// ❌ WRONG - Only delegating, NO own DB operation
@Service
@RequiredArgsConstructor
@Transactional  // ← UNNECESSARY!
public class QuickActionServiceImpl implements QuickActionService {
    
    private final RentalService rentalService;  // ← Other service (has own @Transactional)
    
    @Override
    public QuickActionResultDto approveRental(Long rentalId) {
        // ❌ RentalService already manages its own transaction
        RentalResponse response = rentalService.confirmRental(rentalId);
        return QuickActionResultDto.success(...);
    }
}

// ✅ CORRECT - @Transactional removed
@Service
@RequiredArgsConstructor
public class QuickActionServiceImpl implements QuickActionService {
    
    private final RentalService rentalService;
    
    @Override
    public QuickActionResultDto approveRental(Long rentalId) {
        // Delegated service manages its own transaction
        RentalResponse response = rentalService.confirmRental(rentalId);
        return QuickActionResultDto.success(...);
    }
}
```

### ⚠️ Exception + @Transactional TRAP

```java
// ❌ DANGEROUS - If exception is suppressed, NO rollback occurs
@Transactional
public QuickActionResultDto approveRental(Long rentalId) {
    try {
        rentalService.confirmRental(rentalId);
        return QuickActionResultDto.success(...);
    } catch (Exception e) {
        return QuickActionResultDto.failure(...);  // ❌ Exception caught → NO rollback!
    }
}
```

### Decision Table

| Scenario | @Transactional? |
|----------|-----------------|
| Using my own repository | ✅ Yes |
| Only calling other services | ❌ No |
| Catching exception and returning result DTO | ❌ No (rollback won't work) |
| Multiple repositories must be atomic | ✅ Yes |
| Read-only query | `@Transactional(readOnly = true)` |

---

## ⚠️ EXCEPTION HANDLING RULES

### ⚡ DECISION TABLE

| Scenario | Catch? | Reason |
|----------|--------|--------|
| Controller → Service | ❌ NO | GlobalExceptionHandler should return correct HTTP status |
| Service → External API (Gateway) | ✅ YES | Carry error message with Result type, caller checks |
| Scheduler / Batch Job | ✅ YES | Single item fail shouldn't stop others |
| Service → Service | ❌ NO | Exception should propagate to caller |

---

### 1️⃣ CONTROLLER-FACING SERVICE

**Service methods called directly from controller.**

```java
// ❌ WRONG - Exception suppressed, returns HTTP 200
@Override
public QuickActionResultDto approveRental(Long rentalId) {
    try {
        RentalResponse response = rentalService.confirmRental(rentalId);
        return QuickActionResultDto.success(...);
    } catch (Exception e) {
        return QuickActionResultDto.failure(e.getMessage());  // ← HTTP 200!
    }
}

// ✅ CORRECT - Throw exception, GlobalExceptionHandler catches
@Override
public QuickActionResultDto approveRental(Long rentalId) {
    RentalResponse response = rentalService.confirmRental(rentalId);
    DailySummaryDto summary = dashboardQueryService.fetchDailySummary();
    
    return QuickActionResultDto.success(
        "Rental approved successfully",
        response.status().name(),
        summary
    );
    // If exception thrown → GlobalExceptionHandler → 400/404/500
}
```

**Rule:** DO NOT use generic catch in controller-facing services!

---

### 2️⃣ EXTERNAL API / GATEWAY WRAPPER

**Catch is ACCEPTABLE for external API calls (Stripe, SendGrid, etc.).**

```java
// ✅ OK - External API error carried with Result type
@Override
public PaymentResult chargePayment(Long paymentId, String customerId) {
    try {
        PaymentResult captureResult = paymentGateway.capture(...);
        // DB update...
        return captureResult;
    } catch (Exception e) {
        log.error("Gateway error for payment: {}", paymentId, e);
        payment.updateStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        return PaymentResult.failure("Payment gateway error: " + e.getMessage());
    }
}
```

### ⚠️ CALLER RESPONSIBILITY

Caller of Result type method must check result and throw exception:

```java
// Caller (RentalServiceImpl) - checks result and throws exception
PaymentResult authResult = paymentService.authorize(...);
if (!authResult.success()) {
    throw new PaymentFailedException(
        "Payment authorization failed: " + authResult.message()
    );  // ← This goes to GlobalExceptionHandler
}
```

**Rule:** Gateway wrapper can use catch, but CALLER is responsible for throwing exception!

---

### 3️⃣ SCHEDULER / BATCH JOB

**Single item failure shouldn't stop others.**

```java
// ✅ OK - try-catch for each item in batch
@Scheduled(fixedRate = 300000)
public void processOverdueRentals() {
    try {
        List<Rental> overdueRentals = rentalRepository.findOverdue();
        
        for (Rental rental : overdueRentals) {
            try {
                processLateReturn(rental);
            } catch (Exception e) {
                log.error("Failed to process rental {}: {}", 
                          rental.getId(), e.getMessage());
                // continue - other rentals should keep processing
            }
        }
    } catch (Exception e) {
        log.error("Scheduled job failed completely: {}", e.getMessage(), e);
    }
}
```

**Rule:** Use outer catch + inner (item-level) catch in schedulers.

---

### 4️⃣ SPECIFIC EXCEPTION TRANSFORMATION

**Catch specific exception → Transform to domain exception.**

```java
// ✅ OK - Specific catching and transformation
try {
    PaymentResult refundResult = paymentService.refundPayment(...);
    // ...
} catch (PenaltyWaiverException e) {
    throw e;  // Throw domain exception as-is
} catch (Exception e) {
    log.error("Unexpected error during refund: {}", e.getMessage(), e);
    throw new PenaltyWaiverException("Failed to process refund", e);  // Wrap it
}
```

**Rule:** You can wrap generic exception to domain exception, but DO NOT SUPPRESS!

---

### ❌ NEVER DO THIS

```java
// ❌ NEVER - Silent suppress
catch (Exception e) {
    log.error(e.getMessage());
    // continue as if nothing happened
}

// ❌ NEVER - Swallow exception and return null/empty
catch (Exception e) {
    return null;  // Caller gets NullPointerException
}

// ❌ NEVER - Return failure result in controller-facing service
catch (Exception e) {
    return ResultDto.failure(e.getMessage());  // Error with HTTP 200
}
```

---

### GlobalExceptionHandler HTTP Status Mapping

| Exception | HTTP Status |
|-----------|-------------|
| `BaseException` (subclasses) | Defined in exception (`HttpStatus.NOT_FOUND`, etc.) |
| `MethodArgumentNotValidException` | 400 Bad Request |
| `AccessDeniedException` | 403 Forbidden |
| `AuthenticationException` | 401 Unauthorized |
| `Exception` (other) | 500 Internal Server Error |

---

## 📝 LOGGING RULES

### Log Levels

| Level | When | Example |
|-------|------|---------|
| `ERROR` | Unexpected exception, critical error | Gateway connection error, DB connection |
| `WARN` | Unexpected situation but system working | Retry performed, fallback used |
| `INFO` | Business flow started/completed | "Rental confirmed", "Payment processed" |
| `DEBUG` | Detailed debugging (disabled in production) | Request/response body, step-by-step |

### ✅ CORRECT Log Usage
```java
// ✅ Structured logging - IDs and important values
log.info("Rental confirmed: rentalId={}, userId={}, carId={}", rentalId, userId, carId);

// ✅ Exception with stack trace
log.error("Failed to process payment: paymentId={}", paymentId, exception);

// ✅ Business event
log.info("Car reserved: carId={}, reservedUntil={}", carId, reservedUntil);
```

### ❌ WRONG - Never Do This
```java
// ❌ Logging sensitive data
log.info("User logged in: password={}", password);
log.info("Payment processed: cardNumber={}", cardNumber);

// ❌ Meaningless log
log.info("here");
log.info("test");

// ❌ Swallowing exception and only logging message
log.error(e.getMessage());  // NO stack trace!

// ❌ INFO level log inside loop
for (Rental r : rentals) {
    log.info("Processing rental: {}", r.getId());  // Use WARN or DEBUG!
}
```

---

## ✅ VALIDATION RULES

### Validation Locations

| Layer | Validation Type | Example |
|-------|-----------------|---------|
| DTO (Request) | Bean Validation | `@NotNull`, `@NotBlank`, `@Size` |
| Controller | `@Valid` annotation | Validate request |
| Service | Business validation | "Is car available?", "Is date valid?" |

### DTO Validation
```java
// ✅ CORRECT - Annotations in request DTO
public record CreateRentalRequest(
    @NotNull(message = "Car ID is required")
    Long carId,
    
    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or future")
    LocalDate startDate,
    
    @NotNull(message = "End date is required")
    @Future(message = "End date must be in future")
    LocalDate endDate
) {}
```

### Controller Validation
```java
// ✅ CORRECT - @Valid usage
@PostMapping
public ResponseEntity<RentalResponse> createRental(
        @Valid @RequestBody CreateRentalRequest request,  // ← @Valid
        @AuthenticationPrincipal UserDetails user) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(request, user.getUsername()));
}
```

### Service Business Validation
```java
// ✅ CORRECT - Business rules in service
public RentalResponse create(CreateRentalRequest request, String username) {
    // Business validation
    if (request.endDate().isBefore(request.startDate())) {
        throw new InvalidDateRangeException("End date must be after start date");
    }
    
    CarDto car = carService.getCarDtoById(request.carId());
    if (!car.status().equals(CarStatus.AVAILABLE)) {
        throw new CarNotAvailableException(request.carId());
    }
    
    // ... continue
}
```

---

## 🔒 NULL HANDLING RULES

### Optional Usage

```java
// ✅ CORRECT - Repository returns Optional, use orElseThrow
public CarResponse getCarById(Long id) {
    return carRepository.findByIdAndIsDeletedFalse(id)
            .map(mapper::toResponse)
            .orElseThrow(() -> new CarNotFoundException(id));
}

// ✅ CORRECT - Null-safe chain with Optional
Optional<PaymentDto> payment = paymentService.getPaymentByRentalId(rentalId);
BigDecimal amount = payment
        .map(PaymentDto::amount)
        .orElse(BigDecimal.ZERO);
```

### ❌ WRONG - Never Do This
```java
// ❌ Returning null
public CarDto getCarById(Long id) {
    Car car = carRepository.findById(id).orElse(null);
    if (car == null) return null;  // ❌ Throw exception!
    return mapper.toDto(car);
}

// ❌ Using Optional.get() without check
Car car = carRepository.findById(id).get();  // ❌ NoSuchElementException!

// ❌ Using Optional as field
private Optional<String> description;  // ❌ Optional field in Entity!
```

### Null Check Decision Table

| Situation | Approach |
|-----------|----------|
| Entity not found | `orElseThrow(() -> new NotFoundException(id))` |
| Optional field (e.g., notes) | `@Column(nullable = true)` + return null in getter |
| Cross-module query | Return `Optional<Dto>`, caller handles |
| Collection | Return empty list, NOT null |

---

## 📦 CACHE RULES

### Cache Naming

Format: `{module}:{scope}` or `{module}:{entity}:{suffix}`

```java
// Examples
public static final String CARS_ALL = "car:all";
public static final String CARS_AVAILABLE = "car:available";
public static final String CAR_BY_ID = "car:byId";
public static final String DASHBOARD_SUMMARY = "dashboard:summary";
public static final String RENTAL_STATS = "rental:stats";
```

### Cache Key Strategy
```java
// ✅ CORRECT - Key with SpEL
@Cacheable(value = "car:byId", key = "#id")
public CarDto getCarById(Long id) { ... }

// ✅ CORRECT - Composite key
@Cacheable(value = "rental:byUser", key = "#userId + ':' + #status")
public List<RentalDto> findByUserAndStatus(Long userId, RentalStatus status) { ... }
```

### Cache Eviction
```java
// ✅ CORRECT - Multiple cache eviction
@Caching(evict = {
    @CacheEvict(value = "car:all", allEntries = true),
    @CacheEvict(value = "car:available", allEntries = true),
    @CacheEvict(value = "car:byId", key = "#id")
})
public CarResponse updateCarStatus(Long id, CarStatus status) { ... }

// ❌ WRONG - Single cache evict (others become stale)
@CacheEvict(value = "car:byId", key = "#id")
public CarResponse updateCarStatus(Long id, CarStatus status) { ... }
```

---

## 🏃 COMMANDS

```bash
# Compilation check
mvn compile

# Modularity test
mvn test -Dtest="ModularityTests"

# Specific module test
mvn test -Dtest="com.akif.yourmodule.**"
```

---

**Last Updated:** 2026-01-02 | **Spring Boot:** 3.5.3 | **Status:** ✅ Active
