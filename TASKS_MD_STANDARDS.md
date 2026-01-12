# Tasks.md Standards - Best Practices & Anti-Patterns

## Overview

Bu döküman, spec dosyalarındaki `tasks.md` yazımı için kuralları tanımlar.
Amaç: Her task sonunda **proje compile olmalı** ve **modüler boundary'ler korunmalı**.

---

## Related Documents

| Döküman | Amaç |
|---------|------|
| [git-workflow-generator.md](../../GitFlow/git-workflow-generator.md) | Git commit planı oluşturma kuralları |
| [java-spring-boot-main-standards.md](./java-spring-boot-main-standards.md) | Spring Boot kod standartları |
| [DEVELOPER_GUIDE.md](../../docs/architecture/DEVELOPER_GUIDE.md) | Modül oluşturma ve mimari rehberi |

---

## Golden Rules

### 1. Her Task Compile Olmalı
```
Task N tamamlandıktan sonra: mvn compile → BUILD SUCCESS
```

### 2. Dependency Order
```
Types (Enums, DTOs) → Interfaces → Repositories → Implementations → Controllers → Tests
```

### 3. Cross-Module Boundary
```
Module A → Module B API çağırabilir
Module A ← Module B DTO'su DÖNEMEZ (Module A'nın DTO'sunu döner)
```

---

## Anti-Patterns (Yapılmaması Gerekenler)

### ❌ 1. Checkpoint Task Yazmak

```markdown
# YANLIŞ
- [ ] 11. Checkpoint - Make sure all tests are passing
- [ ] 14. Final Checkpoint - Verify everything works
```

**Neden yanlış?**
- Bu bir task değil, workflow step
- CI/CD'nin işi
- Tasks.md = kod değişikliği gerektiren işler

**Doğrusu:**
- Checkpoint'leri tamamen sil
- Veya ayrı bir "Verification Steps" bölümü yap (checkbox olmadan)

---

### ❌ 2. Cross-Module DTO Return Type

```markdown
# YANLIŞ
- [ ] 5.1 Add findPendingApprovals(Pageable) to RentalService
  - Returns Page<PendingItemDto>  ← Dashboard DTO'su rental'dan dönüyor!
```

**Neden yanlış?**
- `RentalService` (rental module) → `PendingItemDto` (dashboard module)
- Bu circular dependency yaratır
- Modüler boundary ihlali

**Doğrusu:**
```markdown
- [ ] 5.1 Add findPendingApprovals(Pageable) to RentalService
  - Returns Page<RentalResponse> (rental's own DTO)
  - Dashboard maps RentalResponse → PendingItemDto internally
```

---

### ❌ 3. Belirsiz Return Type

```markdown
# YANLIŞ
- [ ] 5.1 Add dashboard query methods to RentalService API
  - Add countByStatus() method
  - Add findPendingApprovals() method
```

**Neden yanlış?**
- Return type belli değil
- Implementation sırasında hata yapılabilir

**Doğrusu:**
```markdown
- [ ] 5.1 Add dashboard query methods to RentalService API
  - Add countByStatus(RentalStatus): int
  - Add findPendingApprovals(Pageable): Page<RentalResponse>
```

---

### ❌ 4. Implementation Öncesi Test Task

```markdown
# YANLIŞ (sıra)
- [ ] 6.1 Write DashboardServiceImpl unit tests
- [ ] 6.2 Implement DashboardServiceImpl
```

**Neden yanlış?**
- Test yazılacak class yok → compile hatası
- TDD yapsan bile, interface önce lazım

**Doğrusu:**
```markdown
- [ ] 6.1 Implement DashboardServiceImpl
- [ ] 6.2 Write DashboardServiceImpl unit tests
```

---

### ❌ 5. Tek Task'ta Çok Fazla İş

```markdown
# YANLIŞ
- [ ] 10.1 Implement all controllers
  - Add DashboardController with 8 endpoints
  - Add AlertController with 2 endpoints
  - Add QuickActionController with 3 endpoints
  - Add OpenAPI documentation
  - Add authorization
```

**Neden yanlış?**
- Atomic değil
- Code review zorlaşır
- Rollback imkansız

**Doğrusu:**
```markdown
- [ ] 10.1 Implement DashboardController
  - GET /api/admin/dashboard/summary
  - GET /api/admin/dashboard/fleet
  - GET /api/admin/dashboard/metrics
  - GET /api/admin/dashboard/revenue
  
- [ ] 10.2 Implement DashboardController pending endpoints
  - GET /api/admin/dashboard/pending/approvals
  - GET /api/admin/dashboard/pending/pickups
  - GET /api/admin/dashboard/pending/returns
  - GET /api/admin/dashboard/pending/overdue

- [ ] 10.3 Implement AlertController
- [ ] 10.4 Implement QuickActionController
```

---

### ❌ 6. Missing Compile Dependencies

```markdown
# YANLIŞ
- [ ] 3.1 Create DashboardService interface
  - Define getDailySummary(): DailySummaryDto  ← Bu DTO nerede tanımlı?

- [ ] 2.1 Create DTOs  ← Interface'den SONRA!
```

**Neden yanlış?**
- Interface, DTO'ya referans veriyor
- Ama DTO henüz tanımlı değil
- Compile hatası

**Doğrusu:**
```markdown
- [ ] 2.1 Create DTOs (DailySummaryDto, FleetStatusDto, ...)
- [ ] 3.1 Create DashboardService interface
  - Define getDailySummary(): DailySummaryDto
```

---

### ❌ 7. Entity + Migration Ayrı Ama Yanlış Sırada

