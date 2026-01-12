# Requirements Document

## Introduction

Bu spec, CarGalleryProject backend'indeki kritik bug'ları ve güvenlik açıklarını düzeltmeyi amaçlar. Bug analiz raporlarında tespit edilen 18 bug'dan kritik ve yüksek öncelikli olanlar bu spec kapsamında ele alınacaktır.

## Glossary

- **CarController**: Araba CRUD operasyonlarını yöneten REST controller
- **RentalController**: Kiralama operasyonlarını yöneten REST controller
- **CarService**: Araba iş mantığını içeren servis katmanı
- **CarRepository**: Araba veritabanı erişim katmanı
- **Filter_Parameters**: Frontend'den gelen filtreleme parametreleri (brand, transmissionType, bodyType, fuelType, minSeats, minPrice, maxPrice)
- **JPA_Specification**: Spring Data JPA'da dinamik query oluşturmak için kullanılan pattern
- **PreAuthorize**: Spring Security'de method-level authorization sağlayan annotation

## Requirements

### Requirement 1: Car Filtering Implementation

**User Story:** As a user, I want to filter cars by various criteria (transmission, body type, fuel type, etc.), so that I can find cars that match my preferences.

#### Acceptance Criteria

1. WHEN a user sends a GET request to `/api/cars/active` with filter parameters, THE CarController SHALL accept and process all filter parameters (brand, model, transmissionType, bodyType, fuelType, minSeats, minPrice, maxPrice)
2. WHEN filter parameters are provided, THE CarService SHALL return only cars matching ALL specified criteria
3. WHEN no filter parameters are provided, THE CarController SHALL return all active cars (backward compatible)
4. WHEN multiple filters are combined, THE System SHALL apply AND logic between all filters
5. IF an invalid filter value is provided, THEN THE System SHALL return a 400 Bad Request with descriptive error message

### Requirement 2: Data Consistency Fix

**User Story:** As a system administrator, I want consistent data values in the database, so that filtering works correctly.

#### Acceptance Criteria

1. THE Database_Migration SHALL update all "Manuel" values to "Manual" in transmission_type column
2. THE Database_Migration SHALL be idempotent (safe to run multiple times)
3. WHEN the migration completes, THE System SHALL have consistent English spelling for all transmission types

### Requirement 3: CarController Authorization

**User Story:** As a system administrator, I want CRUD operations on cars to be restricted to admin users, so that unauthorized users cannot modify car data.

#### Acceptance Criteria

1. WHEN a non-admin user attempts to create a car (POST /api/cars), THE System SHALL return 403 Forbidden
2. WHEN a non-admin user attempts to update a car (PUT /api/cars/{id}), THE System SHALL return 403 Forbidden
3. WHEN a non-admin user attempts to delete a car (DELETE /api/cars/{id}), THE System SHALL return 403 Forbidden
4. WHEN a non-admin user attempts to soft-delete a car (DELETE /api/cars/{id}/soft), THE System SHALL return 403 Forbidden
5. WHEN a non-admin user attempts to restore a car (POST /api/cars/{id}/restore), THE System SHALL return 403 Forbidden
6. WHEN an admin user performs any CRUD operation, THE System SHALL allow the operation
7. THE System SHALL allow all authenticated users to read car data (GET operations)

### Requirement 4: RentalController Authorization

**User Story:** As a system administrator, I want rental management operations to be restricted to admin users, so that only authorized personnel can confirm, pickup, and return rentals.

#### Acceptance Criteria

1. WHEN a non-admin user attempts to confirm a rental (POST /api/rentals/{id}/confirm), THE System SHALL return 403 Forbidden
2. WHEN a non-admin user attempts to process pickup (POST /api/rentals/{id}/pickup), THE System SHALL return 403 Forbidden
3. WHEN a non-admin user attempts to process return (POST /api/rentals/{id}/return), THE System SHALL return 403 Forbidden
4. WHEN an admin user performs rental management operations, THE System SHALL allow the operation
5. THE System SHALL allow users to cancel their own rentals
6. THE System SHALL allow admins to cancel any rental

### Requirement 5: RestoreCar Return Type Fix

**User Story:** As a frontend developer, I want the restoreCar endpoint to return the restored car data, so that I can update the UI correctly.

#### Acceptance Criteria

1. WHEN a car is successfully restored, THE CarController SHALL return the restored CarResponse with HTTP 200
2. THE Response body SHALL contain the complete car data after restoration

### Requirement 6: Database Index Optimization

**User Story:** As a system administrator, I want proper database indexes on filter columns, so that filter queries perform efficiently.

#### Acceptance Criteria

1. THE Database_Migration SHALL create index on transmission_type column
2. THE Database_Migration SHALL create index on body_type column
3. THE Database_Migration SHALL create index on fuel_type column
4. THE Database_Migration SHALL create index on seats column
5. THE Indexes SHALL be created with IF NOT EXISTS clause for idempotency

### Requirement 7: NPE Prevention in VIN Check

**User Story:** As a developer, I want null-safe VIN number comparison, so that the system doesn't crash when VIN is null.

#### Acceptance Criteria

1. WHEN updating a car with null VIN number, THE CarService SHALL handle the comparison safely without throwing NullPointerException
2. THE System SHALL use Objects.equals() for null-safe comparison

### Requirement 8: Cache Key Collision Prevention

**User Story:** As a developer, I want unique cache keys for search requests, so that different searches don't return cached results from other searches.

#### Acceptance Criteria

1. THE CarService SHALL use a collision-resistant cache key strategy for search operations
2. THE Cache key SHALL be based on all search parameters, not just hashCode()