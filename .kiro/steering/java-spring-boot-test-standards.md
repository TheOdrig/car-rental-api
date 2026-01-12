# Java Spring Boot - Test Standards

> Follow these rules when writing tests for new features.

## 📑 TABLE OF CONTENTS

| # | Section | Description |
|---|---------|-------------|
| 1 | [⚡ Quick Start](#-quick-start) | Unit/Integration quick start |
| 2 | [🗺️ Navigation](#️-navigation) | What I'm doing → Where to look |
| 3 | [🚨 Always Do This](#-always-do-this-when-writing-tests) | 5 critical checks |
| 4 | [🔴 Critical Rules](#-critical-rules) | Summary table |
| 5 | [📁 Test Types and Location](#-test-types-and-location) | Unit/Integration/E2E |
| 6 | [🧪 Unit Test Template](#-unit-test-template) | Mockito template |
| 7 | [🌐 Integration Test Template](#-integration-test-template) | MockMvc template |
| 8 | [🎯 Modular API Mock Guide](#-modular-api-mock-guide) | Service mock patterns |
| 9 | [🏭 Test Fixture Factory](#-test-fixture-factory-pattern) | Helper method patterns |
| 10 | [✅ Assertion Style](#-assertion-style) | AssertJ usage |
| 11 | [🧹 Test Cleanup Order](#-test-cleanup-order) | FK constraint cleanup |

---

## ⚡ QUICK START

**Writing a Unit Test:**
```java
@ExtendWith(MockitoExtension.class)         // 1. Mockito
class YourServiceTest {
    @Mock private AuthService authService;   // 2. Public API mock (NOT Repository!)
    @InjectMocks private YourServiceImpl service;
}
```

**Writing an Integration Test:**
```java
@SpringBootTest @Transactional @ActiveProfiles("test")
class YourControllerIT { ... }
```

---

## 🗺️ NAVIGATION

| I'm doing | Go to → |
|-----------|---------|
| Writing unit test | **UNIT TEST TEMPLATE** |
| Writing integration test | **INTEGRATION TEST TEMPLATE** |
| Setting up mocks | **MODULAR API MOCK GUIDE** |
| Creating test fixtures | **TEST FIXTURE FACTORY PATTERN** |
| Writing assertions | **ASSERTION STYLE** |
| Test cleanup / FK error | **TEST CLEANUP ORDER** |

---

## 🚨 ALWAYS DO THIS WHEN WRITING TESTS

### 1. Check DTO Constructor Order
```java
// ❌ WRONG - Order is wrong, compilation error
new DamageReportRequest("description", DamageSeverity.MINOR, "location", DamageCategory.SCRATCH);

// ✅ CORRECT - Open source file, copy the order
new DamageReportRequest(
    "description",           // 1. description
    "location",              // 2. location
    DamageSeverity.MINOR,    // 3. severity
    DamageCategory.SCRATCH   // 4. category
);
```

### 2. Check Service APIs
```java
// ❌ WRONG - Using repository mock (modular architecture violation)
@Mock private UserRepository userRepository;
when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

// ✅ CORRECT - Use public API service mock
@Mock private AuthService authService;
when(authService.getUserByUsername("admin")).thenReturn(userDto);
```

### 3. Use Record Accessors
```java
// ❌ WRONG - Records don't have getters
result.getId();

// ✅ CORRECT - Record accessor pattern
result.id();
```

### 4. Use Helper Methods for Multi-Field Records

> 📖 For detailed examples see: **TEST FIXTURE FACTORY PATTERN** section

```java
// ❌ WRONG - Repeating 22 nulls in every test
new RentalResponse(1L, null, null, null, ... 22 times ...);

// ✅ CORRECT - DRY with helper method
confirmedResponse = createRentalResponse(RentalStatus.CONFIRMED);
```

### 5. Check Enum JSON Serialization (@JsonValue TRAP!)
```java
// RentalStatus enum has @JsonValue:
// @JsonValue
// public String getDisplayName() { return displayName; }

// ❌ WRONG - Using name() for RentalResponse.status
.andExpect(jsonPath("$.status").value(RentalStatus.CONFIRMED.name()));
// Expected: "CONFIRMED" → Actual: "Confirmed" (ERROR!)

// ✅ CORRECT - Enum with @JsonValue uses displayName in API response
.andExpect(jsonPath("$.status").value(RentalStatus.CONFIRMED.getDisplayName()));
// Serializes as "Confirmed" in JSON

// ⚠️ CAUTION: Some DTOs (e.g., QuickActionResultDto) set status as String
// using response.status().name(), in that case use name()!
.andExpect(jsonPath("$.newStatus").value(RentalStatus.CONFIRMED.name()));

// RULE: ALWAYS check the JSON structure returned by the API!
// - If enum type in Entity/DTO has @JsonValue → getDisplayName()
// - If String field is manually set with name() → name()
```

---

## 🔴 CRITICAL RULES

| Rule | ✅ CORRECT | ❌ WRONG |
|------|-----------|----------|
| Integration test mock | `@MockitoBean` | `@MockBean` (deprecated) |
| Unit test mock | `@Mock` + `@InjectMocks` | `@Autowired` |
| Cross-module mock | Service API mock | Repository mock |
| DTO | Record accessor `id()` | Getter `getId()` |
| **@DisplayName language** | **English** | **Turkish** |
| **Test Fixture** | **DRY with helper method** | **Repeating 22 nulls** |

### Spring Boot 3.4+ Import
```java
// ❌ OLD - DO NOT USE
import org.springframework.boot.test.mock.mockito.MockBean;

// ✅ NEW - USE THIS
import org.springframework.test.context.bean.override.mockito.MockitoBean;
```

---

## 📁 TEST TYPES AND LOCATION

| Test Type | Location | When | Annotations |
|-----------|----------|------|-------------|
| **Unit** | `module/unit/` | Service business logic | `@ExtendWith(MockitoExtension.class)` |
| **Integration** | `module/integration/` | Controller API test | `@SpringBootTest` + `@AutoConfigureMockMvc` |
| **E2E** | `module/e2e/` | End-to-end flow | `@SpringBootTest` + `E2ETestBase` |

```
src/test/java/com/akif/{module}/
├── unit/                    # Service tests (Mockito only)
│   └── {Service}Test.java
├── integration/             # Controller tests (Spring + MockMvc)
│   └── {Controller}IntegrationTest.java
└── e2e/                     # Flow tests (Full context)
    └── {Feature}E2ETest.java
```

---

## 🧪 UNIT TEST TEMPLATE

Use this template when writing unit tests for a new service:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("YourServiceImpl Unit Tests")
class YourServiceImplTest {

    // 1. Mock for internal dependencies
    @Mock
    private YourRepository yourRepository;
    
    @Mock
    private YourMapper yourMapper;
    
    // 2. PUBLIC API mock for external module dependencies
    @Mock
    private AuthService authService;      // auth module
    
    @Mock
    private CarService carService;        // car module
    
    @Mock
    private PaymentService paymentService; // payment module
    
    @Mock
    private RentalService rentalService;  // rental module

    @InjectMocks
    private YourServiceImpl service;

    // Test data
    private UserDto testUser;
    private YourEntity testEntity;

    @BeforeEach
    void setUp() {
        // Create DTOs (NOT Entities!)
        testUser = new UserDto(
            1L,                    // id
            "testuser",            // username
            "test@example.com",    // email
            "Test",                // firstName
            "User",                // lastName
            true,                  // enabled
            false                  // isAdmin
        );
        
        testEntity = YourEntity.builder()
            .id(1L)
            .name("test")
            .build();
    }

    @Nested
    @DisplayName("Create Operations")
    class CreateOperations {

        @Test
        @DisplayName("Should create successfully")
        void shouldCreateSuccessfully() {
            // Given
            YourRequest request = new YourRequest("data");
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(yourRepository.save(any())).thenReturn(testEntity);

            // When
            YourResponse result = service.create(request, "testuser");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            verify(yourRepository).save(any());
        }

        @Test
        @DisplayName("Should throw when not found")
        void shouldThrowWhenNotFound() {
            // Given
            when(yourRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(NotFoundException.class);
        }
    }
}
```

---

## 🌐 INTEGRATION TEST TEMPLATE

> 📖 For full example, check existing integration test files (e.g., `RentalControllerIntegrationTest`)

```java
@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("YourController Integration Tests")
class YourControllerIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    private MockMvc mockMvc;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Create admin user + token
        User admin = createEnabledUser("admin", Role.USER, Role.ADMIN);
        adminToken = "Bearer " + generateToken(admin);
    }

    // Helper methods - see TEST FIXTURE FACTORY section
    private User createEnabledUser(String username, Role... roles) { ... }
    private String generateToken(User user) { ... }

    @Test
    @DisplayName("Should create successfully as admin")
    void shouldCreateSuccessfullyAsAdmin() throws Exception {
        YourRequest request = new YourRequest("data");

        mockMvc.perform(post("/api/admin/yours")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }
}
```

### Integration Test Checklist
- [ ] `@SpringBootTest` + `@Transactional` + `@ActiveProfiles("test")`
- [ ] Setup MockMvc with `springSecurity()`
- [ ] Create test user/admin and get token
- [ ] Write `@DisplayName` in ENGLISH

---

## 🎯 MODULAR API MOCK GUIDE

> ⚠️ DTO constructor orders may change! **ALWAYS** check source file or use helper methods.

### AuthService (auth module)
```java
@Mock private AuthService authService;

// Source: com.akif.auth.api.AuthService
when(authService.getUserByUsername("username"))
    .thenReturn(createUserDto()); // << Use helper method!
```

### CarService (car module)
```java
@Mock private CarService carService;

// Source: com.akif.car.api.CarService
when(carService.getCarDtoById(1L)).thenReturn(createCarDto());

// Void methods
doNothing().when(carService).markAsMaintenance(1L);
doNothing().when(carService).releaseCar(1L);
```

### RentalService (rental module)
```java
@Mock private RentalService rentalService;

// Source: com.akif.rental.api.RentalService
when(rentalService.getRentalSummaryById(1L)).thenReturn(createRentalSummary());
doNothing().when(rentalService).incrementDamageReportCount(1L);
```

### PaymentService (payment module)
```java
@Mock private PaymentService paymentService;

// Source: com.akif.payment.api.PaymentService
when(paymentService.getPaymentByRentalId(1L)).thenReturn(Optional.of(createPaymentDto()));
when(paymentService.refundPayment(eq(1L), any())).thenReturn(PaymentResult.success("txn", "ok"));
```

> 📖 For helper method examples see: **TEST FIXTURE FACTORY PATTERN** section

---

## 🏭 TEST FIXTURE FACTORY PATTERN

### Why Use?

Use factory methods instead of **writing 22 nulls in every test** for multi-field records.

### Helper Methods in Test Class

```java
@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    // Test fixtures
    private UserDto testUser;
    private CarDto testCar;
    
    @BeforeEach
    void setUp() {
        testUser = createUserDto();
        testCar = createCarDto();
    }
    
    // ✅ CORRECT - Helper method
    private UserDto createUserDto() {
        return new UserDto(
            1L,                    // id
            "testuser",            // username
            "test@example.com",    // email
            "Test",                // firstName
            "User",                // lastName
            true,                  // enabled
            false                  // isAdmin
        );
    }
    
    private CarDto createCarDto() {
        return new CarDto(
            1L,                    // id
            "34ABC123",            // licensePlate
            "Toyota",              // brand
            "Corolla",             // model
            2024,                  // year
            CarStatus.AVAILABLE,   // status
            new BigDecimal("500.00") // dailyPrice
        );
    }
    
    // Overload for cases requiring status change
    private RentalResponse createRentalResponse(RentalStatus status) {
        return new RentalResponse(
            1L, 1L, 1L, null, null, null, null, null, null,
            status,
            null, null, null, null, null, null, null, null, null, null, null, null
        );
    }
}
```

### Entity Factory for Integration Tests

```java
@SpringBootTest
@Transactional
class RentalControllerIntegrationTest {

    private Car createAvailableCar() {
        Car car = Car.builder()
            .licensePlate("34TEST" + System.currentTimeMillis())
            .brand("Toyota")
            .model("Corolla")
            .year(2024)
            .status(CarStatus.AVAILABLE)
            .dailyPrice(new BigDecimal("500.00"))
            .build();
        return carRepository.save(car);
    }
    
    private User createEnabledUser(String username, Role... roles) {
        User user = User.builder()
            .username(username)
            .email(username + "@test.com")
            .password(passwordEncoder.encode("password123"))
            .roles(Set.of(roles))
            .enabled(true)
            .build();
        return userRepository.save(user);
    }
}
```

---

## ✅ ASSERTION STYLE

### Use AssertJ (NOT JUnit assertions)

```java
// ✅ CORRECT - AssertJ (fluent, readable)
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

assertThat(result).isNotNull();
assertThat(result.id()).isEqualTo(1L);
assertThat(result.status()).isEqualTo(RentalStatus.CONFIRMED);
assertThat(rentals).hasSize(3);
assertThat(rentals).extracting("status").containsOnly(RentalStatus.CONFIRMED);

// Exception assertion
assertThatThrownBy(() -> service.getById(999L))
    .isInstanceOf(NotFoundException.class)
    .hasMessageContaining("not found");
```

### ❌ WRONG - JUnit assertions
```java
// ❌ DO NOT USE - Less readable
import static org.junit.jupiter.api.Assertions.*;

assertEquals(RentalStatus.CONFIRMED, result.status());
assertNotNull(result);
assertTrue(result.isActive());
```

### Collection Assertions
```java
// ✅ CORRECT - Check collection contents
assertThat(cars)
    .hasSize(3)
    .extracting(CarDto::brand)
    .containsExactlyInAnyOrder("Toyota", "Honda", "BMW");

assertThat(rentals)
    .filteredOn(r -> r.status() == RentalStatus.CONFIRMED)
    .hasSize(2);
```

### MockMvc JSON Assertions
```java
// ✅ CORRECT
mockMvc.perform(get("/api/cars/1"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.id").value(1))
    .andExpect(jsonPath("$.brand").value("Toyota"))
    .andExpect(jsonPath("$.status").value("AVAILABLE"));
```

---

## ✅ TEST WRITING CHECKLIST

### When Writing Unit Tests
- [ ] Open DTO source file, check constructor order
- [ ] Use PUBLIC API service mock for external module dependencies (NOT Repository!)
- [ ] Use DTO instead of Entity (when possible)
- [ ] Use `@ExtendWith(MockitoExtension.class)`
- [ ] Follow Given-When-Then structure
- [ ] **Write @DisplayName in ENGLISH** (project language is English!)

### When Writing Integration Tests
- [ ] Use `@SpringBootTest` + `@AutoConfigureWebMvc` + `@ActiveProfiles("test")`
- [ ] Add `@Transactional` (for rollback at test end)
- [ ] Create test data with Repository (this is ACCEPTABLE)
- [ ] Don't forget to create token
- [ ] Check HTTP status and JSON response

### Cross-Module Tests
- [ ] Do NOT import other module's INTERNAL classes
- [ ] Communicate via Public API
- [ ] Use DTOs, not Entities

---

## 🧹 TEST CLEANUP ORDER

> ⚠️ Clean up in **correct order** to avoid FK constraint errors!

### E2E / Integration Test Cleanup
```java
@AfterEach
void tearDown() {
    // 1. Most dependent tables FIRST (child)
    damageReportRepository.deleteAll();
    paymentRepository.deleteAll();
    rentalRepository.deleteAll();
    
    // 2. Parent tables AFTER
    carRepository.deleteAll();
    userRepository.deleteAll();
}
```

### Cleanup Order Rule
```
FK Chain: User → Rental → Payment → DamageReport
                    ↓
                   Car

Cleanup: DamageReport → Payment → Rental → Car → User (REVERSE ORDER!)
```

### Cache Cleanup (If Needed)
```java
@Autowired
private CacheManager cacheManager;

@BeforeEach
void setUp() {
    cacheManager.getCacheNames().forEach(name -> 
        Objects.requireNonNull(cacheManager.getCache(name)).clear()
    );
}
```

---

## 🏃 RUNNING TESTS

```bash
# All tests
mvn test

# Specific module
mvn test -Dtest="com.akif.rental.**"
mvn test -Dtest="com.akif.damage.**"

# By test type
mvn test -Dtest="**/*Test"               # Unit
mvn test -Dtest="**/*IntegrationTest"    # Integration
mvn test -Dtest="**/*E2ETest"            # E2E

# Single test class
mvn test -Dtest="RentalServiceImplTest"
```

---

**Last Updated:** 2026-01-02 | **Spring Boot:** 3.5.3 | **Status:** ✅ Active
