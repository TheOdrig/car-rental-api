# Implementation Plan

## Mevcut Bileşenler (Yeniden Kullanılacak)
- `RentalServiceImpl.validateCarAvailability()` - Car status kontrolü pattern'i
- `RentalServiceImpl.validateRentalDates()` - Tarih validasyonu pattern'i
- `RentalServiceImpl.checkDateOverlap()` - Çakışma kontrolü pattern'i
- `RentalRepository.countOverlappingRentals()` - Overlap query
- `RentalValidationException` - Hata yönetimi
- `CarStatusType.getUnavailableStatuses()` - Blocking status'lar
- `IDynamicPricingService.calculatePrice()` - Fiyat hesaplama
- `ICurrencyConversionService.convert()` - Para birimi dönüşümü
- `CarListResponseDto` - Pagination response pattern
- `PricingRequestDto` - Date validation pattern

---

- [x] 1. Create availability DTOs









  - Create `dto/availability/AvailabilitySearchRequestDto.java` - Date range + filters (reuse PricingRequestDto validation pattern)
  - Create `dto/availability/AvailabilitySearchResponseDto.java` - Paginated results (reuse CarListResponseDto pattern)
  - Create `dto/availability/CarAvailabilityCalendarDto.java` - Monthly calendar with List<DayAvailabilityDto>
  - Create `dto/availability/DayAvailabilityDto.java` - Date + available (boolean) + rentalId (if unavailable)
  - Create `dto/availability/SimilarCarDto.java` - Car info + similarityReasons + similarityScore
  - _Requirements: 1.1, 1.3, 1.4, 2.5, 3.1, 4.1, 4.3_

- [x] 2. Extend repositories for availability queries




  - [x] 2.1 Add RentalRepository query for monthly rentals


    - Add `findByCarIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusInAndIsDeletedFalse()` method
    - Used for calendar generation - find rentals overlapping with a month
    - _Requirements: 3.1, 3.2_
  - [x] 2.2 Add CarRepository query for available cars


    - Add `findAvailableCarsForDateRange()` using @Query with NOT EXISTS subquery
    - Exclude cars with blocking status using `CarStatusType.getUnavailableStatuses()`
    - Exclude cars with overlapping rentals
    - _Requirements: 1.1, 1.2, 1.5_
  - [x] 2.3 Add CarRepository query for similar cars


    - Add `findSimilarCars(bodyType, minPrice, maxPrice, excludeCarId, pageable)` method
    - Filter by body type OR price range
    - _Requirements: 2.2, 2.3_

- [x] 3. Implement ICarAvailabilityService




  - [x] 3.1 Create interface and implementation


    - Create `service/availability/ICarAvailabilityService.java` interface
    - Create `service/availability/impl/CarAvailabilityServiceImpl.java`
    - Inject CarRepository, RentalRepository, IDynamicPricingService, ICurrencyConversionService
    - _Requirements: 1.1, 1.2_
  - [x] 3.2 Implement searchAvailableCars method

    - Reuse `validateRentalDates()` pattern from RentalServiceImpl
    - Query available cars using new repository method
    - Calculate pricing using `IDynamicPricingService.calculatePrice()`
    - Apply currency conversion using `ICurrencyConversionService.convert()`
    - Return paginated AvailabilitySearchResponseDto
    - _Requirements: 1.1, 1.2, 1.5, 4.1, 4.2, 4.3, 4.4, 5.2_
  - [x] 3.3 Implement isCarAvailable method



    - Check car status using `CarStatusType.getUnavailableStatuses()`
    - Check overlaps using existing `countOverlappingRentals()`
    - Return boolean
    - _Requirements: 1.1, 1.2_
  - [x] 3.4 Implement getCarAvailabilityCalendar method

    - Validate month is within 3 months from today
    - Get rentals for car in month using new repository method
    - Build day-by-day availability (iterate each day, check if any rental overlaps)
    - Handle blocked cars (all days unavailable)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 4. Implement ISimilarCarService




  - [x] 4.1 Create interface and implementation


    - Create `service/availability/ISimilarCarService.java` interface
    - Create `service/availability/impl/SimilarCarServiceImpl.java`
    - Inject CarRepository, ICarAvailabilityService, IDynamicPricingService
    - _Requirements: 2.1_
  - [x] 4.2 Implement findSimilarAvailableCars method


    - Get reference car details
    - Calculate price range (±20%)
    - Find cars using new repository method
    - Filter by availability using `isCarAvailable()`
    - Calculate similarity score (body type match: +50, brand match: +30, price match: +20)
    - Return max 5 cars ordered by similarity score
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 5. Create AvailabilitySearchController





  - [x] 5.1 Create controller with search endpoint


    - Create `controller/AvailabilitySearchController.java`
    - Add `POST /api/cars/availability/search` endpoint
    - Add OpenAPI annotations (follow existing controller patterns)
    - _Requirements: 1.1, 1.2, 1.5, 4.1_
  - [x] 5.2 Add calendar endpoint

    - Add `GET /api/cars/{id}/availability/calendar` endpoint
    - Accept `month` parameter (format: yyyy-MM, default: current month)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  - [x] 5.3 Add similar cars endpoint

    - Add `GET /api/cars/{id}/similar` endpoint
    - Accept `startDate`, `endDate`, `limit` (default: 5) parameters
    - _Requirements: 2.1, 2.2, 2.5_

- [x] 6. Update SecurityConfig




  - Add public access for availability endpoints
  - `GET /api/cars/{id}/availability/**` - permitAll
  - `POST /api/cars/availability/search` - permitAll
  - `GET /api/cars/{id}/similar` - permitAll
  - _Requirements: 1.1_

- [x] 7. Checkpoint - Implementation Complete
  - Ensure application compiles and runs, ask the user if questions arise.

- [x] 8. Unit Tests





  - [x] 8.1 Write unit tests for CarAvailabilityServiceImpl


    - Test date validation
    - Test filter combination
    - Test calendar generation
    - Test pricing integration
    - Test pagination
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.1, 3.2, 3.3, 3.4_

  - [x] 8.2 Write unit tests for SimilarCarServiceImpl

    - Test max 5 cars returned
    - Test similarity criteria
    - Test similarity reasons included
    - Test empty result when no similar cars
    - _Requirements: 2.1, 2.2, 2.4, 2.5_

- [x] 9. Integration Tests




  - [x] 9.1 Write integration tests for AvailabilitySearchController



    - Test search with date range
    - Test search with filters
    - Test calendar endpoint
    - Test similar cars endpoint
    - Test error cases (invalid dates, car not found)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 3.1_

- [x] 10. Update Documentation






  - [x] 10.1 Update RENTAL_MVP_PLAN.md

    - Mark "Availability Calendar & Smart Search" as completed
    - Update progress percentage
    - Add implementation notes
    - _Requirements: All_

  - [x] 10.2 Update FEATURE_ROADMAP.md

    - Update Tier 2 progress
    - Add interview talking points for this feature
    - _Requirements: All_

- [ ] 11. Final Checkpoint
  - Ensure all tests pass, ask the user if questions arise.
