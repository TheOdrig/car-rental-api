# Requirements Document

## Introduction

The Availability Calendar & Smart Search feature extends the existing car search functionality to include date-based availability filtering, similar car recommendations, and a visual availability calendar. The existing `CarSearchRequestDto` and `CarRepository.findCarsByCriteria()` already provide multi-criteria filtering (brand, model, fuel type, transmission, body type, seats, price range, year range). This feature adds the critical rental-specific capabilities: checking car availability against existing reservations, recommending alternatives when a car is unavailable, and displaying a monthly availability calendar for individual cars.

## Glossary

- **Availability_Calendar_System**: The system component responsible for determining and displaying car availability based on rental date ranges and existing reservations
- **Smart_Search_System**: The existing search system extended with date-based availability filtering and similar car recommendations
- **Date_Range**: A pair of start and end dates representing the desired rental period
- **Availability_Status**: The state indicating whether a car is available (AVAILABLE) or unavailable (UNAVAILABLE) for a given date range
- **Overlap**: A condition where a requested rental period conflicts with an existing confirmed or in-use rental (already implemented in `RentalRepository.countOverlappingRentals()`)
- **Similar_Car**: A car that shares key attributes (brand, body type, price range) with a reference car
- **Blocking_Status**: Car statuses that prevent rental (MAINTENANCE, DAMAGED, SOLD, INSPECTION)

## Requirements

### Requirement 1

**User Story:** As a customer, I want to search for available cars by specifying my rental dates, so that I can see only cars that are actually available for my trip.

#### Acceptance Criteria

1. WHEN a user provides a start date and end date THEN the Availability_Calendar_System SHALL return only cars that have no overlapping confirmed or in-use rentals for the specified Date_Range
2. WHEN a user searches with a Date_Range THEN the Availability_Calendar_System SHALL exclude cars with Blocking_Status (MAINTENANCE, DAMAGED, SOLD, INSPECTION) from the results
3. WHEN a user provides an invalid Date_Range where start date is after end date THEN the Availability_Calendar_System SHALL reject the request and return a validation error
4. WHEN a user provides a start date in the past THEN the Availability_Calendar_System SHALL reject the request and return a validation error
5. WHEN a user combines date filtering with existing criteria filters THEN the Availability_Calendar_System SHALL apply all filters together (date availability AND existing criteria)

### Requirement 2

**User Story:** As a customer, I want to see similar car recommendations when my preferred car is unavailable, so that I can find alternative options without starting a new search.

#### Acceptance Criteria

1. WHEN a user views a car that is unavailable for their selected dates THEN the Smart_Search_System SHALL display up to 5 similar available cars
2. WHEN calculating similar cars THEN the Smart_Search_System SHALL prioritize cars with the same body type and similar price range (within 20% of the original car's price)
3. WHEN calculating similar cars THEN the Smart_Search_System SHALL consider matching brand as a secondary similarity factor
4. WHEN no similar cars are available for the selected dates THEN the Smart_Search_System SHALL return an empty list
5. WHEN displaying similar cars THEN the Smart_Search_System SHALL include the similarity reason (same body type, same brand, similar price)

### Requirement 3

**User Story:** As a customer, I want to view a calendar showing car availability for a specific car, so that I can choose dates when the car is free.

#### Acceptance Criteria

1. WHEN a user requests a car's availability calendar for a specific month THEN the Availability_Calendar_System SHALL return availability status for each day in that month
2. WHEN displaying the calendar THEN the Availability_Calendar_System SHALL mark days with confirmed or in-use rentals as UNAVAILABLE
3. WHEN displaying the calendar THEN the Availability_Calendar_System SHALL mark days with no rentals as AVAILABLE
4. WHEN the car has Blocking_Status THEN the Availability_Calendar_System SHALL mark all days as UNAVAILABLE
5. WHEN displaying availability THEN the Availability_Calendar_System SHALL support viewing up to 3 months in advance from the current date

### Requirement 4

**User Story:** As a customer, I want to see the calculated rental price for my selected dates in search results, so that I can make an informed decision before booking.

#### Acceptance Criteria

1. WHEN displaying available cars for a Date_Range THEN the Smart_Search_System SHALL show the total estimated price for the rental period
2. WHEN calculating the estimated price THEN the Smart_Search_System SHALL apply dynamic pricing rules using the existing DynamicPricingService
3. WHEN displaying prices THEN the Smart_Search_System SHALL show both daily rate and total price for the selected period
4. WHEN a user specifies a target currency THEN the Smart_Search_System SHALL convert prices using the existing CurrencyConversionService

### Requirement 5

**User Story:** As a system administrator, I want the availability search to be performant, so that customers have a responsive experience.

#### Acceptance Criteria

1. WHEN calculating availability THEN the Availability_Calendar_System SHALL use the existing database index on rental dates (idx_rentals_dates)
2. WHEN returning search results THEN the Smart_Search_System SHALL implement pagination with configurable page size (default 20, maximum 100)
3. WHEN multiple users search simultaneously THEN the Availability_Calendar_System SHALL handle concurrent requests without data inconsistency
