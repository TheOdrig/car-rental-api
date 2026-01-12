# Develop → Main Merge Description Template

> Develop branch'ini main'e merge ederken kullan (production release).
> **Kısa ve öz ol.** Teknik detaylar feature→develop PR'da zaten var.

---

## PR Title Format

```
release: <feature/module name> to production
```

**Örnekler:**
```
release: admin dashboard to production
release: damage management to production
release: late return & penalty system to production
```

---

## PR Description Template (Önerilen)

```markdown
## Summary
[1 cümle: Bu release ne içeriyor?]

## Features
- Feature capability 1
- Feature capability 2
- Feature capability 3

## Technical
- [X] new endpoints
- [Y] database migration(s)
- Tests: All passing ✅

## Breaking Changes
[None / List any breaking changes]

## Deployment
- Migrations run automatically
- [Any manual steps if needed]

---
**Ready for Production:** ✅
```

---

## Örnek: Admin Dashboard

### Title:
```
release: admin dashboard to production
```

### Description:
```markdown
## Summary
Admin Dashboard module with real-time metrics, 5-level alert system, and quick actions.

## Features
- Daily summary (pending approvals, pickups, returns, overdue)
- Fleet status with occupancy rate
- Alert system (5 severity levels: CRITICAL, HIGH, WARNING, MEDIUM, LOW)
- Quick actions (approve, pickup, return from dashboard)
- Event-driven cache invalidation

## Technical
- 13 new endpoints
- 1 database migration (V13)
- Tests: All passing ✅

## Breaking Changes
None

## Deployment
- Migrations run automatically
- No manual steps required

---
**Ready for Production:** ✅
```

---

## Örnek: Damage Management

### Title:
```
release: damage management to production
```

### Description:
```markdown
## Summary
Damage management system with photo evidence, assessment workflow, and dispute resolution.

## Features
- Damage reporting with photo evidence (max 10 photos)
- Severity classification (MINOR/MODERATE/MAJOR/TOTAL_LOSS)
- Assessment workflow with insurance support
- Dispute process for customers
- Vehicle and customer damage history

## Technical
- 14 new endpoints
- 2 database migrations (V10, V11)
- Tests: All passing ✅

## Breaking Changes
None

## Deployment
- Migrations run automatically
- No manual steps required

---
**Ready for Production:** ✅
```

---

## Ne Zaman Detaylı Yazmalı?

| Durum | Format |
|-------|--------|
| Normal release | Kısa (yukarıdaki template) |
| Breaking changes var | Detaylı (migration steps ekle) |
| Manual deployment steps var | Detaylı (Deployment section genişlet) |
| Multiple features | Her feature için mini-section |

---

## Feature → Develop vs Develop → Main

| Aspect | Feature → Develop | Develop → Main |
|--------|-------------------|----------------|
| **Detay** | Yüksek (implementation) | Düşük (capabilities) |
| **API Endpoints listesi** | ✅ Gerekli | ❌ Gereksiz |
| **Configuration** | ✅ Gerekli | ❌ Gereksiz |
| **Why Section** | ✅ Gerekli | ❌ Gereksiz |
| **Breaking Changes** | ✅ Gerekli | ✅ Gerekli |
| **Deployment Notes** | ❌ Gereksiz | ✅ Gerekli (kısa) |

---

## Hızlı Kopyala-Yapıştır

```markdown
## Summary
[Feature] module with [key capabilities].

## Features
- Capability 1
- Capability 2
- Capability 3

## Technical
- X new endpoints
- Y database migration(s)
- Tests: All passing ✅

## Breaking Changes
None

## Deployment
- Migrations run automatically
- No manual steps required

---
**Ready for Production:** ✅
```

