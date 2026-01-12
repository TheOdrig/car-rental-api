# Implementation Plan

## 1. Set up Exchange Rate Client Infrastructure

- [x] 1.1 Create ExchangeRateClient interface and implementation
  - Create `IExchangeRateClient` interface with `fetchRates(String baseCurrency)` method
  - Create `ExchangeRateClientImpl` using RestClient
  - Configure 5-second timeout for connect and read
  - Add error handling for API failures
  - _Requirements: 2.1, 3.1, 3.2, 3.3_

- [x] 1.2 Create DTOs for API response
  - Create `ExchangeRateApiResponse` record for external API response
  - Create `ExchangeRateResponse` record for internal use
  - Create `RateSource` enum (LIVE, CACHED, FALLBACK)
  - _Requirements: 4.4_

- [ ]* 1.3 Write property test for API response parsing
  - **Property 6: Response Metadata Completeness**
  - **Validates: Requirements 4.4**

## 2. Implement Currency Conversion Service

- [x] 2.1 Create CurrencyConversionService interface and implementation
  - Create `ICurrencyConversionService` interface
  - Implement `convert(amount, from, to)` method
  - Implement `getRate(from, to)` method
  - Implement `getAllRates()` method
  - Implement `refreshRates()` method
  - _Requirements: 1.1, 1.4, 4.1, 4.2, 4.3_

- [x] 2.2 Implement conversion calculation logic
  - Calculate converted amount using exchange rate
  - Apply correct rounding (HALF_UP, 2 decimal places, 0 for JPY)
  - Handle same-currency conversion (rate = 1.0)
  - _Requirements: 1.1, 1.4_

- [ ]* 2.3 Write property test for conversion correctness
  - **Property 1: Conversion Mathematical Correctness**
  - **Validates: Requirements 1.1, 1.4, 5.4**

- [ ]* 2.4 Write property test for round-trip consistency
  - **Property 8: Round-Trip Consistency**
  - **Validates: Requirements 1.1, 1.4**

## 3. Implement Caching Strategy

- [x] 3.1 Configure Caffeine cache for exchange rates
  - Add cache configuration with 1-hour TTL
  - Configure cache key strategy (base currency)
  - Enable cache statistics for monitoring
  - _Requirements: 2.2, 2.3, 2.4_

- [x] 3.2 Integrate caching with CurrencyConversionService
  - Add `@Cacheable` annotation for rate fetching
  - Add `@CacheEvict` for manual refresh
  - Implement cache-aside pattern
  - _Requirements: 2.2, 2.3_

- [ ]* 3.3 Write property test for cache consistency
  - **Property 4: Cache Consistency**
  - **Validates: Requirements 2.2, 2.3**

## 4. Implement Fallback Mechanism

- [x] 4.1 Create fallback rates configuration
  - Define static fallback rates for all supported currencies
  - Create `FallbackRatesConfig` class
  - Make rates configurable via application.properties
  - _Requirements: 3.1_

- [x] 4.2 Implement fallback logic in service
  - Catch API exceptions and timeout
  - Switch to fallback rates on failure
  - Set RateSource to FALLBACK in response
  - Log warning when using fallback
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ]* 4.3 Write property test for fallback activation
  - **Property 5: Fallback Activation**
  - **Validates: Requirements 3.1, 3.4**

## 5. Checkpoint - Ensure all tests pass
- Ensure all tests pass, ask the user if questions arise.

## 6. Create Currency REST API Endpoints

- [x] 6.1 Create CurrencyController
  - Implement `GET /api/exchange-rates` endpoint
  - Implement `GET /api/exchange-rates/{from}/{to}` endpoint
  - Implement `POST /api/convert` endpoint
  - Implement `POST /api/exchange-rates/refresh` (admin only)
  - Add Swagger documentation
  - _Requirements: 4.1, 4.2, 4.3_

- [x] 6.2 Create request/response DTOs
  - Create `ConvertRequest` record
  - Create `ConversionResult` record
  - Create `ExchangeRatesResponse` record
  - _Requirements: 4.3, 4.4_

- [x] 6.3 Add input validation
  - Validate currency codes against CurrencyType enum
  - Validate amount is positive
  - Return proper error responses for invalid input
  - _Requirements: 1.3_

- [ ]* 6.4 Write property test for invalid currency rejection
  - **Property 3: Invalid Currency Rejection**
  - **Validates: Requirements 1.3**

## 7. Integrate Currency Conversion with Car Endpoints

- [x] 7.1 Modify CarController to accept currency parameter
  - Add `currency` query parameter to `GET /api/cars`
  - Add `currency` query parameter to `GET /api/cars/{id}`
  - Pass currency to service layer
  - _Requirements: 5.1, 5.2_

- [x] 7.2 Modify CarService to support currency conversion
  - Inject CurrencyConversionService
  - Convert prices when currency parameter is provided
  - Return both original and converted prices
  - _Requirements: 5.1, 5.2, 5.3_

- [x] 7.3 Update CarResponseDto for dual price display
  - Add `originalPrice` and `originalCurrency` fields
  - Add `convertedPrice` and `displayCurrency` fields
  - Add `rateSource` field
  - Update CarMapper
  - _Requirements: 5.3_

- [ ]* 7.4 Write property test for default currency behavior
  - **Property 2: Default Currency Behavior**
  - **Validates: Requirements 1.2**

- [ ]* 7.5 Write property test for dual price display
  - **Property 7: Dual Price Display**
  - **Validates: Requirements 5.1, 5.2, 5.3**

## 8. Integrate Currency Conversion with Rental Endpoints

- [x] 8.1 Modify RentalController to accept currency parameter
  - Add `currency` query parameter to rental endpoints
  - Pass currency to service layer
  - _Requirements: 5.4_

- [x] 8.2 Modify RentalService to support currency conversion
  - Convert total rental price when currency specified
  - Include conversion metadata in response
  - _Requirements: 5.4_

- [x] 8.3 Update RentalResponseDto for currency display
  - Add converted price fields
  - Update RentalMapper
  - _Requirements: 5.4_

## 9. Add Scheduled Rate Refresh

- [x] 9.1 Create scheduled task for rate refresh
  - Add `@Scheduled` method to refresh rates every hour
  - Log refresh operations
  - Handle refresh failures gracefully
  - _Requirements: 2.4_

## 10. Final Checkpoint - Write Tests and Ensure All Pass

- [x] 10.1 Write unit tests for CurrencyConversionService
  - Test convert() with various currency pairs
  - Test getRate() and getAllRates()
  - Test fallback behavior when API fails
  - _Requirements: 1.1, 1.4, 3.1_

- [x] 10.2 Write unit tests for CurrencyController
  - Test all endpoints with valid/invalid inputs
  - Test error responses
  - _Requirements: 4.1, 4.2, 4.3_

- [x] 10.3 Write integration tests for currency conversion flow
  - Test end-to-end conversion with mocked API
  - Test cache behavior
  - _Requirements: 2.2, 2.3_

- [x] 10.4 Ensure all existing tests pass
  - Run full test suite
  - Fix any regressions

## 11. Documentation and Cleanup

- [x] 11.1 Update Swagger documentation
  - Add descriptions for all new endpoints
  - Add request/response examples
  - Document error responses
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 11.2 Update README.md
  - Add currency conversion feature description
  - Add API usage examples
  - Document supported currencies
