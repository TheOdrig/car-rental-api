# Design Document: Backend Bug Fixes

## Overview

Bu design dokümanı, CarGalleryProject backend'indeki kritik bug'ların ve güvenlik açıklarının düzeltilmesi için teknik tasarımı içerir. Ana odak noktaları:

1. Car filtering implementasyonu (BUG-001, BUG-007)
2. Data consistency fix (BUG-003)
3. Authorization güvenlik düzeltmeleri (SEC-001, SEC-002)
4. Return type fix (BUG-004)
5. Database index optimizasyonu (BUG-005)
6. NPE prevention (BUG-008)
7. Cache key collision prevention (BUG-009)

## Architecture

### Mevcut Mimari

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  CarController  │────▶│   CarService    │────▶│  CarRepository  │
│  (REST API)     │     │  (Business)     │     │  (Data Access)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │                      │
         │                      │
         ▼                      ▼
┌─────────────────┐     ┌─────────────────┐
│ SecurityConfig  │     │   CarMapper     │
│ (Authorization) │     │  (DTO Mapping)  │
└─────────────────┘     └─────────────────┘
```

### Değişiklik Planı

1. **CarController**: Filter parametreleri ekleme + @PreAuthorize annotation'ları
2. **CarService**: `findActiveCarsWithFilters()` metodu ekleme
3. **CarRepository**: Mevcut `findAvailableCarsForDateRange` query'sini adapte etme
4. **RentalController**: @PreAuthorize annotation'ları ekleme
5. **Database Migration**: Data fix + index ekleme

## Components and Interfaces

### 1. CarFilterRequest DTO (Yeni)

```java
package com.akif.car.internal.dto.request;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarFilterRequest {
    
    @Size(max = 50)
    private String brand;
    
    @Size(max = 50)
    private String model;
    
    @Size(max = 20)
    private String transmissionType;
    
    @Size(max = 20)
    private String bodyType;
    
    @Size(max = 20)
    private String fuelType;
    
    @Min(1)
    @Max(9)
    private Integer minSeats;
    
    @DecimalMin("0.0")
    private BigDecimal minPrice;
    
    @DecimalMax("999999999.99")
    private BigDecimal maxPrice;
    
    private CarStatusType status;
}
```

### 2. CarController Değişiklikleri

```java
// Mevcut getAllActiveCars metodunu güncelle
@GetMapping("/active")
public ResponseEntity<Page<CarResponse>> getAllActiveCars(
        @PageableDefault(size = 20) Pageable pageable,
        @RequestParam(required = false) CurrencyType currency,
        // YENİ PARAMETRELER
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) String model,
        @RequestParam(required = false) String transmissionType,
        @RequestParam(required = false) String bodyType,
        @RequestParam(required = false) String fuelType,
        @RequestParam(required = false) Integer minSeats,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice) {
    
    CarFilterRequest filter = CarFilterRequest.builder()
            .brand(brand)
            .model(model)
            .transmissionType(transmissionType)
            .bodyType(bodyType)
            .fuelType(fuelType)
            .minSeats(minSeats)
            .minPrice(minPrice)
            .maxPrice(maxPrice)
            .build();
    
    Page<CarResponse> cars = carService.findActiveCarsWithFilters(filter, pageable);
    // ... currency conversion
    return ResponseEntity.ok(cars);
}

// Authorization annotations
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<CarResponse> createCar(...) { }

@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<CarResponse> updateCar(...) { }

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> deleteCar(...) { }

@DeleteMapping("/{id}/soft")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> softDeleteCar(...) { }

@PostMapping("/{id}/restore")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<CarResponse> restoreCar(...) {
    carService.restoreCar(id);
    CarResponse car = carService.getCarById(id);  // FIX: Return car data
    return ResponseEntity.ok(car);
}
```

### 3. CarService Değişiklikleri

```java
// Interface'e ekle
Page<CarResponse> findActiveCarsWithFilters(CarFilterRequest filter, Pageable pageable);

