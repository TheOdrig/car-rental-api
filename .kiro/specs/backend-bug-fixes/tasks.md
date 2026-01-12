# Implementation Plan: Backend Bug Fixes

## Overview

Bu plan, CarGalleryProject backend'indeki kritik bug'ların düzeltilmesi için adım adım implementasyon görevlerini içerir. Görevler bağımlılık sırasına göre düzenlenmiştir.

## Tasks

- [x] 1. Database Migration - Data Fix ve Index Ekleme
  - [x] 1.1 V15__fix_data_and_add_indexes.sql migration dosyası oluştur
    - "Manuel" → "Manual" data fix
    - transmission_type, body_type, fuel_type, seats için index ekleme
    - _Requirements: 2.1, 2.2, 2.3, 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 2. CarFilterRequest DTO Oluşturma
  - [x] 2.1 ~~CarFilterRequest sınıfını oluştur~~ → Mevcut `CarSearchRequest` kullanılacak
    - `com.akif.car.internal.dto.request` paketinde zaten mevcut
    - brand, model, transmissionType, bodyType, fuelType, minSeats, minPrice, maxPrice alanları VAR
    - Validation annotation'ları mevcut
    - _Requirements: 1.1_

- [x] 3. CarRepository Filter Query Ekleme
  - [x] 3.1 ~~findActiveCarsWithFilters query metodunu ekle~~ → Mevcut `findCarsByCriteria` genişletildi
    - transmissionType, bodyType, fuelType, minSeats parametreleri eklendi
    - Tüm filter parametrelerini destekleyen JPQL query
    - NULL parametreler için optional filtering
    - _Requirements: 1.2, 1.4_

- [x] 4. CarService Filter Implementasyonu
  - [x] 4.1 ~~CarService interface'ine findActiveCarsWithFilters metodunu ekle~~ → Mevcut `searchCars` kullanılıyor
    - _Requirements: 1.2_
  - [x] 4.2 ~~CarServiceImpl'de findActiveCarsWithFilters implementasyonu~~ → Mevcut `searchCars` zaten filter destekliyor
    - Cache annotation mevcut
    - _Requirements: 1.2, 1.3, 1.4_
  - [x] 4.3 NPE fix - updateCar metodunda Objects.equals kullanıldı
    - _Requirements: 7.1_
  - [x] 4.4 Cache key fix - searchCars metodunda toString() kullanıldı
    - _Requirements: 8.1, 8.2_
  - [x] 4.5 restoreCar metodunu CarResponse dönecek şekilde güncellendi
    - _Requirements: 5.1, 5.2_

- [x] 5. CarController Filter ve Authorization Güncellemesi
  - [x] 5.1 ~~getAllActiveCars metoduna filter parametreleri ekle~~ → Mevcut `searchCars` endpoint filter destekliyor
    - CarSearchController zaten filter desteği sağlıyor
    - _Requirements: 1.1, 1.3_
  - [x] 5.2 createCar metoduna @PreAuthorize("hasRole('ADMIN')") eklendi
    - _Requirements: 3.1_
  - [x] 5.3 updateCar metoduna @PreAuthorize("hasRole('ADMIN')") eklendi
    - _Requirements: 3.2_
  - [x] 5.4 deleteCar metoduna @PreAuthorize("hasRole('ADMIN')") eklendi
    - _Requirements: 3.3_
  - [x] 5.5 softDeleteCar metoduna @PreAuthorize("hasRole('ADMIN')") eklendi
    - _Requirements: 3.4_
  - [x] 5.6 restoreCar metodunu güncellendi - @PreAuthorize eklendi ve CarResponse dönüyor
    - _Requirements: 3.5, 5.1, 5.2_

- [x] 6. RentalController Authorization Güncellemesi
  - [x] 6.1 confirmRental metoduna @PreAuthorize("hasRole('ADMIN')") eklendi
    - _Requirements: 4.1_
  - [x] 6.2 pickupRental metoduna @PreAuthorize("hasRole('ADMIN')") eklendi
    - _Requirements: 4.2_
  - [x] 6.3 returnRental metoduna @PreAuthorize("hasRole('ADMIN')") eklendi
    - _Requirements: 4.3_

- [x] 7. Checkpoint - Build ve Test ✅
  - Proje derlendi ve mevcut testler geçti
  - `mvn clean test` → Exit code: 0

- [x] 8. Unit Test Yazımı
  - [x] 8.1 ~~CarFilterRequest validation testleri~~ → Mevcut CarSearchRequest kullanıldığı için gerek yok
    - _Requirements: 1.5_
  - [x] 8.2 CarServiceImplBugFixTest oluşturuldu
    - NPE fix testi (VIN null senaryoları)
    - restoreCar CarResponse dönüş testi
    - _Requirements: 1.2, 1.3, 1.4, 7.1, 5.1, 5.2_

- [x] 9. Integration Test Yazımı
  - [x] 9.1 ~~CarController filter endpoint integration testi~~ → Mevcut testler filter'ı destekliyor
    - _Requirements: 1.1, 1.2_
  - [x] 9.2 CarControllerAuthorizationTest oluşturuldu
    - Admin ve non-admin senaryoları (403 Forbidden)
    - Read operasyonları tüm kullanıcılara açık
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  - [x] 9.3 RentalControllerAuthorizationTest oluşturuldu
    - confirmRental, pickupRental, returnRental - ADMIN only
    - requestRental, cancelRental - USER allowed
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [x] 10. Final Checkpoint ✅
  - Tüm testler geçti
  - `mvn test` → Exit code: 0

## Notes

- Migration dosyası V15 numarasıyla oluşturulacak (V14 mevcut son migration)
- @PreAuthorize için `@EnableMethodSecurity` annotation'ının SecurityConfig'de aktif olduğundan emin ol
- Filter query'si case-insensitive olmalı (LOWER fonksiyonu kullanılacak)
- Tüm değişiklikler backward compatible olmalı (filter parametreleri optional)
