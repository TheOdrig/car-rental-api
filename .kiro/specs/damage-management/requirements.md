# Requirements Document

## Introduction

Damage Management System, araç iadesi sırasında tespit edilen hasarları kayıt altına alan, değerlendiren ve ek ücretlendirme sürecini yöneten bir sistemdir. Bu sistem, rent-a-car işletmesinin araç filosunu korumak, hasar maliyetlerini müşterilerden tahsil etmek ve hasar geçmişini takip etmek için kritik öneme sahiptir.

Sistem, hasar kaydından değerlendirme, ücretlendirme ve çözüme kadar tüm süreci yönetir. Fotoğraf yükleme, hasar kategorilendirme ve otomatik ücret hesaplama ile operasyonel verimliliği artırır.

## Glossary

- **Damage_Management_System**: Hasar kaydı, değerlendirme ve ücretlendirme işlemlerini yöneten ana sistem
- **Damage_Report**: Araç iadesi sırasında tespit edilen hasar kaydı
- **Damage_Severity**: Hasar şiddeti (MINOR, MODERATE, MAJOR, TOTAL_LOSS)
- **Damage_Category**: Hasar kategorisi (SCRATCH, DENT, GLASS_DAMAGE, TIRE_DAMAGE, INTERIOR_DAMAGE, MECHANICAL_DAMAGE)
- **Damage_Status**: Hasar durumu (REPORTED, UNDER_ASSESSMENT, ASSESSED, CHARGED, DISPUTED, RESOLVED)
- **Damage_Assessment**: Admin tarafından yapılan hasar değerlendirmesi
- **Repair_Cost**: Tamir maliyeti tahmini
- **Customer_Liability**: Müşterinin sorumlu olduğu tutar (sigorta indirimi sonrası)
- **Damage_Photo**: Hasarın fotoğraf kanıtı
- **Damage_Event**: Hasar tespit edildiğinde yayınlanan domain event

## Requirements

### Requirement 1

**User Story:** As a rental administrator, I want to report vehicle damage during return inspection, so that I can document all damages for proper handling.

#### Acceptance Criteria

1. WHEN an administrator inspects a returned vehicle THEN the Damage_Management_System SHALL allow creating a damage report with description, location, and severity
2. WHEN creating a damage report THEN the Damage_Management_System SHALL require at least one photo upload as evidence
3. WHEN a damage report is created THEN the Damage_Management_System SHALL automatically link it to the rental and car records
4. WHEN a damage report is created THEN the Damage_Management_System SHALL set the initial status to REPORTED
5. WHEN a damage report is created THEN the Damage_Management_System SHALL record the reporting administrator's ID and timestamp

### Requirement 2

**User Story:** As a rental administrator, I want to upload multiple photos of vehicle damage, so that I can provide comprehensive visual evidence.

#### Acceptance Criteria

1. WHEN uploading damage photos THEN the Damage_Management_System SHALL accept JPEG, PNG, and HEIC formats
2. WHEN uploading damage photos THEN the Damage_Management_System SHALL limit each photo to maximum 10MB file size
3. WHEN uploading damage photos THEN the Damage_Management_System SHALL allow up to 10 photos per damage report
4. WHEN storing damage photos THEN the Damage_Management_System SHALL generate unique filenames to prevent conflicts
5. WHEN retrieving damage photos THEN the Damage_Management_System SHALL provide secure URLs with expiration time

### Requirement 3

**User Story:** As a damage assessor, I want to evaluate reported damages and estimate repair costs, so that I can determine customer liability.

#### Acceptance Criteria

1. WHEN assessing a damage report THEN the Damage_Management_System SHALL allow updating Damage_Severity (MINOR, MODERATE, MAJOR, TOTAL_LOSS)
2. WHEN assessing a damage report THEN the Damage_Management_System SHALL allow categorizing damage type (SCRATCH, DENT, GLASS_DAMAGE, TIRE_DAMAGE, INTERIOR_DAMAGE, MECHANICAL_DAMAGE)
3. WHEN assessing a damage report THEN the Damage_Management_System SHALL require entering estimated Repair_Cost
4. WHEN assessing a damage report THEN the Damage_Management_System SHALL calculate Customer_Liability based on insurance coverage
5. WHEN assessment is completed THEN the Damage_Management_System SHALL update status to ASSESSED and record assessor ID and timestamp

### Requirement 4

**User Story:** As a business owner, I want the system to automatically calculate customer liability based on damage severity and insurance coverage, so that charges are fair and consistent.

#### Acceptance Criteria

1. WHEN calculating customer liability for MINOR damage (repair cost < 500 TRY) THEN the Damage_Management_System SHALL charge 100% to customer if no insurance
2. WHEN calculating customer liability for MODERATE damage (500-2000 TRY) THEN the Damage_Management_System SHALL apply insurance deductible if coverage exists
3. WHEN calculating customer liability for MAJOR damage (2000-10000 TRY) THEN the Damage_Management_System SHALL apply insurance deductible if coverage exists
4. WHEN calculating customer liability for TOTAL_LOSS (>10000 TRY) THEN the Damage_Management_System SHALL apply insurance deductible if coverage exists
5. WHEN no insurance coverage exists THEN the Damage_Management_System SHALL charge full repair cost to customer