// Implementation
@Override
@Cacheable(value = "cars", key = "'activeFiltered:' + #filter.toString() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
public Page<CarResponse> findActiveCarsWithFilters(CarFilterRequest filter, Pageable pageable) {
    log.debug("Finding active cars with filters: {}", filter);
    
    Page<Car> cars = carRepository.findActiveCarsWithFilters(
            filter.getBrand(),
            filter.getModel(),
            filter.getTransmissionType(),
            filter.getBodyType(),
            filter.getFuelType(),
            filter.getMinSeats(),
            filter.getMinPrice(),
            filter.getMaxPrice(),
            pageable
    );
    
    return cars.map(carMapper::toDto);
}

// NPE Fix in updateCar
if (!Objects.equals(existingCar.getVinNumber(), carRequest.getVinNumber())) {
    checkVinNumberUniqueness(carRequest.getVinNumber());
}
```

### 4. CarRepository Değişiklikleri

```java
@Query("SELECT c FROM Car c WHERE " +
        "c.isDeleted = false AND " +
        "(:brand IS NULL OR LOWER(c.brand) = LOWER(:brand)) AND " +
        "(:model IS NULL OR LOWER(c.model) = LOWER(:model)) AND " +
        "(:transmissionType IS NULL OR LOWER(c.transmissionType) = LOWER(:transmissionType)) AND " +
        "(:bodyType IS NULL OR LOWER(c.bodyType) = LOWER(:bodyType)) AND " +
        "(:fuelType IS NULL OR LOWER(c.fuelType) = LOWER(:fuelType)) AND " +
        "(:minSeats IS NULL OR c.seats >= :minSeats) AND " +
        "(:minPrice IS NULL OR c.price >= :minPrice) AND " +
        "(:maxPrice IS NULL OR c.price <= :maxPrice)")
Page<Car> findActiveCarsWithFilters(
        @Param("brand") String brand,
        @Param("model") String model,
        @Param("transmissionType") String transmissionType,
        @Param("bodyType") String bodyType,
        @Param("fuelType") String fuelType,
        @Param("minSeats") Integer minSeats,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable);
```

### 5. RentalController Authorization

```java
@PostMapping("/{id}/confirm")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<RentalResponse> confirmRental(...) { }

@PostMapping("/{id}/pickup")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<RentalResponse> pickupRental(...) { }

@PostMapping("/{id}/return")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<RentalResponse> returnRental(...) { }
```

## Data Models

### Database Migration V15

```sql
-- V15__fix_data_and_add_indexes.sql

-- Fix data inconsistency: "Manuel" -> "Manual"
UPDATE gallery.car 
SET transmission_type = 'Manual' 
WHERE LOWER(transmission_type) = 'manuel';

-- Add missing indexes for filter performance
CREATE INDEX IF NOT EXISTS idx_car_transmission_type ON gallery.car(transmission_type);
CREATE INDEX IF NOT EXISTS idx_car_body_type ON gallery.car(body_type);
CREATE INDEX IF NOT EXISTS idx_car_fuel_type ON gallery.car(fuel_type);
CREATE INDEX IF NOT EXISTS idx_car_seats ON gallery.car(seats);
```

## Error Handling

### Filter Validation Errors

| Error Condition | HTTP Status | Response |
|----------------|-------------|----------|
| Invalid minPrice (negative) | 400 | `{"error": "minPrice must be >= 0"}` |
| Invalid minSeats (< 1 or > 9) | 400 | `{"error": "minSeats must be between 1 and 9"}` |
| minPrice > maxPrice | 400 | `{"error": "minPrice cannot exceed maxPrice"}` |

### Authorization Errors

| Error Condition | HTTP Status | Response |
|----------------|-------------|----------|
| Non-admin attempts write | 403 | `{"error": "Access Denied"}` |
| Unauthenticated request | 401 | `{"error": "Unauthorized"}` |

## Testing Strategy

### Unit Tests

1. **CarFilterRequest Validation Tests**
   - Test each validation constraint
   - Test builder pattern

2. **CarServiceImpl Tests**
   - Test `findActiveCarsWithFilters` with various filter combinations
   - Test null-safe VIN comparison
   - Test cache key generation

3. **CarRepository Tests**
   - Test `findActiveCarsWithFilters` query with mock data

### Integration Tests

1. **CarController Integration Tests**
   - Test filter endpoint with real database
   - Test authorization with different user roles
   - Test filter combinations (brand + transmission, price range, etc.)

2. **RentalController Integration Tests**
   - Test authorization for admin operations
   - Test user can only cancel own rentals

3. **Database Migration Tests**
   - Verify "Manuel" → "Manual" data fix
   - Verify indexes are created
