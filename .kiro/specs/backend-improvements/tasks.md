# Implementation Plan: Backend Improvements

## Overview

Bu plan, CarGalleryProject backend'indeki medium/low priority bug'ların ve performans iyileştirmelerinin implementasyonunu içerir.

## Tasks

- [x] 1. Database Migration - Seed Data Image URLs ✅
  - [x] 1.1 V16__add_seed_images.sql migration dosyası oluştur
    - Mevcut seed data'ya image_url ve thumbnail_url ekle
    - Placeholder image URL'leri kullan
    - _Requirements: 6.1, 6.2, 6.3_

- [x] 2. Atomic View/Like Count Updates ✅
  - [x] 2.1 CarRepository'ye atomic update metodları ekle
    - incrementViewCount(@Param("id") Long id) - @Modifying @Query
    - incrementLikeCount(@Param("id") Long id) - @Modifying @Query
    - decrementLikeCount(@Param("id") Long id) - @Modifying @Query (0'ın altına düşmemeli)
    - _Requirements: 5.1, 5.2, 5.3_
  - [x] 2.2 CarServiceImpl'de view/like metodlarını atomic update kullanacak şekilde güncelle
    - Mevcut read-modify-write pattern'i kaldır
    - Repository atomic metodlarını çağır
    - _Requirements: 5.2, 5.3_

- [x] 3. Cache Eviction Optimization ✅
  - [x] 3.1 CarServiceImpl updateCar metodunda cache eviction'ı optimize et
    - allEntries=true yerine spesifik key'ler kullan
    - _Requirements: 4.1, 4.2_
  - [x] 3.2 CarServiceImpl incrementViewCount/incrementLikeCount cache eviction'ı optimize et
    - Sadece ilgili car'ın cache'ini sil
    - _Requirements: 4.1, 4.2_

- [x] 4. Similar Cars Endpoint Fix ✅
  - [x] 4.1 AvailabilitySearchController getSimilarAvailableCars metodunu güncelle
    - startDate ve endDate parametrelerini optional yap
    - Default değerler: today ve today+30
    - _Requirements: 2.1, 2.2, 2.3_

- [x] 5. Featured Endpoint Path Fix ✅
  - [x] 5.1 CarController'a /featured endpoint ekle
    - getFeaturedCars metodu
    - Mevcut CarService.getFeaturedCars() metodunu kullan
    - Currency conversion desteği
    - _Requirements: 7.3_

- [x] 6. Sorting Implementation ✅
  - [x] 6.1 CarController getAllActiveCars metodunda sorting'i aktif et
    - @PageableDefault'a default sort ekle (createTime, DESC)
    - Spring Pageable otomatik sort parametrelerini handle ediyor
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  - [x] 6.2 Swagger/OpenAPI dokümantasyonuna desteklenen sort alanlarını ekle
    - _Requirements: 1.2_

- [x] 7. N+1 Query Prevention (Hazırlık) ✅
  - [x] 7.1 CarRepository findByIsDeletedFalse metoduna @EntityGraph ekle
    - Şu an lazy relation yok, gelecek için hazırlık
    - _Requirements: 3.1, 3.2, 3.3_

- [x] 8. Checkpoint - Build ve Test
  - Projeyi derle ve mevcut testlerin geçtiğinden emin ol
  - Hata varsa kullanıcıya sor

- [x] 9. Unit Test Yazımı ✅
  - [x] 9.1 Atomic update metodları için unit test yaz
    - incrementViewCount, incrementLikeCount, decrementLikeCount
    - _Requirements: 5.1, 5.2, 5.3_
  - [x] 9.2 Similar cars default date logic için unit test yaz
    - _Requirements: 2.1, 2.2_

- [x] 10. Integration Test Yazımı ✅
  - [x] 10.1 Sorting integration testi yaz
    - Farklı sort parametreleri test et
    - _Requirements: 1.1, 1.2, 1.3_
  - [x] 10.2 Featured endpoint integration testi yaz
    - _Requirements: 7.3_

- [x] 11. Final Checkpoint ✅
  - Tüm testlerin geçtiğinden emin ol
  - Hata varsa kullanıcıya sor

## Notes

- Migration V16 numarasıyla oluşturulacak (V15 önceki spec'te kullanıldı)
- Atomic update'ler @Modifying annotation gerektirir
- Spring Pageable sorting için ?sort=field,direction formatı kullanılır (örn: ?sort=price,asc)
- @EntityGraph şu an boş olabilir çünkü Car entity'de lazy relation yok
