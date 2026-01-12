# Requirements Document

## Introduction

Late Return & Penalty System, kiralama süresini aşan araç iadelerini otomatik olarak tespit eden, ceza hesaplayan ve tahsilat sürecini yöneten bir sistemdir. Bu sistem, rent-a-car işletmesinin gelir kaybını önlemek ve müşterilerin zamanında iade yapmasını teşvik etmek için kritik öneme sahiptir.

Sistem, grace period (tolerans süresi) ile müşteri dostu bir yaklaşım sunarken, geç iadeler için adil ve şeffaf bir ceza mekanizması uygular. Otomatik tespit, bildirim ve ödeme tahsilatı ile operasyonel verimliliği artırır.

## Glossary

- **Late_Return_System**: Geç iade tespit, ceza hesaplama ve tahsilat işlemlerini yöneten ana sistem
- **Grace_Period**: Geç iade için tolerans süresi (varsayılan: 1 saat), bu süre içinde ceza uygulanmaz
- **Penalty**: Geç iade nedeniyle uygulanan ek ücret
- **Hourly_Penalty_Rate**: Saatlik ceza oranı (günlük kiralama ücretinin yüzdesi)
- **Daily_Penalty_Rate**: Günlük ceza oranı (günlük kiralama ücretinin katı)
- **Late_Hours**: Grace period sonrası geçen saat sayısı
- **Late_Days**: Grace period sonrası geçen tam gün sayısı
- **Penalty_Cap**: Maksimum ceza limiti (günlük kiralama ücretinin katı olarak)
- **Late_Return_Status**: Geç iade durumu (ON_TIME, GRACE_PERIOD, LATE, SEVERELY_LATE)
- **Penalty_Payment**: Ceza ödemesi için oluşturulan ödeme kaydı
- **Late_Return_Event**: Geç iade tespit edildiğinde yayınlanan domain event

## Requirements

### Requirement 1

**User Story:** As a system administrator, I want the system to automatically detect late returns, so that I can take timely action on overdue rentals.

#### Acceptance Criteria

1. WHEN a rental's end date passes and the car is not returned THEN the Late_Return_System SHALL mark the rental as late within 15 minutes of the scheduled return time
2. WHEN the Late_Return_System detects a late return THEN the Late_Return_System SHALL record the exact timestamp of late detection
3. WHEN a rental is within the Grace_Period (1 hour after end date) THEN the Late_Return_System SHALL mark the status as GRACE_PERIOD without applying penalties
4. WHEN a rental exceeds the Grace_Period THEN the Late_Return_System SHALL mark the status as LATE and begin penalty calculation
5. WHEN a rental exceeds 24 hours past the end date THEN the Late_Return_System SHALL mark the status as SEVERELY_LATE

### Requirement 2

**User Story:** As a business owner, I want the system to calculate penalties based on how late the return is, so that customers are fairly charged for delays.

#### Acceptance Criteria

1. WHEN calculating penalties for Late_Hours between 1 and 6 THEN the Late_Return_System SHALL apply Hourly_Penalty_Rate of 10% of daily rental price per hour
2. WHEN calculating penalties for Late_Hours between 7 and 24 THEN the Late_Return_System SHALL charge one full Daily_Penalty_Rate (150% of daily rental price)
3. WHEN calculating penalties for Late_Days exceeding 1 day THEN the Late_Return_System SHALL charge Daily_Penalty_Rate (150% of daily rental price) for each additional day
4. WHEN the calculated penalty exceeds the Penalty_Cap (5x daily rental price) THEN the Late_Return_System SHALL limit the penalty to the Penalty_Cap amount
5. WHEN a rental is returned during Grace_Period THEN the Late_Return_System SHALL apply zero penalty

### Requirement 3

**User Story:** As a customer, I want to receive notifications about my late return status, so that I can take action to minimize penalties.

#### Acceptance Criteria

1. WHEN a rental enters GRACE_PERIOD status THEN the Late_Return_System SHALL send a warning notification to the customer within 5 minutes
2. WHEN a rental enters LATE status THEN the Late_Return_System SHALL send a late return notification with current penalty amount
3. WHEN a rental enters SEVERELY_LATE status THEN the Late_Return_System SHALL send an urgent notification with escalation warning
4. WHEN the penalty amount increases by more than 50% of daily rate THEN the Late_Return_System SHALL send an updated penalty notification
5. WHEN a late rental is returned THEN the Late_Return_System SHALL send a final penalty summary notification

### Requirement 4

**User Story:** As a system administrator, I want the system to automatically collect penalty payments, so that revenue is not lost due to late returns.

#### Acceptance Criteria

1. WHEN a late rental is returned THEN the Late_Return_System SHALL create a Penalty_Payment record with the calculated penalty amount
2. WHEN a Penalty_Payment is created THEN the Late_Return_System SHALL attempt to charge the customer's payment method on file
3. IF the automatic penalty charge fails THEN the Late_Return_System SHALL mark the payment as PENDING and notify the administrator
4. WHEN a Penalty_Payment is successfully captured THEN the Late_Return_System SHALL update the rental record with penalty details
5. WHEN processing penalty payment THEN the Late_Return_System SHALL use the same currency as the original rental

### Requirement 5

**User Story:** As a system administrator, I want to view late return reports, so that I can monitor and manage overdue rentals effectively.

#### Acceptance Criteria

1. WHEN an administrator requests late return report THEN the Late_Return_System SHALL return all rentals with LATE or SEVERELY_LATE status
2. WHEN displaying late return data THEN the Late_Return_System SHALL include rental ID, customer info, car info, late duration, and current penalty amount
3. WHEN filtering late returns by date range THEN the Late_Return_System SHALL return only rentals with end dates within the specified range
4. WHEN sorting late returns THEN the Late_Return_System SHALL support sorting by late duration, penalty amount, and end date
5. WHEN calculating late return statistics THEN the Late_Return_System SHALL provide total count, total penalty amount, and average late duration

### Requirement 6

**User Story:** As a business owner, I want to configure penalty rates and grace period, so that I can adjust the policy based on business needs.

#### Acceptance Criteria

1. WHEN configuring Grace_Period THEN the Late_Return_System SHALL accept values between 0 and 120 minutes
2. WHEN configuring Hourly_Penalty_Rate THEN the Late_Return_System SHALL accept values between 5% and 25% of daily rental price
3. WHEN configuring Daily_Penalty_Rate THEN the Late_Return_System SHALL accept values between 100% and 200% of daily rental price
4. WHEN configuring Penalty_Cap THEN the Late_Return_System SHALL accept values between 3x and 10x daily rental price
5. WHEN configuration values are updated THEN the Late_Return_System SHALL apply new values only to future late returns

### Requirement 7

**User Story:** As a customer service representative, I want to manually waive or adjust penalties, so that I can handle exceptional circumstances.

#### Acceptance Criteria

1. WHEN an administrator requests penalty waiver THEN the Late_Return_System SHALL allow full or partial waiver with mandatory reason
2. WHEN a penalty is waived THEN the Late_Return_System SHALL record the waiver reason, administrator ID, and timestamp
3. WHEN a penalty is partially waived THEN the Late_Return_System SHALL recalculate the remaining amount to be charged
4. WHEN viewing penalty history THEN the Late_Return_System SHALL display all adjustments and waivers with audit trail
5. WHEN a waiver is applied after payment THEN the Late_Return_System SHALL initiate a refund for the waived amount