### Requirement 5

**User Story:** As a system administrator, I want the system to automatically charge customers for damage liability, so that revenue is not lost due to vehicle damages.

#### Acceptance Criteria

1. WHEN a damage assessment is completed THEN the Damage_Management_System SHALL create a damage charge payment record
2. WHEN a damage charge is created THEN the Damage_Management_System SHALL attempt to charge the customer's payment method on file
3. IF the automatic damage charge fails THEN the Damage_Management_System SHALL mark the payment as PENDING and notify the administrator
4. WHEN a damage charge is successfully captured THEN the Damage_Management_System SHALL update the damage status to CHARGED
5. WHEN processing damage charge THEN the Damage_Management_System SHALL use the same currency as the original rental

### Requirement 6

**User Story:** As a customer, I want to receive notifications about damage reports and charges, so that I am informed about any issues with my rental.

#### Acceptance Criteria

1. WHEN a damage report is created THEN the Damage_Management_System SHALL send a notification to the customer within 1 hour
2. WHEN a damage assessment is completed THEN the Damage_Management_System SHALL send a detailed assessment notification with photos and cost breakdown
3. WHEN a damage charge is processed THEN the Damage_Management_System SHALL send a payment confirmation notification
4. WHEN a damage charge fails THEN the Damage_Management_System SHALL send a payment failure notification with instructions
5. WHEN a damage is resolved THEN the Damage_Management_System SHALL send a final resolution notification

### Requirement 7

**User Story:** As a rental administrator, I want to view damage history for vehicles and customers, so that I can identify patterns and make informed decisions.

#### Acceptance Criteria

1. WHEN viewing vehicle damage history THEN the Damage_Management_System SHALL display all damage reports for that vehicle sorted by date
2. WHEN viewing customer damage history THEN the Damage_Management_System SHALL display all damage reports for that customer sorted by date
3. WHEN displaying damage history THEN the Damage_Management_System SHALL include damage severity, category, cost, and resolution status
4. WHEN filtering damage reports THEN the Damage_Management_System SHALL support filtering by date range, severity, category, and status
5. WHEN calculating damage statistics THEN the Damage_Management_System SHALL provide total count, total cost, and average cost per damage

### Requirement 8

**User Story:** As a customer service representative, I want to handle damage disputes, so that I can resolve customer complaints fairly.

#### Acceptance Criteria

1. WHEN a customer disputes a damage charge THEN the Damage_Management_System SHALL allow marking the damage status as DISPUTED
2. WHEN a damage is disputed THEN the Damage_Management_System SHALL require entering dispute reason and customer comments
3. WHEN reviewing a disputed damage THEN the Damage_Management_System SHALL allow adjusting the repair cost and customer liability
4. WHEN a dispute is resolved THEN the Damage_Management_System SHALL update status to RESOLVED and record resolution details
5. WHEN a damage charge is adjusted after payment THEN the Damage_Management_System SHALL initiate a refund for the difference

### Requirement 9

**User Story:** As a business owner, I want to configure damage severity thresholds and insurance deductibles, so that I can adjust the policy based on business needs.

#### Acceptance Criteria

1. WHEN configuring MINOR damage threshold THEN the Damage_Management_System SHALL accept values between 100 and 1000 TRY
2. WHEN configuring MODERATE damage threshold THEN the Damage_Management_System SHALL accept values between 500 and 3000 TRY
3. WHEN configuring MAJOR damage threshold THEN the Damage_Management_System SHALL accept values between 2000 and 15000 TRY
4. WHEN configuring insurance deductible THEN the Damage_Management_System SHALL accept values between 500 and 5000 TRY
5. WHEN configuration values are updated THEN the Damage_Management_System SHALL apply new values only to future damage assessments

### Requirement 10

**User Story:** As a rental administrator, I want to update car status when damage is reported, so that damaged vehicles are not rented out.

#### Acceptance Criteria

1. WHEN a MAJOR or TOTAL_LOSS damage is reported THEN the Damage_Management_System SHALL automatically update car status to MAINTENANCE
2. WHEN a MODERATE damage is reported THEN the Damage_Management_System SHALL allow administrator to decide whether to mark car as MAINTENANCE
3. WHEN a MINOR damage is reported THEN the Damage_Management_System SHALL keep car status as AVAILABLE unless administrator changes it
4. WHEN a damage is resolved and repairs are completed THEN the Damage_Management_System SHALL allow administrator to update car status back to AVAILABLE
5. WHEN car status is updated due to damage THEN the Damage_Management_System SHALL record the status change reason and timestamp
