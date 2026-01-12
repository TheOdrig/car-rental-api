# Design Document: Admin Dashboard & Operations Panel

## Overview

Admin Dashboard & Operations Panel, rent-a-car işletmesinin operasyonel yönetimini merkezi bir panel üzerinden sağlayan bir sistemdir. Bu sistem, mevcut modüllerin (rental, car, payment, damage) verilerini aggregate ederek admin kullanıcılara anlık durum görünürlüğü, performans metrikleri ve hızlı aksiyon alma imkanı sunar.

Dashboard, Spring Modulith mimarisine uygun olarak yeni bir `dashboard` modülü olarak implement edilecek ve diğer modüllerin public API'lerini kullanarak veri toplayacaktır. Caffeine cache ile performans optimize edilecek ve event-driven cache invalidation ile veri tutarlılığı sağlanacaktır.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Dashboard Module                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐         │
│  │ DashboardController │  │ AlertController │  │ QuickActionController │ │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘         │
│           │                    │                    │                   │
│  ┌────────▼────────────────────▼────────────────────▼────────┐         │
│  │                    DashboardService                        │         │
│  │  - getDailySummary()                                       │         │
│  │  - getFleetStatus()                                        │         │
│  │  - getMonthlyMetrics()                                     │         │
│  │  - getAlerts()                                             │         │
│  │  - getRevenueAnalytics()                                   │         │
│  └────────┬───────────────────────────────────────────────────┘         │
│           │                                                              │
│  ┌────────▼────────┐  ┌─────────────────┐  ┌─────────────────┐         │
│  │ AlertService    │  │ CacheManager    │  │ EventListener   │         │
│  │ - generateAlerts│  │ - dashboardCache│  │ - onRentalEvent │         │
│  │ - acknowledge   │  │ - fleetCache    │  │ - onCarEvent    │         │
│  └─────────────────┘  │ - revenueCache  │  │ - onPaymentEvent│         │
│                       └─────────────────┘  └─────────────────┘         │
├─────────────────────────────────────────────────────────────────────────┤
│                    Cross-Module Dependencies (Public APIs)               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐                │
│  │ RentalService │ │ CarService │ │ PaymentService │ │ DamageService │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘                │
└─────────────────────────────────────────────────────────────────────────┘
```

### Module Dependencies

```java
@ApplicationModule(
    allowedDependencies = {"shared", "rental::api", "car::api", "payment::api", "damage::api"}
)
package com.akif.dashboard;
```

## Components and Interfaces

### Public API (api/ package)

```java
// com.akif.dashboard.api.DashboardService
public interface DashboardService {
    DailySummaryDto getDailySummary();
    FleetStatusDto getFleetStatus();
    MonthlyMetricsDto getMonthlyMetrics(LocalDate startDate, LocalDate endDate);
    List<AlertDto> getActiveAlerts();
    RevenueAnalyticsDto getRevenueAnalytics(int days);
    Page<PendingItemDto> getPendingApprovals(Pageable pageable);
    Page<PendingItemDto> getTodaysPickups(Pageable pageable);
    Page<PendingItemDto> getTodaysReturns(Pageable pageable);
    Page<PendingItemDto> getOverdueRentals(Pageable pageable);
}

// com.akif.dashboard.api.AlertService
public interface AlertService {
    List<AlertDto> generateAlerts();
    AlertDto acknowledgeAlert(Long alertId, String adminUsername);
    List<AlertDto> getAlertsByType(AlertType type);
}

// com.akif.dashboard.api.QuickActionService
public interface QuickActionService {
    QuickActionResultDto approveRental(Long rentalId, String adminUsername);
    QuickActionResultDto processPickup(Long rentalId, String adminUsername, String notes);
    QuickActionResultDto processReturn(Long rentalId, String adminUsername, String notes);
}
```

### DTOs (api/ package)

```java
// Daily Summary
public record DailySummaryDto(
    int pendingApprovals,
    int todaysPickups,
    int todaysReturns,
    int overdueRentals,
    int pendingDamageAssessments,
    LocalDateTime generatedAt
) {}

