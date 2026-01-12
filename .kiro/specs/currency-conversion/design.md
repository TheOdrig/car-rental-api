# Design Document: Real-Time Currency Conversion

## Overview

This document describes the technical design for implementing real-time currency conversion in the Rent-a-Car application. The feature integrates with ExchangeRate-API to fetch live exchange rates, caches them for performance, and provides fallback mechanisms for resilience.

## Architecture

### High-Level Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────────┐
│   Controller    │────▶│  Currency        │────▶│  ExchangeRate-API   │
│   Layer         │     │  Service         │     │  (External)         │
└─────────────────┘     └──────────────────┘     └─────────────────────┘
                               │
                               ▼
                        ┌──────────────────┐
                        │  Caffeine Cache  │
                        │  (1 hour TTL)    │
                        └──────────────────┘
                               │
                               ▼
                        ┌──────────────────┐
                        │  Fallback Rates  │
                        │  (Static Config) │
                        └──────────────────┘
```

### Component Interaction Flow

```
User Request ──▶ CarController ──▶ CarService ──▶ CurrencyService
                                                        │
                                    ┌───────────────────┴───────────────────┐
                                    ▼                                       ▼
                              Cache Hit?                              Cache Miss
                                    │                                       │
                                    ▼                                       ▼
                              Return Rate                         Call ExchangeRate-API
                                                                           │
                                                          ┌────────────────┴────────────────┐
                                                          ▼                                 ▼
                                                    API Success                        API Failure
                                                          │                                 │
                                                          ▼                                 ▼
                                                    Cache & Return                   Use Fallback Rates
```

## Components and Interfaces

### 1. ExchangeRateClient

External API client for fetching exchange rates.

```java
public interface ExchangeRateClient {
    
    /**
     * Fetches current exchange rates from external API
     * @param baseCurrency The base currency code (e.g., "USD")
     * @return ExchangeRateResponse containing rates for all supported currencies
     * @throws ExchangeRateApiException if API call fails
     */
    ExchangeRateResponse fetchRates(String baseCurrency);
}
```

### 2. CurrencyConversionService

Core service for currency conversion operations.

```java
public interface ICurrencyConversionService {
    
    /**
     * Converts amount from one currency to another
     * @param amount The amount to convert
     * @param from Source currency
     * @param to Target currency
     * @return ConversionResult with converted amount and metadata
     */
    ConversionResult convert(BigDecimal amount, CurrencyType from, CurrencyType to);
    
    /**
     * Gets current exchange rate between two currencies
     * @param from Source currency
     * @param to Target currency
     * @return ExchangeRate with rate value and metadata
     */
    ExchangeRate getRate(CurrencyType from, CurrencyType to);
    
    /**
     * Gets all current exchange rates
     * @return Map of currency pairs to rates
     */
    ExchangeRatesResponse getAllRates();
    
    /**
     * Refreshes exchange rates from external API
     * Called by scheduler or manually by admin
     */
    void refreshRates();
}
```

### 3. CurrencyController

REST API endpoints for currency operations.

```java
@RestController
@RequestMapping("/api/exchange-rates")
public class CurrencyController {
    
    GET  /api/exchange-rates                    // Get all rates
    GET  /api/exchange-rates/{from}/{to}        // Get specific rate
    POST /api/convert                           // Convert amount
    POST /api/exchange-rates/refresh            // Force refresh (admin)
}
```

### 4. Enhanced CarController

Modified car endpoints to support currency parameter.

```java
GET /api/cars?currency=USD                      // List cars with USD prices
GET /api/cars/{id}?currency=EUR                 // Get car with EUR price
```

## Data Models

### ExchangeRateResponse (from API)

```java
public record ExchangeRateResponse(
    String base,
    LocalDateTime timestamp,
    Map<String, BigDecimal> rates,
    RateSource source  // LIVE or FALLBACK
) {}
```

### ConversionResult

```java
public record ConversionResult(
    BigDecimal originalAmount,
    CurrencyType originalCurrency,
    BigDecimal convertedAmount,
    CurrencyType targetCurrency,
    BigDecimal exchangeRate,
    LocalDateTime rateTimestamp,
    RateSource source  // LIVE or FALLBACK
) {}
```

### ExchangeRate

```java
public record ExchangeRate(
    CurrencyType from,
    CurrencyType to,
    BigDecimal rate,
    LocalDateTime timestamp,
    RateSource source
) {}
```

### RateSource Enum

```java
public enum RateSource {
    LIVE,      // From external API
    FALLBACK,  // Static fallback rates
    CACHED     // From cache (originally from API)
}
```

### ConvertRequest DTO

```java
public record ConvertRequest(
    @NotNull BigDecimal amount,
    @NotNull CurrencyType fromCurrency,
    @NotNull CurrencyType toCurrency
) {}
```

### CarResponseDto Enhancement

```java
public class CarResponseDto {
    // Existing fields...
    
