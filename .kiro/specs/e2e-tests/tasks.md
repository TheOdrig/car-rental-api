# Implementation Plan

- [x] 1. Set up E2E test infrastructure









  - [x] 1.1 Create TestEventCaptor for capturing domain events
    - Create `src/test/java/com/akif/e2e/infrastructure/TestEventCaptor.java`
    - Implement @EventListener to capture all domain events
    - Add methods: `getEventsOfType()`, `clear()`, `getAllEvents()`
    - _Requirements: 1.5, 6.1, 6.2, 6.3_
  - [x] 1.2 Create E2ETestBase abstract class
    - Create `src/test/java/com/akif/e2e/infrastructure/E2ETestBase.java`
    - Add common test setup: MockMvc, ObjectMapper, JwtTokenProvider
    - Add helper methods: `generateUserToken()`, `generateAdminToken()`, `createAndGetRentalId()`
    - Mock ReminderScheduler with @MockitoBean
    - _Requirements: 1.1, 1.2, 7.1_
  - [x] 1.3 Create TestDataBuilder utility
    - Create `src/test/java/com/akif/e2e/infrastructure/TestDataBuilder.java`
    - Add methods: `createTestUser()`, `createAvailableCar()`, `createRentalRequest()`
    - Add TestFixtures constants (BASE_PRICE, DEFAULT_CURRENCY, dates)
    - _Requirements: 1.1, 3.1, 3.2_

- [x] 2. Implement Complete Rental Lifecycle E2E Tests




  - [x] 2.1 Create RentalLifecycleE2ETest class


    - Create `src/test/java/com/akif/e2e/rental/RentalLifecycleE2ETest.java`
    - Extend E2ETestBase
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - [x] 2.2 Implement full rental lifecycle test

    - Test: request → confirm → pickup → return flow
    - Verify status transitions: REQUESTED → CONFIRMED → IN_USE → RETURNED
    - Verify payment states: AUTHORIZED → CAPTURED
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - [x] 2.3 Implement email event verification test

    - Verify RentalConfirmedEvent is published on confirm
    - Verify PaymentCapturedEvent is published on pickup
    - Use TestEventCaptor to capture events
    - _Requirements: 1.5, 6.1, 6.2_

- [x] 3. Implement Cancellation and Refund E2E Tests




  - [x] 3.1 Create CancellationRefundE2ETest class


    - Create `src/test/java/com/akif/e2e/rental/CancellationRefundE2ETest.java`
    - Extend E2ETestBase
    - _Requirements: 2.1, 2.2, 2.3_
  - [x] 3.2 Implement REQUESTED rental cancellation test


    - Test: request → cancel flow
    - Verify no payment operations occur
    - Verify status changes to CANCELLED
    - _Requirements: 2.1_
  - [x] 3.3 Implement CONFIRMED rental cancellation with refund test


    - Test: request → confirm → cancel flow
    - Verify refund is processed
    - Verify RentalCancelledEvent is published
    - _Requirements: 2.2, 2.4_
  - [x] 3.4 Implement IN_USE rental cancellation test


    - Test: request → confirm → pickup → cancel flow
    - Verify partial refund based on remaining days
    - _Requirements: 2.3_

- [x] 4. Implement Dynamic Pricing Integration E2E Tests



  - [x] 4.1 Create PricingIntegrationE2ETest class


    - Create `src/test/java/com/akif/e2e/pricing/PricingIntegrationE2ETest.java`
    - Extend E2ETestBase
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  - [x] 4.2 Implement early booking discount test

    - Test rental request 30+ days in advance
    - Verify 15% early booking discount is applied
    - _Requirements: 3.1_
  - [x] 4.3 Implement duration discount test

    - Test rental request for 14+ days
    - Verify duration discount is applied
    - _Requirements: 3.2_
  - [x] 4.4 Implement weekend surcharge test

    - Test rental including weekend days
    - Verify weekend surcharge is applied
    - _Requirements: 3.3_
  - [x] 4.5 Implement price breakdown verification test

    - Verify all applied modifiers are listed in response
    - _Requirements: 3.5_

- [x] 5. Implement Currency Conversion E2E Tests




  - [x] 5.1 Create CurrencyConversionE2ETest class


    - Create `src/test/java/com/akif/e2e/pricing/CurrencyConversionE2ETest.java`
    - Extend E2ETestBase
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  - [x] 5.2 Implement USD conversion test

    - Test rental prices with currency=USD parameter
    - Verify prices are converted from TRY to USD
    - _Requirements: 4.1_
  - [x] 5.3 Implement dual price display test

    - Verify both original and converted prices are returned
    - _Requirements: 4.2_
  - [x] 5.4 Implement fallback rates test

    - Mock exchange rate API failure
    - Verify fallback rates are used
    - Verify warning flag in response
    - _Requirements: 4.3_

