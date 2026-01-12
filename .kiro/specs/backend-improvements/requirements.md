# Requirements Document

## Introduction

Bu spec, CarGalleryProject backend'indeki medium ve low priority bug'ları ve performans iyileştirmelerini kapsar. Kritik bug'lar ayrı bir spec'te (backend-bug-fixes) ele alınmıştır.

## Glossary

- **N+1_Query**: Bir ana sorgu + her sonuç için ayrı sorgu çalıştırılması problemi
- **Race_Condition**: İki eşzamanlı işlemin aynı veriyi değiştirmeye çalışması durumu
- **Cache_Eviction**: Cache'den veri silme stratejisi
- **EntityGraph**: JPA'da lazy loading problemini çözmek için kullanılan annotation
- **Atomic_Update**: Veritabanında tek bir SQL ile güncelleme yapma

## Requirements

### Requirement 1: Sorting Implementation

**User Story:** As a user, I want to sort car listings by various criteria (price, name, rating), so that I can find cars more easily.

#### Acceptance Criteria

1. WHEN a user sends a GET request with sort parameter, THE CarController SHALL accept sort field and direction parameters
2. THE System SHALL support sorting by: price, brand, model, productionYear, viewCount, likeCount, rating, createTime
3. THE System SHALL support both ascending (asc) and descending (desc) sort directions
4. WHEN no sort parameter is provided, THE System SHALL use default sorting (createTime desc)
5. IF an invalid sort field is provided, THEN THE System SHALL return 400 Bad Request

### Requirement 2: Similar Cars Endpoint Fix

**User Story:** As a frontend developer, I want the similar cars endpoint to work without required date parameters, so that I can show similar cars on car detail pages.

#### Acceptance Criteria

1. WHEN startDate and endDate are not provided, THE AvailabilitySearchController SHALL use default date range (today + 30 days)
2. THE System SHALL make startDate and endDate parameters optional
3. WHEN dates are provided, THE System SHALL use the provided dates for availability check

### Requirement 3: N+1 Query Prevention

**User Story:** As a system administrator, I want optimized database queries, so that the application performs well under load.

#### Acceptance Criteria

1. WHEN fetching car lists, THE CarRepository SHALL use @EntityGraph to eagerly load required associations
2. THE System SHALL NOT execute additional queries for each car in a list
3. THE Query count for listing N cars SHALL be O(1), not O(N)

### Requirement 4: Cache Eviction Optimization

**User Story:** As a system administrator, I want efficient cache management, so that cache invalidation doesn't affect unrelated data.

#### Acceptance Criteria

1. WHEN a single car is updated, THE System SHALL only evict cache entries related to that car
2. THE System SHALL NOT use allEntries=true for single-entity operations
3. WHEN a car is created or deleted, THE System MAY evict list caches

### Requirement 5: View/Like Count Race Condition Fix

**User Story:** As a developer, I want thread-safe view and like count updates, so that counts are accurate under concurrent access.

#### Acceptance Criteria

1. WHEN multiple users view the same car simultaneously, THE System SHALL correctly increment view count for each view
2. THE System SHALL use atomic database update (UPDATE ... SET count = count + 1) instead of read-modify-write
3. THE System SHALL NOT lose any view or like count due to race conditions

### Requirement 6: Seed Data Image URLs

**User Story:** As a frontend developer, I want seed data to include image URLs, so that the UI displays car images correctly in development.

#### Acceptance Criteria

1. THE Seed_Data SHALL include valid image_url for each car
2. THE Seed_Data SHALL include valid thumbnail_url for each car
3. THE Image URLs SHALL point to placeholder images or real car images

### Requirement 7: API Endpoint Consistency

**User Story:** As a frontend developer, I want consistent API endpoint naming, so that integration is straightforward.

#### Acceptance Criteria

1. THE CarController /active endpoint SHALL be the primary endpoint for listing available cars
2. THE System SHALL document which endpoint to use for each use case
3. THE Featured cars endpoint SHALL be accessible at /api/cars/featured (not /api/cars/search/featured)
