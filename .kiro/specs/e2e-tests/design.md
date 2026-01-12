# E2E Tests Design Document

## Overview

Bu doküman, Rent-a-Car uygulaması için End-to-End (E2E) test suite'inin teknik tasarımını tanımlar. E2E testler, tüm Tier 1 özelliklerinin (Rental, Currency Conversion, Dynamic Pricing, OAuth2, Payment Gateway, Email Notification) birlikte çalışmasını doğrular.

### Amaç
- Gerçek kullanıcı senaryolarını simüle etmek
- Tüm sistem bileşenlerinin entegrasyonunu test etmek
- Mevcut integration testlerden farklı olarak uçtan uca akışları doğrulamak

### Kapsam
- Complete rental lifecycle (request → confirm → pickup → return)
- Cancellation ve refund akışları
- Dynamic pricing entegrasyonu
- Currency conversion entegrasyonu
- Payment gateway (StubPaymentGateway) entegrasyonu
- Email event publishing
- OAuth2 authentication
- Authorization ve security
- Error handling
- Concurrency

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        E2E Test Suite                                │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │ Scenario Tests  │  │ Integration     │  │ Concurrency     │     │
│  │ (Full Flows)    │  │ Tests           │  │ Tests           │     │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘     │
│           │                    │                    │               │
│  ┌────────▼────────────────────▼────────────────────▼────────┐     │
│  │                    Test Infrastructure                     │     │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │     │
│  │  │ TestDataBuilder│ │ EventCaptor  │  │ MockServices │     │     │
│  │  └──────────────┘  └──────────────┘  └──────────────┘     │     │
│  └───────────────────────────────────────────────────────────┘     │
├─────────────────────────────────────────────────────────────────────┤
│                    Application Under Test                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                 │
│  │ Controllers │  │ Services    │  │ Repositories│                 │
│  └─────────────┘  └─────────────┘  └─────────────┘                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                 │
│  │ StubPayment │  │ MockEmail   │  │ H2 Database │                 │
│  │ Gateway     │  │ Sender      │  │             │                 │
│  └─────────────┘  └─────────────┘  └─────────────┘                 │
└─────────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Test Infrastructure Components

#### TestDataBuilder
Test verisi oluşturmak için builder pattern kullanan utility sınıfı.

```java
public class TestDataBuilder {
    public static User createTestUser(String username, Role... roles);
    public static Car createAvailableCar(String licensePlate, BigDecimal price);
    public static RentalRequestDto createRentalRequest(Long carId, LocalDate start, LocalDate end);
}
```

#### EventCaptor
Domain event'lerini yakalamak için test utility.

```java
@Component
@Profile("test")
public class TestEventCaptor {
    private List<Object> capturedEvents = new ArrayList<>();
    
    @EventListener
    public void captureEvent(Object event) {
        capturedEvents.add(event);
    }
    
    public <T> List<T> getEventsOfType(Class<T> eventType);
    public void clear();
}
```

#### E2ETestBase
Tüm E2E testlerin extend edeceği base class (Spring Boot 3.5+ uyumlu).

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class E2ETestBase {
    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JwtTokenProvider tokenProvider;
    @Autowired protected TestEventCaptor eventCaptor;
    
    // Spring Boot 3.4+ için @MockitoBean kullanılır
    @MockitoBean protected ReminderScheduler reminderScheduler;
    
    protected String generateUserToken(User user);
    protected String generateAdminToken(User admin);
    protected Long createAndGetRentalId(RentalRequestDto request, String token);
}
```

> **Import:** `import org.springframework.test.context.bean.override.mockito.MockitoBean;`

### 2. Test Categories

#### Scenario Tests
Tam kullanıcı senaryolarını test eden sınıflar:
- `RentalLifecycleE2ETest` - Tam kiralama akışı
- `CancellationRefundE2ETest` - İptal ve iade akışları
- `PricingIntegrationE2ETest` - Dynamic pricing entegrasyonu
- `CurrencyConversionE2ETest` - Döviz dönüşümü

#### Security Tests
Yetkilendirme ve güvenlik testleri:
- `AuthorizationE2ETest` - Role-based access control
- `OAuth2IntegrationE2ETest` - Social login entegrasyonu

#### Edge Case Tests
Hata durumları ve edge case'ler:
- `ErrorHandlingE2ETest` - Hata yönetimi
- `ConcurrencyE2ETest` - Eşzamanlılık testleri

## Data Models

### Test Configuration

```java
@TestConfiguration
public class E2ETestConfig {
    
    @Bean
    @Primary
    public IPaymentGateway stubPaymentGateway() {
        return new StubPaymentGateway();
    }
    
    @Bean
    @Primary
    public IEmailSender mockEmailSender() {
        return new MockEmailSender();
    }
    
    @Bean
    public TestEventCaptor testEventCaptor() {
        return new TestEventCaptor();
    }
}
```

### Test Data Fixtures

```java
public class TestFixtures {
    public static final BigDecimal BASE_PRICE = new BigDecimal("500.00");
    public static final CurrencyType DEFAULT_CURRENCY = CurrencyType.TRY;
    