    // New fields for currency conversion
    private BigDecimal originalPrice;
    private CurrencyType originalCurrency;
    private BigDecimal convertedPrice;      // null if no conversion
    private CurrencyType displayCurrency;   // null if no conversion
    private RateSource rateSource;          // null if no conversion
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing the acceptance criteria, the following redundancies were identified and consolidated:
- Properties 4.1, 4.2, 4.3 (API endpoints) consolidated into Property 3 (API response consistency)
- Properties 5.1, 5.2 (car listing/detail) consolidated into Property 4 (car price conversion)
- Properties 1.1, 5.4 (conversion calculation) consolidated into Property 1 (conversion correctness)

### Properties

**Property 1: Conversion Mathematical Correctness**
*For any* valid amount and currency pair (from, to), the converted amount SHALL equal `amount * exchangeRate(from, to)` rounded to the target currency's decimal places using HALF_UP rounding mode.
**Validates: Requirements 1.1, 1.4, 5.4**

**Property 2: Default Currency Behavior**
*For any* request without a currency parameter, the response SHALL contain prices in TRY (Base_Currency) with no conversion applied.
**Validates: Requirements 1.2**

**Property 3: Invalid Currency Rejection**
*For any* currency code that is not in the Supported_Currencies set (TRY, USD, EUR, GBP, JPY), the system SHALL return HTTP 400 with an error message.
**Validates: Requirements 1.3**

**Property 4: Cache Consistency**
*For any* sequence of conversion requests within the cache TTL (1 hour), all requests SHALL return the same exchange rate for the same currency pair.
**Validates: Requirements 2.2, 2.3**

**Property 5: Fallback Activation**
*For any* conversion request when the Exchange_Rate_API is unavailable, the system SHALL return a valid conversion result with RateSource set to FALLBACK.
**Validates: Requirements 3.1, 3.4**

**Property 6: Response Metadata Completeness**
*For any* exchange rate or conversion response, the response SHALL include: rate source (LIVE, CACHED, or FALLBACK), and last update timestamp.
**Validates: Requirements 4.4**

**Property 7: Dual Price Display**
*For any* car listing or detail request with a currency parameter different from the car's original currency, the response SHALL include both original price with original currency AND converted price with target currency.
**Validates: Requirements 5.1, 5.2, 5.3**

**Property 8: Round-Trip Consistency**
*For any* amount converted from currency A to B and back to A, the result SHALL be within 1% of the original amount (accounting for rounding).
**Validates: Requirements 1.1, 1.4**

## Error Handling

### Error Scenarios

| Scenario | HTTP Status | Error Code | Response |
|----------|-------------|------------|----------|
| Invalid currency code | 400 | INVALID_CURRENCY | "Unsupported currency: {code}" |
| Negative amount | 400 | INVALID_AMOUNT | "Amount must be positive" |
| API unavailable | 200 | N/A | Returns with FALLBACK source |
| API timeout | 200 | N/A | Returns with FALLBACK source |
| Same currency conversion | 200 | N/A | Returns original amount (rate = 1.0) |

### Fallback Rates Configuration

```java
// Static fallback rates (base: TRY)
public static final Map<CurrencyType, BigDecimal> FALLBACK_RATES = Map.of(
    CurrencyType.TRY, BigDecimal.ONE,
    CurrencyType.USD, new BigDecimal("0.029"),   // 1 TRY = 0.029 USD
    CurrencyType.EUR, new BigDecimal("0.027"),   // 1 TRY = 0.027 EUR
    CurrencyType.GBP, new BigDecimal("0.023"),   // 1 TRY = 0.023 GBP
    CurrencyType.JPY, new BigDecimal("4.35")     // 1 TRY = 4.35 JPY
);
```

## Testing Strategy

### Unit Testing

- **CurrencyConversionService**: Test conversion logic with mocked ExchangeRateClient
- **ExchangeRateClient**: Test API response parsing
- **Fallback mechanism**: Test fallback activation when API fails

### Property-Based Testing

Using **jqwik** library for property-based testing in Java.

Each property test will:
1. Generate random valid inputs (amounts, currency pairs)
2. Execute the conversion
3. Verify the property holds

**Test Configuration:**
- Minimum 100 iterations per property
- Seed-based reproducibility for debugging

### Integration Testing

- **API endpoints**: Test full request/response cycle
- **Cache behavior**: Test cache hit/miss scenarios
- **Fallback behavior**: Test with mocked API failures

## External API Integration

### ExchangeRate-API

**Base URL:** `https://api.exchangerate-api.com/v4/latest/{base}`

**Example Request:**
```
GET https://api.exchangerate-api.com/v4/latest/TRY
```

**Example Response:**
```json
{
  "base": "TRY",
  "date": "2024-01-15",
  "time_last_updated": 1705276801,
  "rates": {
    "TRY": 1,
    "USD": 0.033,
    "EUR": 0.030,
    "GBP": 0.026,
    "JPY": 4.89
  }
}
```

### Client Configuration

Spring Boot 3.2+ ile **RestClient** kullanılacak (RestTemplate yerine önerilen modern API).

```java
@Configuration
public class ExchangeRateClientConfig {
    
    @Bean
    public RestClient exchangeRateRestClient() {
        return RestClient.builder()
            .baseUrl("https://api.exchangerate-api.com/v4/latest")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .requestFactory(() -> {
                var factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(Duration.ofSeconds(5));
                factory.setReadTimeout(Duration.ofSeconds(5));
                return factory;
            })
            .build();
    }
}
```

**RestClient Avantajları:**
- Fluent API (daha okunabilir)
- Modern Spring Boot 3.2+ standardı
- Daha iyi error handling
- WebClient'a benzer syntax ama blocking

## Caching Strategy

### Caffeine Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("exchangeRates");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .recordStats());
        return cacheManager;
    }
}
```

### Cache Key Strategy

- Key: Base currency code (e.g., "TRY")
- Value: Map of target currencies to rates
- TTL: 1 hour

## Sequence Diagrams

### Successful Conversion (Cache Hit)

```
User          Controller       Service         Cache
 │                │               │              │
 │  GET /cars?    │               │              │
 │  currency=USD  │               │              │
 │───────────────▶│               │              │
 │                │  convert()    │              │
 │                │──────────────▶│              │
 │                │               │  get("TRY")  │
 │                │               │─────────────▶│
 │                │               │   rates      │
 │                │               │◀─────────────│
 │                │   result      │              │
 │                │◀──────────────│              │
 │   response     │               │              │
 │◀───────────────│               │              │
