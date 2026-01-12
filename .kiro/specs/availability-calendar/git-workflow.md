# Git Workflow - Availability Calendar & Smart Search

## Branch Strategy

```
main
  └── feature/availability-calendar
```

## Commit Plan

### Commit 1: DTOs
```
feat(availability): add availability search DTOs

- Add 5 new DTOs for availability feature
- Include date validation and pagination support
- Organize under dto/availability/ package
```

### Commit 2: Extend repositories for availability queries
```
feat(availability): add repository queries for availability

- Add findOverlappingRentalsForCar() to RentalRepository
- Add findAvailableCarsForDateRange() with NOT EXISTS subquery
- Add findSimilarCars() for similar car recommendations
- Exclude cars with blocking status
- Support calendar generation and date range filtering
```

### Commit 3: Implement ICarAvailabilityService
```
feat(availability): implement car availability service

- Create ICarAvailabilityService interface
- Implement searchAvailableCars with date validation
- Implement isCarAvailable for availability check
- Implement getCarAvailabilityCalendar for monthly view
- Integrate dynamic pricing and currency conversion
- Handle blocked cars and validate date ranges
```

### Commit 4: Implement ISimilarCarService
```
feat(availability): implement similar car service

- Create ISimilarCarService interface
- Implement findSimilarAvailableCars
- Calculate similarity score (body type: +50, brand: +30, price: +20)
- Filter by availability and return top 5 cars
```

### Commit 5: Create AvailabilitySearchController
```
feat(availability): add availability search endpoints

- Create AvailabilitySearchController
- Add POST /api/cars/availability/search endpoint
- Add GET /api/cars/{id}/availability/calendar endpoint
- Add GET /api/cars/{id}/similar endpoint
- Add OpenAPI annotations
```

### Commit 6: Update SecurityConfig
```
feat(availability): configure public access for endpoints

- Add permitAll for availability search endpoints
- Allow anonymous access for browsing
```

### Commit 7: Implementation Checkpoint
```
chore(availability): verify implementation compiles and runs

- Run mvn clean compile, verify app starts, check endpoints
```

### Commit 8: Unit Tests
```
test(availability): add unit tests for services

- Add CarAvailabilityServiceImplTest
- Add SimilarCarServiceImplTest
- Test date validation, filters, calendar, pricing, similarity scoring
```

### Commit 9: Integration Tests
```
test(availability): add integration tests for controller

- Add AvailabilitySearchControllerIntegrationTest
- Test endpoints with valid/invalid inputs, error cases, JSON structure
```

### Commit 10: Update Documentation
```
docs(availability): update project documentation

- Mark feature as completed in RENTAL_MVP_PLAN.md
- Update FEATURE_ROADMAP.md with interview points
- Add implementation notes and progress updates
```

### Commit 11: Final Checkpoint
```
test(availability): verify all tests pass

- Run tests, verify coverage >80%, manual API testing
```

## Merge & Rollback

```bash
# Merge to main
git checkout main
git merge feature/availability-calendar

# Rollback if needed
git revert <commit-hash>

# Rollback entire feature
git revert -m 1 <merge-commit-hash>
```

## Testing

```bash
# Run all availability tests
mvn test -Dtest="*Availability*"

# Run specific test class
mvn test -Dtest="CarAvailabilityServiceImplTest"

# Run integration tests only
mvn test -Dtest="AvailabilitySearchControllerIntegrationTest"

# Run with verbose output
mvn test -Dtest="*Availability*" -X

# Run all tests with coverage
mvn clean test jacoco:report
```

## API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /api/cars/availability/search | Search available cars by date range | Public |
| GET | /api/cars/{id}/availability/calendar | Get monthly availability calendar | Public |
| GET | /api/cars/{id}/similar | Get similar available cars | Public |

## Test Categories

| Category | Test Class | Coverage |
|----------|-----------|----------|
| Service | CarAvailabilityServiceImplTest | Date validation, filters, calendar, pricing |
| Service | SimilarCarServiceImplTest | Similarity criteria, max 5 cars, scoring |
| Integration | AvailabilitySearchControllerIntegrationTest | All endpoints, error cases |

## Key Features

### 1. Date-based Availability Search
- Filter cars by rental date range
- Exclude cars with overlapping rentals
- Exclude cars with blocking status
- Apply existing filters (brand, model, price, etc.)
- Dynamic pricing integration
- Currency conversion support
- Pagination (default: 20, max: 100)

### 2. Similar Car Recommendations
- Find up to 5 similar available cars
- Similarity scoring:
  - Same body type: +50 points
  - Same brand: +30 points
  - Similar price (±20%): +20 points
- Include similarity reasons
- Filter by availability

### 3. Availability Calendar
- Monthly view (up to 3 months ahead)
- Day-by-day availability status
- Show rental ID for unavailable days
- Handle blocked cars (all days unavailable)
- Block reasons (MAINTENANCE, DAMAGED, etc.)

## Reused Components

| Component | Source | Usage |
|-----------|--------|-------|
| validateRentalDates() | RentalServiceImpl | Date validation pattern |
| checkDateOverlap() | RentalServiceImpl | Overlap check pattern |
| countOverlappingRentals() | RentalRepository | Existing overlap query |
| RentalValidationException | exception/ | Error handling |
| CarStatusType.getUnavailableStatuses() | enums/ | Blocking statuses |
| IDynamicPricingService | service/pricing/ | Price calculation |
| ICurrencyConversionService | service/currency/ | Currency conversion |
| CarListResponseDto | dto/response/ | Pagination pattern |
| PricingRequestDto | dto/request/ | Date validation pattern |

## Dependencies

- Spring Boot 3.5.6
- Spring Data JPA
- Spring Validation
- MapStruct 1.5.5
- Lombok
- JUnit 5
- Mockito
- MockMvc
- AssertJ
- @MockitoBean (Spring Boot 3.4+)

## Database Indexes

Existing indexes that will be utilized:
- `idx_rentals_dates` - Rental date range queries
- `idx_car_status` - Car status filtering
- `idx_car_brand` - Brand filtering
- `idx_car_price` - Price range filtering

## Correctness Properties

The implementation validates 13 correctness properties:
1. Available cars have no rental conflicts
2. Available cars have rentable status
3. Combined filters are applied conjunctively
4. Similar cars count constraint (max 5)
5. Similar cars meet similarity criteria
6. Similar cars include similarity reason
7. Calendar contains all days in month
8. Calendar availability matches rental data
9. Blocked cars show all days unavailable
10. Search results include pricing information
11. Dynamic pricing is applied correctly
12. Currency conversion is applied correctly
13. Pagination respects size constraints

## Performance Considerations

- Use existing database indexes
- Implement pagination (default: 20, max: 100)
- Cache availability results (optional)
- Optimize NOT EXISTS subquery
- Batch price calculations
- Minimize N+1 queries

## Error Handling

| Error Code | HTTP Status | Condition |
|------------|-------------|-----------|
| INVALID_DATE_RANGE | 400 | startDate > endDate |
| PAST_START_DATE | 400 | startDate < today |
| DATE_RANGE_TOO_LONG | 400 | endDate - startDate > 90 days |
| CALENDAR_MONTH_TOO_FAR | 400 | month > currentMonth + 3 |
| INVALID_PAGE_SIZE | 400 | size < 1 or size > 100 |
| CAR_NOT_FOUND | 404 | Car ID does not exist |
