# Implementation Plan

- [x] 1. Set up core infrastructure and data models




  - [x] 1.1 Create LateReturnStatus enum





    - Create `LateReturnStatus` enum with ON_TIME, GRACE_PERIOD, LATE, SEVERELY_LATE values
    - Add display name and description for each status
    - _Requirements: 1.3, 1.4, 1.5_
  - [x] 1.2 Create PenaltyConfig configuration class


    - Create `@ConfigurationProperties` class for penalty settings
    - Add gracePeriodMinutes, hourlyPenaltyRate, dailyPenaltyRate, penaltyCapMultiplier
    - Add validation for configuration values (grace period 0-120, rates within bounds)
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  - [x] 1.3 Extend Rental entity with late return fields


    - Add lateReturnStatus, lateDetectedAt, actualReturnTime fields
    - Add lateHours, penaltyAmount, penaltyPaid fields
    - _Requirements: 1.2, 4.4_
  - [x] 1.4 Create Flyway migration for Rental entity extensions


    - Add new columns to rentals table
    - Add index on late_return_status column
    - _Requirements: 1.2, 4.4_
  - [x] 1.5 Create PenaltyWaiver entity


    - Create entity with rental reference, amounts, reason, adminId, timestamps
    - Add refundInitiated and refundTransactionId fields
    - _Requirements: 7.1, 7.2_
  - [x] 1.6 Create Flyway migration for PenaltyWaiver table


    - Create penalty_waivers table with foreign key to rentals
    - Add indexes for rental_id and admin_id
    - _Requirements: 7.1, 7.2_
  - [x] 1.7 Create PenaltyWaiverRepository


    - Create JpaRepository interface
    - Add findByRentalId query method
    - _Requirements: 7.4_

- [x] 2. Implement penalty calculation service




  - [x] 2.1 Create IPenaltyCalculationService interface


    - Define calculatePenalty, calculateHourlyPenalty, calculateDailyPenalty methods
    - Define applyPenaltyCap method
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 2.2 Create PenaltyResult DTO

    - Create record with penaltyAmount, dailyRate, lateHours, lateDays, status, breakdown
    - Add cappedAtMax flag
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - [x] 2.3 Implement PenaltyCalculationServiceImpl


    - Implement hourly penalty calculation (1-6 hours: 10% per hour)
    - Implement daily penalty calculation (7-24 hours: 150%, 1+ days: 150% per day)
    - Implement penalty cap enforcement (5x daily rate)
    - Implement grace period zero penalty
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. Implement late return detection service




  - [x] 3.1 Create ILateReturnDetectionService interface


    - Define detectLateReturns, calculateLateStatus, calculateLateHours methods
    - _Requirements: 1.1, 1.3, 1.4, 1.5_
  - [x] 3.2 Add repository query for overdue rentals


    - Add findOverdueRentals query to RentalRepository
    - Filter by IN_USE status and end date before current date
    - _Requirements: 1.1_
  - [x] 3.3 Implement LateReturnDetectionServiceImpl


    - Implement late status calculation based on time elapsed
    - Implement grace period detection
    - Implement severely late detection (24+ hours)
    - Record late detection timestamp
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 4. Implement late return scheduler




  - [x] 4.1 Create LateReturnScheduler component


    - Create @Scheduled method running every 15 minutes
    - Implement pagination for processing large datasets
    - Implement error handling and logging
    - _Requirements: 1.1_
  - [x] 4.2 Integrate detection service with scheduler


    - Call detection service for each overdue rental
    - Update rental with late status and detection timestamp
    - _Requirements: 1.1, 1.2_

- [x] 5. Implement late return notification events




  - [x] 5.1 Create GracePeriodWarningEvent


    - Extend RentalEvent with rental details and remaining grace time
    - _Requirements: 3.1_
  - [x] 5.2 Create LateReturnNotificationEvent


    - Extend RentalEvent with penalty amount and late duration
    - _Requirements: 3.2_
  - [x] 5.3 Create SeverelyLateNotificationEvent


    - Extend RentalEvent with escalation warning details
    - _Requirements: 3.3_
  - [x] 5.4 Create PenaltySummaryEvent


    - Extend RentalEvent with final penalty details
    - _Requirements: 3.5_
  - [x] 5.5 Update detection service to publish events


    - Publish appropriate event on status change
    - _Requirements: 3.1, 3.2, 3.3_
  - [x] 5.6 Create email templates for late return notifications


    - Create grace-period-warning.html template
    - Create late-return-notification.html template
    - Create severely-late-notification.html template
    - Create penalty-summary.html template
    - _Requirements: 3.1, 3.2, 3.3, 3.5_
  - [x] 5.7 Update EmailEventListener for late return events


    - Add handlers for GracePeriodWarningEvent
    - Add handlers for LateReturnNotificationEvent
    - Add handlers for SeverelyLateNotificationEvent
    - Add handlers for PenaltySummaryEvent
    - _Requirements: 3.1, 3.2, 3.3, 3.5_

- [x] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement penalty payment service





  - [x] 7.1 Create IPenaltyPaymentService interface


    - Define createPenaltyPayment, chargePenalty, handleFailedPenaltyPayment methods
    - _Requirements: 4.1, 4.2, 4.3_
  - [x] 7.2 Implement PenaltyPaymentServiceImpl


    - Create penalty payment record with calculated amount
    - Attempt automatic charge via payment gateway
    - Handle failed payments (mark as PENDING, notify admin)
    - Ensure currency consistency with original rental
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  - [x] 7.3 Integrate penalty payment with rental return flow


    - Calculate penalty on late return
    - Create and process penalty payment
    - Update rental with penalty details
    - Publish penalty summary event
    - _Requirements: 4.1, 4.4_

