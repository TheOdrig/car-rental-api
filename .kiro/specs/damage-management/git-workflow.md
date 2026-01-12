# Git Workflow - Damage Management System

## Branch Strategy

```
main
  └── feature/damage-management
```

## Task Design Rules

> ⚠️ **Her task sonunda proje MUTLAKA compile olmalı**

---

## Commit Plan

### Phase 1: Foundation

#### Task 1: Core Types and Contracts
```
feat(damage): add core types, DTOs, entities and interfaces

Add damage management type system:
- 3 enums: DamageStatus, DamageSeverity, DamageCategory
- 5 request DTOs, 6 response DTOs
- 2 entities: DamageReport, DamagePhoto
- 6 service interfaces

Note: Entities created here for interface compilation.
Migration added in Task 2 for infrastructure separation.
```

### Phase 2: Infrastructure

#### Task 2: Database Infrastructure
```
chore(db)!: add damage tables migration

Create V10__create_damage_tables.sql with damage_reports and damage_photos.
Add indexes on rental_id, car_id, status, reported_at.

BREAKING CHANGE: New damage_reports and damage_photos tables.
```

```
feat(rental)!: extend Rental entity with damage tracking

Add hasDamageReports and damageReportsCount fields.
Add migration for new columns.

BREAKING CHANGE: Rental entity schema updated.
```

```
feat(damage): add DamageConfig configuration

Add @ConfigurationProperties for severity thresholds,
insurance deductible, photo upload limits, and storage settings.
```

#### Task 3: Repositories
```
feat(damage): add DamageReportRepository and DamagePhotoRepository

Add findByIdAndIsDeletedFalse, findByRentalIdAndIsDeletedFalse,
findByCarIdAndIsDeletedFalse, findByRental_UserIdAndIsDeletedFalse.
Add custom search and statistics queries.
```

### Phase 3: Service Implementations

#### Task 4: File Upload Service
```
feat(storage): implement LocalFileStorageService

Add local file system storage for development.
Implement file upload, deletion, secure URL generation, validation.
Add @Profile("!prod") annotation.
```

```
feat(storage): implement R2FileStorageService

Add Cloudflare R2 storage for production with pre-signed URLs.
Use S3-compatible API. Add @Profile("prod") annotation.
```

#### Task 5: Damage Report Service
```
feat(damage): implement DamageReportServiceImpl

Add createDamageReport, getDamageReport, uploadDamagePhotos,
deleteDamagePhoto, getPhotoUrl methods.
Publish DamageReportedEvent after creation.
```

#### Task 6: Damage Assessment Service
```
feat(damage): implement DamageAssessmentServiceImpl

Add assessDamage, updateAssessment, calculateCustomerLiability,
determineSeverity methods.
Add car status update logic for MAJOR/TOTAL_LOSS.
Publish DamageAssessedEvent after assessment.
```

#### Task 7: Damage Charge Service
```
feat(payment): implement DamageChargeServiceImpl

Add createDamageCharge, chargeDamage, handleFailedDamageCharge.
Ensure currency consistency with rental.
Publish DamageChargedEvent after successful charge.
```

#### Task 8: Damage Dispute Service
```
feat(damage): implement DamageDisputeServiceImpl

Add createDispute, resolveDispute, processRefundForAdjustment.
Publish DamageDisputedEvent and DamageResolvedEvent.
```

#### Task 9: Damage History Service
```
feat(damage): implement DamageHistoryServiceImpl

Add getDamagesByVehicle, getDamagesByCustomer, searchDamages,
getDamageStatistics methods with pagination and filtering.
```

### Phase 4: API Layer

#### Task 10: Controllers
```
feat(api): add DamageReportController

Add POST /api/admin/damages, GET /api/admin/damages/{id},
POST /api/admin/damages/{id}/photos, DELETE /api/admin/damages/{id}/photos/{photoId},
GET /api/damages/photos/{photoId}/url endpoints.
Add OpenAPI annotations and admin authorization.
```

```
feat(api): add DamageAssessmentController

Add POST /api/admin/damages/{id}/assess, PUT /api/admin/damages/{id}/assess
endpoints with admin authorization.
```

