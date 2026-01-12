# Requirements: API Documentation

## Overview

Bu spec, Car Rental API projesinin eksik API dokümantasyonunu oluşturmayı hedefler. Error codes, API conventions ve rate limiting dokümantasyonu eklenecek.

## Actors

- **API Consumer**: API'yi kullanan frontend/mobile developer
- **Integration Partner**: API'yi entegre eden 3rd party developer
- **Backend Developer**: API'yi geliştiren ve bakımını yapan developer

## User Stories

### Story 1: Error Codes Documentation

**As an** API consumer  
**I want** to see all possible error codes and their meanings  
**So that** I can handle errors properly in my application

**Acceptance Criteria:**
- `docs/api/ERROR_CODES.md` dosyası oluşturulmuş
- Tüm BaseException subclass'ları listelenmiş
- Her error code için: code, HTTP status, description, solution
- Module bazlı gruplandırma (auth, car, rental, payment, damage, dashboard)
- Örnek error response JSON formatı gösterilmiş

### Story 2: API Conventions Documentation

**As an** API consumer  
**I want** to understand API conventions and standards  
**So that** I can correctly format requests and parse responses

**Acceptance Criteria:**
- `docs/api/API_CONVENTIONS.md` dosyası oluşturulmuş
- Pagination format (page, size, sort) belgelenmiş
- Date/time format (ISO 8601) belgelenmiş
- Error response format belgelenmiş
- Naming conventions (camelCase, kebab-case) belgelenmiş
- Authentication header format belgelenmiş
- Content-Type requirements belgelenmiş

### Story 3: Rate Limiting Documentation

**As an** API consumer  
**I want** to understand rate limiting policies  
**So that** I can design my application to handle rate limits gracefully

**Acceptance Criteria:**
- `docs/api/RATE_LIMITING.md` dosyası oluşturulmuş
- Endpoint kategorileri ve limitleri belgelenmiş (varsa)
- Rate limit header'ları açıklanmış
- 429 Too Many Requests response handling belgelenmiş
- Retry-After header kullanımı açıklanmış
- Best practices for avoiding rate limits

## Functional Requirements

### FR-1: Error Code Completeness
- Tüm exception sınıfları dokümante edilmeli
- ERROR_CODE constant'ları kullanılmalı
- HTTP status mapping doğru olmalı

### FR-2: Convention Consistency
- Mevcut API davranışı ile tutarlı olmalı
- Swagger/OpenAPI ile uyumlu olmalı

### FR-3: Practical Examples
- Her section için örnek request/response
- cURL örnekleri dahil edilmeli

## Non-Functional Requirements

### NFR-1: Discoverability
- Table of contents ile kolay navigasyon
- Anchor links ile section'lara atlama

### NFR-2: Maintainability
- Yeni error code eklendiğinde güncelleme kolay
- Template formatı tutarlı

## Out of Scope

- Swagger/OpenAPI spec güncellenmesi
- Postman Collection oluşturulması
- API versioning stratejisi (ayrı spec)
- Webhook documentation (operations spec'te)

## Dependencies

- BaseException: `com.akif.shared.exception.BaseException`
- GlobalExceptionHandler: `com.akif.shared.exception.GlobalExceptionHandler`
- Mevcut Swagger UI: `/swagger-ui.html`
- Exception sınıfları: `*Exception.java` dosyaları
