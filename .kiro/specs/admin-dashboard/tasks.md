# Implementation Plan

## 1. Set up Dashboard Module Structure

- [x] 1.1 Create dashboard module package structure
  - Create `com.akif.dashboard` package with api/, domain/, internal/, web/ subpackages
  - Create `package-info.java` with `@ApplicationModule(allowedDependencies = {"shared", "rental", "car", "payment", "damage"})`
  - Create `api/package-info.java` with `@NamedInterface("api")`
  - _Requirements: 9.1, 9.2, 9.3_

- [x] 1.2 Create Alert entity and enums
  - Create `AlertType` enum (LATE_RETURN, FAILED_PAYMENT, LOW_AVAILABILITY, UNRESOLVED_DISPUTE, MAINTENANCE_REQUIRED)
  - Create `AlertSeverity` enum with priority (CRITICAL=1, HIGH=2, WARNING=3, MEDIUM=4, LOW=5)
  - Create `Alert` entity with type, severity, title, message, actionUrl, referenceId, acknowledged fields
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [x] 1.3 Create database migration for alerts table
  - Create `V13__create_dashboard_alerts_table.sql`
  - Add indexes for type, severity, acknowledged, created_at columns
  - _Requirements: 4.1_

## 2. Implement Public API DTOs

- [x] 2.1 Create daily summary and fleet status DTOs
  - Create `DailySummaryDto` record (pendingApprovals, todaysPickups, todaysReturns, overdueRentals, pendingDamageAssessments, generatedAt)
  - Create `FleetStatusDto` record (totalCars, availableCars, rentedCars, maintenanceCars, damagedCars, occupancyRate, generatedAt)
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 2.2 Create monthly metrics and revenue analytics DTOs
  - Create `MonthlyMetricsDto` record (totalRevenue, completedRentals, cancelledRentals, penaltyRevenue, damageCharges, averageRentalDurationDays, startDate, endDate, generatedAt)
  - Create `RevenueAnalyticsDto` record with DailyRevenueDto, MonthlyRevenueDto, RevenueBreakdownDto
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 6.1, 6.2, 6.3, 6.4_

- [x] 2.3 Create alert and pending item DTOs
  - Create `AlertDto` record (id, type, severity, title, message, actionUrl, acknowledged, acknowledgedAt, acknowledgedBy, createdAt)
  - Create `PendingItemDto` record (rentalId, customerName, customerEmail, carId, carBrand, carModel, licensePlate, startDate, endDate, totalAmount, status, lateHours, createdAt)
  - Create `QuickActionResultDto` record (success, message, newStatus, updatedSummary)
  - _Requirements: 4.5, 7.1, 7.2, 7.3, 7.4, 7.5, 5.1, 5.2, 5.3, 5.4, 5.5_

## 3. Implement Public API Interfaces

- [x] 3.1 Create DashboardService interface
  - Define getDailySummary(), getFleetStatus(), getMonthlyMetrics(), getActiveAlerts(), getRevenueAnalytics() methods
  - Define getPendingApprovals(), getTodaysPickups(), getTodaysReturns(), getOverdueRentals() methods with Pageable
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3, 7.4_

- [x] 3.2 Create AlertService interface
  - Define generateAlerts(), acknowledgeAlert(), getAlertsByType() methods
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [x] 3.3 Create QuickActionService interface
  - Define approveRental(), processPickup(), processReturn() methods
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

## 4. Implement Repository Layer

- [x] 4.1 Create AlertRepository
  - Add findByAcknowledgedFalseOrderBySeverityAsc() method
  - Add findByTypeAndAcknowledgedFalse() method
  - Add existsByTypeAndReferenceIdAndAcknowledgedFalse() method for duplicate prevention
  - _Requirements: 4.5, 4.6_

## 5. Extend Existing Module APIs for Dashboard Queries

