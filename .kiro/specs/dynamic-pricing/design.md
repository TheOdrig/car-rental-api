# Design Document: Dynamic Pricing System

## Overview

This document describes the technical design for implementing a Dynamic Pricing System in the Rent-a-Car application. The feature uses the Strategy Pattern to apply multiple pricing rules (seasonality, early booking, duration, weekend, demand) in a pluggable and configurable manner. Each strategy calculates a price modifier that is combined to produce the final rental price.

## Architecture

### High-Level Architecture

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────────┐
│   Controller    │────▶│  DynamicPricing      │────▶│  PricingStrategy    │
│   Layer         │     │  Service             │     │  (Interface)        │
└─────────────────┘     └──────────────────────┘     └─────────────────────┘
                               │                              │
                               │                    ┌─────────┴─────────┐
                               │                    │                   │
                               ▼              ┌─────▼─────┐       ┌─────▼─────┐
                        ┌──────────────┐      │ Season    │       │ Duration  │
                        │  Pricing     │      │ Strategy  │       │ Strategy  │
                        │  Config      │      └───────────┘       └───────────┘
                        └──────────────┘            │                   │
                                              ┌─────▼─────┐       ┌─────▼─────┐
                                              │ EarlyBook │       │ Weekend   │
                                              │ Strategy  │       │ Strategy  │
                                              └───────────┘       └───────────┘
                                                    │
                                              ┌─────▼─────┐
                                              │ Demand    │
                                              │ Strategy  │
                                              └───────────┘
```

### Strategy Pattern Flow

```
PricingRequest ──▶ DynamicPricingService
                          │
                          ▼
              ┌───────────────────────┐
              │ For each enabled      │
              │ PricingStrategy:      │
              │   calculate modifier  │
              └───────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │ Combine all modifiers │
              │ (multiplication)      │
              └───────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │ Apply min/max caps    │
              │ Return PricingResult  │
              └───────────────────────┘
```

## Components and Interfaces

### 1. PricingStrategy Interface (Strategy Pattern)

```java
public interface PricingStrategy {
    
    /**
     * Calculates the price modifier for this strategy
     * @param context Contains all information needed for calculation
     * @return PriceModifier with multiplier and description
     */
    PriceModifier calculate(PricingContext context);
    
    /**
     * Returns the strategy name for display
     */
    String getStrategyName();
    
    /**
     * Checks if this strategy is enabled
     */
    boolean isEnabled();
    
    /**
     * Returns the order in which this strategy should be applied
     */
    int getOrder();
}
```

### 2. Concrete Strategies

```java
// Season-based pricing
@Component
@Order(1)
public class SeasonPricingStrategy implements PricingStrategy {
    // Peak season: 1.25x, Off-peak: 0.90x
}

// Early booking discount
@Component
@Order(2)
public class EarlyBookingStrategy implements PricingStrategy {
    // 30+ days: 0.85x, 14-29 days: 0.90x, 7-13 days: 0.95x
}

// Duration discount
@Component
@Order(3)
public class DurationDiscountStrategy implements PricingStrategy {
    // 7-13 days: 0.90x, 14-29 days: 0.85x, 30+ days: 0.80x
}

// Weekend pricing
@Component
@Order(4)
public class WeekendPricingStrategy implements PricingStrategy {
    // Fri-Sun: 1.15x per weekend day
}

// Demand-based pricing
@Component
@Order(5)
public class DemandPricingStrategy implements PricingStrategy {
    // >80% occupancy: 1.20x, 50-80%: 1.10x
}
```

### 3. DynamicPricingService

```java
public interface IDynamicPricingService {
    
    /**
     * Calculates the final price with all applicable strategies
     * @param carId The car being rented
     * @param startDate Rental start date
     * @param endDate Rental end date
     * @param bookingDate Date when booking is made (for early booking calc)
     * @return PricingResult with breakdown
     */
    PricingResult calculatePrice(Long carId, LocalDate startDate, 
                                  LocalDate endDate, LocalDate bookingDate);
    
    /**
     * Gets price preview without creating a rental
     */
    PricingResult previewPrice(Long carId, LocalDate startDate, 
                                LocalDate endDate);
    
