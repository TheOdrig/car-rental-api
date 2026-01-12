# Implementation Plan

- [x] 1. Project Setup and Dependencies






  - [x] 1.1 Add Stripe Java SDK dependency to pom.xml


    - Add `com.stripe:stripe-java` dependency
    - _Requirements: 6.1, 6.2_

  - [x] 1.2 Add Stripe configuration properties


    - Create `StripeConfig` class with API key, webhook secret, success/cancel URLs
    - Add properties to `application.properties` and `application-production.properties`
    - _Requirements: 6.1, 6.2_

- [x] 2. Data Model Updates






  - [x] 2.1 Update Payment entity with Stripe-specific fields


    - Add `stripeSessionId`, `stripePaymentIntentId`, `idempotencyKey`, `refundedAmount`, `failureReason` fields
    - _Requirements: 1.2, 4.4_

  - [x] 2.2 Create WebhookEvent entity and repository


    - Create `WebhookEvent` entity with `eventId`, `eventType`, `payload`, `status`, `processedAt`, `errorMessage`
    - Create `WebhookEventStatus` enum (RECEIVED, PROCESSING, PROCESSED, FAILED, DUPLICATE)
    - Create `WebhookEventRepository` with `findByEventId` method
    - _Requirements: 3.2, 3.3_

  - [x] 2.3 Create database migration for new tables and columns


    - Add Flyway migration `V5__add_stripe_payment_fields.sql`
    - _Requirements: 2.1, 2.2_

- [x] 3. Core Stripe Integration





  - [x] 3.1 Create IdempotencyKeyGenerator component


    - Implement deterministic key generation based on rental ID and timestamp
    - _Requirements: 3.1_

  - [x] 3.2 Create StripePaymentGateway service


    - Implement `IPaymentGateway` interface with `@Profile("prod")`
    - Implement `createCheckoutSession` method with metadata and redirect URLs
    - Implement `authorize`, `capture`, `refund`, `partialRefund` methods
    - Add retry logic with exponential backoff (3 attempts)
    - _Requirements: 1.1, 1.2, 1.3, 4.1, 4.2, 4.4, 6.3, 6.4_

- [x] 4. Exception Handling





  - [x] 4.1 Create Stripe-specific exception classes


    - Create `StripeIntegrationException` with Stripe error code and message
    - Create `WebhookSignatureException` with event ID
    - Create `ReconciliationException` with reconciliation date
    - _Requirements: 2.2, 4.3, 6.4_

  - [x] 4.2 Update GlobalExceptionHandler for Stripe exceptions


    - Add handlers for new exception types
    - Return appropriate HTTP status codes
    - _Requirements: 2.2, 4.3_

- [x] 5. Webhook Handling





  - [x] 5.1 Create StripeWebhookHandler service


    - Implement signature verification using Stripe SDK
    - Implement `processCheckoutSessionCompleted` → update Payment to CAPTURED
    - Implement `processCheckoutSessionExpired` → update Payment to FAILED
    - Implement `processPaymentIntentFailed` → update Payment to FAILED with reason
    - Implement duplicate event detection using WebhookEventRepository
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.2, 3.3_


  - [x] 5.2 Create StripeWebhookController

    - Create POST `/api/webhooks/stripe` endpoint
    - Extract signature from `Stripe-Signature` header
    - Return 400 for invalid signature, 200 for success/duplicate
    - _Requirements: 2.1, 2.2_

- [x] 6. Reconciliation Service







  - [x] 6.1 Create DTOs for reconciliation

    - Create `ReconciliationReport` record
    - Create `Discrepancy` record
    - Create `DiscrepancyType` enum
    - _Requirements: 5.5_

  - [x] 6.2 Create PaymentReconciliationService


    - Implement `fetchDatabasePayments(LocalDate date)` method
    - Implement `fetchStripePayments(LocalDate date)` method using Stripe API
    - Implement `comparePayments` method to identify discrepancies
    - Implement `runDailyReconciliation` method
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_


  - [x] 6.3 Create scheduled job for daily reconciliation

    - Add `@Scheduled` method to run reconciliation daily
    - Log reconciliation results
    - _Requirements: 5.1, 7.4_

- [x] 7. Audit Logging






  - [x] 7.1 Add audit logging to all payment operations

    - Log operation type, rental ID, amount, timestamp for all gateway operations
    - Log event type, event ID, processing result for webhooks
    - Log error details including Stripe error codes on failures
    - Log discrepancy details in reconciliation
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 8. Checkpoint - Implementation Complete



  - Ensure application compiles and runs, ask the user if questions arise.

- [x] 9. Unit Tests





  - [x] 9.1 Write unit tests for IdempotencyKeyGenerator


    - Test same inputs produce same key
    - Test different inputs produce different keys
    - _Requirements: 3.1_

  - [x] 9.2 Write unit tests for StripePaymentGateway


    - Test checkout session creation with correct metadata
    - Test refund processing (full and partial)
    - Test retry logic on network failures
    - _Requirements: 1.2, 1.3, 4.2, 4.4, 6.3_

  - [x] 9.3 Write unit tests for StripeWebhookHandler


    - Test signature verification (valid/invalid)
    - Test event processing for each event type
    - Test duplicate event detection
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.2, 3.3_

  - [x] 9.4 Write unit tests for PaymentReconciliationService


    - Test discrepancy detection (missing in Stripe, missing in DB, mismatched)
    - Test report generation
    - _Requirements: 5.2, 5.3, 5.4, 5.5_

- [x] 10. Integration Tests





  - [x] 10.1 Write integration tests for StripeWebhookController



    - Test endpoint with valid/invalid signatures
    - Test event processing flow
    - _Requirements: 2.1, 2.2, 2.3_

- [ ] 11. Final Checkpoint
  - Ensure all tests pass, ask the user if questions arise.