- [x] 5.1 Add dashboard query methods to RentalService API
  - Add countByStatus(RentalStatus status) method
  - Add countTodaysPickups() method (CONFIRMED, startDate = today)
  - Add countTodaysReturns() method (IN_USE, endDate = today)
  - Add countOverdueRentals() method (IN_USE, endDate < today)
  - Add findPendingApprovals(Pageable) method
  - Add findTodaysPickups(Pageable) method
  - Add findTodaysReturns(Pageable) method
  - Add findOverdueRentals(Pageable) method
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 7.1, 7.2, 7.3, 7.4_

- [x] 5.2 Add dashboard query methods to CarService API
  - Add countByStatus(CarStatusType status) method
  - Add countTotalActiveCars() method (excludes SOLD)
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 5.3 Add dashboard query methods to PaymentService API
  - Add sumCapturedPaymentsBetween(LocalDateTime start, LocalDateTime end) method
  - Add getDailyRevenue(int days) method
  - Add getMonthlyRevenue(int months) method
  - _Requirements: 3.1, 6.1, 6.2_

- [x] 5.4 Add dashboard query methods to DamageService API
  - Add countPendingAssessments() method (REPORTED or UNDER_ASSESSMENT)
  - Add countUnresolvedDisputesOlderThan(int days) method
  - _Requirements: 1.5, 4.4_

## 6. Implement Dashboard Service

- [x] 6.1 Implement DashboardQueryService (internal)
  - Inject RentalService, CarService, PaymentService, DamageService
  - Implement aggregation methods using public APIs
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 6.2 Implement DashboardServiceImpl with caching
  - Implement getDailySummary() with @Cacheable("dailySummary")
  - Implement getFleetStatus() with @Cacheable("fleetStatus")
  - Implement getMonthlyMetrics() with @Cacheable("monthlyMetrics")
  - Implement getRevenueAnalytics() with @Cacheable("revenueAnalytics")
  - Implement pending items methods (no caching, real-time data)
  - _Requirements: 1.6, 8.1, 8.6_

- [x] 6.3 Write unit tests for DashboardServiceImpl
  - Test daily summary calculation accuracy
  - Test fleet status calculation accuracy
  - Test occupancy rate calculation
  - Test monthly metrics aggregation
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_

## 7. Implement Alert Service

- [x] 7.1 Implement AlertServiceImpl
  - Implement generateAlerts() with condition checks
  - Implement acknowledgeAlert() with timestamp recording
  - Implement getAlertsByType() with filtering
  - Add @Scheduled(fixedRate = 300000) for periodic alert generation
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [x] 7.2 Write unit tests for AlertServiceImpl
  - Test CRITICAL alert generation for late returns >24h
  - Test HIGH alert generation for failed payments
  - Test WARNING alert generation for low availability <20%
  - Test MEDIUM alert generation for unresolved disputes >7 days
  - Test alert sorting by severity
  - Test alert acknowledgment
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

## 8. Implement Quick Action Service

- [x] 8.1 Implement QuickActionServiceImpl
  - Inject RentalService for rental operations
  - Implement approveRental() calling rentalService.confirmRental()
  - Implement processPickup() calling rentalService.pickupRental()
  - Implement processReturn() calling rentalService.returnRental()
  - Return updated DailySummaryDto after each action
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 8.2 Write unit tests for QuickActionServiceImpl
  - Test successful approve action
  - Test successful pickup action
  - Test successful return action
  - Test error handling for invalid rental states
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

## 9. Implement Cache Invalidation

- [x] 9.1 Create DashboardEventListener
  - Listen to RentalConfirmedEvent → evict dailySummary, fleetStatus
  - Listen to RentalCancelledEvent → evict dailySummary, fleetStatus, monthlyMetrics
  - Listen to PaymentCapturedEvent → evict revenueAnalytics, monthlyMetrics
  - Listen to DamageReportedEvent → evict dailySummary
  - _Requirements: 8.2, 8.3, 8.4, 8.5_

- [x] 9.2 Configure dashboard cache settings
  - Configure Caffeine cache with 5 min TTL for summary data
  - Configure Caffeine cache with 15 min TTL for analytics data
  - _Requirements: 8.6_