// Fleet Status
public record FleetStatusDto(
    int totalCars,
    int availableCars,
    int rentedCars,
    int maintenanceCars,
    int damagedCars,
    BigDecimal occupancyRate,
    LocalDateTime generatedAt
) {}

// Monthly Metrics
public record MonthlyMetricsDto(
    BigDecimal totalRevenue,
    int completedRentals,
    int cancelledRentals,
    BigDecimal penaltyRevenue,
    BigDecimal damageCharges,
    double averageRentalDurationDays,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime generatedAt
) {}

// Alert
public record AlertDto(
    Long id,
    AlertType type,
    AlertSeverity severity,
    String title,
    String message,
    String actionUrl,
    boolean acknowledged,
    LocalDateTime acknowledgedAt,
    String acknowledgedBy,
    LocalDateTime createdAt
) {}

// Revenue Analytics
public record RevenueAnalyticsDto(
    List<DailyRevenueDto> dailyRevenue,
    List<MonthlyRevenueDto> monthlyRevenue,
    RevenueBreakdownDto breakdown,
    BigDecimal periodChangePercent,
    LocalDateTime generatedAt
) {}

public record DailyRevenueDto(LocalDate date, BigDecimal amount) {}
public record MonthlyRevenueDto(YearMonth month, BigDecimal amount) {}
public record RevenueBreakdownDto(
    BigDecimal rentalRevenue,
    BigDecimal penaltyRevenue,
    BigDecimal damageCharges
) {}

// Pending Item
public record PendingItemDto(
    Long rentalId,
    String customerName,
    String customerEmail,
    Long carId,
    String carBrand,
    String carModel,
    String licensePlate,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal totalAmount,
    String status,
    Integer lateHours,
    LocalDateTime createdAt
) {}

// Quick Action Result
public record QuickActionResultDto(
    boolean success,
    String message,
    String newStatus,
    DailySummaryDto updatedSummary
) {}
```

### Enums

```java
public enum AlertType {
    LATE_RETURN,
    FAILED_PAYMENT,
    LOW_AVAILABILITY,
    UNRESOLVED_DISPUTE,
    MAINTENANCE_REQUIRED
}

public enum AlertSeverity {
    CRITICAL(1),
    HIGH(2),
    WARNING(3),
    MEDIUM(4),
    LOW(5);
    
    private final int priority;
    
    AlertSeverity(int priority) {
        this.priority = priority;
    }
    
    public int getPriority() {
        return priority;
    }
}
```

### Internal Services

```java
// com.akif.dashboard.internal.service.DashboardServiceImpl
@Service
@RequiredArgsConstructor
class DashboardServiceImpl implements DashboardService {
    private final DashboardQueryService queryService;
    private final CacheManager cacheManager;
    
    @Cacheable(value = "dailySummary", key = "'today'")
    public DailySummaryDto getDailySummary() { ... }
    
    @Cacheable(value = "fleetStatus", key = "'current'")
    public FleetStatusDto getFleetStatus() { ... }
    
    @Cacheable(value = "monthlyMetrics", key = "#startDate + '-' + #endDate")
    public MonthlyMetricsDto getMonthlyMetrics(LocalDate startDate, LocalDate endDate) { ... }
}

// com.akif.dashboard.internal.service.DashboardQueryService
@Service
@RequiredArgsConstructor
class DashboardQueryService {
    private final RentalService rentalService;
    private final CarService carService;
    private final PaymentService paymentService;
    private final DamageService damageService;
    
    // Aggregation queries using public APIs
}

// com.akif.dashboard.internal.service.AlertServiceImpl
@Service
@RequiredArgsConstructor
class AlertServiceImpl implements AlertService {
    private final AlertRepository alertRepository;
    private final DashboardQueryService queryService;
    
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void checkAndGenerateAlerts() { ... }
}
```

### Event Listeners for Cache Invalidation

```java
// com.akif.dashboard.internal.listener.DashboardEventListener
@Component
@RequiredArgsConstructor
class DashboardEventListener {
    private final CacheManager cacheManager;
    