    /**
     * Gets all enabled strategies
     */
    List<PricingStrategy> getEnabledStrategies();
}
```

### 4. Enhanced RentalService Integration

```java
// RentalService will use DynamicPricingService
public RentalResponseDto requestRental(RentalRequestDto request, String username) {
    // ... existing logic ...
    
    PricingResult pricing = dynamicPricingService.calculatePrice(
        request.getCarId(),
        request.getStartDate(),
        request.getEndDate(),
        LocalDate.now()
    );
    
    rental.setDailyPrice(pricing.getEffectiveDailyPrice());
    rental.setTotalPrice(pricing.getFinalPrice());
    // Store pricing breakdown for transparency
}
```

## Data Models

### PricingContext

```java
public record PricingContext(
    Long carId,
    BigDecimal basePrice,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate bookingDate,
    int rentalDays,
    int leadTimeDays,
    String carCategory  // For demand calculation
) {}
```

### PriceModifier

```java
public record PriceModifier(
    String strategyName,
    BigDecimal multiplier,      // e.g., 0.85 for 15% discount, 1.20 for 20% surcharge
    String description,         // e.g., "Early booking discount (30+ days)"
    boolean isDiscount          // true if multiplier < 1
) {}
```

### PricingResult

```java
public record PricingResult(
    BigDecimal basePrice,
    BigDecimal baseTotalPrice,      // basePrice * days
    List<PriceModifier> appliedModifiers,
    BigDecimal combinedMultiplier,  // All modifiers multiplied
    BigDecimal finalPrice,
    BigDecimal effectiveDailyPrice, // finalPrice / days
    BigDecimal totalSavings,        // baseTotalPrice - finalPrice (if positive)
    int rentalDays
) {}
```

### SeasonConfig (Database/Properties)

```java
public record SeasonConfig(
    String name,            // "Summer Peak", "Winter Off-Peak"
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal multiplier,  // 1.25 for peak, 0.90 for off-peak
    boolean isActive
) {}
```



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing the acceptance criteria, the following consolidations were made:
- Early booking tiers (2.1-2.4) consolidated into Property 1 (lead time → correct multiplier)
- Duration tiers (3.1-3.4) consolidated into Property 2 (duration → correct multiplier)
- Weekend/weekday (4.1-4.3) consolidated into Property 3 (day of week → correct rate)
- Demand tiers (5.1-5.3) consolidated into Property 4 (occupancy → correct multiplier)
- Season pricing (1.1-1.3) consolidated into Property 5 (date range → correct multiplier)

### Properties

**Property 1: Early Booking Discount Correctness**
*For any* booking with lead time L days, the early booking multiplier SHALL be:
- 0.85 when L >= 30
- 0.90 when 14 <= L < 30
- 0.95 when 7 <= L < 14
- 1.00 when L < 7
**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

**Property 2: Duration Discount Correctness**
*For any* rental with duration D days, the duration multiplier SHALL be:
- 0.90 when 7 <= D < 14
- 0.85 when 14 <= D < 30
- 0.80 when D >= 30
- 1.00 when D < 7
**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

**Property 3: Weekend Pricing Correctness**
*For any* rental day, the day multiplier SHALL be 1.15 if the day is Friday, Saturday, or Sunday, and 1.00 otherwise.
**Validates: Requirements 4.1, 4.2, 4.3**

**Property 4: Demand Pricing Correctness**
*For any* car category with occupancy rate O, the demand multiplier SHALL be:
- 1.20 when O > 80%
- 1.10 when 50% <= O <= 80%
- 1.00 when O < 50%
**Validates: Requirements 5.1, 5.2, 5.3**

**Property 5: Season Pricing Correctness**
*For any* rental date within a configured season, the season multiplier SHALL match the configured value for that season.
**Validates: Requirements 1.1, 1.2, 1.3**

**Property 6: Modifier Combination Correctness**
*For any* set of applicable modifiers [m1, m2, ..., mn], the combined multiplier SHALL equal m1 * m2 * ... * mn.
**Validates: Requirements 6.2**

**Property 7: Price Cap Enforcement**
*For any* calculated final price P, P SHALL be within the configured minimum and maximum price bounds.
**Validates: Requirements 6.4**

**Property 8: Price Breakdown Completeness**
*For any* pricing calculation, the result SHALL contain: base price, list of applied modifiers with names and values, combined multiplier, and final price.
**Validates: Requirements 7.1, 7.2**

## Error Handling

| Scenario | Response |
|----------|----------|
| Car not found | 404 Not Found |
| Invalid date range (end < start) | 400 Bad Request |
| No strategies enabled | Return base price with empty modifiers |
| Configuration error | Log warning, use default values |

## Testing Strategy

### Unit Testing

- **Each Strategy**: Test individual strategy calculations with various inputs
- **DynamicPricingService**: Test modifier combination logic
- **Edge cases**: Boundary dates, zero-day rentals, max discounts

### Property-Based Testing

Using **jqwik** library for property-based testing in Java.

Each property test will:
1. Generate random valid inputs (dates, durations, lead times)
2. Execute the pricing calculation
3. Verify the property holds

**Test Configuration:**
- Minimum 100 iterations per property
- Seed-based reproducibility for debugging

### Integration Testing

- **Full pricing flow**: Test complete calculation with all strategies
- **RentalService integration**: Test pricing is correctly applied to rentals
- **Configuration changes**: Test runtime config updates

## Sequence Diagram

### Price Calculation Flow

```
Client        Controller      PricingService     Strategies        Config
  │               │                │                 │               │
  │  POST /price  │                │                 │               │
  │──────────────▶│                │                 │               │
  │               │ calculatePrice │                 │               │
  │               │───────────────▶│                 │               │
  │               │                │  getEnabled()   │               │
  │               │                │────────────────▶│               │
  │               │                │   strategies    │               │
  │               │                │◀────────────────│               │
  │               │                │                 │               │
  │               │                │ for each strategy:              │
  │               │                │  calculate()    │               │
  │               │                │────────────────▶│               │
  │               │                │   modifier      │               │
  │               │                │◀────────────────│               │
  │               │                │                 │               │
  │               │                │  getMinMax()    │               │
  │               │                │─────────────────────────────────▶│
  │               │                │   bounds        │               │
  │               │                │◀─────────────────────────────────│
  │               │                │                 │               │
  │               │  PricingResult │                 │               │
  │               │◀───────────────│                 │               │
  │   response    │                │                 │               │
  │◀──────────────│                │                 │               │