```

### Conversion with API Call (Cache Miss)

```
User      Controller    Service       Cache      API
 │            │            │            │         │
 │  request   │            │            │         │
 │───────────▶│            │            │         │
 │            │  convert() │            │         │
 │            │───────────▶│            │         │
 │            │            │  get()     │         │
 │            │            │───────────▶│         │
 │            │            │  null      │         │
 │            │            │◀───────────│         │
 │            │            │  fetchRates()        │
 │            │            │─────────────────────▶│
 │            │            │     rates            │
 │            │            │◀─────────────────────│
 │            │            │  put()     │         │
 │            │            │───────────▶│         │
 │            │   result   │            │         │
 │            │◀───────────│            │         │
 │  response  │            │            │         │
 │◀───────────│            │            │         │
```

### Fallback Scenario

```
User      Controller    Service       Cache      API
 │            │            │            │         │
 │  request   │            │            │         │
 │───────────▶│            │            │         │
 │            │  convert() │            │         │
 │            │───────────▶│            │         │
 │            │            │  get()     │         │
 │            │            │───────────▶│         │
 │            │            │  null      │         │
 │            │            │◀───────────│         │
 │            │            │  fetchRates()        │
 │            │            │─────────────────────▶│
 │            │            │     TIMEOUT/ERROR    │
 │            │            │◀─────────────────────│
 │            │            │                      │
 │            │            │  useFallbackRates()  │
 │            │   result   │  (source=FALLBACK)   │
 │            │◀───────────│            │         │
 │  response  │            │            │         │
 │◀───────────│            │            │         │
```
