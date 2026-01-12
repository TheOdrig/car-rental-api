# Requirements Document

## Introduction

Bu doküman, Rent-a-Car uygulamasının End-to-End (E2E) test gereksinimlerini tanımlar. E2E testler, Tier 1'de tamamlanan tüm özelliklerin (Rental, Currency Conversion, Dynamic Pricing, OAuth2, Payment Gateway, Email Notification) birlikte çalışmasını doğrular. Payment testleri için StubPaymentGateway kullanılır (Stripe API key gerektirmez). Mevcut integration testlerden farklı olarak, E2E testler gerçek kullanıcı senaryolarını simüle eder ve tüm sistem bileşenlerinin entegrasyonunu test eder.

## Glossary

- **E2E_Test_Suite**: Tüm sistem bileşenlerinin birlikte çalışmasını doğrulayan test koleksiyonu
- **Full_Rental_Flow**: Kullanıcı kaydından araç iadesine kadar tüm kiralama sürecini kapsayan akış
- **Payment_Flow**: StubPaymentGateway üzerinden authorize, capture ve refund işlemlerinin akışı
- **Email_Verification**: Email event'lerinin doğru şekilde publish edildiğini ve işlendiğini doğrulama
- **Price_Calculation_Flow**: Dynamic pricing stratejileri ve currency conversion'ın birlikte çalışması
- **Test_Container**: Testler için izole edilmiş veritabanı ve servis ortamı
- **Mock_External_Services**: SendGrid, ExchangeRate-API gibi dış servislerin mock'lanması (Payment için StubPaymentGateway kullanılır)
- **Scenario_Test**: Gerçek kullanıcı senaryolarını simüle eden uçtan uca test

## Requirements

### Requirement 1: Complete Rental Lifecycle E2E Test

**User Story:** As a QA engineer, I want to verify the complete rental lifecycle works correctly, so that I can ensure all system components integrate properly from user registration to car return.

#### Acceptance Criteria

1. WHEN a user registers and requests a rental THEN the E2E_Test_Suite SHALL verify the rental is created with REQUESTED status and correct pricing
2. WHEN an admin confirms the rental THEN the E2E_Test_Suite SHALL verify payment authorization occurs and rental status changes to CONFIRMED
3. WHEN an admin processes pickup THEN the E2E_Test_Suite SHALL verify payment capture occurs and rental status changes to IN_USE
4. WHEN an admin processes return THEN the E2E_Test_Suite SHALL verify rental status changes to RETURNED and car becomes available
5. WHEN the rental lifecycle completes THEN the E2E_Test_Suite SHALL verify all email events were published for confirmation, payment receipt, and return

### Requirement 2: Cancellation and Refund E2E Test

**User Story:** As a QA engineer, I want to verify the cancellation and refund flow works correctly, so that I can ensure customers receive refunds when cancelling confirmed rentals.

#### Acceptance Criteria

1. WHEN a user cancels a REQUESTED rental THEN the E2E_Test_Suite SHALL verify no payment operations occur and rental status changes to CANCELLED
2. WHEN a user cancels a CONFIRMED rental THEN the E2E_Test_Suite SHALL verify refund is processed and rental status changes to CANCELLED
3. WHEN a user cancels an IN_USE rental THEN the E2E_Test_Suite SHALL verify partial refund is processed based on remaining days
4. WHEN a cancellation occurs THEN the E2E_Test_Suite SHALL verify cancellation email event is published with correct refund information

### Requirement 3: Dynamic Pricing Integration E2E Test

**User Story:** As a QA engineer, I want to verify dynamic pricing strategies are correctly applied during rental creation, so that I can ensure customers see accurate prices.

#### Acceptance Criteria

1. WHEN a rental is requested with early booking (30+ days advance) THEN the E2E_Test_Suite SHALL verify 15% early booking discount is applied
2. WHEN a rental is requested for 14+ days duration THEN the E2E_Test_Suite SHALL verify duration discount is applied
3. WHEN a rental includes weekend days THEN the E2E_Test_Suite SHALL verify weekend surcharge is applied
4. WHEN multiple pricing strategies apply THEN the E2E_Test_Suite SHALL verify all modifiers are combined correctly in the final price
5. WHEN a rental is created THEN the E2E_Test_Suite SHALL verify price breakdown includes all applied modifiers

### Requirement 4: Currency Conversion Integration E2E Test

**User Story:** As a QA engineer, I want to verify currency conversion works correctly across all rental operations, so that I can ensure international customers see accurate prices.

#### Acceptance Criteria

1. WHEN a user requests rental prices in USD THEN the E2E_Test_Suite SHALL verify prices are converted from TRY to USD using current exchange rates
2. WHEN a user views rental details with currency parameter THEN the E2E_Test_Suite SHALL verify both original and converted prices are returned
3. WHEN exchange rate API is unavailable THEN the E2E_Test_Suite SHALL verify fallback rates are used and response includes warning flag
4. WHEN a rental is created THEN the E2E_Test_Suite SHALL verify total price can be displayed in multiple currencies

