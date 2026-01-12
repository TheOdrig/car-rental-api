# Git Workflow - Late Return & Penalty System

## Branch Strategy

```
main
  └── feature/late-return-penalty
```

## Commit Plan

### Task 1.1: LateReturnStatus Enum
```
feat(penalty): add LateReturnStatus enum

Add enum with ON_TIME, GRACE_PERIOD, LATE, SEVERELY_LATE values
for tracking late return status classification.
```

### Task 1.2: PenaltyConfig
```
feat(penalty): add PenaltyConfig configuration

Add @ConfigurationProperties for grace period, penalty rates,
and cap multiplier with validation.
```

### Task 1.3-1.4: Rental Entity Extension
```
feat(rental)!: extend Rental entity with late return fields

Add lateReturnStatus, lateDetectedAt, actualReturnTime,
lateHours, penaltyAmount, penaltyPaid fields.

BREAKING CHANGE: Rental entity schema updated, requires migration.
```

```
chore(db): add migration for Rental late return fields

Add V8__add_rental_late_return_fields.sql migration.
```

### Task 1.5-1.7: PenaltyWaiver Entity
```
feat(penalty): add PenaltyWaiver entity and repository

Add entity for tracking penalty waivers with audit trail.
```

```
chore(db): add migration for PenaltyWaiver table

Add V9__create_penalty_waivers_table.sql migration.
```

### Task 2: Penalty Calculation Service
```
feat(penalty): implement penalty calculation service

Add IPenaltyCalculationService with hourly/daily penalty
calculation and cap enforcement.
```

### Task 3: Late Return Detection Service
```
feat(penalty): implement late return detection service

Add ILateReturnDetectionService for status classification
and late hours/days calculation.
```

### Task 4: Late Return Scheduler
```
feat(penalty): add late return scheduler

Add @Scheduled job running every 15 minutes to detect
overdue rentals with pagination support.
```

### Task 5: Notification Events
```
feat(penalty): add late return notification events

Add GracePeriodWarningEvent, LateReturnNotificationEvent,
SeverelyLateNotificationEvent, PenaltySummaryEvent.
```

```
feat(email): add late return email templates

Add HTML templates for grace period, late, severely late,
and penalty summary notifications.
```

```
feat(email): update EmailEventListener for late return events

Add event handlers for late return notification events.
```

### Task 7: Penalty Payment Service
```
feat(payment): implement penalty payment service

Add automatic penalty charging on late rental return
with payment gateway integration.
```

### Task 8: Penalty Waiver Service
```
feat(penalty): implement penalty waiver service

Add full/partial waiver with mandatory reason and audit trail.
Support refund processing for post-payment waivers.
```

### Task 9: Late Return Reporting
```
feat(penalty): implement late return reporting service

Add filtering, sorting, and statistics for late returns.
```

### Task 10: REST API Endpoints
```
feat(api): add late return REST endpoints

Add LateReturnController and PenaltyWaiverController
with OpenAPI documentation.
```

### Task 11: Exception Handling
```
feat(penalty): add custom exceptions

Add LateReturnException, PenaltyCalculationException,
PenaltyWaiverException, InvalidPenaltyConfigException.
```

```
feat(api): update GlobalExceptionHandler for penalty exceptions

Add handlers for late return and penalty exceptions.
```

### Task 13: Unit Tests
```
test(penalty): add comprehensive unit tests for late return system

Add 93 unit tests covering penalty calculation, detection,
scheduling, payment, waiver, and reporting services.
```

### Task 14: Integration Tests
```
test(api): add LateReturnController integration tests

Test late return report and statistics endpoints.
```

```
test(api): add PenaltyWaiverController integration tests

Test penalty waiver and history endpoints with authorization.
```

### Task 15: E2E Tests
```
test(e2e): add late return penalty E2E test

Test complete flow: rental → late detection → penalty → payment.
Verify scheduler, events, payment gateway, and waiver integration.
```

## Merge & Rollback

```bash
# Merge
git checkout main
git merge feature/late-return-penalty

# Rollback
git revert <commit-hash>
```

## Testing

```bash
# Run all late return tests
mvn test -Dtest="*LateReturn*,*Penalty*"

# Run specific test class
mvn test -Dtest="PenaltyCalculationServiceImplTest"

# Run integration tests
mvn test -Dtest="LateReturnControllerIntegrationTest"
```

## Key Components

| Component | Description |
|-----------|-------------|
| LateReturnScheduler | 15-minute scheduled job for detection |
| PenaltyCalculationService | Hourly/daily penalty calculation |
| LateReturnDetectionService | Status classification |
| PenaltyPaymentService | Automatic penalty charging |
| PenaltyWaiverService | Admin waiver with audit trail |
| LateReturnReportService | Reporting and statistics |