    public static final LocalDate FUTURE_START = LocalDate.now().plusDays(1);
    public static final LocalDate FUTURE_END = LocalDate.now().plusDays(5);
    
    public static final LocalDate EARLY_BOOKING_START = LocalDate.now().plusDays(35);
    public static final LocalDate EARLY_BOOKING_END = LocalDate.now().plusDays(40);
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. 
Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, the following correctness properties have been identified:

### Property 1: Pricing Modifier Combination
*For any* rental request with multiple applicable pricing strategies, the final price SHALL equal the base price multiplied by all applicable modifiers combined.

**Validates: Requirements 3.4**

### Property 2: Date Overlap Prevention
*For any* confirmed rental for a specific car and date range, all subsequent rental requests for overlapping dates on the same car SHALL be rejected with HTTP 400.

**Validates: Requirements 8.1**

### Property 3: Authorization Enforcement
*For any* admin-only operation (confirm, pickup, return) attempted by a USER role, the system SHALL return HTTP 403 Forbidden.

**Validates: Requirements 9.1, 9.2**

### Property 4: State Transition Validity
*For any* rental, only valid state transitions SHALL be allowed:
- REQUESTED → CONFIRMED (via confirm)
- REQUESTED → CANCELLED (via cancel)
- CONFIRMED → IN_USE (via pickup)
- CONFIRMED → CANCELLED (via cancel)
- IN_USE → RETURNED (via return)
- IN_USE → CANCELLED (via cancel)

All other transitions SHALL return an error.

**Validates: Requirements 10.3**

## Error Handling

### Test Failure Scenarios

| Scenario | Expected Behavior | Verification |
|----------|-------------------|--------------|
| Car not found | HTTP 404 | Assert status code and error message |
| Car not available | HTTP 400 | Assert status code and error code |
| Invalid state transition | HTTP 400 | Assert error code matches InvalidRentalStateException |
| Unauthorized access | HTTP 403 | Assert status code |
| Unauthenticated access | HTTP 401/403 | Assert status code |
| Date overlap | HTTP 400 | Assert error code matches RentalDateOverlapException |
| Payment failure | HTTP 402 | Assert rental state unchanged |

### Assertion Patterns

```java
// Status assertion
.andExpect(status().isCreated())
.andExpect(jsonPath("$.status").value("Requested"))

// Error assertion
.andExpect(status().isBadRequest())
.andExpect(jsonPath("$.errorCode").value("CAR_NOT_AVAILABLE"))

// Event assertion
assertThat(eventCaptor.getEventsOfType(RentalConfirmedEvent.class))
    .hasSize(1)
    .first()
    .satisfies(event -> {
        assertThat(event.getRentalId()).isEqualTo(rentalId);
    });
```

## Testing Strategy

### Dual Testing Approach

E2E testler iki ana kategoride organize edilir:

#### 1. Scenario-Based Tests (Example Tests)
Spesifik kullanıcı senaryolarını test eden testler:
- Full rental lifecycle
- Cancellation flows
- Specific pricing scenarios
- Currency conversion scenarios

#### 2. Property-Based Tests
Genel kuralları doğrulayan testler:
- Pricing modifier combination
- Date overlap prevention
- Authorization enforcement
- State transition validity

### Test Framework (Spring Boot 3.5+ Güncel)

- **JUnit 5**: Test framework
- **Spring Boot Test**: Integration test support (`@SpringBootTest`, `@AutoConfigureMockMvc`)
- **MockMvc**: HTTP request simulation
- **AssertJ**: Fluent assertions
- **@MockitoBean**: Spring Boot 3.4+ için mock annotation (deprecated `@MockBean` yerine)
- **@MockitoSpyBean**: Spring Boot 3.4+ için spy annotation (deprecated `@SpyBean` yerine)

> **NOT:** Spring Boot 3.4.0'dan itibaren `@MockBean` ve `@SpyBean` deprecated edildi. 
> Testlerde `@MockitoBean` ve `@MockitoSpyBean` kullanılacak.

### Test Execution Order

1. **Setup Phase**: Test data oluşturma, mock'ları yapılandırma
2. **Action Phase**: API çağrıları yapma
3. **Verification Phase**: Response ve side effect'leri doğrulama
4. **Cleanup Phase**: @Transactional ile otomatik rollback

### Test Isolation

- Her test `@Transactional` ile izole edilir
- Event captor her testten önce temizlenir
- Mock'lar her testten önce reset edilir

### Test Naming Convention

```
should{ExpectedBehavior}_when{Condition}

Örnekler:
- shouldCreateRentalWithRequestedStatus_whenValidRequest
- shouldReturn403_whenUserAttemptsAdminOperation
- shouldApplyAllPricingModifiers_whenMultipleStrategiesApply
```

### Coverage Targets

| Category | Target |
|----------|--------|
| Rental Lifecycle | 100% of states |
| Pricing Strategies | All 5 strategies |
| Currency Conversion | All supported currencies |
| Authorization | All protected endpoints |
| Error Scenarios | All defined error codes |

