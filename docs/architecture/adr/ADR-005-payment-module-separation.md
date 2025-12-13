# ADR-005: Payment Module as Separate Infrastructure Module

## Status
**Accepted**

## Context

Initially, payment operations were inside the `rental` module:

```
rental/
├── internal/
│   ├── gateway/
│   │   ├── IPaymentGateway.java
│   │   └── StripePaymentGateway.java
│   └── webhook/
│       └── StripeWebhookHandler.java
```

**Problem:** The damage module also needed to process payments (damage charges). This would mean:
1. Damage → Rental dependency would form (just for payment)
2. Or IPaymentGateway would move to shared (anti-pattern)

## Decision

**Payment** was created as a separate infrastructure module.

## Rationale

### Alternatives Evaluated

| Approach | Pros | Cons |
|----------|------|------|
| **Payment in Rental** | Simple, existing structure | Damage module depends on rental |
| **Payment in Shared** | Everyone can access | Shared kernel bloats, business logic |
| **Separate Payment Module** | Clean isolation | New module overhead |

### Why Separate Module?

1. **Single Responsibility:** Payment = payment operations only
2. **Clear Dependencies:** `rental → payment`, `damage → payment`
3. **Gateway Abstraction:** Easy to swap Stripe for another provider
4. **Webhook Isolation:** Payment webhooks in one place
5. **Reconciliation:** Payment-specific scheduled jobs

### Final Architecture

```
payment/
├── api/
│   └── PaymentService.java          # Public interface
├── domain/
│   ├── WebhookEvent.java
│   └── enums/
│       ├── PaymentStatus.java
│       └── WebhookEventStatus.java
├── internal/
│   ├── gateway/
│   │   ├── IPaymentGateway.java      # Internal interface
│   │   └── StripePaymentGateway.java
│   ├── webhook/
│   │   └── StripeWebhookHandler.java
│   ├── reconciliation/
│   │   └── PaymentReconciliationService.java
│   ├── scheduler/
│   │   └── ReconciliationScheduler.java
│   ├── repository/
│   │   └── WebhookEventRepository.java
│   └── config/
│       └── StripeConfig.java
└── web/
    └── StripeWebhookController.java
```

### Public API

```java
public interface PaymentService {
    PaymentResult authorize(BigDecimal amount, CurrencyType currency, String customerId);
    PaymentResult capture(String transactionId, BigDecimal amount);
    PaymentResult refund(String transactionId, BigDecimal amount);
    CheckoutSessionResult createCheckoutSession(CheckoutSessionRequest request);
}
```

### Usage by Other Modules

```java
// Rental module
@Service
public class RentalServiceImpl {
    private final PaymentService paymentService;  // Inject public API
    
    public void confirmRental(Long rentalId) {
        PaymentResult result = paymentService.authorize(amount, currency, customerId);
        // ...
    }
}

// Damage module  
@Service
public class DamageChargeServiceImpl {
    private final PaymentService paymentService;  // Same public API
    
    public void chargeDamage(Long damageId) {
        PaymentResult result = paymentService.capture(transactionId, amount);
        // ...
    }
}
```

## Consequences

### Positive
- Payment logic in one place
- Multiple consumers (rental, damage)
- Gateway swap is easy
- Payment-specific testing

### Negative
- Extra module (management overhead)
- Module count increased (7 → 8)

### Future Considerations

- If subscription payments are added, payment module will expand
- Different gateways (PayPal, iyzico) can be added
- Payment analytics as separate dashboard

## Related ADRs
- ADR-001: Spring Modulith decision
- ADR-002: Cross-module entity strategy