```
feat(api): add DamageDisputeController

Add POST /api/damages/{id}/dispute, POST /api/admin/damages/{id}/resolve
endpoints with role-based authorization.
```

```
feat(api): add DamageHistoryController

Add GET /api/admin/damages/vehicle/{carId},
GET /api/admin/damages/customer/{userId}, GET /api/damages/me,
GET /api/admin/damages/search, GET /api/admin/damages/statistics endpoints.

Compile check: Controllers accessible via Swagger.
```

#### Task 11: Exception Handlers
```
feat(damage): add damage exception handlers

Add DamageReportException, DamageAssessmentException, FileUploadException,
DamageDisputeException, InvalidDamageConfigException to GlobalExceptionHandler.

Compile check: Exceptions extend BaseException.
```

### Phase 5: Events & Notifications

#### Task 12: Events and Email Notifications
```
feat(damage): add damage events

Add DamageReportedEvent, DamageAssessedEvent, DamageChargedEvent,
DamageDisputedEvent, DamageResolvedEvent.
```

```
feat(email): add damage email templates

Add damage-reported.html, damage-assessed.html, damage-charged.html,
damage-charge-failed.html, damage-disputed.html, damage-resolved.html
templates.
```

```
feat(email): add damage event listeners

Add listeners for all damage events.
Send corresponding emails via EmailSender.

Compile check: Events publish and listeners fire.
```

### Phase 6: Verification

#### Task 13: Manual Testing Checkpoint
```
chore(damage): manual testing checkpoint

Manually test damage report creation, photo upload, assessment, charge,
dispute flow via Postman/Swagger.
```

### Phase 7: Automated Testing (Optional)

#### Tasks 14-17: Testing
```
test(damage): add unit tests

Add FileUploadService, DamageReportService, DamageAssessmentService,
DamageChargeService, DamageDisputeService, DamageHistoryService unit tests.
```

```
test(api): add integration tests

Add DamageReportController, DamageAssessmentController,
DamageHistoryController integration tests.
```

```
test(damage): add E2E damage flow test

Test complete flow: create rental → return → report damage → upload photos
→ assess → charge → dispute → resolve.
Verify status transitions, events, payment, car status.
```

```
test(damage): final test verification

Run all unit, integration, E2E tests.
Ensure all tests pass.
```

### Phase 8: Documentation

#### Task 18: Documentation
```
docs(readme): add damage management documentation

Add Damage Management System section to README.
Document workflow, API endpoints, configuration, file storage setup,
example usage scenarios.
Update technology stack with Cloudflare R2.
```

---

## Merge & Rollback

```bash
# Merge to main
git checkout main
git merge feature/damage-management

# Rollback specific commit
git revert <commit-hash>

# Rollback migration (if needed)
# Delete V10__create_damage_tables.sql
# Run: mvn flyway:clean flyway:migrate
```

## Testing

```bash
# Run all damage tests
mvn test -Dtest="*Damage*"

# Run specific test classes
mvn test -Dtest="DamageReportServiceTest"
mvn test -Dtest="DamageAssessmentServiceTest"
mvn test -Dtest="DamageReportControllerIntegrationTest"

# Run E2E test
mvn test -Dtest="DamageManagementE2ETest"

# Run with coverage
mvn clean test jacoco:report
```

## Key Components

| Component | Description |
|-----------|-------------|
| DamageReport | Entity tracking vehicle damage with photos, assessment, charge |
| DamagePhoto | Entity storing damage photo metadata and file paths |
| DamageStatus | Enum: REPORTED → UNDER_ASSESSMENT → ASSESSED → CHARGED → DISPUTED → RESOLVED |
| DamageSeverity | Enum: MINOR, MODERATE, MAJOR, TOTAL_LOSS (based on repair cost) |
| DamageCategory | Enum: SCRATCH, DENT, GLASS_DAMAGE, TIRE_DAMAGE, INTERIOR_DAMAGE, MECHANICAL_DAMAGE |
| DamageReportService | Service for creating reports and managing photos |
| DamageAssessmentService | Service for assessing damage and calculating liability |
| DamageChargeService | Service for creating and processing damage charges |
| DamageDisputeService | Service for handling customer disputes and resolutions |
| DamageHistoryService | Service for querying damage history and statistics |
| FileUploadService | Service for file storage (local/S3) with secure URLs |
| DamageReportController | REST API for damage reporting (admin only) |
| DamageAssessmentController | REST API for damage assessment (admin only) |
| DamageDisputeController | REST API for disputes (customer + admin) |
| DamageHistoryController | REST API for damage history and statistics |

