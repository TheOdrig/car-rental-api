# Implementation Plan: Damage Management System

## Task Design Rules

> ⚠️ **Her task sonunda proje MUTLAKA compile olmalı**
> - Task içinde interface varsa, referans verdiği tipler de aynı task'ta olmalı
> - Her task sonunda mevcut testler geçmeli
> - Her task atomik ve bağımsız olmalı

---

## Task List

### Phase 1: Foundation (Types & Contracts)

- [x] 1. Create core types and contracts
  - **Enums**: DamageStatus, DamageSeverity, DamageCategory
  - **DTOs**: All request and response DTOs
  - **Entities**: DamageReport, DamagePhoto (with all fields)
  - **Interfaces**: IDamageReportService, IDamageAssessmentService, IDamageChargeService, IDamageDisputeService, IDamageHistoryService, IFileUploadService
  - _Compile check_: ✅ All types resolve, no missing imports
  - _Requirements: 1.1, 1.4, 2.1, 3.1, 3.2, 5.1, 6.1, 7.1, 8.1, 9.1, 10.1_

### Phase 2: Infrastructure

- [x] 2. Create database infrastructure
  - [x] 2.1 Create Flyway migration V10__create_damage_tables.sql
    - Create damage_reports table with all columns
    - Create damage_photos table with foreign key
    - Add indexes on rental_id, car_id, status, reported_at
    - _Requirements: 1.1, 1.2, 7.1, 7.2_
  - [x] 2.2 Extend Rental entity with damage tracking
    - Add hasDamageReports boolean field
    - Add damageReportsCount integer field
    - Create migration V11 for new columns
    - _Requirements: 1.3_
  - [x] 2.3 Create DamageConfig configuration class
    - Add severity thresholds (minor, moderate, major)
    - Add insurance deductible settings
    - Add photo upload limits (max count, max size, allowed types)
    - Add storage settings (directory, URL expiration)
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_
  - _Compile check_: ✅ Application compiles, migrations ready

- [x] 3. Create repositories
  - [x] 3.1 Create DamageReportRepository
    - Add findByIdAndIsDeletedFalse
    - Add findByRentalIdAndIsDeletedFalse
    - Add findByCarIdAndIsDeletedFalse (for vehicle history)
    - Add findByRental_UserIdAndIsDeletedFalse (for customer history)
    - Add custom search query with filters
    - Add statistics aggregation query
    - _Requirements: 1.1, 7.1, 7.2, 7.3, 7.4, 7.5_
  - [x] 3.2 Create DamagePhotoRepository
    - Add findByDamageReportIdAndIsDeletedFalse
    - Add countByDamageReportIdAndIsDeletedFalse (for max photo check)
    - _Requirements: 2.1, 2.3_
  - _Compile check_: ✅ Repositories extend JpaRepository correctly

### Phase 3: Service Implementations