    @EventListener
    public void onRentalConfirmed(RentalConfirmedEvent event) {
        evictDailySummaryCache();
        evictFleetStatusCache();
    }
    
    @EventListener
    public void onRentalCancelled(RentalCancelledEvent event) {
        evictDailySummaryCache();
        evictFleetStatusCache();
        evictMonthlyMetricsCache();
    }
    
    @EventListener
    public void onPaymentCaptured(PaymentCapturedEvent event) {
        evictRevenueCache();
        evictMonthlyMetricsCache();
    }
    
    @EventListener
    public void onDamageReported(DamageReportedEvent event) {
        evictDailySummaryCache();
    }
    
    private void evictDailySummaryCache() {
        cacheManager.getCache("dailySummary").clear();
    }
    
    private void evictFleetStatusCache() {
        cacheManager.getCache("fleetStatus").clear();
    }
    
    private void evictMonthlyMetricsCache() {
        cacheManager.getCache("monthlyMetrics").clear();
    }
    
    private void evictRevenueCache() {
        cacheManager.getCache("revenueAnalytics").clear();
    }
}
```

### Controllers

```java
// com.akif.dashboard.web.DashboardController
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Dashboard")
class DashboardController {
    private final DashboardService dashboardService;
    
    @GetMapping("/summary")
    public ResponseEntity<DailySummaryDto> getDailySummary() { ... }
    
    @GetMapping("/fleet")
    public ResponseEntity<FleetStatusDto> getFleetStatus() { ... }
    
    @GetMapping("/metrics")
    public ResponseEntity<MonthlyMetricsDto> getMonthlyMetrics(
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) { ... }
    
    @GetMapping("/revenue")
    public ResponseEntity<RevenueAnalyticsDto> getRevenueAnalytics(
        @RequestParam(defaultValue = "30") int days) { ... }
    
    @GetMapping("/pending/approvals")
    public ResponseEntity<Page<PendingItemDto>> getPendingApprovals(Pageable pageable) { ... }
    
    @GetMapping("/pending/pickups")
    public ResponseEntity<Page<PendingItemDto>> getTodaysPickups(Pageable pageable) { ... }
    
    @GetMapping("/pending/returns")
    public ResponseEntity<Page<PendingItemDto>> getTodaysReturns(Pageable pageable) { ... }
    
    @GetMapping("/pending/overdue")
    public ResponseEntity<Page<PendingItemDto>> getOverdueRentals(Pageable pageable) { ... }
}

// com.akif.dashboard.web.AlertController
@RestController
@RequestMapping("/api/admin/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Alerts")
class AlertController {
    private final AlertService alertService;
    
    @GetMapping
    public ResponseEntity<List<AlertDto>> getActiveAlerts() { ... }
    
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertDto> acknowledgeAlert(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails user) { ... }
}

// com.akif.dashboard.web.QuickActionController
@RestController
@RequestMapping("/api/admin/quick-actions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Quick Actions")
class QuickActionController {
    private final QuickActionService quickActionService;
    
    @PostMapping("/rentals/{id}/approve")
    public ResponseEntity<QuickActionResultDto> approveRental(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails user) { ... }
    
    @PostMapping("/rentals/{id}/pickup")
    public ResponseEntity<QuickActionResultDto> processPickup(
        @PathVariable Long id,
        @RequestParam(required = false) String notes,
        @AuthenticationPrincipal UserDetails user) { ... }
    
