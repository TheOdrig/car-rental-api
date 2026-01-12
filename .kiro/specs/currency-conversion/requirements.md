# Requirements Document

## Introduction

This document specifies the requirements for a Real-Time Currency Conversion feature in the Rent-a-Car application. The feature enables users to view car rental prices in their preferred currency using live exchange rates from an external API. The system will cache exchange rates to optimize performance and provide fallback mechanisms when the external API is unavailable.

## Glossary

- **Currency_Conversion_Service**: The system component responsible for converting monetary amounts between different currencies using real-time exchange rates.
- **Exchange_Rate_API**: The external service (ExchangeRate-API) that provides current currency exchange rates.
- **Base_Currency**: The default currency in which car prices are stored in the database (TRY - Turkish Lira).
- **Target_Currency**: The currency to which the user wants to convert prices.
- **Exchange_Rate**: The ratio at which one currency can be exchanged for another.
- **Rate_Cache**: The in-memory storage that holds exchange rates to reduce external API calls.
- **Fallback_Rate**: A predefined static exchange rate used when the external API is unavailable.
- **Supported_Currencies**: The set of currencies the system can convert between, matching the existing CurrencyType enum (TRY, USD, EUR, GBP, JPY).

## Requirements

### Requirement 1

**User Story:** As a user, I want to view car rental prices in my preferred currency, so that I can understand the cost in familiar monetary terms.

#### Acceptance Criteria

1. WHEN a user requests car prices with a target currency parameter THEN the Currency_Conversion_Service SHALL return prices converted to the specified target currency
2. WHEN a user does not specify a target currency THEN the Currency_Conversion_Service SHALL return prices in the Base_Currency (TRY)
3. WHEN a user specifies an unsupported currency THEN the Currency_Conversion_Service SHALL return an error response with HTTP 400 status and a descriptive message
4. WHEN converting prices THEN the Currency_Conversion_Service SHALL round converted amounts to 2 decimal places using HALF_UP rounding mode

### Requirement 2

**User Story:** As a system administrator, I want the system to fetch and cache exchange rates efficiently, so that API costs are minimized and response times are fast.

#### Acceptance Criteria

1. WHEN the application starts THEN the Currency_Conversion_Service SHALL fetch current exchange rates from the Exchange_Rate_API
2. WHEN exchange rates are fetched successfully THEN the Currency_Conversion_Service SHALL store rates in the Rate_Cache with a 1-hour time-to-live
3. WHEN a conversion is requested and cached rates exist THEN the Currency_Conversion_Service SHALL use cached rates without calling the Exchange_Rate_API
4. WHEN cached rates expire THEN the Currency_Conversion_Service SHALL fetch fresh rates from the Exchange_Rate_API on the next conversion request
5. WHEN the Exchange_Rate_API returns rates THEN the Currency_Conversion_Service SHALL log the fetch timestamp and rate count

### Requirement 3

**User Story:** As a system administrator, I want the system to handle API failures gracefully, so that users can still use the application when the external API is unavailable.

#### Acceptance Criteria

1. IF the Exchange_Rate_API is unavailable THEN the Currency_Conversion_Service SHALL use Fallback_Rates for currency conversion
2. IF the Exchange_Rate_API returns an error THEN the Currency_Conversion_Service SHALL log the error with severity WARN and continue with Fallback_Rates
3. IF the Exchange_Rate_API call times out after 5 seconds THEN the Currency_Conversion_Service SHALL use Fallback_Rates
4. WHEN using Fallback_Rates THEN the Currency_Conversion_Service SHALL include a warning flag in the response indicating rates may not be current
5. WHEN the Exchange_Rate_API becomes available after a failure THEN the Currency_Conversion_Service SHALL resume using live rates on the next cache refresh

### Requirement 4

**User Story:** As a developer, I want a clean API endpoint for currency conversion, so that I can easily integrate it with other services.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/exchange-rates THEN the Currency_Conversion_Service SHALL return current exchange rates for all Supported_Currencies
2. WHEN a GET request is made to /api/exchange-rates/{from}/{to} THEN the Currency_Conversion_Service SHALL return the specific exchange rate between the two currencies
3. WHEN a POST request is made to /api/convert with amount, fromCurrency, and toCurrency THEN the Currency_Conversion_Service SHALL return the converted amount
4. WHEN any exchange rate endpoint is called THEN the Currency_Conversion_Service SHALL include metadata showing rate source (LIVE or FALLBACK) and last update timestamp

### Requirement 5

**User Story:** As a user, I want to see car listings with prices in my preferred currency, so that I can compare cars easily.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/cars with currency query parameter THEN the Currency_Conversion_Service SHALL return car listings with prices converted to the specified currency
2. WHEN a GET request is made to /api/cars/{id} with currency query parameter THEN the Currency_Conversion_Service SHALL return car details with prices converted to the specified currency
3. WHEN returning converted prices THEN the Currency_Conversion_Service SHALL include both original price with original currency and converted price with target currency
4. WHEN a rental price is calculated THEN the Currency_Conversion_Service SHALL convert the total rental price to the user's preferred currency if specified

### Requirement 6

**User Story:** As a system administrator, I want to monitor exchange rate operations, so that I can ensure the system is functioning correctly.

#### Acceptance Criteria

1. WHEN exchange rates are fetched from the API THEN the Currency_Conversion_Service SHALL log the operation with INFO level including timestamp and number of rates received
2. WHEN a conversion is performed THEN the Currency_Conversion_Service SHALL log the conversion details at DEBUG level
3. WHEN fallback rates are used THEN the Currency_Conversion_Service SHALL log a WARN level message indicating degraded service
4. WHEN an API error occurs THEN the Currency_Conversion_Service SHALL log the error details including HTTP status code and error message