## Configuration

```properties
# Damage Management Configuration
damage.minor-threshold=500
damage.moderate-threshold=2000
damage.major-threshold=10000
damage.default-insurance-deductible=1000.00
damage.max-photos-per-report=10
damage.max-photo-size-bytes=10485760
damage.allowed-photo-types=image/jpeg,image/png,image/heic
damage.photo-storage-directory=damage-photos
damage.photo-url-expiration-minutes=60

# Cloudflare R2 Configuration (production only)
r2.endpoint=${R2_ENDPOINT}
r2.bucket-name=${R2_BUCKET_NAME}
r2.access-key-id=${R2_ACCESS_KEY_ID}
r2.secret-access-key=${R2_SECRET_ACCESS_KEY}
```

## Dependencies

```xml
<!-- Cloudflare R2 via AWS SDK S3-compatible API (production) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.21.0</version>
</dependency>
```

## API Endpoints Summary

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /api/admin/damages | Create damage report | ADMIN |
| GET | /api/admin/damages/{id} | Get damage report | ADMIN |
| POST | /api/admin/damages/{id}/photos | Upload photos | ADMIN |
| DELETE | /api/admin/damages/{id}/photos/{photoId} | Delete photo | ADMIN |
| GET | /api/damages/photos/{photoId}/url | Get secure photo URL | USER |
| POST | /api/admin/damages/{id}/assess | Assess damage | ADMIN |
| PUT | /api/admin/damages/{id}/assess | Update assessment | ADMIN |
| POST | /api/damages/{id}/dispute | Create dispute | USER (own) |
| POST | /api/admin/damages/{id}/resolve | Resolve dispute | ADMIN |
| GET | /api/admin/damages/vehicle/{carId} | Get vehicle history | ADMIN |
| GET | /api/admin/damages/customer/{userId} | Get customer history | ADMIN |
| GET | /api/damages/me | Get my damages | USER |
| GET | /api/admin/damages/search | Search damages | ADMIN |
| GET | /api/admin/damages/statistics | Get statistics | ADMIN |

## Breaking Changes

### Migration V10
- **New tables**: `damage_reports`, `damage_photos`
- **Rental entity**: Added `hasDamageReports`, `damageReportsCount` fields
- **Indexes**: rental_id, car_id, status, reported_at, assessed_at

### API Changes
- 14 new endpoints added
- New authorization rules (admin-only for reporting/assessment)

## Rollback Strategy

### Database Rollback
```sql
-- If migration V10 needs rollback
DROP TABLE IF EXISTS damage_photos;
DROP TABLE IF EXISTS damage_reports;
ALTER TABLE rentals DROP COLUMN IF EXISTS has_damage_reports;
ALTER TABLE rentals DROP COLUMN IF EXISTS damage_reports_count;
```

### Code Rollback
```bash
# Revert all damage commits
git revert <first-commit-hash>^..<last-commit-hash>

# Or revert entire feature branch merge
git revert -m 1 <merge-commit-hash>
```

## Compile Check Points

| After Task | Expected State |
|------------|---------------|
| Task 1 | All types compile, no missing imports |
| Task 2 | Application starts, DB tables created |
| Task 3 | Repositories inject correctly |
| Task 4-9 | Services inject and implement interfaces |
| Task 10 | Controllers accessible via Swagger |
| Task 11 | Exceptions handled correctly |
| Task 12 | Events publish and listeners fire |

## Post-Merge Checklist

- [ ] Run all tests: `mvn clean test`
- [ ] Run Flyway migration: `mvn flyway:migrate`
- [ ] Verify database tables created
- [ ] Test file upload (local storage)
- [ ] Configure Cloudflare R2 (production)
- [ ] Test damage report creation via Swagger
- [ ] Test photo upload
- [ ] Test damage assessment
- [ ] Test charge creation
- [ ] Test dispute flow
- [ ] Verify email notifications sent
- [ ] Update API documentation
- [ ] Update README.md