- [x] 4. Implement file upload service
  - [x] 4.1 Create LocalFileStorageService for development
    - Implement file upload to local directory
    - Implement file deletion
    - Implement secure URL generation (file:// protocol)
    - Add file type and size validation
    - Add @Profile("!prod") annotation
    - **Create FileUploadException** (for compile-aware)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  - [x] 4.2 Create R2FileStorageService for production
    - Implement R2 upload with S3-compatible API
    - Implement R2 deletion
    - Implement pre-signed URL generation
    - Add @Profile("prod") annotation
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  - _Compile check_: ✅ Both services implement IFileUploadService

- [x] 5. Implement damage report service
  - [x] 5.1 Create DamageReportServiceImpl
    - Implement createDamageReport (validate rental, create report, set status to REPORTED)
    - Implement getDamageReport with authorization check
    - Implement uploadDamagePhotos (validate count/size, save files, create photo records)
    - Implement deleteDamagePhoto with authorization check
    - Implement getPhotoUrl (generate secure URL)
    - Publish DamageReportedEvent after creation
    - **Create DamageReportException** (for compile-aware) ✅
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 6.1_
  - _Compile check_: ✅ Service implements IDamageReportService

- [x] 6. Implement damage assessment service
  - [x] 6.1 Create DamageAssessmentServiceImpl
    - Implement assessDamage (validate status, calculate liability, update fields, set status to ASSESSED)
    - Implement updateAssessment (allow updates before charge)
    - Implement calculateCustomerLiability (insurance logic)
    - Implement determineSeverity (based on repair cost thresholds)
    - Publish DamageAssessedEvent after assessment
    - **Create DamageAssessmentException** (for compile-aware) ✅
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 6.2_
  - [x] 6.2 Add car status update logic
    - Update car status to MAINTENANCE for MAJOR or TOTAL_LOSS
    - Allow admin decision for MODERATE
    - Keep AVAILABLE for MINOR
    - Record status change reason and timestamp
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  - _Compile check_: ✅ Service implements IDamageAssessmentService

- [x] 7. Implement damage charge service
  - [x] 7.1 Create DamageChargeServiceImpl
    - Implement createDamageCharge (create Payment record with customer liability)
    - Implement chargeDamage (call payment gateway, update status to CHARGED)
    - Implement handleFailedDamageCharge (mark as PENDING, notify admin)
    - Ensure currency consistency with original rental
    - Publish DamageChargedEvent after successful charge
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.3_
  - _Compile check_: ✅ Service implements IDamageChargeService

- [x] 8. Implement damage dispute service
  - [x] 8.1 Create DamageDisputeServiceImpl
    - Implement createDispute (validate status, set status to DISPUTED, record reason)
    - Implement resolveDispute (adjust amounts, set status to RESOLVED, process refund if needed)
    - Implement processRefundForAdjustment (call payment gateway refund)
    - Publish DamageDisputedEvent and DamageResolvedEvent
    - **Create DamageDisputeException** (for compile-aware) ✅
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 6.4, 6.5_
  - _Compile check_: ✅ Service implements IDamageDisputeService

- [x] 9. Implement damage history service
  - [x] 9.1 Create DamageHistoryServiceImpl
    - Implement getDamagesByVehicle with pagination
    - Implement getDamagesByCustomer with pagination and authorization
    - Implement searchDamages with filters (date range, severity, category, status)
    - Implement getDamageStatistics with aggregations
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  - _Compile check_: ✅ Service implements IDamageHistoryService

### Phase 4: API Layer

- [x] 10. Implement controllers
  - [x] 10.1 Create DamageReportController
    - POST /api/admin/damages - Create damage report (admin only)
    - GET /api/admin/damages/{id} - Get damage report
    - POST /api/admin/damages/{id}/photos - Upload photos (admin only)
    - DELETE /api/admin/damages/{id}/photos/{photoId} - Delete photo (admin only)
    - GET /api/damages/photos/{photoId}/url - Get secure photo URL
    - Add OpenAPI annotations
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 2.4, 2.5_
  - [x] 10.2 Create DamageAssessmentController
    - POST /api/admin/damages/{id}/assess - Assess damage (admin only)
    - PUT /api/admin/damages/{id}/assess - Update assessment (admin only)
    - Add OpenAPI annotations
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  - [x] 10.3 Create DamageDisputeController
    - POST /api/damages/{id}/dispute - Create dispute (customer, own rentals only)
    - POST /api/admin/damages/{id}/resolve - Resolve dispute (admin only)
    - Add OpenAPI annotations
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  - [x] 10.4 Create DamageHistoryController
    - GET /api/admin/damages/vehicle/{carId} - Get vehicle damage history (admin only)
    - GET /api/admin/damages/customer/{userId} - Get customer damage history (admin only)
    - GET /api/damages/me - Get my damage history (customer, own only)
    - GET /api/admin/damages/search - Search damages (admin only)
    - GET /api/admin/damages/statistics - Get statistics (admin only)
    - Add OpenAPI annotations
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  - _Compile check_: ✅ Controllers inject services, endpoints accessible

- [x] 11. Add exception handlers to GlobalExceptionHandler
  - [x] 11.1 Add handlers for damage exceptions
    - Handle DamageReportException (created in Task 5) ✅
    - Handle DamageAssessmentException (created in Task 6) ✅
    - Handle FileUploadException (created in Task 4) ✅
    - Handle DamageDisputeException (created in Task 8) ✅
  - _Note: Added generic BaseException handler - catches all custom exceptions_
  - _Compile check_: ✅ Exceptions handled with appropriate HTTP status codes

### Phase 5: Events & Notifications

- [x] 12. Implement events and email notifications
  - [x] 12.1 Create damage events
    - DamageReportedEvent ✅
    - DamageAssessedEvent ✅
    - DamageChargedEvent ✅
    - DamageDisputedEvent ✅
    - DamageResolvedEvent ✅
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  - [x] 12.2 Create damage email templates
    - damage-reported.html - Notify customer about damage report ✅
    - damage-assessed.html - Notify customer about assessment and charge ✅
    - damage-charged.html - Payment confirmation ✅
    - damage-charge-failed.html - Payment failure notification ✅
    - damage-disputed.html - Dispute confirmation ✅
    - damage-resolved.html - Dispute resolution notification ✅
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  - [x] 12.3 Add event listeners to EmailEventListener
    - Listen to all damage events → send corresponding emails ✅
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  - _Compile check_: ✅ Events implement ApplicationEvent pattern

### Phase 6: Verification

- [x] 13. Manual testing checkpoint
  - Manually test damage report creation via Postman/Swagger
  - Manually test photo upload
  - Manually test damage assessment
  - Manually test charge creation
  - Manually test dispute flow
  - Ask the user if questions arise

### Phase 7: Automated Testing (Optional)

- [x]* 14. Write unit tests
  - [x]* 14.1 FileUploadService tests
  - [x]* 14.2 DamageReportService tests
  - [x]* 14.3 DamageAssessmentService tests
  - [x]* 14.4 DamageChargeService tests
  - [x]* 14.5 DamageDisputeService tests
  - [x]* 14.6 DamageHistoryService tests
  - _Requirements: All service layer_

- [x]* 15. Write integration tests
  - [x]* 15.1 DamageReportController integration tests
  - [x]* 15.2 DamageAssessmentController integration tests
  - [x]* 15.3 DamageDisputeController integration tests
  - [x]* 15.4 DamageHistoryController integration tests
  - _Requirements: All controller endpoints_

- [x]* 16. Write end-to-end test
  - [x]* 16.1 Complete damage flow E2E test
    - Create rental → Return → Report damage → Upload photos → Assess → Charge → Dispute → Resolve
    - Verify all status transitions, email events, payment, car status
    - _Requirements: All_

- [x]* 17. Final test verification
  - Run all unit tests
  - Run all integration tests
  - Run E2E test
  - Ensure all tests pass

### Phase 8: Documentation

- [x] 18. Update project documentation
  - [x] 18.1 Update README.md
    - Add Damage Management System section
    - Document workflow (REPORTED → UNDER_ASSESSMENT → ASSESSED → CHARGED → DISPUTED → RESOLVED)
    - Add API endpoints documentation (14 endpoints)
    - Add configuration properties section
    - Add file storage setup instructions
    - _Requirements: All_

---

## Implementation Notes

### Compile Check Points

| After Task | Expected State |
|------------|---------------|
| Task 1 | All types compile, no missing imports |
| Task 2 | Application starts, DB tables created |
| Task 3 | Repositories inject correctly |
| Task 4-9 | Services inject and implement interfaces |
| Task 10 | Controllers accessible via Swagger |
| Task 11 | Exceptions handled correctly |
| Task 12 | Events publish and listeners fire |

### Database Migrations
- **V10__create_damage_tables.sql**: Create damage_reports and damage_photos tables
- Indexes: rental_id, car_id, status, reported_at, assessed_at

### Configuration Properties
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

### Security Configuration
- Damage report creation: `@PreAuthorize("hasRole('ADMIN')")`
- Damage assessment: `@PreAuthorize("hasRole('ADMIN')")`
- Dispute creation: `@PreAuthorize("hasRole('USER')")` + ownership check
- Dispute resolution: `@PreAuthorize("hasRole('ADMIN')")`
- History viewing: Admin (all), Customer (own only)

### Event-Driven Architecture
- **DamageReportedEvent**: Triggered after damage report creation
- **DamageAssessedEvent**: Triggered after assessment completion
- **DamageChargedEvent**: Triggered after successful charge
- **DamageDisputedEvent**: Triggered when customer disputes
- **DamageResolvedEvent**: Triggered when dispute is resolved

### File Storage Strategy
- **Development**: Local file system (`uploads/damage-photos/`)
- **Production**: Cloudflare R2 with pre-signed URLs (S3-compatible API)
- **Profile-based switching**: `@Profile("!prod")` vs `@Profile("prod")`

### Testing Strategy
- **Unit Tests**: Service layer logic (marked with *)
- **Integration Tests**: Controller endpoints (marked with *)
- **E2E Tests**: Complete damage flow (marked with *)
- **Test Framework**: JUnit 5, Mockito, MockMvc, AssertJ, MockMultipartFile

### Dependencies to Add
```xml
<!-- Cloudflare R2 via AWS SDK S3-compatible API (production) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.21.0</version>
</dependency>
```

### API Endpoints Summary
```
POST   /api/admin/damages                           - Create damage report
GET    /api/admin/damages/{id}                      - Get damage report
POST   /api/admin/damages/{id}/photos               - Upload photos
DELETE /api/admin/damages/{id}/photos/{photoId}    - Delete photo
GET    /api/damages/photos/{photoId}/url            - Get secure photo URL
POST   /api/admin/damages/{id}/assess               - Assess damage
PUT    /api/admin/damages/{id}/assess               - Update assessment
POST   /api/damages/{id}/dispute                    - Create dispute
POST   /api/admin/damages/{id}/resolve              - Resolve dispute
GET    /api/admin/damages/vehicle/{carId}           - Get vehicle history
GET    /api/admin/damages/customer/{userId}         - Get customer history
GET    /api/damages/me                              - Get my damages
GET    /api/admin/damages/search                    - Search damages
GET    /api/admin/damages/statistics                - Get statistics
```
