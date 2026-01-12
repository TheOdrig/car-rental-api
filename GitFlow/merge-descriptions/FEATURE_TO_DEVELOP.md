# Feature → Develop Merge Description Template

> Feature branch'ini develop'a merge ederken kullan.
> Bu format Conventional Commits standardına uygun ve CI/CD entegrasyonu için optimize edilmiştir.

---

## PR Title Format

```
<type>(<scope>): <short description>
```

**Type Seçenekleri:**
- `feat` → Yeni özellik
- `fix` → Bug düzeltme
- `refactor` → Kod iyileştirme (feature/fix değil)
- `docs` → Dokümantasyon
- `test` → Test ekleme/düzeltme
- `chore` → Build, config değişiklikleri

**Örnekler:**
```
feat(damage): implement damage management system
feat(payment): add Stripe webhook handling
fix(rental): resolve date overlap validation
refactor(auth): simplify JWT token generation
docs(readme): add damage management section
```

---

## PR Description Template

Aşağıdaki template'i kopyala ve doldur:

```markdown
## Summary
[1-2 cümle: Bu PR ne yapıyor?]

## Why
[Neden bu değişiklik gerekli? Problem neydi?]

## What Changed
[Ana değişikliklerin bullet list'i]

### Features
- Feature 1
- Feature 2
- Feature 3

### Technical
- [Entities/Services/Controllers eklenenler]
- [Database migrations]
- [Events/Integrations]

## API Endpoints
```http
POST   /api/endpoint1    # Description
GET    /api/endpoint2    # Description
```

## Configuration
```properties
config.key1=value1
config.key2=value2
```

## Testing
- **Unit Tests:** X tests (Y classes)
- **Integration Tests:** X tests (Y controllers)
- **E2E Tests:** X scenarios
- **Results:** All passing ✅

## Documentation
- [ ] README.md updated
- [ ] API docs (Swagger) updated
- [ ] Spec files updated

## Breaking Changes
[None / List any breaking changes]

## Checklist
- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] Documentation updated
- [ ] PR title follows conventional commits
- [ ] No merge conflicts

---
**Spec:** `.kiro/specs/<feature-name>/`
**Tier:** [1-4]
**Estimated:** X days | **Actual:** X days
```

---

## Örnek: Damage Management

### Title:
```
feat(damage): implement damage management system
```

### Description:
```markdown
## Summary
Complete damage management system with photo evidence, assessment workflow, liability calculation, and dispute resolution.

## Why
- Track vehicle damages throughout rental lifecycle
- Automate liability calculation with insurance support
- Provide dispute resolution process for customers

## What Changed

### Features
- Damage reporting workflow with photo evidence (max 10 photos)
- Severity classification: MINOR/MODERATE/MAJOR/TOTAL_LOSS
- Assessment with insurance deductible support
- Dispute process: rental owner disputes, admin resolves
- State machine: REPORTED → ASSESSED → CHARGED → DISPUTED → RESOLVED
- Automatic car status update (MAJOR → MAINTENANCE)

### Technical
- 2 Entities: DamageReport, DamagePhoto
- 4 Service Interfaces: Report, Assessment, Dispute, FileUpload
- 3 Controllers: 14 REST endpoints
- 2 Database Migrations: V10, V11
- 5 Domain Events: DamageReported, Assessed, Charged, Disputed, Resolved

## API Endpoints
```http
POST   /api/admin/damages                    # Create report
POST   /api/admin/damages/{id}/photos        # Upload photos
POST   /api/admin/damages/{id}/assess        # Assess damage
POST   /api/damages/{id}/dispute             # Create dispute
POST   /api/admin/damages/{id}/resolve       # Resolve dispute
GET    /api/damages/me                       # Customer history
GET    /api/admin/damages/vehicle/{carId}    # Vehicle history
GET    /api/admin/damages/statistics         # Statistics
```

## Configuration
```properties
damage.minor-threshold=500
damage.moderate-threshold=2000
damage.major-threshold=10000
damage.max-photos-per-report=10
damage.max-photo-size-bytes=10485760
```

## Testing
- **Unit Tests:** 15 tests (4 service classes)
- **Integration Tests:** 12 tests (3 controllers)
- **E2E Tests:** 7 scenarios (lifecycle, auth, history)
- **Results:** All passing ✅

## Documentation
- [x] README.md updated
- [x] API docs (Swagger) updated
- [x] Spec files updated

## Breaking Changes
None

## Checklist
- [x] Code compiles without errors
- [x] All tests pass
- [x] Documentation updated
- [x] PR title follows conventional commits
- [x] No merge conflicts

---
**Spec:** `.kiro/specs/damage-management/`
**Tier:** 2 (Business Features)
**Estimated:** 2 days | **Actual:** 2 days
```

---

## Quick Reference

| Section | Zorunlu? | Açıklama |
|---------|----------|----------|
| Summary | ✅ Yes | 1-2 cümle özet |
| Why | ✅ Yes | Motivasyon |
| What Changed | ✅ Yes | Değişiklikler listesi |
| API Endpoints | ⚠️ If applicable | Yeni endpoint'ler |
| Configuration | ⚠️ If applicable | Yeni config'ler |
| Testing | ✅ Yes | Test coverage |
| Documentation | ✅ Yes | Güncellenen docs |
| Breaking Changes | ✅ Yes | "None" bile olsa belirt |
| Checklist | ✅ Yes | PR hazırlık kontrolü |