### Requirement 5: Payment Gateway E2E Test

**User Story:** As a QA engineer, I want to verify payment gateway operations work correctly using StubPaymentGateway, so that I can ensure payment flows are properly integrated without external dependencies.

#### Acceptance Criteria

1. WHEN a rental is confirmed THEN the E2E_Test_Suite SHALL verify StubPaymentGateway.authorize is called and payment status changes to AUTHORIZED
2. WHEN a rental pickup is processed THEN the E2E_Test_Suite SHALL verify StubPaymentGateway.capture is called and payment status changes to CAPTURED
3. WHEN a confirmed rental is cancelled THEN the E2E_Test_Suite SHALL verify StubPaymentGateway.refund is called and payment status changes to REFUNDED
4. WHEN payment authorization fails THEN the E2E_Test_Suite SHALL verify rental remains in REQUESTED state and appropriate error is returned

### Requirement 6: Email Notification E2E Test

**User Story:** As a QA engineer, I want to verify email notifications are sent at correct lifecycle points, so that I can ensure customers receive timely communications.

#### Acceptance Criteria

1. WHEN a rental is confirmed THEN the E2E_Test_Suite SHALL verify RentalConfirmedEvent is published with correct rental details
2. WHEN payment is captured THEN the E2E_Test_Suite SHALL verify PaymentCapturedEvent is published with correct payment details
3. WHEN a rental is cancelled THEN the E2E_Test_Suite SHALL verify RentalCancelledEvent is published with refund information
4. WHEN email sending fails THEN the E2E_Test_Suite SHALL verify retry mechanism is triggered

### Requirement 7: OAuth2 Authentication E2E Test

**User Story:** As a QA engineer, I want to verify OAuth2 social login integrates correctly with the rental system, so that I can ensure social login users can complete rentals.

#### Acceptance Criteria

1. WHEN a user authenticates via OAuth2 THEN the E2E_Test_Suite SHALL verify JWT tokens are issued and user can access protected endpoints
2. WHEN a social login user requests a rental THEN the E2E_Test_Suite SHALL verify the rental is associated with the correct user
3. WHEN a social login user's token expires THEN the E2E_Test_Suite SHALL verify refresh token flow works correctly

### Requirement 8: Date Overlap and Availability E2E Test

**User Story:** As a QA engineer, I want to verify date overlap prevention works correctly, so that I can ensure cars are not double-booked.

#### Acceptance Criteria

1. WHEN a confirmed rental exists for specific dates THEN the E2E_Test_Suite SHALL verify new rental requests for overlapping dates are rejected
2. WHEN a rental is cancelled THEN the E2E_Test_Suite SHALL verify the dates become available for new rentals
3. WHEN multiple REQUESTED rentals exist for same dates THEN the E2E_Test_Suite SHALL verify only one can be confirmed

### Requirement 9: Authorization and Security E2E Test

**User Story:** As a QA engineer, I want to verify authorization rules are enforced correctly, so that I can ensure users can only access permitted operations.

#### Acceptance Criteria

1. WHEN a USER role attempts admin operations (confirm, pickup, return) THEN the E2E_Test_Suite SHALL verify HTTP 403 is returned
2. WHEN an unauthenticated user attempts protected operations THEN the E2E_Test_Suite SHALL verify HTTP 401 or 403 is returned
3. WHEN a user attempts to cancel another user's rental THEN the E2E_Test_Suite SHALL verify HTTP 403 is returned
4. WHEN a user attempts to view another user's rental details THEN the E2E_Test_Suite SHALL verify HTTP 403 is returned

### Requirement 10: Error Handling and Edge Cases E2E Test

**User Story:** As a QA engineer, I want to verify error handling works correctly across all flows, so that I can ensure the system fails gracefully.

#### Acceptance Criteria

1. WHEN a rental is requested for a non-existent car THEN the E2E_Test_Suite SHALL verify HTTP 404 is returned with descriptive error message
2. WHEN a rental is requested for an unavailable car (SOLD, RESERVED) THEN the E2E_Test_Suite SHALL verify HTTP 400 is returned
3. WHEN invalid state transitions are attempted THEN the E2E_Test_Suite SHALL verify appropriate error responses are returned
4. WHEN payment fails during confirmation THEN the E2E_Test_Suite SHALL verify rental remains in REQUESTED state

### Requirement 11: Performance and Concurrency E2E Test

**User Story:** As a QA engineer, I want to verify the system handles concurrent operations correctly, so that I can ensure data integrity under load.

#### Acceptance Criteria

1. WHEN multiple users request the same car for overlapping dates simultaneously THEN the E2E_Test_Suite SHALL verify only one rental can be confirmed
2. WHEN multiple confirm requests arrive for the same rental simultaneously THEN the E2E_Test_Suite SHALL verify idempotency prevents duplicate payment authorization
3. WHEN cache expires during high traffic THEN the E2E_Test_Suite SHALL verify exchange rate refresh does not cause errors