- [x] 8. Implement penalty waiver service




  - [x] 8.1 Create IPenaltyWaiverService interface


    - Define waivePenalty, waiveFullPenalty, getPenaltyHistory methods
    - Define processRefundForWaiver method
    - _Requirements: 7.1, 7.3, 7.4, 7.5_
  - [x] 8.2 Implement PenaltyWaiverServiceImpl


    - Implement full and partial waiver with mandatory reason
    - Record waiver details (reason, adminId, timestamp, amounts)
    - Recalculate remaining penalty for partial waivers
    - Initiate refund for waivers after payment
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 9. Implement late return reporting




  - [x] 9.1 Create ILateReturnReportService interface


    - Define getLateReturns and getStatistics methods
    - _Requirements: 5.1, 5.5_
  - [x] 9.2 Create LateReturnReportDto and LateReturnStatisticsDto


    - Create DTOs with required fields
    - _Requirements: 5.2, 5.5_
  - [x] 9.3 Create LateReturnFilterDto


    - Add date range, status, sorting options
    - _Requirements: 5.3, 5.4_
  - [x] 9.4 Add repository queries for reporting


    - Add findLateReturns with filters
    - Add statistics aggregation queries
    - _Requirements: 5.1, 5.3, 5.5_
  - [x] 9.5 Implement LateReturnReportServiceImpl


    - Implement late returns query with filtering
    - Implement statistics calculation
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 10. Implement REST API endpoints




  - [x] 10.1 Create LateReturnController


    - Add GET /api/admin/late-returns endpoint for report
    - Add GET /api/admin/late-returns/statistics endpoint
    - _Requirements: 5.1, 5.5_
  - [x] 10.2 Create PenaltyWaiverController


    - Add POST /api/admin/rentals/{id}/penalty/waive endpoint
    - Add GET /api/admin/rentals/{id}/penalty/history endpoint
    - _Requirements: 7.1, 7.4_
  - [x] 10.3 Add OpenAPI documentation

    - Add @Tag, @Operation, @ApiResponses annotations
    - Document request/response schemas
    - _Requirements: 5.1, 5.5, 7.1, 7.4_

- [x] 11. Implement exception handling






  - [x] 11.1 Create custom exceptions

    - Create LateReturnException
    - Create PenaltyCalculationException
    - Create PenaltyWaiverException
    - Create InvalidPenaltyConfigException
    - _Requirements: All_

  - [x] 11.2 Update GlobalExceptionHandler

    - Add handlers for new exceptions
    - Return appropriate HTTP status codes
    - _Requirements: All_

- [x] 12. Checkpoint - Ensure application compiles and runs
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Write unit tests




  - [x] 13.1 Write unit tests for PenaltyCalculationService


    - Test hourly penalty calculation (1-6 hours)
    - Test daily penalty calculation (7-24 hours, multi-day)
    - Test penalty cap enforcement
    - Test grace period zero penalty
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  - [x] 13.2 Write unit tests for LateReturnDetectionService


    - Test on-time rental (no late status)
    - Test grace period detection
    - Test late detection (1-24 hours)
    - Test severely late detection (24+ hours)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  - [x] 13.3 Write unit tests for LateReturnScheduler


    - Test scheduler execution
    - Test pagination handling
    - Test error recovery
    - _Requirements: 1.1_
  - [x] 13.4 Write unit tests for PenaltyPaymentService


    - Test successful penalty payment
    - Test failed penalty payment handling
    - Test currency consistency
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 13.5 Write unit tests for PenaltyWaiverService

    - Test full waiver
    - Test partial waiver calculation
    - Test waiver after payment (refund)
    - Test invalid waiver amount
    - _Requirements: 7.1, 7.2, 7.3, 7.5_
  - [x] 13.6 Write unit tests for LateReturnReportService


    - Test filtering by date range
    - Test sorting options
    - Test statistics calculation
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 14. Write integration tests




  - [x] 14.1 Write integration tests for LateReturnController


    - Test late returns report endpoint
    - Test statistics endpoint
    - Test authorization (admin only)
    - _Requirements: 5.1, 5.5_
  - [x] 14.2 Write integration tests for PenaltyWaiverController


    - Test penalty waiver endpoint
    - Test penalty history endpoint
    - Test authorization (admin only)
    - _Requirements: 7.1, 7.4_

- [x] 15. Write E2E test for late return flow
  - Create `src/test/java/com/akif/e2e/penalty/LateReturnPenaltyE2ETest.java`
  - Extend `E2ETestBase` and use `@SpringBootTest` with `@ActiveProfiles("test")`
  - Test complete flow: rental creation → late detection → penalty calculation → payment
  - Test scheduler triggering late detection (mock time advancement)
  - Test event publishing (GracePeriodWarningEvent, LateReturnNotificationEvent, SeverelyLateNotificationEvent)
  - Test penalty payment gateway integration
  - Test penalty waiver with refund processing
  - Use `@MockitoSpyBean` for `IPaymentGateway` to verify payment calls
  - Use `TestEventCaptor` to verify event publishing
  - _Requirements: All_
  - **Status**: ✅ Completed - All 6 tests passing

- [x] 16. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 17. Update documentation






  - [x] 17.1 Update RENTAL_MVP_PLAN.md

    - Mark Late Return & Penalty System as completed
    - Add implementation notes and endpoint summary
    - _Requirements: All_
  - [x] 17.2 Update FEATURE_ROADMAP.md


    - Update Tier 2 progress
    - Add interview talking points
    - _Requirements: All_
