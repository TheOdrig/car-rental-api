# Implementation Plan

## 1. Set up Pricing Infrastructure

- [x] 1.1 Create PricingStrategy interface and base classes
  - Create `PricingStrategy` interface with calculate(), getStrategyName(), isEnabled(), getOrder()
  - Create `PriceModifier` record (strategyName, multiplier, description, isDiscount)
  - Create `PricingContext` record (carId, basePrice, startDate, endDate, bookingDate, rentalDays, leadTimeDays, carCategory)
  - Create `PricingResult` record (basePrice, baseTotalPrice, appliedModifiers, combinedMultiplier, finalPrice, effectiveDailyPrice, totalSavings, rentalDays)
  - _Requirements: 6.1, 7.1_

- [x] 1.2 Create PricingConfig for externalized configuration
  - Create `@ConfigurationProperties` class for all pricing settings
  - Add default values for all tiers and multipliers
  - Add validation for multiplier ranges
  - _Requirements: 8.1, 8.2, 8.3_

## 2. Implement Pricing Strategies

- [x] 2.1 Implement EarlyBookingStrategy
  - Calculate lead time from booking date to start date
  - Apply correct multiplier based on tier (30+: 0.85, 14-29: 0.90, 7-13: 0.95, <7: 1.0)
  - Return PriceModifier with description
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ]* 2.2 Write property test for early booking discount
  - **Property 1: Early Booking Discount Correctness**
  - **Validates: Requirements 2.1, 2.2, 2.3, 2.4**

- [x] 2.3 Implement DurationDiscountStrategy
  - Calculate rental duration in days
  - Apply correct multiplier based on tier (7-13: 0.90, 14-29: 0.85, 30+: 0.80, <7: 1.0)
  - Return PriceModifier with description
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ]* 2.4 Write property test for duration discount
  - **Property 2: Duration Discount Correctness**
  - **Validates: Requirements 3.1, 3.2, 3.3, 3.4**

- [x] 2.5 Implement WeekendPricingStrategy
  - Check each day in rental period for weekend (Fri, Sat, Sun)
  - Calculate weighted multiplier based on weekend/weekday ratio
  - Return PriceModifier with description
  - _Requirements: 4.1, 4.2, 4.3_

- [ ]* 2.6 Write property test for weekend pricing
  - **Property 3: Weekend Pricing Correctness**
  - **Validates: Requirements 4.1, 4.2, 4.3**

## 3. Checkpoint - Ensure all tests pass
- Ensure all tests pass, ask the user if questions arise.

## 4. Implement Advanced Strategies

- [x] 4.1 Implement SeasonPricingStrategy
  - Load season configurations from properties
  - Check if rental dates fall within peak/off-peak seasons
  - Handle rentals spanning multiple seasons with weighted average
  - Return PriceModifier with description
  - _Requirements: 1.1, 1.2, 1.3_

- [ ]* 4.2 Write property test for season pricing
  - **Property 5: Season Pricing Correctness**
  - **Validates: Requirements 1.1, 1.2, 1.3**

- [x] 4.3 Implement DemandPricingStrategy
  - Query rental repository for occupancy rate
  - Calculate occupancy for car category in date range
  - Apply correct multiplier based on threshold (>80%: 1.20, 50-80%: 1.10, <50%: 1.0)
  - Return PriceModifier with description
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ]* 4.4 Write property test for demand pricing
  - **Property 4: Demand Pricing Correctness**
  - **Validates: Requirements 5.1, 5.2, 5.3**

## 5. Implement DynamicPricingService

- [x] 5.1 Create IDynamicPricingService interface and implementation
  - Inject all PricingStrategy beans
  - Implement calculatePrice() method
  - Implement previewPrice() method
  - Implement getEnabledStrategies() method
  - _Requirements: 6.1, 6.2, 6.3_

- [x] 5.2 Implement modifier combination logic
  - Collect all enabled strategies
  - Execute each strategy and collect modifiers
  - Multiply all modifiers to get combined multiplier
  - Apply min/max price caps
  - Build and return PricingResult
  - _Requirements: 6.2, 6.4, 7.1_

- [ ]* 5.3 Write property test for modifier combination
  - **Property 6: Modifier Combination Correctness**
  - **Validates: Requirements 6.2**

- [ ]* 5.4 Write property test for price cap enforcement
  - **Property 7: Price Cap Enforcement**
  - **Validates: Requirements 6.4**

- [ ]* 5.5 Write property test for price breakdown completeness
  - **Property 8: Price Breakdown Completeness**
  - **Validates: Requirements 7.1, 7.2**

## 6. Checkpoint - Ensure all tests pass
- Ensure all tests pass, ask the user if questions arise.

## 7. Create Pricing REST API

- [x] 7.1 Create PricingController
  - Implement `POST /api/pricing/calculate` endpoint
  - Implement `GET /api/pricing/preview` endpoint
  - Implement `GET /api/pricing/strategies` endpoint (list enabled strategies)
  - Add Swagger documentation
  - _Requirements: 7.1, 7.2, 7.3_

- [x] 7.2 Create request/response DTOs
  - Create `PricingRequest` record (carId, startDate, endDate)
  - Create `PricingResponse` record (extends PricingResult with formatted output)
  - _Requirements: 7.1_

## 8. Integrate with RentalService

- [x] 8.1 Modify RentalService to use DynamicPricingService
  - Inject DynamicPricingService into RentalServiceImpl
  - Call calculatePrice() when creating rental request
  - Store pricing breakdown in rental (optional: new field or separate table)
  - _Requirements: 6.2, 7.1_

- [x] 8.2 Update RentalResponseDto for pricing display
  - Add appliedDiscounts field (list of modifier descriptions)
  - Add originalPrice and finalPrice fields
  - Add totalSavings field
  - _Requirements: 7.1, 7.2_

## 9. Write Unit and Integration Tests

- [x] 9.1 Write unit tests for each strategy
  - Test EarlyBookingStrategy with various lead times
  - Test DurationDiscountStrategy with various durations
  - Test WeekendPricingStrategy with various date ranges
  - Test SeasonPricingStrategy with peak/off-peak dates
  - Test DemandPricingStrategy with various occupancy levels
  - _Requirements: 2.1-2.4, 3.1-3.4, 4.1-4.3, 1.1-1.3, 5.1-5.3_

- [x] 9.2 Write unit tests for DynamicPricingService
  - Test with all strategies enabled
  - Test with some strategies disabled
  - Test price cap enforcement
  - Test edge cases (same day rental, max duration)
  - _Requirements: 6.2, 6.3, 6.4_

- [x] 9.3 Write integration tests for pricing flow
  - Test full pricing calculation via API
  - Test rental creation with dynamic pricing
  - _Requirements: 7.1, 8.1_

## 10. Final Checkpoint - Ensure all tests pass
- Ensure all tests pass, ask the user if questions arise.

## 11. Documentation and Configuration

- [x] 11.1 Add pricing configuration to application.properties
  - Add all pricing tiers and multipliers
  - Add season date ranges
  - Add strategy enable/disable flags
  - _Requirements: 8.1, 8.4_

- [x] 11.2 Update README.md
  - Add dynamic pricing feature description
  - Add configuration examples
  - Document pricing strategies