```markdown
# YANLIŞ
- [ ] 1.3 Create Flyway migration V13
- [ ] 1.2 Create Alert entity  ← Migration'dan SONRA entity?
```

**Neden yanlış?**
- Migration entity'ye bakarak yazılır
- Entity yokken migration yazmak anlamsız

**Doğrusu:**
```markdown
- [ ] 1.2 Create Alert entity
- [ ] 1.3 Create Flyway migration V13
```

---

### ❌ 8. Property-Based Test Task Yazmak

```markdown
# YANLIŞ
- [ ] 7.1 Write property-based tests using JQwik
- [ ] 7.2 Add QuickTheories for randomized testing
```

**Neden yanlış?**
- Spring Boot ekosisteminde **property-based testing önerilmiyor**
- JQwik, QuickTheories gibi kütüphaneler ek complexity ekler
- Spring Boot test piramidi: Unit → Integration → E2E
- Aynı coverage'ı geleneksel testlerle alabilirsin

**Doğrusu:**
```markdown
- [ ] 7.1 Write unit tests with edge cases
- [ ] 7.2 Write integration tests for controller layer
- [ ] 7.3 Write E2E tests for critical flows
```

**Not:** Property-based testing şu durumlarda düşünülebilir:
- Algoritma-yoğun kod (parsing, serialization)
- Domain-specific kurallar (pure functions)
- AMA Spring Boot CRUD uygulamalarında gereksiz overhead

---

### ❌ 9. Spring Modulith `::api` Syntax Kullanmak

```markdown
# YANLIŞ
- [ ] 1.1 Create package-info.java with:
  @ApplicationModule(allowedDependencies = {"rental::api", "car::api"})
```

**Neden yanlış?**
- `@NamedInterface("api")` zaten `api/` paketini PUBLIC yapar
- Tüm **sub-package'ler DEFAULT olarak INTERNAL'dır**
- `::api` yazmak **gereksiz verbose**
- Proje genelinde tutarsızlık yaratır

**Spring Modulith Visibility Kuralları:**

| Paket | Visibility |
|-------|------------|
| Root package | PUBLIC (varsayılan API) |
| `api/` paketi (`@NamedInterface` ile) | PUBLIC |
| Diğer tüm sub-packages (`domain/`, `web/`, `internal/`) | **INTERNAL** |

**Doğrusu:**
```markdown
- [ ] 1.1 Create package-info.java with:
  @ApplicationModule(allowedDependencies = {"rental", "car", "payment"})
```

**Önemli:** `allowedDependencies = {"rental"}` otomatik olarak sadece:
- Root package + `@NamedInterface` ile işaretli paketlere erişim sağlar
- `domain/`, `internal/`, `web/` paketleri erişilemez kalır

---

## Correct Task Structure Template

```markdown
## 1. Foundation (Types & Config)
- [ ] 1.1 Create enums (Type, Status, etc.)
- [ ] 1.2 Create configuration classes
- [ ] 1.3 Create entities with proper annotations
- [ ] 1.4 Create Flyway migrations

## 2. API Layer (DTOs & Interfaces)
- [ ] 2.1 Create request/response DTOs as records
- [ ] 2.2 Create public API interfaces with explicit return types

## 3. Data Layer
- [ ] 3.1 Create repositories with custom query methods

## 4. Cross-Module Extensions (if needed)
- [ ] 4.1 Extend ModuleX API with query methods
  - methodName(params): ReturnType (module's own DTO)
  - Note: Caller maps to its own DTOs internally

## 5. Business Logic
- [ ] 5.1 Implement InternalHelperService
- [ ] 5.2 Implement PublicApiServiceImpl
- [ ] 5.3 Write unit tests for services

## 6. Presentation Layer
- [ ] 6.1 Implement controllers (max 4 endpoints per task)
- [ ] 6.2 Add OpenAPI annotations

## 7. Integration Tests
- [ ] 7.1 Write controller integration tests

## 8. E2E Tests
- [ ] 8.1 Write E2E tests (max 3-4 scenarios per task)

## 9. Documentation
- [ ] 9.1 Update relevant documentation files
```

---

## Checklist (Her tasks.md İçin)

- [ ] Her task sonunda proje compile oluyor mu?
- [ ] Dependency sırası doğru mu? (Types → Interfaces → Impl → Controllers → Tests)
- [ ] Cross-module return type'lar modülün kendi DTO'ları mı?
- [ ] Return type'lar explicit yazılmış mı?
- [ ] Checkpoint task'ları var mı? (varsa sil)
- [ ] Her task atomic mi? (tek bir logical change)
- [ ] Entity ve migration sırası doğru mu?
- [ ] Test task'ları implementation'dan sonra mı?
- [ ] Property-based test task'ı var mı? (varsa sil, Unit/Integration/E2E yap)
- [ ] `allowedDependencies`'de `::api` var mı? (varsa kaldır, gereksiz)

---

## Quick Reference

| Soru | Cevap |
|------|-------|
| Checkpoint task yazmalı mıyım? | ❌ Hayır, CI'ın işi |
| Cross-module method ne döner? | Modülün kendi DTO'su |
| Max endpoint per task? | 4 |
| Max test scenario per task? | 3-4 |
| Test task nerede olmalı? | Implementation'dan sonra |
| Entity + Migration aynı task mı? | Evet veya Entity önce |
| Property-based test yazmalı mıyım? | ❌ Hayır, Unit/Integration/E2E yeterli |
| `::api` syntax kullanmalı mıyım? | ❌ Hayır, `@NamedInterface` yeterli |

---

**Son Güncelleme:** 2025-12-16 | **Durum:** ✅ Aktif