- [x] 6. Implement Payment Gateway E2E Tests





  - [x] 6.1 Create PaymentGatewayE2ETest class


    - Create `src/test/java/com/akif/e2e/payment/PaymentGatewayE2ETest.java`
    - Extend E2ETestBase
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  - [x] 6.2 Implement payment authorization test

    - Verify StubPaymentGateway.authorize is called on confirm
    - Verify payment status changes to AUTHORIZED
    - _Requirements: 5.1_
  - [x] 6.3 Implement payment capture test

    - Verify StubPaymentGateway.capture is called on pickup
    - Verify payment status changes to CAPTURED
    - _Requirements: 5.2_
  - [x] 6.4 Implement refund test

    - Verify StubPaymentGateway.refund is called on cancel
    - Verify payment status changes to REFUNDED
    - _Requirements: 5.3_
  - [x] 6.5 Implement payment failure handling test

    - Mock payment authorization failure
    - Verify rental remains in REQUESTED state
    - _Requirements: 5.4_

- [x] 7. Checkpoint - Make sure all tests are passing
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement Authorization and Security E2E Tests







  - [x] 8.1 Create AuthorizationE2ETest class

    - Create `src/test/java/com/akif/e2e/security/AuthorizationE2ETest.java`
    - Extend E2ETestBase
    - _Requirements: 9.1, 9.2, 9.3, 9.4_
  - [x] 8.2 Implement USER role admin operation test

    - Test USER attempting confirm, pickup, return
    - Verify HTTP 403 is returned
    - _Requirements: 9.1_


  - [x] 8.3 Implement unauthenticated access test

    - Test protected endpoints without token
    - Verify HTTP 401/403 is returned


    - _Requirements: 9.2_

  - [x] 8.4 Implement cross-user cancellation test

    - Test user attempting to cancel another user's rental


    - Verify HTTP 403 is returned


    - _Requirements: 9.3_
  - [x] 8.5 Implement cross-user view test

    - Test user attempting to view another user's rental
    - Verify HTTP 403 is returned
    - _Requirements: 9.4_

- [x] 9. Implement Date Overlap and Availability E2E Tests




  - [x] 9.1 Create DateOverlapE2ETest class


    - Create `src/test/java/com/akif/e2e/edge-cases/DateOverlapE2ETest.java`
    - Extend E2ETestBase
    - _Requirements: 8.1, 8.2, 8.3_
  - [x] 9.2 Implement overlapping dates rejection test


    - Create confirmed rental for specific dates
    - Attempt new rental for overlapping dates
    - Verify HTTP 400 is returned
    - _Requirements: 8.1_
  - [x] 9.3 Implement availability after cancellation test


    - Create and cancel rental
    - Verify same dates become available
    - _Requirements: 8.2_
  - [x] 9.4 Implement multiple REQUESTED confirmation test




    - Create multiple REQUESTED rentals for same dates
    - Verify only one can be confirmed
    - _Requirements: 8.3_

- [x] 10. Implement Error Handling E2E Tests




  - [x] 10.1 Create ErrorHandlingE2ETest class

    - Create `src/test/java/com/akif/e2e/edge-cases/ErrorHandlingE2ETest.java`
    - Extend E2ETestBase
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 10.2 Implement non-existent car test
    - Request rental for non-existent car ID
    - Verify HTTP 404 with descriptive error message
    - _Requirements: 10.1_

  - [x] 10.3 Implement unavailable car test
    - Request rental for SOLD car
    - Verify HTTP 400 is returned

    - _Requirements: 10.2_
  - [x] 10.4 Implement invalid state transition test
    - Attempt invalid transitions (e.g., REQUESTED → IN_USE)

    - Verify appropriate error responses
    - _Requirements: 10.3_
  - [x] 10.5 Implement payment failure during confirmation test

    - Mock payment failure
    - Verify rental remains in REQUESTED state
    - _Requirements: 10.4_

- [x] 11. Implement Concurrency E2E Tests






  - [x] 11.1 Create ConcurrencyE2ETest class

    - Create `src/test/java/com/akif/e2e/edge-cases/ConcurrencyE2ETest.java`
    - Extend E2ETestBase
    - _Requirements: 11.1, 11.2, 11.3_

  - [x] 11.2 Implement concurrent rental confirmation test

    - Create multiple REQUESTED rentals for same car/dates
    - Attempt concurrent confirmations
    - Verify only one succeeds
    - _Requirements: 11.1_


  - [x] 11.3 Implement idempotent confirmation test
    - Send multiple confirm requests for same rental
    - Verify idempotency prevents duplicate payment
    - _Requirements: 11.2_

- [x] 12. Final Checkpoint - Make sure all tests are passing



  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Update README with E2E Test Documentation




  - Add E2E Testing section to README.md
  - Document test categories (Rental Lifecycle, Pricing, Currency, Payment, Security, Edge Cases, Concurrency)
  - Document test infrastructure (E2ETestBase, TestDataBuilder, TestEventCaptor, TestFixtures)
  - Add instructions for running E2E tests
  - Document test coverage and what scenarios are tested
  - _Requirements: All requirements (comprehensive documentation)_

