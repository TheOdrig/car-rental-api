# Critical Test Scenarios

## Table of Contents
1. [Rental Lifecycle](#1-rental-lifecycle)
2. [Payment Processing](#2-payment-processing)
3. [Authentication & Authorization](#3-authentication--authorization)
4. [Late Return & Penalties](#4-late-return--penalties)
5. [Damage Management](#5-damage-management)
6. [Edge Cases](#6-edge-cases)
7. [Error Handling](#7-error-handling)
8. [Coverage Gaps](#8-coverage-gaps)

---

## 1. Rental Lifecycle
The rental lifecycle is the core business flow: Request → Confirm → Pickup → Return (or Cancel).

### Happy Path Scenarios
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 1.1 | User requests rental | `RentalServiceTest#shouldSuccessfullyCreateRentalRequest` |
| 1.2 | Admin confirms rental with payment authorization | `RentalServiceTest#shouldSuccessfullyConfirmRentalAndAuthorizePayment` |
| 1.3 | Admin processes pickup with payment capture | `RentalServiceTest#shouldSuccessfullyProcessPickupAndCapturePayment` |
| 1.4 | Admin processes return | `RentalServiceTest#shouldSuccessfullyProcessReturn` |
| 1.5 | User cancels rental (no refund for REQUESTED status) | `RentalServiceTest#shouldSuccessfullyCancelRequestedRentalWithoutRefund` |
| 1.6 | Full lifecycle E2E test | `RentalLifecycleE2ETest` |

### Edge Cases
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 1.7 | Date overlap detection | `RentalServiceTest#shouldThrowExceptionWhenDateOverlapExists` |
| 1.8 | Past date validation | `RentalServiceTest#shouldThrowExceptionWhenStartDateIsInPast` |
| 1.9 | Car not available | `RentalServiceTest#shouldThrowExceptionWhenCarNotAvailable` |
| 1.10 | Concurrent booking prevention | `ConcurrencyE2ETest` |
| 1.11 | Date overlap E2E | `DateOverlapE2ETest` |
| 1.12 | Cancellation with refund | `CancellationRefundE2ETest` |

### Error Handling
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 1.13 | User tries to cancel another user's rental | `RentalServiceTest#shouldThrowExceptionWhenUserTriesToCancelOtherUsersRental` |
| 1.14 | Payment authorization fails during confirm | `RentalServiceTest#shouldThrowExceptionWhenPaymentAuthorizationFails` |

---

## 2. Payment Processing
Payment processing uses Stripe for authorization, capture, and refund operations.

### Happy Path Scenarios
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 2.1 | Payment authorization | `StripePaymentGatewayTest#shouldReturnSuccessResultForAuthorize` |
| 2.2 | Payment capture | `StripePaymentGatewayTest#shouldReturnSuccessResultForCapture` |
| 2.3 | Payment refund | `StripePaymentGatewayTest#shouldHandleRefundMetadataCorrectly` |
| 2.4 | Full payment gateway E2E | `PaymentGatewayE2ETest` |

### Webhook Handling
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 2.5 | Stripe webhook signature verification | `StripeWebhookHandlerTest` |
| 2.6 | Webhook controller integration | `StripeWebhookControllerIntegrationTest` |

### Edge Cases
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 2.7 | Idempotency key generation | `StripePaymentGatewayTest#shouldGenerateIdempotencyKeyWhenCreatingCheckoutSession` |
| 2.8 | Config URL validation | `StripePaymentGatewayTest#shouldUseCorrectUrlsFromConfig` |
| 2.9 | Payment reconciliation | `PaymentReconciliationServiceTest` |

---

## 3. Authentication & Authorization
Authentication supports JWT login and OAuth2 providers (Google, GitHub).

### Happy Path Scenarios
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 3.1 | User registration | `AuthServiceTest#shouldRegisterUserSuccessfully` |
| 3.2 | User login | `AuthServiceTest#shouldLoginUserSuccessfully` |
| 3.3 | Token refresh | `AuthServiceTest#shouldRefreshTokenSuccessfully` |
| 3.4 | Authorization E2E | `AuthorizationE2ETest` |

### Error Handling
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 3.5 | Username already exists | `AuthServiceTest#shouldThrowExceptionWhenUsernameAlreadyExists` |
| 3.6 | Email already exists | `AuthServiceTest#shouldThrowExceptionWhenEmailAlreadyExists` |
| 3.7 | Invalid credentials | `AuthServiceTest#shouldThrowExceptionForInvalidCredentials` |
| 3.8 | Invalid refresh token | `AuthServiceTest#shouldThrowExceptionForInvalidRefreshToken` |
| 3.9 | Expired refresh token | `AuthServiceTest#shouldThrowExceptionForExpiredRefreshToken` |
| 3.10 | User not found during refresh | `AuthServiceTest#shouldThrowExceptionWhenUserNotFoundDuringRefresh` |

### Edge Cases
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 3.11 | Null register request | `AuthServiceTest#shouldHandleNullRegisterRequest` |
| 3.12 | Null login request | `AuthServiceTest#shouldHandleNullLoginRequest` |
| 3.13 | OAuth2 error handling | `OAuth2ErrorHandlingTest` |
| 3.14 | OAuth2 controller integration | `OAuth2ControllerIntegrationTest` |

---

## 4. Late Return & Penalties
Late return detection and penalty calculation are scheduled tasks.

### Happy Path Scenarios
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 4.1 | Late return detection | `LateReturnDetectionServiceImplTest` |
| 4.2 | Penalty calculation | `PenaltyCalculationServiceImplTest` |
| 4.3 | Penalty payment | `PenaltyPaymentServiceImplTest` |
| 4.4 | Penalty waiver | `PenaltyWaiverServiceImplTest` |
| 4.5 | Late return scheduler | `LateReturnSchedulerTest` |
| 4.6 | Late return penalty E2E | `LateReturnPenaltyE2ETest` |

### Integration Tests
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 4.7 | Late return controller | `LateReturnControllerIntegrationTest` |
| 4.8 | Penalty waiver controller | `PenaltyWaiverControllerIntegrationTest` |
| 4.9 | Late return report | `LateReturnReportServiceImplTest` |

---

## 5. Damage Management
Damage reporting, assessment, and disputes are handled in the damage module.

### Test Coverage
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 5.1 | Damage reporting | `damage/unit/` tests |
| 5.2 | Damage assessment | `damage/integration/` tests |
| 5.3 | Damage disputes | `damage/e2e/` tests |

> **Note:** Specific test file references to be added after detailed review of damage module.

---

## 6. Edge Cases
Cross-cutting edge cases that span multiple modules.

### Documented Edge Cases
| # | Scenario | Test Reference |
| :--- | :--- | :--- |
| 6.1 | Concurrent booking attempts | `ConcurrencyE2ETest` |
| 6.2 | Date overlap prevention | `DateOverlapE2ETest` |
| 6.3 | General error handling | `ErrorHandlingE2ETest` |

---

## 7. Error Handling
Common error scenarios and their test coverage.

### HTTP Error Responses
| # | Error | Scenario | Test Reference |
| :--- | :--- | :--- | :--- |
| 7.1 | 400 Bad Request | Invalid input validation | `ErrorHandlingE2ETest` |
| 7.2 | 401 Unauthorized | Missing/invalid JWT token | `AuthControllerIntegrationTest` |
| 7.3 | 403 Forbidden | Role-based access denied | `RentalControllerAuthorizationTest` |
| 7.4 | 404 Not Found | Resource not found | `RentalControllerIntegrationTest` |
| 7.5 | 409 Conflict | Date overlap, duplicate registration | Various tests |

---

## 8. Coverage Gaps
Identified scenarios that may need additional test coverage.

| Gap | Priority | Notes |
| :--- | :--- | :--- |
| Damage module detailed scenarios | Medium | Need to add specific test references |
| Currency conversion edge cases | Low | Exchange rate failures |
| Notification delivery failures | Low | Email sending failures |
| Reminder scheduler edge cases | Medium | Scheduler timing issues |

### Priority Actions
- [ ] Review damage module tests and add specific references
- [ ] Add currency conversion failure tests
- [ ] Add notification retry logic tests

---

**Last Updated:** January 2026
