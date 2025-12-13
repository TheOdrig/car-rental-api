# ADR-003: Event-Driven Inter-Module Communication

## Status
**Accepted**

## Context

An inter-module communication mechanism needed to be chosen. Specifically:

- Rental → Notification (email sending)
- Damage → Notification (email sending)
- Payment → Notification (receipt)

**Problem:** If `RentalService` directly calls `EmailNotificationService`:
1. Circular dependency risk
2. Tight coupling
3. Testing difficulty

## Decision

**Spring Application Events** for event-driven communication.

## Rationale

### Alternatives Evaluated

| Approach | Pros | Cons |
|----------|------|------|
| **Direct Service Call** | Simple, sync | Tight coupling, circular dep |
| **Interface Injection** | Loose coupling | Still sync, exception handling |
| **Spring Events** | Async, decoupled | Eventual consistency |
| **Message Queue** | Full isolation | Infrastructure overhead |

### Why Spring Events?

1. **Built-in:** Ready in Spring Framework
2. **Async Support:** Non-blocking with `@Async`
3. **Testable:** Testable with event capture
4. **No Infrastructure:** No RabbitMQ/Kafka required
5. **Type-Safe:** Event classes provide compile-time checks

### Implementation Pattern

**Publisher (Rental Module):**
```java
@Service
public class RentalServiceImpl {
    private final ApplicationEventPublisher eventPublisher;
    
    public void confirmRental(Long rentalId) {
        // Business logic...
        
        // Publish event
        eventPublisher.publishEvent(new RentalConfirmedEvent(
            rental.getId(),
            rental.getUserEmail(),
            rental.getCarBrand(),
            rental.getStartDate(),
            rental.getTotalPrice()
        ));
    }
}
```

**Listener (Notification Module):**
```java
@Component
public class EmailEventListener {
    private final EmailNotificationService emailService;
    
    @EventListener
    @Async
    public void handleRentalConfirmed(RentalConfirmedEvent event) {
        emailService.sendRentalConfirmation(event);
    }
}
```

### Event Design Rules

1. **No Entity References:**
   ```java
   // ❌ Wrong
   record RentalConfirmedEvent(Rental rental) {}
   
   // ✅ Correct
   record RentalConfirmedEvent(
       Long rentalId,
       String userEmail,
       String carBrand,
       LocalDate startDate,
       BigDecimal totalPrice
   ) {}
   ```

2. **Immutable (Records):** Use Java records

3. **Self-Contained:** Event carries all data the listener needs

4. **Public API:** Events are in module's public package

### Event Flow

```
┌─────────────┐    Event    ┌──────────────────┐
│   Rental    │ ─────────▶  │   Notification   │
│   Module    │             │   Module         │
└─────────────┘             └──────────────────┘
      │                            │
      ▼                            ▼
RentalConfirmedEvent    EmailEventListener
      │                            │
      └────────────────────────────┘
            ApplicationEventPublisher
```

## Consequences

### Positive
- Modules loosely coupled
- Async processing (faster UI)
- Easy testing (mock event publisher)
- Extendable (add new listener, publisher unchanged)

### Negative
- Eventual consistency (email delayed)
- Debugging difficulty (event flow tracking)
- Error handling (async exceptions)

### Error Handling Strategy

```java
@EventListener
@Async
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
public void handleRentalConfirmed(RentalConfirmedEvent event) {
    // Retry with exponential backoff
}
```

## Related ADRs
- ADR-001: Spring Modulith decision
- ADR-002: Cross-module entity strategy