    @PostMapping("/rentals/{id}/return")
    public ResponseEntity<QuickActionResultDto> processReturn(
        @PathVariable Long id,
        @RequestParam(required = false) String notes,
        @AuthenticationPrincipal UserDetails user) { ... }
}
```

## Data Models

### Alert Entity

```java
@Entity
@Table(name = "dashboard_alerts",
    indexes = {
        @Index(name = "idx_alert_type", columnList = "type"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_acknowledged", columnList = "acknowledged"),
        @Index(name = "idx_alert_created", columnList = "created_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Alert extends BaseEntity {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AlertType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "message", nullable = false, length = 1000)
    private String message;
    
    @Column(name = "action_url")
    private String actionUrl;
    
    @Column(name = "reference_id")
    private Long referenceId;
    
    @Column(name = "acknowledged", nullable = false)
    private boolean acknowledged = false;
    
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
    
    @Column(name = "acknowledged_by")
    private String acknowledgedBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    public void acknowledge(String adminUsername) {
        this.acknowledged = true;
        this.acknowledgedAt = LocalDateTime.now();
        this.acknowledgedBy = adminUsername;
    }
}
```

### Database Migration

```sql
-- V13__create_dashboard_alerts_table.sql
CREATE TABLE dashboard_alerts (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    action_url VARCHAR(500),
    reference_id BIGINT,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_alert_type ON dashboard_alerts(type);
CREATE INDEX idx_alert_severity ON dashboard_alerts(severity);
CREATE INDEX idx_alert_acknowledged ON dashboard_alerts(acknowledged);
CREATE INDEX idx_alert_created ON dashboard_alerts(created_at);
```

## Correctness Criteria

Aşağıdaki doğruluk kriterleri, sistemin beklenen davranışlarını tanımlar ve testler tarafından doğrulanacaktır:

### Criteria 1: Daily Summary Counts Accuracy

Dashboard daily summary counts SHALL accurately reflect:
- Pending approvals = count of rentals with status REQUESTED
- Today's pickups = count of rentals with status CONFIRMED and startDate = today
- Today's returns = count of rentals with status IN_USE and endDate = today
- Overdue rentals = count of rentals with status IN_USE and endDate < today
- Pending damage assessments = count of damage reports with status REPORTED or UNDER_ASSESSMENT

**Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**

### Criteria 2: Fleet Status Counts Accuracy

Fleet status counts SHALL accurately reflect:
- Available cars = count of cars with status AVAILABLE
- Rented cars = count of cars with status RESERVED (with active rental)
- Maintenance cars = count of cars with status MAINTENANCE
- Damaged cars = count of cars with status DAMAGED

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

### Criteria 3: Occupancy Rate Calculation

Occupancy rate SHALL equal (rentedCars / totalActiveCars) × 100, where totalActiveCars excludes SOLD cars.

**Validates: Requirements 2.5**

### Criteria 4: Monthly Metrics Aggregation

Monthly metrics SHALL accurately calculate:
- Total revenue = sum of captured payment amounts within the date range
- Completed rentals = count of rentals with status RETURNED within the date range
- Cancelled rentals = count of rentals with status CANCELLED within the date range
- Penalty revenue = sum of penalty amounts from rentals within the date range
- Average rental duration = average of (endDate - startDate) for completed rentals

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

### Criteria 5: Alert Generation Conditions

Alerts SHALL be generated when:
- CRITICAL: Late returns exceeding 24 hours exist
- HIGH: Failed payments in last 24 hours exist
- WARNING: Fleet availability < 20%
- MEDIUM: Unresolved damage disputes older than 7 days exist

**Validates: Requirements 4.1, 4.2, 4.3, 4.4**

### Criteria 6: Alert Sorting by Severity

Alerts SHALL be sorted by severity priority where CRITICAL (1) < HIGH (2) < WARNING (3) < MEDIUM (4) < LOW (5).

**Validates: Requirements 4.5**

### Criteria 7: Revenue Analytics Aggregation

Revenue analytics SHALL accurately calculate:
- Daily revenue = sum of payments grouped by date
- Monthly revenue = sum of payments grouped by month
- Revenue breakdown = sum of payments grouped by type (rental, penalty, damage)
- Period change = ((current - previous) / previous) × 100

**Validates: Requirements 6.1, 6.2, 6.3, 6.4**

### Criteria 8: Pending Items Pagination and Content

Pending items query response SHALL:
- Return items matching the filter criteria (status, date)
- Include all required fields (customerName, carDetails, dates, totalAmount)
- Respect pagination parameters (page, size)
- Sort overdue rentals by lateHours descending

**Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5**

## Error Handling

### Exception Types

```java
// Dashboard-specific exceptions
public class DashboardException extends RuntimeException {
    public DashboardException(String message) {
        super(message);
    }
}

public class AlertNotFoundException extends DashboardException {
    public AlertNotFoundException(Long alertId) {
        super("Alert not found with id: " + alertId);
    }
}

public class QuickActionFailedException extends DashboardException {
    public QuickActionFailedException(String action, Long rentalId, String reason) {
        super(String.format("Quick action '%s' failed for rental %d: %s", action, rentalId, reason));
    }
}
```

### Error Response Handling

| Error Condition | HTTP Status | Response |
|-----------------|-------------|----------|
| Alert not found | 404 | `{"error": "Alert not found with id: X"}` |
| Quick action failed | 400 | `{"error": "Quick action failed", "reason": "..."}` |
| Unauthorized access | 401 | `{"error": "Unauthorized"}` |
| Forbidden (non-admin) | 403 | `{"error": "Access denied"}` |
| Invalid date range | 400 | `{"error": "Invalid date range"}` |

## Testing Strategy

### Testing Approach

Bu feature için üç katmanlı test stratejisi uygulanacaktır:

- **Unit Tests**: Service logic, calculation accuracy, edge cases
- **Integration Tests**: Controller endpoints, authorization, cache behavior
- **E2E Tests**: Complete dashboard workflows, cross-module interactions

### Unit Test Coverage

| Component | Test Focus |
|-----------|------------|
| DashboardServiceImpl | Daily summary calculation, fleet status, monthly metrics |
| AlertServiceImpl | Alert generation logic, severity assignment, acknowledgment |
| QuickActionServiceImpl | Rental status transitions, error handling |
| DashboardEventListener | Cache invalidation on events |

### Integration Test Coverage

| Test Class | Test Scenarios |
|------------|----------------|
| DashboardControllerIntegrationTest | Authorization (admin-only), endpoint responses, pagination |
| AlertControllerIntegrationTest | Alert listing, acknowledgment, filtering |
| QuickActionControllerIntegrationTest | Approve, pickup, return actions |

### E2E Test Coverage

| Test Class | Test Scenarios |
|------------|----------------|
| DashboardE2ETest | Complete dashboard workflow |

**E2E Test Scenarios:**

1. **Dashboard Summary Flow**
   - Create rentals with various statuses
   - Verify daily summary counts are accurate
   - Verify fleet status reflects car states
   - Verify monthly metrics aggregation

2. **Alert Generation Flow**
   - Create late return scenario (>24 hours)
   - Verify CRITICAL alert is generated
   - Acknowledge alert
   - Verify alert status updated

3. **Quick Action Flow**
   - Create pending rental (REQUESTED)
   - Approve via quick action
   - Verify status changed to CONFIRMED
   - Process pickup via quick action
   - Verify status changed to IN_USE
   - Process return via quick action
   - Verify status changed to RETURNED

4. **Revenue Analytics Flow**
   - Create payments across multiple days
   - Verify daily revenue aggregation
   - Verify revenue breakdown by source
   - Verify period comparison calculation

5. **Cache Invalidation Flow**
   - Request dashboard summary (cache miss)
   - Request again (cache hit)
   - Trigger rental status change event
   - Request again (cache invalidated, fresh data)

6. **Authorization Flow**
   - Attempt dashboard access as USER role → 403
   - Attempt dashboard access as ADMIN role → 200
   - Attempt dashboard access without auth → 401

### Cache Configuration

```java
@Configuration
public class DashboardCacheConfig {
    
    @Bean
    public CacheManager dashboardCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)  // Summary data
            .maximumSize(100));
        return cacheManager;
    }
    
    // Analytics cache with longer TTL
    @Bean
    public Cache analyticsCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(50)
            .build();
    }
}
```
