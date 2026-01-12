# 🧪 Learning Sandbox - İleri Konular

> **Amaç:** Bu dosya rent-a-car projesine EKLENMEYECEK, ayrı sandbox projelerinde öğrenilecek konuları içerir.
> Overengineering'den kaçınmak için bu konuları ayrı tutuyoruz.

---

## 📋 İçindekiler
1. [Design Patterns (GoF)](#-design-patterns-gof)
2. [Enterprise Patterns](#-enterprise-patterns)
3. [Spring AOP](#-spring-aop)
4. [Spring WebFlux](#-spring-webflux)
5. [Öğrenme Stratejisi](#-öğrenme-stratejisi)

---

## 🎨 Design Patterns (GoF)

### Creational Patterns

#### 1. Factory Pattern
**Ne zaman kullan:** Nesne yaratım logic'i karmaşıklaştığında
```java
// Sandbox projesi: payment-factory-demo
public interface PaymentGateway {
    PaymentResult process(PaymentRequest request);
}

public class PaymentGatewayFactory {
    public static PaymentGateway create(PaymentType type) {
        return switch (type) {
            case CREDIT_CARD -> new CreditCardGateway();
            case PAYPAL -> new PayPalGateway();
            case CRYPTO -> new CryptoGateway();
        };
    }
}
```
**Egzersiz:** Farklı araba tipleri için `CarFactory` yaz (Sedan, SUV, Sports)

#### 2. Builder Pattern
**Ne zaman kullan:** Çok parametreli nesne yaratımında
```java
Rental rental = Rental.builder()
    .carId(1L)
    .userId(2L)
    .startDate(LocalDate.now())
    .endDate(LocalDate.now().plusDays(7))
    .build();
```
**Egzersiz:** `RentalRequestBuilder` yaz, validation ile

#### 3. Singleton Pattern
**Ne zaman kullan:** Tek instance gerektiğinde (Spring @Bean zaten bunu yapar)
```java
@Configuration
public class AppConfig {
    @Bean
    public PaymentGateway paymentGateway() {
        return new StripePaymentGateway(); // Singleton by default
    }
}
```


### Behavioral Patterns

#### 4. Strategy Pattern
**Ne zaman kullan:** Aynı işi farklı yollarla yapmak gerektiğinde
```java
// Sandbox projesi: pricing-strategy-demo
public interface PricingStrategy {
    BigDecimal calculate(Car car, int days);
}

@Component
public class StandardPricing implements PricingStrategy {
    public BigDecimal calculate(Car car, int days) {
        return car.getDailyRate().multiply(BigDecimal.valueOf(days));
    }
}

@Component
public class WeekendPricing implements PricingStrategy {
    public BigDecimal calculate(Car car, int days) {
        return car.getDailyRate()
            .multiply(BigDecimal.valueOf(1.2)) // %20 weekend surcharge
            .multiply(BigDecimal.valueOf(days));
    }
}

// Kullanım
@Service
public class RentalPricingService {
    private final Map<String, PricingStrategy> strategies;
    
    public BigDecimal calculatePrice(Car car, int days, String strategyType) {
        return strategies.get(strategyType).calculate(car, days);
    }
}
```
**Egzersiz:** Farklı discount stratejileri yaz (Student, Senior, Corporate)

#### 5. Observer Pattern
**Ne zaman kullan:** Bir olay olduğunda birden fazla şeyin tepki vermesi gerektiğinde
```java
// Spring Events ile Observer Pattern
@DomainEvent
public class RentalCreatedEvent {
    private final Long rentalId;
    private final Long userId;
}

@Component
public class EmailNotificationListener {
    @EventListener
    public void onRentalCreated(RentalCreatedEvent event) {
        // Email gönder
    }
}

@Component
public class AuditLogListener {
    @EventListener
    public void onRentalCreated(RentalCreatedEvent event) {
        // Audit log yaz
    }
}
```
**Egzersiz:** SMS, Push notification listener'ları ekle

#### 6. Template Method Pattern
**Ne zaman kullan:** Algoritma iskeleti sabit, adımlar değişken olduğunda
```java
public abstract class RentalProcessTemplate {
    
    // Template method - final, değiştirilemez
    public final RentalResult process(RentalRequest request) {
        validate(request);
        Car car = reserveCar(request);
        PaymentResult payment = processPayment(request);
        Rental rental = createRental(request, car, payment);
        sendNotification(rental);
        return new RentalResult(rental);
    }
    
    // Hook methods - subclass'lar override eder
    protected abstract void validate(RentalRequest request);
    protected abstract PaymentResult processPayment(RentalRequest request);
    
    // Default implementations
    protected Car reserveCar(RentalRequest request) { /* default */ }
    protected void sendNotification(Rental rental) { /* default */ }
}

public class StandardRentalProcess extends RentalProcessTemplate {
    @Override
    protected void validate(RentalRequest request) {
        // Standard validation
    }
    
    @Override
    protected PaymentResult processPayment(RentalRequest request) {
        // Standard payment
    }
}

public class PremiumRentalProcess extends RentalProcessTemplate {
    @Override
    protected void validate(RentalRequest request) {
        // Premium validation (VIP check, etc.)
    }
    
    @Override
    protected PaymentResult processPayment(RentalRequest request) {
        // Premium payment (loyalty points, etc.)
    }
}
```
**Egzersiz:** `ReportGenerationTemplate` yaz (PDF, Excel, CSV)

#### 7. Chain of Responsibility
**Ne zaman kullan:** Request'i sırayla işleyecek handler'lar olduğunda
```java
public interface ValidationHandler {
    void setNext(ValidationHandler next);
    void validate(RentalRequest request);
}

public class DateValidationHandler implements ValidationHandler {
    private ValidationHandler next;
    
    @Override
    public void validate(RentalRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new ValidationException("Invalid date range");
        }
        if (next != null) next.validate(request);
    }
}

public class CarAvailabilityHandler implements ValidationHandler {
    private ValidationHandler next;
    
    @Override
    public void validate(RentalRequest request) {
        if (!carService.isAvailable(request.getCarId(), request.getStartDate())) {
            throw new ValidationException("Car not available");
        }
        if (next != null) next.validate(request);
    }
}

// Kullanım
ValidationHandler chain = new DateValidationHandler();
chain.setNext(new CarAvailabilityHandler());
chain.setNext(new UserCreditHandler());
chain.validate(request);
```
**Egzersiz:** Logging, Authentication, Authorization chain yaz


### Structural Patterns

#### 8. Decorator Pattern
**Ne zaman kullan:** Mevcut nesneye dinamik olarak özellik eklemek istediğinde
```java
public interface CarService {
    CarDto getById(Long id);
}

@Service
@Primary
public class CachingCarServiceDecorator implements CarService {
    private final CarService delegate;
    private final Cache cache;
    
    @Override
    public CarDto getById(Long id) {
        return cache.get(id, () -> delegate.getById(id));
    }
}

@Service
public class LoggingCarServiceDecorator implements CarService {
    private final CarService delegate;
    
    @Override
    public CarDto getById(Long id) {
        log.info("Getting car: {}", id);
        CarDto result = delegate.getById(id);
        log.info("Found car: {}", result);
        return result;
    }
}
```
**Egzersiz:** Metrics decorator ekle (response time tracking)

#### 9. Adapter Pattern
**Ne zaman kullan:** Farklı interface'leri uyumlu hale getirmek istediğinde
```java
// External Stripe API
public class StripeApi {
    public StripeCharge createCharge(String token, int amountCents) { }
}

// Bizim interface'imiz
public interface PaymentGateway {
    PaymentResult process(PaymentRequest request);
}

// Adapter
@Component
public class StripePaymentAdapter implements PaymentGateway {
    private final StripeApi stripeApi;
    
    @Override
    public PaymentResult process(PaymentRequest request) {
        int cents = request.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
        StripeCharge charge = stripeApi.createCharge(request.getToken(), cents);
        return new PaymentResult(charge.getId(), charge.getStatus());
    }
}
```
**Egzersiz:** PayPal, Iyzico adapter'ları yaz

#### 10. Proxy Pattern
**Ne zaman kullan:** Nesneye erişimi kontrol etmek istediğinde
```java
public interface RentalService {
    RentalDto getById(Long id);
}

@Service
public class SecureRentalServiceProxy implements RentalService {
    private final RentalService delegate;
    private final SecurityContext securityContext;
    
    @Override
    public RentalDto getById(Long id) {
        RentalDto rental = delegate.getById(id);
        
        // Access control
        if (!securityContext.getCurrentUser().equals(rental.getUserId()) 
            && !securityContext.isAdmin()) {
            throw new AccessDeniedException("Not authorized");
        }
        
        return rental;
    }
}
```
**Egzersiz:** Lazy loading proxy yaz (image'ler için)

---

## 🏢 Enterprise Patterns

### 1. CQRS (Command Query Responsibility Segregation)
**Ne zaman kullan:** Read ve write operasyonları farklı optimize etmek istediğinde
```java
// Command (Write)
@Service
public class RentalCommandService {
    public Long createRental(CreateRentalCommand command) {
        // Normalized write model
        Rental rental = new Rental();
        rental.setCarId(command.getCarId());
        rental.setUserId(command.getUserId());
        return rentalRepository.save(rental).getId();
    }
}

// Query (Read)
@Service
public class RentalQueryService {
    public RentalDetailView getRentalDetail(Long id) {
        // Denormalized read model - tek query ile tüm data
        return rentalViewRepository.findDetailById(id);
    }
}

// Read model (denormalized)
@Entity
@Table(name = "rental_detail_view")
public class RentalDetailView {
    private Long id;
    private String carBrand;
    private String carModel;
    private String userName;
    private String userEmail;
    private LocalDate startDate;
    private BigDecimal totalPrice;
    // Tüm bilgiler tek tabloda
}
```
**Egzersiz:** Car listing için read-optimized view oluştur

### 2. Saga Pattern
**Ne zaman kullan:** Distributed transaction gerektiğinde
```java
// Orchestration-based Saga
@Service
public class RentalSagaOrchestrator {
    
    public RentalResult executeRentalSaga(RentalRequest request) {
        try {
            // Step 1: Reserve car
            CarReservation reservation = carService.reserve(request.getCarId());
            
            // Step 2: Process payment
            PaymentResult payment = paymentService.charge(request.getPaymentInfo());
            
            // Step 3: Create rental
            Rental rental = rentalService.create(request, reservation, payment);
            
            return RentalResult.success(rental);
            
        } catch (PaymentFailedException e) {
            // Compensating transaction
            carService.cancelReservation(reservation.getId());
            return RentalResult.failed("Payment failed");
            
        } catch (Exception e) {
            // Rollback all
            carService.cancelReservation(reservation.getId());
            paymentService.refund(payment.getId());
            return RentalResult.failed("Unexpected error");
        }
    }
}
```
**Egzersiz:** Choreography-based saga (event-driven) yaz

### 3. Event Sourcing
**Ne zaman kullan:** Tam audit trail ve state rebuild gerektiğinde
```java
// Events
public sealed interface RentalEvent {
    Long rentalId();
    Instant occurredAt();
}

public record RentalCreated(Long rentalId, Long carId, Long userId, Instant occurredAt) 
    implements RentalEvent {}

public record RentalConfirmed(Long rentalId, Long confirmedBy, Instant occurredAt) 
    implements RentalEvent {}

public record RentalPickedUp(Long rentalId, String location, Instant occurredAt) 
    implements RentalEvent {}

// Event Store
@Repository
public interface RentalEventStore {
    void append(RentalEvent event);
    List<RentalEvent> getEvents(Long rentalId);
}

// Aggregate rebuild
public class RentalAggregate {
    private Long id;
    private RentalStatus status;
    
    public static RentalAggregate rebuild(List<RentalEvent> events) {
        RentalAggregate aggregate = new RentalAggregate();
        events.forEach(aggregate::apply);
        return aggregate;
    }
    
    private void apply(RentalEvent event) {
        switch (event) {
            case RentalCreated e -> { this.id = e.rentalId(); this.status = PENDING; }
            case RentalConfirmed e -> { this.status = CONFIRMED; }
            case RentalPickedUp e -> { this.status = ACTIVE; }
        }
    }
}
```
**Egzersiz:** Car status için event sourcing yaz

### 4. Circuit Breaker (Resilience4j)
**Ne zaman kullan:** External service'lere karşı koruma gerektiğinde
```java
@Service
public class PaymentService {
    
    @CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
    @Retry(name = "payment")
    @TimeLimiter(name = "payment")
    public PaymentResult processPayment(PaymentRequest request) {
        return externalPaymentGateway.charge(request);
    }
    
    public PaymentResult paymentFallback(PaymentRequest request, Exception e) {
        log.error("Payment gateway unavailable, queuing for retry", e);
        paymentQueue.enqueue(request);
        return PaymentResult.pending("Payment queued for processing");
    }
}

// application.yml
resilience4j:
  circuitbreaker:
    instances:
      payment:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
  retry:
    instances:
      payment:
        maxAttempts: 3
        waitDuration: 1s
```
**Egzersiz:** Email service için circuit breaker ekle


---

## 🎯 Spring AOP

### AOP Nedir?
Aspect-Oriented Programming - Cross-cutting concerns'leri (logging, security, transaction) business logic'ten ayırır.

### Temel Kavramlar
```java
// Aspect - Cross-cutting concern'ü tanımlar
@Aspect
@Component
public class LoggingAspect {
    
    // Pointcut - Nerede çalışacak
    @Pointcut("execution(* com.akif.service.*.*(..))")
    public void serviceLayer() {}
    
    // Advice - Ne yapacak
    @Before("serviceLayer()")
    public void logBefore(JoinPoint joinPoint) {
        log.info("Calling: {}", joinPoint.getSignature().getName());
    }
    
    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logAfter(JoinPoint joinPoint, Object result) {
        log.info("Returned: {}", result);
    }
    
    @Around("serviceLayer()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;
        log.info("{} executed in {}ms", joinPoint.getSignature().getName(), duration);
        return result;
    }
}
```

### Pratik Kullanım Örnekleri

#### 1. Performance Monitoring
```java
@Aspect
@Component
public class PerformanceAspect {
    
    @Around("@annotation(Timed)")
    public Object measureTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        Object result = joinPoint.proceed();
        
        stopWatch.stop();
        metricsService.recordTiming(
            joinPoint.getSignature().getName(), 
            stopWatch.getTotalTimeMillis()
        );
        
        return result;
    }
}

// Kullanım
@Timed
public RentalDto createRental(RentalRequest request) { }
```

#### 2. Audit Logging
```java
@Aspect
@Component
public class AuditAspect {
    
    @AfterReturning(
        pointcut = "@annotation(auditable)",
        returning = "result"
    )
    public void audit(JoinPoint joinPoint, Auditable auditable, Object result) {
        AuditLog log = AuditLog.builder()
            .action(auditable.action())
            .entityType(auditable.entityType())
            .entityId(extractId(result))
            .userId(SecurityContextHolder.getContext().getAuthentication().getName())
            .timestamp(Instant.now())
            .build();
        
        auditRepository.save(log);
    }
}

// Kullanım
@Auditable(action = "CREATE", entityType = "RENTAL")
public Rental createRental(RentalRequest request) { }
```

#### 3. Exception Handling
```java
@Aspect
@Component
public class ExceptionHandlingAspect {
    
    @AfterThrowing(pointcut = "execution(* com.akif.service.*.*(..))", throwing = "ex")
    public void handleException(JoinPoint joinPoint, Exception ex) {
        log.error("Exception in {}: {}", 
            joinPoint.getSignature().getName(), 
            ex.getMessage(), 
            ex
        );
        
        // Alert gönder
        alertService.sendAlert(
            "Service Exception",
            joinPoint.getSignature().toString(),
            ex.getMessage()
        );
    }
}
```

#### 4. Caching with AOP
```java
@Aspect
@Component
public class CachingAspect {
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    
    @Around("@annotation(Cacheable)")
    public Object cache(ProceedingJoinPoint joinPoint) throws Throwable {
        String key = generateKey(joinPoint);
        
        if (cache.containsKey(key)) {
            log.debug("Cache hit: {}", key);
            return cache.get(key);
        }
        
        Object result = joinPoint.proceed();
        cache.put(key, result);
        log.debug("Cache miss, stored: {}", key);
        
        return result;
    }
}
```

### Egzersizler
1. **Rate Limiting Aspect:** Her user için dakikada max 10 request
2. **Retry Aspect:** Failed method'ları 3 kez retry et
3. **Security Aspect:** Method seviyesinde role check

---

## ⚡ Spring WebFlux

### WebFlux Nedir?
Reactive, non-blocking web framework. Yüksek concurrency gerektiren uygulamalar için.

### Ne Zaman Kullan?
- Çok sayıda concurrent connection (10K+)
- I/O-bound işlemler (external API calls, DB queries)
- Streaming data
- Microservices arası iletişim

### Ne Zaman KULLANMA?
- CPU-bound işlemler
- Basit CRUD uygulamaları
- Team reactive programming bilmiyorsa
- Blocking library'ler kullanıyorsan (JDBC)

### Temel Kavramlar
```java
// Mono - 0 veya 1 element
Mono<Car> car = carRepository.findById(1L);

// Flux - 0 veya N element
Flux<Car> cars = carRepository.findAll();

// Operators
Mono<CarDto> carDto = carRepository.findById(1L)
    .map(car -> carMapper.toDto(car))
    .switchIfEmpty(Mono.error(new NotFoundException("Car not found")));

Flux<CarDto> availableCars = carRepository.findAll()
    .filter(car -> car.getStatus() == AVAILABLE)
    .map(carMapper::toDto)
    .take(10);
```

### Reactive Controller
```java
@RestController
@RequestMapping("/api/reactive/cars")
public class ReactiveCarController {
    
    private final ReactiveCarService carService;
    
    @GetMapping
    public Flux<CarDto> getAllCars() {
        return carService.findAll();
    }
    
    @GetMapping("/{id}")
    public Mono<ResponseEntity<CarDto>> getCarById(@PathVariable Long id) {
        return carService.findById(id)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public Mono<ResponseEntity<CarDto>> createCar(@RequestBody Mono<CarCreateRequest> request) {
        return request
            .flatMap(carService::create)
            .map(car -> ResponseEntity.status(HttpStatus.CREATED).body(car));
    }
    
    // Server-Sent Events (real-time updates)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CarDto> streamCars() {
        return carService.findAll()
            .delayElements(Duration.ofSeconds(1)); // Her saniye bir car
    }
}
```

### Reactive Service
```java
@Service
public class ReactiveCarService {
    
    private final ReactiveCarRepository carRepository;
    private final WebClient webClient; // Reactive HTTP client
    
    public Flux<CarDto> findAll() {
        return carRepository.findAll()
            .map(carMapper::toDto);
    }
    
    public Mono<CarDto> findById(Long id) {
        return carRepository.findById(id)
            .map(carMapper::toDto);
    }
    
    // External API call (non-blocking)
    public Mono<PriceQuote> getPriceQuote(Long carId, int days) {
        return webClient.get()
            .uri("/pricing/quote?carId={carId}&days={days}", carId, days)
            .retrieve()
            .bodyToMono(PriceQuote.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> Mono.just(PriceQuote.defaultQuote()));
    }
    
    // Parallel calls
    public Mono<CarDetailDto> getCarWithDetails(Long id) {
        Mono<Car> carMono = carRepository.findById(id);
        Mono<List<Review>> reviewsMono = reviewService.getReviews(id);
        Mono<PriceQuote> priceMono = getPriceQuote(id, 1);
        
        return Mono.zip(carMono, reviewsMono, priceMono)
            .map(tuple -> new CarDetailDto(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3()
            ));
    }
}
```

### R2DBC (Reactive Database)
```java
// Dependency: spring-boot-starter-data-r2dbc

@Repository
public interface ReactiveCarRepository extends ReactiveCrudRepository<Car, Long> {
    
    Flux<Car> findByStatus(CarStatus status);
    
    @Query("SELECT * FROM cars WHERE brand = :brand")
    Flux<Car> findByBrand(String brand);
}
```

### WebClient (Reactive HTTP Client)
```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl("https://api.example.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(ExchangeFilterFunctions.basicAuthentication("user", "pass"))
            .build();
    }
}

// Kullanım
@Service
public class ExternalApiService {
    
    private final WebClient webClient;
    
    public Mono<ExternalData> fetchData(String id) {
        return webClient.get()
            .uri("/data/{id}", id)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, 
                response -> Mono.error(new ClientException("Client error")))
            .onStatus(HttpStatus::is5xxServerError,
                response -> Mono.error(new ServerException("Server error")))
            .bodyToMono(ExternalData.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }
}
```

### Egzersizler
1. **Reactive Car Search:** Filter, sort, paginate with Flux
2. **Real-time Notifications:** WebSocket + Flux for rental updates
3. **Parallel API Calls:** Fetch car + reviews + pricing in parallel
4. **Backpressure Handling:** Handle slow consumers


---

## 📚 Öğrenme Stratejisi

### Sandbox Proje Yapısı
```
learning-sandbox/
├── design-patterns-demo/
│   ├── creational/
│   │   ├── factory-demo/
│   │   ├── builder-demo/
│   │   └── singleton-demo/
│   ├── behavioral/
│   │   ├── strategy-demo/
│   │   ├── observer-demo/
│   │   ├── template-method-demo/
│   │   └── chain-of-responsibility-demo/
│   └── structural/
│       ├── decorator-demo/
│       ├── adapter-demo/
│       └── proxy-demo/
├── enterprise-patterns-demo/
│   ├── cqrs-demo/
│   ├── saga-demo/
│   ├── event-sourcing-demo/
│   └── circuit-breaker-demo/
├── spring-aop-demo/
│   ├── logging-aspect/
│   ├── audit-aspect/
│   ├── performance-aspect/
│   └── security-aspect/
└── spring-webflux-demo/
    ├── reactive-crud/
    ├── webclient-demo/
    ├── sse-demo/
    └── r2dbc-demo/
```

### Öğrenme Sırası (Önerilen)

#### Hafta 1-2: Design Patterns Temelleri
1. Factory Pattern → Builder Pattern
2. Strategy Pattern → Template Method
3. Observer Pattern (Spring Events ile)

#### Hafta 3: Structural Patterns
1. Decorator Pattern
2. Adapter Pattern
3. Proxy Pattern
4. Chain of Responsibility

#### Hafta 4: Spring AOP
1. Basic aspects (logging, timing)
2. Custom annotations
3. Pointcut expressions

#### Hafta 5-6: Enterprise Patterns
1. CQRS (basit implementasyon)
2. Circuit Breaker (Resilience4j)
3. Event Sourcing (temel kavramlar)
4. Saga Pattern (orchestration)

#### Hafta 7-8: Spring WebFlux
1. Mono/Flux temelleri
2. Reactive operators
3. WebClient
4. R2DBC

### Her Pattern İçin Checklist
- [ ] Problemi anla (ne zaman kullanılır?)
- [ ] Basit örnek yaz
- [ ] Unit test yaz
- [ ] Rent-a-car context'inde düşün (ama EKLEME!)
- [ ] Refactoring.guru'dan oku
- [ ] Real-world örnek bul

### Kaynaklar

#### Design Patterns
- https://refactoring.guru/design-patterns (EN İYİ)
- "Head First Design Patterns" kitabı
- https://www.baeldung.com/design-patterns-series

#### Spring AOP
- https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop
- https://www.baeldung.com/spring-aop

#### Spring WebFlux
- https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html
- https://www.baeldung.com/spring-webflux
- "Hands-On Reactive Programming in Spring 5" kitabı

#### Enterprise Patterns
- https://microservices.io/patterns/
- "Microservices Patterns" - Chris Richardson
- "Building Microservices" - Sam Newman

---

## ⚠️ Önemli Uyarılar

### YAPMA:
- ❌ Bu pattern'leri rent-a-car projesine zorla ekleme
- ❌ "Pattern kullandım" demek için pattern kullanma
- ❌ Basit problemi karmaşık çözümle çözme
- ❌ Tüm pattern'leri aynı anda öğrenmeye çalışma

### YAP:
- ✅ Her pattern'i ayrı sandbox'ta dene
- ✅ Gerçek ihtiyaç olduğunda pattern uygula
- ✅ YAGNI (You Ain't Gonna Need It) prensibini unut
- ✅ Önce basit çözüm, sonra gerekirse refactor

---

**Bu dosya öğrenme referansın. Rent-a-car projesinde MVP'yi bitir, sonra buraya dön! 🎯**
