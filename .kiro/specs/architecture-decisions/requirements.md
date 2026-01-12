# Requirements: Architecture Decisions Documentation

## Overview

Bu spec, Car Rental API projesinin eksik Architecture Decision Records (ADR) dokümantasyonunu oluşturmayı hedefler. Mevcut 5 ADR'ye (001-005) ek olarak 3 kritik business logic ADR'si yazılacak.

## Actors

- **Developer**: ADR'leri okuyarak mimari kararları anlayan geliştirici
- **Tech Lead**: Mimari kararları değerlendiren ve onaylayan teknik lider
- **New Team Member**: Projeye yeni katılan ve kararların gerekçelerini öğrenmek isteyen kişi

## User Stories

### Story 1: Dynamic Pricing Strategy Documentation (ADR-006)

**As a** developer  
**I want** to understand why Strategy Pattern was chosen for dynamic pricing  
**So that** I can extend or modify pricing logic correctly

**Acceptance Criteria:**
- ADR-006 dosyası `docs/architecture/adr/` dizininde oluşturulmuş
- 5 pricing strategy (Season, EarlyBooking, Duration, Weekend, Demand) açıklanmış
- Strategy Pattern seçim gerekçesi belirtilmiş
- Alternatifler (Rule Engine, ML-based, Hardcoded) değerlendirilmiş
- Strategy execution order (1-5) ve chain of responsibility açıklanmış
- Price caps (min/max daily price) mekanizması belgelenmiş
- PricingConfig yapılandırması referans verilmiş

### Story 2: Late Return Penalty Documentation (ADR-007)

**As a** developer  
**I want** to understand the penalty calculation business rules  
**So that** I can maintain or adjust penalty logic when needed

**Acceptance Criteria:**
- ADR-007 dosyası `docs/architecture/adr/` dizininde oluşturulmuş
- Grace period (60 dakika) gerekçesi açıklanmış
- Hourly penalty rate (%10) ve threshold (1-6 saat) belgelenmiş
- Daily penalty rate (%150) ve threshold (>6 saat) belgelenmiş
- Penalty cap (5× daily rate) gerekçesi açıklanmış
- LateReturnStatus enum değerleri (ON_TIME, GRACE_PERIOD, LATE, SEVERELY_LATE) açıklanmış
- PenaltyConfig validation kuralları referans verilmiş
- Alternatif modeller (flat fee, progressive, no cap) değerlendirilmiş

### Story 3: Authentication Strategy Documentation (ADR-008)

**As a** developer  
**I want** to understand the JWT + OAuth2 authentication architecture  
**So that** I can implement secure authentication flows correctly

**Acceptance Criteria:**
- ADR-008 dosyası `docs/architecture/adr/` dizininde oluşturulmuş
- JWT token yapısı (access + refresh) açıklanmış
- Token expiration süreleri ve gerekçeleri belirtilmiş
- OAuth2 provider seçimi (Google, GitHub) gerekçelendirilmiş
- Token claims (userId, roles) belgelenmiş
- HMAC-SHA signing strategy açıklanmış
- Alternatifler (Session-based, Opaque tokens, PASETO) değerlendirilmiş
- Security considerations (token storage, refresh flow) belirtilmiş

## Functional Requirements

### FR-1: ADR Format Consistency
- Tüm ADR'ler mevcut format ile tutarlı olmalı (ADR-003 referans)
- Sections: Status, Context, Decision, Rationale, Consequences, Related ADRs
- Mermaid diyagramları kullanılabilir

### FR-2: Code References
- Her ADR ilgili kod dosyalarını referans vermeli
- Configuration sınıfları ve default değerler belirtilmeli

### FR-3: Alternatives Evaluation
- Her karar için en az 3 alternatif değerlendirilmeli
- Pros/Cons tablosu ile karşılaştırma yapılmalı

## Non-Functional Requirements

### NFR-1: Readability
- ADR'ler 5 dakikada okunabilir uzunlukta olmalı
- Teknik jargon açıklanmalı

### NFR-2: Maintainability
- ADR'ler güncellenebilir yapıda olmalı
- Version history için Status section kullanılmalı

## Out of Scope

- ADR-009 ile ADR-013 (Currency, Email, Caching, Database, Alert) bu spec kapsamında değil
- Mevcut ADR'lerin (001-005) güncellenmesi
- UML diyagramları (ayrı spec'te ele alınacak)

## Dependencies

- Mevcut ADR formatı: `docs/architecture/adr/ADR-003-event-driven-communication.md`
- Pricing implementation: `com.akif.car.internal.service.pricing.*`
- Pricing config: `com.akif.rental.internal.config.PricingConfig`
- Penalty implementation: `com.akif.rental.internal.service.penalty.*`
- JWT implementation: `com.akif.shared.security.JwtTokenProvider`
- OAuth2 implementation: `com.akif.auth.internal.service.oauth2.*`
