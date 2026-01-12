# Git Workflow - Stripe Payment Gateway

## Branch Strategy

```
main
  └── feature/stripe-payment-gateway
```

## Commit Plan

### Task 1: Project Setup
```
feat(stripe): add Stripe SDK and configuration

- Add com.stripe:stripe-java dependency to pom.xml
- Create StripeConfig with API key, webhook secret, URLs
- Add Stripe properties to application.properties
- Add production properties to application-production.properties

Establishes Stripe integration foundation with proper configuration
management for test and production environments.
```

### Task 2: Data Models
```
feat(stripe): add Stripe-specific data models

- Update Payment entity with Stripe fields (sessionId, paymentIntentId,
  idempotencyKey, refundedAmount, failureReason)
- Add WebhookEvent entity for event tracking
- Add WebhookEventStatus enum (RECEIVED, PROCESSING, PROCESSED, FAILED, DUPLICATE)
- Create WebhookEventRepository with findByEventId
- Add Flyway migration V5__add_stripe_payment_fields.sql

Enables tracking of Stripe-specific payment data and webhook events
for idempotency and audit purposes.
```

### Task 3: Core Integration
```
feat(stripe): implement Stripe payment gateway

- Create IdempotencyKeyGenerator with deterministic key generation
- Implement StripePaymentGateway service with @Profile("prod")
- Implement createCheckoutSession with metadata and redirect URLs
- Implement authorize, capture, refund, partialRefund methods
- Add retry logic with exponential backoff (3 attempts)

Core Stripe integration implementing IPaymentGateway interface,
replacing stub gateway in production with real payment processing.
```

### Task 4: Exception Handling
```
feat(stripe): add Stripe exception handling

- Create StripeIntegrationException with error code and message
- Create WebhookSignatureException with event ID
- Create ReconciliationException with reconciliation date
- Update GlobalExceptionHandler for Stripe exceptions
- Map Stripe errors to appropriate HTTP status codes

Provides clear error handling for Stripe API failures, webhook
signature issues, and reconciliation problems.
```

### Task 5: Webhook Processing
```
feat(stripe): implement webhook event processing

- Create StripeWebhookHandler service
- Implement signature verification using Stripe SDK
- Process checkout.session.completed → Payment CAPTURED
- Process checkout.session.expired → Payment FAILED
- Process payment_intent.payment_failed → Payment FAILED with reason
- Implement duplicate event detection via WebhookEventRepository
- Create StripeWebhookController with POST /api/webhooks/stripe
- Extract and verify Stripe-Signature header

Enables asynchronous payment status updates via Stripe webhooks,
ensuring payment state accuracy regardless of user browser behavior.
```

### Task 6: Reconciliation
```
feat(stripe): add payment reconciliation service

- Create ReconciliationReport, Discrepancy, DiscrepancyType DTOs
- Implement PaymentReconciliationService
- Add fetchDatabasePayments and fetchStripePayments methods
- Implement comparePayments for discrepancy detection
- Add runDailyReconciliation method
- Create @Scheduled job for daily reconciliation

Provides automated daily reconciliation between database and Stripe,
identifying missing or mismatched payments for financial accuracy.
```

### Task 7: Audit Logging
```
feat(stripe): add comprehensive audit logging

- Log all payment operations (type, rental ID, amount, timestamp)
- Log webhook events (type, event ID, processing result)
- Log error details with Stripe error codes
- Log reconciliation discrepancies

Enables investigation of payment issues and maintains compliance
with audit requirements for financial transactions.
```

### Task 9-10: Tests
```
test(stripe): add comprehensive test coverage

- Add unit tests for IdempotencyKeyGenerator
- Add unit tests for StripePaymentGateway (checkout, refund, retry)
- Add unit tests for StripeWebhookHandler (signature, events, duplicates)
- Add unit tests for PaymentReconciliationService (discrepancies)
- Add integration tests for StripeWebhookController

Validates Stripe integration correctness across all scenarios
including error handling, idempotency, and webhook processing.
```

## Final Merge
```
git checkout main
git merge feature/stripe-payment-gateway
git push origin main
```

## Rollback Plan
```
# Revert specific commit
git revert <commit-hash>

# Or reset to previous state
git reset --hard <previous-commit>

# Emergency: disable Stripe in production
# Set spring.profiles.active=dev to use stub gateway
```

## Testing Strategy

### Local Testing
```bash
# Use Stripe test mode keys
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_test_...

# Test webhook locally with Stripe CLI
stripe listen --forward-to localhost:8080/api/webhooks/stripe
stripe trigger checkout.session.completed
```

### Production Deployment
```bash
# Set production keys via environment variables
STRIPE_API_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Configure webhook endpoint in Stripe Dashboard
# https://yourdomain.com/api/webhooks/stripe
```