```

## Configuration

### application.properties

```properties
# Season Configuration
pricing.season.peak.start=06-01
pricing.season.peak.end=08-31
pricing.season.peak.multiplier=1.25

pricing.season.offpeak.start=11-01
pricing.season.offpeak.end=02-28
pricing.season.offpeak.multiplier=0.90

# Early Booking Discounts
pricing.early-booking.tier1.days=30
pricing.early-booking.tier1.multiplier=0.85
pricing.early-booking.tier2.days=14
pricing.early-booking.tier2.multiplier=0.90
pricing.early-booking.tier3.days=7
pricing.early-booking.tier3.multiplier=0.95

# Duration Discounts
pricing.duration.tier1.days=7
pricing.duration.tier1.multiplier=0.90
pricing.duration.tier2.days=14
pricing.duration.tier2.multiplier=0.85
pricing.duration.tier3.days=30
pricing.duration.tier3.multiplier=0.80

# Weekend Pricing
pricing.weekend.multiplier=1.15
pricing.weekend.days=FRIDAY,SATURDAY,SUNDAY

# Demand Pricing
pricing.demand.high.threshold=80
pricing.demand.high.multiplier=1.20
pricing.demand.moderate.threshold=50
pricing.demand.moderate.multiplier=1.10

# Price Caps
pricing.min-daily-price=100
pricing.max-daily-price=10000

# Strategy Enable/Disable
pricing.strategy.season.enabled=true
pricing.strategy.early-booking.enabled=true
pricing.strategy.duration.enabled=true
pricing.strategy.weekend.enabled=true
pricing.strategy.demand.enabled=true
```
