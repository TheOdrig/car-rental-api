# Design Document: Backend Improvements

## Overview

Bu tasarım dokümanı, CarGalleryProject backend'indeki medium/low priority bug'ların ve performans iyileştirmelerinin teknik çözümlerini detaylandırır.

## Architecture

### Değişiklik Alanları

```
┌─────────────────────────────────────────────────────────────────┐
│                        CHANGES                                   │
├─────────────────────────────────────────────────────────────────┤
│ CarController          → Sorting parameters                      │
│ AvailabilityController → Optional date parameters                │
│ CarSearchController    → Featured endpoint path                  │
│ CarRepository          → @EntityGraph, atomic updates            │
│ CarServiceImpl         → Cache key optimization                  │
│ V16__seed_images.sql   → Image URLs for seed data               │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Sorting Implementation

**CarController Değişiklikleri:**

```java
@GetMapping("/active")
public ResponseEntity<Page<CarResponse>> getAllActiveCars(
        @PageableDefault(size = 20, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestParam(required = false) CurrencyType currency,
        // Filter parameters...
        // Sorting - Spring Pageable otomatik handle eder
        // ?sort=price,asc veya ?sort=brand,desc
        ) {
    // Pageable zaten sort bilgisini içeriyor
    Page<CarResponse> cars = carService.findActiveCarsWithFilters(filter, pageable);
    return ResponseEntity.ok(cars);
}
```

**Desteklenen Sort Alanları:**
- price, brand, model, productionYear
- viewCount, likeCount, rating
- createTime (default)

### 2. Similar Cars Endpoint Fix

**AvailabilitySearchController Değişiklikleri:**

```java
@GetMapping("/{id}/similar")
public ResponseEntity<List<SimilarCarDto>> getSimilarAvailableCars(
        @PathVariable Long id,
        @RequestParam(required = false) String startDate,  // Optional yapıldı
        @RequestParam(required = false) String endDate,    // Optional yapıldı
        @RequestParam(defaultValue = "5") int limit) {
    
    LocalDate start = startDate != null 
        ? LocalDate.parse(startDate) 
        : LocalDate.now();
    LocalDate end = endDate != null 
        ? LocalDate.parse(endDate) 
        : LocalDate.now().plusDays(30);
    
    List<SimilarCarDto> similarCars = availabilityService
        .findSimilarAvailableCars(id, start, end, limit);
    return ResponseEntity.ok(similarCars);
}
```

### 3. N+1 Query Prevention

**CarRepository @EntityGraph Ekleme:**

```java
@EntityGraph(attributePaths = {})  // Car entity'de lazy relation yoksa boş bırak
@Query("SELECT c FROM Car c WHERE c.isDeleted = false")
Page<Car> findByIsDeletedFalse(Pageable pageable);

// Eğer gelecekte relation eklenirse:
// @EntityGraph(attributePaths = {"images", "features"})
```

**Not:** Mevcut Car entity'de lazy-loaded relation yok, bu yüzden şu an N+1 problemi kritik değil. Ancak gelecekte relation eklenirse @EntityGraph hazır olacak.

### 4. Cache Eviction Optimization

**CarServiceImpl Değişiklikleri:**

```java
// ÖNCE (Kötü - tüm cache siliniyor)
@CacheEvict(value = "cars", allEntries = true)
public CarResponse updateCar(Long id, CarRequest carRequest) { }

// SONRA (İyi - sadece ilgili entry siliniyor)
@Caching(evict = {
    @CacheEvict(value = "cars", key = "#id"),
    @CacheEvict(value = "cars", key = "'dto:' + #id"),
    @CacheEvict(value = "cars", key = "'licensePlate:' + #result.licensePlate")
})
public CarResponse updateCar(Long id, CarRequest carRequest) { }

// Liste cache'leri için ayrı eviction (create/delete'de)
@Caching(evict = {
    @CacheEvict(value = "cars", allEntries = true),  // Liste cache'leri
    @CacheEvict(value = "car-statistics", allEntries = true)
})
public CarResponse createCar(CarRequest carRequest) { }
```

### 5. View/Like Count Atomic Update

**CarRepository Atomic Update Metodları:**

```java
@Modifying
@Query("UPDATE Car c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
void incrementViewCount(@Param("id") Long id);

@Modifying
@Query("UPDATE Car c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
void incrementLikeCount(@Param("id") Long id);

@Modifying
@Query("UPDATE Car c SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END WHERE c.id = :id")
void decrementLikeCount(@Param("id") Long id);
```

**CarServiceImpl Değişiklikleri:**

```java
@Override
@Transactional
@CacheEvict(value = "cars", key = "#id")
public void incrementViewCount(Long id) {
    log.debug("Incrementing view count for car id: {}", id);
    validateCarId(id);
    
    // Atomic update - race condition yok
    carRepository.incrementViewCount(id);
    
    log.info("Successfully incremented view count for car: ID={}", id);
}
```

### 6. Seed Data Image URLs

**V16__add_seed_images.sql:**

```sql
-- Update existing seed data with placeholder images
UPDATE gallery.car SET 
    image_url = 'https://placehold.co/800x600/e2e8f0/475569?text=Volkswagen+Golf',
    thumbnail_url = 'https://placehold.co/400x300/e2e8f0/475569?text=VW+Golf'
WHERE license_plate = '34ABC123';

UPDATE gallery.car SET 
    image_url = 'https://placehold.co/800x600/1e3a5f/ffffff?text=BMW+320i',
    thumbnail_url = 'https://placehold.co/400x300/1e3a5f/ffffff?text=BMW+320i'
WHERE license_plate = '06DEF456';

UPDATE gallery.car SET 
    image_url = 'https://placehold.co/800x600/333333/ffffff?text=Audi+A4',
    thumbnail_url = 'https://placehold.co/400x300/333333/ffffff?text=Audi+A4'
WHERE license_plate = '35JKL789';

UPDATE gallery.car SET 
    image_url = 'https://placehold.co/800x600/0066cc/ffffff?text=Honda+Civic',
    thumbnail_url = 'https://placehold.co/400x300/0066cc/ffffff?text=Honda+Civic'
WHERE license_plate = '16XYZ321';

UPDATE gallery.car SET 
    image_url = 'https://placehold.co/800x600/cc0000/ffffff?text=Toyota+Corolla',
    thumbnail_url = 'https://placehold.co/400x300/cc0000/ffffff?text=Toyota+Corolla'
WHERE license_plate = '01MNO654';
```

### 7. Featured Endpoint Path Fix

**CarController'a Featured Endpoint Ekleme:**

```java
@GetMapping("/featured")
@Operation(summary = "Get featured cars")
public ResponseEntity<Page<CarResponse>> getFeaturedCars(
        @PageableDefault(size = 10) Pageable pageable,
        @RequestParam(required = false) CurrencyType currency) {
    
    Page<CarResponse> cars = carService.getFeaturedCars(pageable);
    if (currency != null) {
        cars.forEach(car -> applyPriceConversion(car, currency));
    }
    return ResponseEntity.ok(cars);
}
```

Bu sayede `/api/cars/featured` doğrudan erişilebilir olacak (CarSearchController'daki `/api/cars/search/featured` yerine).

## Data Models

Mevcut Car entity'de değişiklik yok. Sadece migration ile seed data güncelleniyor.

## Error Handling

### Sorting Errors

| Error Condition | HTTP Status | Response |
|----------------|-------------|----------|
| Invalid sort field | 400 | `{"error": "Invalid sort property: xyz"}` |
| Invalid sort direction | 400 | `{"error": "Sort direction must be 'asc' or 'desc'"}` |

### Date Parameter Errors

| Error Condition | HTTP Status | Response |
|----------------|-------------|----------|
| Invalid date format | 400 | `{"error": "Invalid date format. Use yyyy-MM-dd"}` |
| End date before start | 400 | `{"error": "End date must be after start date"}` |

## Testing Strategy

### Unit Tests

1. **Sorting Tests**
   - Test default sorting (createTime desc)
   - Test each supported sort field
   - Test invalid sort field handling

2. **Similar Cars Tests**
   - Test with dates provided
   - Test with dates omitted (default behavior)

3. **Atomic Update Tests**
   - Test incrementViewCount
   - Test incrementLikeCount
   - Test decrementLikeCount boundary (can't go below 0)

### Integration Tests

1. **CarController Sorting Integration Test**
   - Test ?sort=price,asc returns correctly sorted results
   - Test ?sort=brand,desc returns correctly sorted results

2. **Cache Eviction Integration Test**
   - Update a car, verify only that car's cache is evicted
   - Verify other cars' cache entries remain

3. **Concurrent View Count Test**
   - Simulate multiple concurrent view increments
   - Verify final count equals number of increments
