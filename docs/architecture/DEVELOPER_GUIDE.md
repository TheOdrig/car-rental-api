# Developer Guide: Spring Modulith Architecture

This guide explains how to create new modules, add module dependencies, and follow architectural rules in the Car Rental API.

---

## Table of Contents

1. [Module Structure](#module-structure)
2. [Creating a New Module](#creating-a-new-module)
3. [Adding Cross-Module Dependencies](#adding-cross-module-dependencies)
4. [Public vs Internal Packages](#public-vs-internal-packages)
5. [Event-Driven Communication](#event-driven-communication)
6. [Testing Modules](#testing-modules)
7. [Anti-Patterns to Avoid](#anti-patterns-to-avoid)
8. [Verification Commands](#verification-commands)

---

## Module Structure

Each module follows this structure:

```
module/
├── api/                  # Public API (interfaces + DTOs)
│   ├── ModuleService.java
│   └── ModuleDto.java
├── domain/               # Entities
│   ├── Entity.java
│   └── enums/
│       └── EntityStatus.java
├── internal/             # Internal implementation (private)
│   ├── service/
│   │   └── InternalService.java
│   ├── repository/       # JPA repositories
│   │   └── EntityRepository.java
│   ├── dto/              # Internal DTOs (request/response)
│   │   └── ModuleRequestDto.java
│   ├── mapper/           # MapStruct mappers
│   │   └── EntityMapper.java
│   └── config/
│       └── ModuleConfig.java
├── web/                  # REST controllers
│   └── EntityController.java
└── package-info.java     # Module configuration
```

### Visibility Rules

| Package | Visibility | Access |
|---------|------------|--------|
| `api/` | PUBLIC | Service interfaces + DTOs |
| `internal/` | PRIVATE | Services, repositories, mappers, config |
| `domain/` | PRIVATE | Only this module |
| `web/` | PRIVATE | HTTP layer only |

---

## Creating a New Module

### Step 1: Create Package Structure

```
com.akif.newmodule/
├── api/
├── domain/
├── internal/
│   ├── repository/
│   ├── service/
│   └── mapper/
└── web/
```

### Step 2: Create package-info.java

```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"shared"}  // Add dependencies here
)
package com.akif.newmodule;
```

### Step 3: Create Public API Interface

```java
// com.akif.newmodule.api.NewModuleService.java
package com.akif.newmodule.api;

public interface NewModuleService {
    NewModuleDto getById(Long id);
    List<NewModuleDto> findAll();
}
```

### Step 4: Create Public DTO

```java
// com.akif.newmodule.api.NewModuleDto.java
package com.akif.newmodule.api;

public record NewModuleDto(
    Long id,
    String name,
    LocalDateTime createdAt
) {}
```

### Step 5: Implement Service

```java
// com.akif.newmodule.internal.NewModuleServiceImpl.java
package com.akif.newmodule.internal;

@Service
class NewModuleServiceImpl implements NewModuleService {
    // Implementation
}
```

### Step 6: Verify

```bash
mvn test -Dtest=ModularityTests
```

---

## Adding Cross-Module Dependencies

### Step 1: Update package-info.java

```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"auth", "car", "shared"}  // Add new dependency
)
package com.akif.rental;
```

### Step 2: Inject Public API

```java
@Service
class RentalServiceImpl implements RentalService {
    
    private final CarService carService;      // ✅ Public API
    private final AuthService authService;    // ✅ Public API
    
    // ❌ NEVER: private final CarRepository carRepository;
    
    public RentalServiceImpl(CarService carService, AuthService authService) {
        this.carService = carService;
        this.authService = authService;
    }
    
    public void createRental(RentalRequest request, String username) {
        // Use public API
        CarDto car = carService.getCarById(request.carId());
        UserDto user = authService.getUserByUsername(username);
        
        // Business logic...
    }
}
```

### Dependency Rules

| ✅ Allowed | ❌ Not Allowed |
|-----------|----------------|
| `CarService` (interface) | `CarRepository` |
| `AuthService` (interface) | `User` entity |
| `CarDto` (public DTO) | `CarServiceImpl` |
| Public events | Internal services |

---

## Public vs Internal Packages

### Public (Accessible from Other Modules)

```java
// api/ package
package com.akif.car.api;

public record CarDto(...) {}           // ✅ Public DTO
public interface CarService {}         // ✅ Public interface
```

```java
// Events at top-level (or in api/)
package com.akif.car;

public record CarReservedEvent(...) {} // ✅ Public event
```

### Internal (Module-Private)

```java
// Internal package
package com.akif.car.internal;

@Service
class CarServiceImpl {}        // ❌ Cannot be accessed directly
class PricingCalculator {}     // ❌ Module-internal helper
```

### API Package Pattern

```java
// com.akif.car.api.CarService.java
package com.akif.car.api;

@NamedInterface("api")  // Explicitly named as public API
public interface CarService {
    CarDto getCarById(Long id);
    boolean isCarAvailable(Long carId, LocalDate start, LocalDate end);
    void reserveCar(Long carId);
    void releaseCar(Long carId);
}
```

---

## Event-Driven Communication

### Publishing Events

```java
@Service
class RentalServiceImpl {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public void confirmRental(Long rentalId) {
        // Business logic...
        
        // Publish event (no entity references!)
        eventPublisher.publishEvent(new RentalConfirmedEvent(
            rental.getId(),
            rental.getUserEmail(),     // ✅ Primitive
            rental.getCarBrand(),      // ✅ String
            rental.getTotalPrice()     // ✅ BigDecimal
        ));
    }
}
```

### Event Record Design

```java
// com.akif.rental.RentalConfirmedEvent.java (top-level = public)
package com.akif.rental;

public record RentalConfirmedEvent(
    Long rentalId,
    String userEmail,
    String carBrand,
    String carModel,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal totalPrice
) {}

// ❌ NEVER:
// public record RentalConfirmedEvent(Rental rental) {}
```

### Listening to Events

```java
// com.akif.notification.listener.EmailEventListener.java
package com.akif.notification.listener;

@Component
class EmailEventListener {
    
    private final EmailNotificationService emailService;
    
    @EventListener
    @Async
    public void handleRentalConfirmed(RentalConfirmedEvent event) {
        emailService.sendRentalConfirmation(
            event.userEmail(),
            event.carBrand(),
            event.startDate(),
            event.totalPrice()
        );
    }
}
```

---

## Testing Modules

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class RentalServiceTest {
    
    @Mock
    private CarService carService;  // Mock public API
    
    @Mock
    private AuthService authService;  // Mock public API
    
    @InjectMocks
    private RentalServiceImpl rentalService;
    
    @Test
    void shouldCreateRental() {
        // Arrange
        when(carService.getCarById(1L)).thenReturn(testCarDto());
        when(authService.getUserByUsername("john")).thenReturn(testUserDto());
        
        // Act & Assert...
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Transactional
class RentalModuleIntegrationTest {
    
    @Autowired
    private RentalService rentalService;  // Use public API
    
    @MockBean
    private PaymentService paymentService;  // Mock external module
    
    @Test
    void shouldConfirmRental() {
        // Test with real services within module
        // Mock external module dependencies
    }
}
```

### Modularity Tests

```java
// src/test/java/com/akif/ModularityTests.java
class ModularityTests {
    
    private final ApplicationModules modules = 
        ApplicationModules.of(CarRentalApplication.class);
    
    @Test
    void verifiesModularStructure() {
        modules.verify();  // Fails if boundaries violated
    }
}
```

---

## Anti-Patterns to Avoid

### ❌ 1. Cross-Module Repository Access

```java
// WRONG: Accessing another module's repository
@Service
class RentalServiceImpl {
    private final CarRepository carRepository;  // ❌ NEVER!
}

// CORRECT: Use public API
@Service
class RentalServiceImpl {
    private final CarService carService;  // ✅
}
```

### ❌ 2. Entity References in Events

```java
// WRONG: Entity in event
public record RentalConfirmedEvent(Rental rental) {}  // ❌

// CORRECT: Primitive data only
public record RentalConfirmedEvent(
    Long rentalId,
    String userEmail,
    BigDecimal totalPrice
) {}  // ✅
```

### ❌ 3. Business Logic in Shared Kernel

```java
// WRONG: Business logic in shared
package com.akif.shared.service;
public class PenaltyCalculator {}  // ❌

// CORRECT: Keep in domain module
package com.akif.rental.internal.penalty;
class PenaltyCalculationService {}  // ✅
```

### ❌ 4. Circular Dependencies

```java
// WRONG: A depends on B, B depends on A
// rental → car (reserveCar)
// car → rental (getActiveRentals)  // ❌ Circular!

// CORRECT: Use events
// car publishes CarAvailabilityRequested
// rental listens and responds
```

### ❌ 5. Skipping package-info.java

```java
// Every module MUST have package-info.java
@ApplicationModule(allowedDependencies = {"shared"})
package com.akif.newmodule;
```

---

## Verification Commands

### Run ModularityTests

```bash
mvn test -Dtest=ModularityTests
```

### Check Shared Kernel Size

```bash
# Linux/Mac
find src/main/java -path "*/shared/*" -name "*.java" ! -name "package-info.java" | wc -l

# Windows PowerShell
(Get-ChildItem -Path "src\main\java\com\akif\shared" -Filter "*.java" -Recurse | Where-Object { $_.Name -ne "package-info.java" }).Count
```

### List Module Dependencies

```bash
grep -r "allowedDependencies" src/main/java/com/akif/*/package-info.java
```

### Generate Documentation

```bash
mvn test -Dtest=ModularityTests#createsModuleDocumentation
# Output: target/spring-modulith-docs/
```

### Full Test Suite

```bash
mvn clean test
```

---

## Quick Reference

### Module Checklist

- [ ] `package-info.java` created
- [ ] Public API interface in `api/`
- [ ] Public DTOs in `api/` package
- [ ] Entities in `domain/`
- [ ] Services in `internal/`
- [ ] Controllers in `web/`
- [ ] `ModularityTests` passes

### Dependency Checklist

- [ ] Only access public APIs
- [ ] No repository injection from other modules
- [ ] No entity references in events
- [ ] Denormalized data for cross-module queries