## 10. Implement Controllers

- [x] 10.1 Implement DashboardController
  - Add @PreAuthorize("hasRole('ADMIN')") for all endpoints
  - Implement GET /api/admin/dashboard/summary
  - Implement GET /api/admin/dashboard/fleet
  - Implement GET /api/admin/dashboard/metrics with date range params
  - Implement GET /api/admin/dashboard/revenue with days param
  - Implement GET /api/admin/dashboard/pending/approvals with pagination
  - Implement GET /api/admin/dashboard/pending/pickups with pagination
  - Implement GET /api/admin/dashboard/pending/returns with pagination
  - Implement GET /api/admin/dashboard/pending/overdue with pagination
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3, 7.4, 7.5, 9.1, 9.2, 9.3_

- [x] 10.2 Implement AlertController
  - Add @PreAuthorize("hasRole('ADMIN')") for all endpoints
  - Implement GET /api/admin/alerts
  - Implement POST /api/admin/alerts/{id}/acknowledge
  - _Requirements: 4.5, 4.6, 9.1, 9.2, 9.3_

- [x] 10.3 Implement QuickActionController
  - Add @PreAuthorize("hasRole('ADMIN')") for all endpoints
  - Implement POST /api/admin/quick-actions/rentals/{id}/approve
  - Implement POST /api/admin/quick-actions/rentals/{id}/pickup
  - Implement POST /api/admin/quick-actions/rentals/{id}/return
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 9.1, 9.2, 9.3_

## 11. Checkpoint - Ensure all tests pass

- [x] 11. Checkpoint - Make sure all tests are passing
  - Ensure all tests pass, ask the user if questions arise.

## 12. Integration Tests

- [x] 12.1 Write DashboardControllerIntegrationTest
  - Test admin-only access (403 for USER role)
  - Test unauthorized access (401 without auth)
  - Test successful dashboard summary retrieval
  - Test successful fleet status retrieval
  - Test monthly metrics with date range
  - Test pending items pagination
  - _Requirements: 9.1, 9.2, 9.3_

- [x] 12.2 Write AlertControllerIntegrationTest
  - Test alert listing with sorting
  - Test alert acknowledgment
  - Test admin-only access
  - _Requirements: 4.5, 4.6, 9.1, 9.2, 9.3_

- [x] 12.3 Write QuickActionControllerIntegrationTest
  - Test approve action
  - Test pickup action
  - Test return action
  - Test error handling for invalid states
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

## 13. E2E Tests

- [x] 13.1 Write DashboardE2ETest
  - Test complete dashboard summary flow (create rentals, verify counts)
  - Test alert generation flow (create late return, verify CRITICAL alert)
  - Test quick action flow (approve → pickup → return)
  - Test revenue analytics flow (create payments, verify aggregation)
  - Test cache invalidation flow (request, change, verify refresh)
  - Test authorization flow (USER → 403, ADMIN → 200, no auth → 401)
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3, 6.1, 6.2, 6.3, 6.4, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3_

## 14. Final Checkpoint

- [x] 14. Final Checkpoint - Make sure all tests are passing
  - Ensure all tests pass, ask the user if questions arise.

## 15. Documentation

- [x] 15.1 Update RENTAL_MVP_PLAN.md
  - Mark Admin Dashboard & Operations Panel as completed
  - Add implementation notes and endpoint summary
  - Update progress percentage
  - _Requirements: Documentation_

- [x] 15.2 Update FEATURE_ROADMAP.md
  - Mark Admin Dashboard as completed in Tier 3
  - Add interview talking points
  - _Requirements: Documentation_

- [x] 15.3 Update README.md
  - Add Admin Dashboard to Features table (9 modules now)
  - Add dashboard module to Modules table
  - Add Admin Dashboard API endpoints to API Overview
  - Add dashboard/ to Project Structure
  - Update Technical Highlights
  - _Requirements: Documentation_
