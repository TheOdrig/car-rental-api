# Git Workflow - E2E Tests

## Branch Strategy

```
main
  └── test/e2e-tests
```

## Commit Plan

### Task 1: Test Infrastructure
```
test(e2e): set up E2E test infrastructure

- Add TestEventCaptor for domain event capture
- Add E2ETestBase abstract class with common setup
- Add TestDataBuilder and TestFixtures utilities
- Organize infrastructure under e2e/infrastructure/
```

### Task 2: Rental Lifecycle Tests
```
test(e2e): add complete rental lifecycle E2E tests

- Test full flow: request → confirm → pickup → return
- Verify status transitions and payment states
- Verify email event publishing
```

### Task 3: Cancellation & Refund Tests
```
test(e2e): add cancellation and refund E2E tests
```

### Task 4: Dynamic Pricing Tests
```
test(e2e): add dynamic pricing integration E2E tests

- Test early booking, duration, weekend, season strategies
- Verify price breakdown and modifier combinations
```

### Task 5: Currency Conversion Tests
```
test(e2e): add currency conversion E2E tests

- Test USD/EUR conversion with dual price display
- Verify fallback rates when API unavailable
```

### Task 6: Payment Gateway Tests
```
test(e2e): add payment gateway E2E tests

- Test authorize, capture, refund flows with StubPaymentGateway
- Verify payment failure handling
```

### Task 8: Authorization Tests
```
test(e2e): add authorization and security E2E tests

- Test role-based access control (USER vs ADMIN)
- Verify cross-user operation prevention
```

### Task 9: Date Overlap Tests
```
test(e2e): add date overlap and availability E2E tests

- Test double-booking prevention
- Verify availability after cancellation
```

### Task 10: Error Handling Tests
```
test(e2e): add error handling E2E tests

- Test 404, 400, 403 error scenarios
- Verify invalid state transition handling
```

### Task 11: Concurrency Tests
```
test(e2e): add concurrency E2E tests

- Test concurrent rental confirmations
- Verify idempotency in parallel requests
```

## Merge & Rollback

```bash
# Merge
git checkout main
git merge test/e2e-tests

# Rollback
git revert <commit-hash>
```

## Testing

```bash
# Run all E2E tests
mvn test -Dtest="com.akif.rental.unit.e2e.*"

# Run specific test class
mvn test -Dtest="RentalLifecycleE2ETest"

# Run with verbose output
mvn test -Dtest="com.akif.rental.unit.e2e.*" -X

# Skip E2E tests in CI (if needed)
mvn test -DexcludeGroups="e2e"
```

## Test Categories

| Category | Test Class | Coverage |
|----------|-----------|----------|
| Lifecycle | RentalLifecycleE2ETest | Request → Confirm → Pickup → Return |
| Cancellation | CancellationRefundE2ETest | Cancel flows, refunds |
| Pricing | PricingIntegrationE2ETest | All pricing strategies |
| Currency | CurrencyConversionE2ETest | TRY, USD, EUR, fallback |
| Payment | PaymentGatewayE2ETest | Authorize, capture, refund |
| Auth | AuthorizationE2ETest | Role-based access |
| Overlap | DateOverlapE2ETest | Double-booking prevention |
| Errors | ErrorHandlingE2ETest | 404, 400, 403, invalid states |
| Concurrency | ConcurrencyE2ETest | Parallel requests |

## Dependencies

- JUnit 5
- Spring Boot Test
- MockMvc
- AssertJ
- @MockitoBean (Spring Boot 3.4+)

