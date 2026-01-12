# Git Workflow - Dynamic Pricing System

## Branch Strategy

```
main
  └── feature/dynamic-pricing
```

## Commit Plan

### Task 1: Infrastructure
```
feat(pricing): add strategy pattern infrastructure

- Add PricingStrategy interface for pluggable pricing rules
- Add value objects: PriceModifier, PricingContext, PricingResult
- Add PricingConfig with externalized configuration support

Enables flexible pricing strategies that can be combined and configured
without code changes, supporting multiple discount/surcharge rules.
```

### Task 2: Basic Strategies
```
feat(pricing): implement basic pricing strategies

- Add EarlyBookingStrategy with tiered discounts (5-15%)
- Add DurationDiscountStrategy for longer rentals (10-20%)
- Add WeekendPricingStrategy with day-based multipliers

Implements time-based and duration-based pricing adjustments that
automatically apply based on booking and rental parameters.
```

### Task 4: Advanced Strategies
```
feat(pricing): implement season and demand strategies

- Add SeasonPricingStrategy with peak/off-peak periods
- Add DemandPricingStrategy based on occupancy rates
- Support weighted calculations for multi-period rentals

Enables dynamic pricing based on seasonal demand and real-time
availability, maximizing revenue during high-demand periods.
```

### Task 5: DynamicPricingService
```
feat(pricing): implement dynamic pricing service

- Add IDynamicPricingService with price calculation logic
- Implement strategy combination via multiplication
- Add min/max price cap enforcement
- Generate detailed pricing breakdown

Orchestrates all pricing strategies and produces final prices with
complete transparency on applied modifiers.
```

### Task 7: REST API
```
feat(pricing): add pricing REST endpoints

- Add POST /api/pricing/calculate for full calculations
- Add GET /api/pricing/preview for price estimates
- Add GET /api/pricing/strategies to list active rules
- Include Swagger/OpenAPI documentation

Exposes pricing functionality via REST API for frontend integration
and external system access.
```

### Task 8: RentalService Integration
```
feat(rental): integrate dynamic pricing into rental flow

- Inject DynamicPricingService into rental creation
- Calculate prices automatically on rental requests
- Add pricing breakdown to rental responses

Replaces static pricing with dynamic calculations, applying all
configured strategies to every rental automatically.
```

### Task 9: Tests
```
test(pricing): add comprehensive test coverage

- Add unit tests for all 5 pricing strategies
- Add DynamicPricingService integration tests
- Add end-to-end pricing flow tests

Ensures pricing calculations are correct across all scenarios and
strategy combinations.
```

### Task 11: Documentation
```
docs(pricing): add configuration and documentation

- Add pricing properties to application.properties
- Update README with dynamic pricing feature guide
- Document configuration options and examples

Provides clear guidance for configuring and using the dynamic
pricing system.
```

## Final Merge
```
git checkout main
git merge feature/dynamic-pricing
git push origin main
```

## Rollback Plan
```
git revert <commit-hash>
# veya
git reset --hard <previous-commit>
```
