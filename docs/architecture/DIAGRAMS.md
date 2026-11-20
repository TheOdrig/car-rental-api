# Architecture diagrams

These flows describe the API's local portfolio scope. The Stripe integration uses test mode and the development and test profiles use a stub gateway. [Developer guide](DEVELOPER_GUIDE.md) · [Payment design](adr/ADR-011-stripe-manual-capture-payment-intents.md)

## Rental lifecycle

```mermaid
stateDiagram-v2
    [*] --> REQUESTED: customer requests a rental
    REQUESTED --> CONFIRMED: admin approves
    REQUESTED --> CANCELLED: cancellation or request expiry
    CONFIRMED --> CANCELLED: cancellation, unpaid expiry or no-show
    CONFIRMED --> IN_USE: admin records pickup
    IN_USE --> RETURNED: admin records return or early return
```

An unpaid approval becomes eligible for expiry at the later of midnight UTC at the start of the start date and approval time plus the grace period. The job checks pending or failed payment attempts before cancelling. An authorized rental can be released by the no-show sweep. [Payment deadline](adr/ADR-014-payment-deadline-after-approval.md)

Pickup requires a confirmed rental and an authorized or captured payment. The date must be on or after the start date and on or before both the end date and the last day before the no-show cutoff. Pickup is refused while another rental has the same car in use. [Rental lifecycle service](../../src/main/java/com/akif/rental/internal/service/RentalLifecycleService.java)

An early return compares the used-period price with the full-period price under current pricing, scales the result to the original rental total and refunds the eligible difference. The service uses a prorated amount if repricing is unavailable under the conditions described in the [pricing decision](adr/ADR-006-dynamic-pricing-strategy.md).

## Payment states

The diagram shows the normal rental payment path, retry, cancellation and refund. Provider events can also reconcile a payment that missed an earlier update. [Payment entity](../../src/main/java/com/akif/payment/domain/Payment.java) · [Payment state service](../../src/main/java/com/akif/payment/internal/service/PaymentStateService.java)

```mermaid
stateDiagram-v2
    [*] --> PENDING: rental approval
    PENDING --> AUTHORIZED: card hold authorized
    PENDING --> FAILED: payment fails
    FAILED --> PENDING: retry
    PENDING --> CANCELLED: cancellation
    AUTHORIZED --> CANCELLED: release hold
    FAILED --> CANCELLED: cancellation
    AUTHORIZED --> CAPTURED: capture at pickup
    CAPTURED --> REFUNDED: total refunds reach captured amount
    REFUNDED --> CAPTURED: a failed refund reduces refunded total
```

## Rental payment and pickup

```mermaid
sequenceDiagram
    participant Customer
    participant Admin
    participant API
    participant Stripe
    Customer->>API: POST /api/v1/rentals/request
    API-->>Customer: 201 REQUESTED
    Admin->>API: Approve rental
    API->>API: Create PENDING payment and publish RentalConfirmedEvent
    API-->>Admin: CONFIRMED
    Customer->>API: Request payment intent within the payment window
    API->>Stripe: Create or reuse manual-capture intent
    API-->>Customer: Payment intent details
    Customer->>Stripe: Confirm payment method
    Stripe->>API: amount_capturable_updated webhook
    API->>API: Mark payment AUTHORIZED
    Admin->>API: Record pickup within the allowed dates
    API->>Stripe: Capture if payment is AUTHORIZED
    API-->>Admin: IN_USE
```

The notification module handles `RentalConfirmedEvent`. A captured payment can be partially refunded and keep its `CAPTURED` status. A full refund moves it to `REFUNDED`. [E-mail listener](../../src/main/java/com/akif/notification/internal/listener/EmailEventListener.java) · [Refund service](../../src/main/java/com/akif/payment/internal/service/PaymentRefundService.java)

## Stripe webhook processing

```mermaid
sequenceDiagram
    participant Stripe
    participant API
    participant DB as PostgreSQL
    Stripe->>API: POST /api/v1/webhooks/stripe
    API->>API: Verify signature and timestamp
    alt missing or invalid signature
        API-->>Stripe: 400
    else verified event
        API->>DB: Insert event if absent and lock stored row
        alt already processed or marked duplicate
            API-->>Stripe: 200
        else valid event
            API->>DB: Apply relevant change and record completion
            API-->>Stripe: 200
        else rejected event data
            API->>DB: Record failure
            API-->>Stripe: 200
        else unexpected processing exception
            API->>DB: Record failure
            API-->>Stripe: Error response
        end
    end
```

The handler records validation failures and acknowledges them. Unexpected processing failures reach the HTTP error path, allowing redelivery. A redelivered failed event can be processed again under the event lock. [Webhook handler](../../src/main/java/com/akif/payment/internal/webhook/StripeWebhookHandler.java) · [Webhook security](../security/AUTHENTICATION.md#stripe-webhook-security)
