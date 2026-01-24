# Email Template Usage Guide

This guide documents the redesigned Thymeleaf email template system for the CarRental platform. All templates follow a unified design system, support dark mode, and use shared components for maintainability.

---

## 🏗️ Design System & Components

### Brand Identity
- **Primary Color:** `#4356d9` (Brand Blue)
- **Success Color:** `#10b981` (Emerald Green)
- **Warning Color:** `#f59e0b` (Amber)
- **Danger Color:** `#ef4444` (Red)

### Shared Components (`/templates/email/shared/`)
All templates include these fragments to maintain consistency:

| Component | Usage | Fragment Call |
|-----------|-------|---------------|
| `header` | Brand logo and page title | `th:replace="~{email/shared/header :: header('Title')}"` |
| `footer` | Copyright and auto-message info | `th:replace="~{email/shared/footer :: footer}"` |
| `button` | Primary Call-to-Action | `th:replace="~{email/shared/button :: primaryButton('Text', ${link})}"` |
| `info-box`| Blue-accented information box | `th:replace="~{email/shared/info-box :: infoBox('Message')}"` |
| `warning-box`| Amber-accented warning message | `th:replace="~{email/shared/warning-box :: warningBox('Message')}"` |

---

## 🗂️ Template Reference

### 1. Authentication (`/auth/`)

#### `password-reset.html`
- **Purpose:** Sent when a user requests a password reset.
- **Variables:**
  - `${email}`: User's email address.
  - `${resetLink}`: The unique URL to reset the password.
  - `${expirationHours}`: Token validity duration (default: 1).

---

### 2. Rental Lifecycle (`/rental/`)

#### `rental-confirmation.html`
- **Purpose:** Immediate confirmation after a successful booking.
- **Variables:**
  - `${rentalId}`, `${carBrand}`, `${carModel}`, `${pickupDate}`, `${returnDate}`, `${totalPrice}`, `${currency}`, `${pickupLocation}`.

#### `pickup-reminder.html`
- **Purpose:** Sent 24h before scheduled pickup.
- **Variables:**
  - `${rentalId}`, `${pickupDate}`, `${pickupLocation}`, `${carBrand}`, `${carModel}`, `${timeWindow}`.

#### `return-reminder.html`
- **Purpose:** Sent 24h before scheduled return.
- **Variables:**
  - `${rentalId}`, `${returnDate}`, `${returnLocation}`, `${dailyPenaltyRate}`.

#### `cancellation-confirmation.html`
- **Purpose:** Confirms booking cancellation and refund status.
- **Variables:**
  - `${rentalId}`, `${cancellationDate}`, `${cancellationReason}`, `${refundProcessed}` (boolean), `${refundAmount}`, `${refundTransactionId}`, `${refundTimeline}`.

---

### 3. Payment (`/payment/`)

#### `payment-receipt.html`
- **Purpose:** Standard receipt for successful payments.
- **Variables:**
  - `${paymentId}`, `${rentalId}`, `${transactionId}`, `${amount}`, `${currency}`, `${paymentDate}`.

---

### 4. Late Return & Penalties (`/penalty/`)

#### `grace-period-warning.html`
- **Purpose:** Sent when the vehicle is slightly past due but within grace period.
- **Variables:**
  - `${rentalId}`, `${carBrand}`, `${carModel}`, `${licensePlate}`, `${scheduledReturnTime}`, `${remainingGraceMinutes}`.

#### `late-return-notification.html`
- **Purpose:** Sent when penalty starts accumulating.
- **Variables:**
  - `${rentalId}`, `${carBrand}`, `${carModel}`, `${licensePlate}`, `${scheduledReturnTime}`, `${lateHours}`, `${currentPenaltyAmount}`, `${currency}`.

#### `severely-late-notification.html`
- **Purpose:** Final warning before legal action (e.g., 3 days late).
- **Variables:**
  - `${rentalId}`, `${carBrand}`, `${carModel}`, `${licensePlate}`, `${scheduledReturnTime}`, `${lateHours}`, `${lateDays}`, `${currentPenaltyAmount}`, `${currency}`, `${escalationWarning}`.

#### `penalty-summary.html`
- **Purpose:** Sent after the vehicle is returned, summarizing final late charges.
- **Variables:**
  - `${rentalId}`, `${carBrand}`, `${carModel}`, `${licensePlate}`, `${scheduledReturnTime}`, `${actualReturnTime}`, `${lateHours}`, `${lateDays}`, `${finalPenaltyAmount}`, `${currency}`, `${penaltyBreakdown}`, `${cappedAtMax}` (boolean).

---

### 5. Damage Management (`/damage/`)

#### `damage-reported.html`
- **Purpose:** Confirms receipt of a damage report.
- **Variables:**
  - `${rentalId}`, `${carLicensePlate}`, `${reportDate}`, `${description}`, `${severity}`.

#### `damage-assessed.html`
- **Purpose:** Sent after the maintenance team evaluates repair costs.
- **Variables:**
  - `${rentalId}`, `${carLicensePlate}`, `${severity}`, `${repairCost}`, `${customerLiability}`, `${currency}`, `${assessedAt}`, `${hasInsurance}` (boolean), `${deductible}`.

#### `damage-charged.html`
- **Purpose:** Receipt for a processed damage charge.
- **Variables:**
  - `${rentalId}`, `${transactionId}`, `${chargedAmount}`, `${currency}`, `${paymentDate}`.

#### `damage-disputed.html`
- **Purpose:** Confirms a customer has disputed an assessment.
- **Variables:**
  - `${rentalId}`, `${disputeReason}`, `${disputeDate}`.

#### `damage-resolved.html`
- **Purpose:** Sent after a dispute is reviewed and resolved.
- **Variables:**
  - `${rentalId}`, `${adjustedCharge}`, `${refundAmount}`, `${currency}`, `${resolutionNotes}`, `${resolvedAt}`.

#### `damage-charge-failed.html`
- **Purpose:** Alerts user that a damage payment failed.
- **Variables:**
  - `${rentalId}`, `${amount}`, `${currency}`, `${failureReason}`, `${failedAt}`.

---

## 🌙 Dark Mode Support

Templates use the following CSS strategy for dark mode:
1. `prefers-color-scheme: dark` media query.
2. Helper classes:
   - `.text-label`: Used for labels (muted in dark mode).
   - `.text-value`: Used for dynamic data (bright in dark mode).
   - `.text-muted`: Used for secondary text/footers (dimmed in dark mode).
   - `.detail-box`: Contextual backgrounds that switch between light gray and deep navy.

## 🛠️ Developer Tips

1. **Inline CSS:** Email clients have limited CSS support. Always use inline styles for core properties and use the `<style>` block only for responsive `@media` and dark mode queries.
2. **Variable Testing:** Before updating a template, verify the variables in `EmailTemplateService.java`.
3. **SVG Icons:** Always use inline SVGs with `stroke-width` or `fill` matching the context color. Avoid external image dependencies where possible.
