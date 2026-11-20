# ADR-011: Stripe manual-capture PaymentIntents

**Status:** Accepted


## Context

Rentals and damage assessments both need payment operations. Their modules own when to charge or refund, while the payment module owns provider calls, payment state, webhooks and refund records.

## Decision

Both consumers use `payment::api`. The customer authorizes a rental PaymentIntent and pickup captures an authorized payment. The payment module serializes refunds by locking the payment row and records refunds per operation. Cancellation before capture releases the hold when applicable. Unpaid approvals expire according to [ADR-014](ADR-014-payment-deadline-after-approval.md). [Payment API](../../../src/main/java/com/akif/payment/api/PaymentService.java) · [Refund service](../../../src/main/java/com/akif/payment/internal/service/PaymentRefundService.java)

Payment operations use deterministic idempotency keys scoped by a database installation ID. The damage charge key includes damage ID, payment ID and attempt. [Idempotency keys](../../../src/main/java/com/akif/payment/internal/service/gateway/IdempotencyKeyGenerator.java)

## Consequences

`dev` and `test` use a stub gateway. The Stripe integration runs only with `prod`, which accepts test-mode keys. Rental configuration uses a seven-day hold assumption for bounding the no-show period and rejects incompatible values at startup. [Startup validator](../../../src/main/java/com/akif/shared/config/ProdConfigurationValidator.java) · [Rental properties](../../../src/main/java/com/akif/rental/internal/config/RentalProperties.java) · [Webhook flow](../DIAGRAMS.md#stripe-webhook-processing)
