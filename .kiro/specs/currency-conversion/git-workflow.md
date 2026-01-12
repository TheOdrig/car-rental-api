# Git Workflow - Currency Conversion Feature

## Branch Strategy

```
develop
  │
  └── feature/currency-conversion    ← Ana feature branch
        │
        ├── Commit 1: Infrastructure
        ├── Commit 2: Service
        ├── Commit 3: Caching
        ├── Commit 4: Fallback
        ├── Commit 5: API Endpoints
        ├── Commit 6: Car Integration
        ├── Commit 7: Rental Integration
        ├── Commit 8: Scheduler
        └── Commit 9: Documentation
```

## Initial Setup

```bash
# 1. develop branch'inden başla
git checkout develop
git pull origin develop

# 2. Feature branch oluştur
git checkout -b feature/currency-conversion

# 3. İlk boş commit (branch oluşturuldu)
git commit --allow-empty -m "chore: create currency-conversion feature branch"
git push -u origin feature/currency-conversion
```

---

## Commit Plan (Task Sırasına Göre)

### Commit 1: Exchange Rate Client Infrastructure
**Tasks:** 1.1, 1.2

```bash
git add .
git commit -m "feat(currency): add exchange rate API client

- Implement RestClient integration with ExchangeRate-API v6
- Add DTOs for API response mapping
- Configure timeout settings"
```

---

### Commit 2: Currency Conversion Service
**Tasks:** 2.1, 2.2

```bash
git add .
git commit -m "feat(currency): implement currency conversion service

- Add convert, getRate, getAllRates methods
- Handle HALF_UP rounding with currency-specific decimals
- Support cross-rate calculation via USD base"
```

---

### Commit 3: Caching Strategy
**Tasks:** 3.1, 3.2

```bash
git add .
git commit -m "feat(currency): add Caffeine caching for exchange rates

- Configure per-cache TTL settings
- Extract cache service to fix AOP proxy issue"
```

---

### Commit 4: Fallback Mechanism
**Tasks:** 4.1, 4.2

```bash
git add .
git commit -m "feat(currency): add configurable fallback rates

- Create FallbackRatesConfig with properties support
- Return FALLBACK source when API unavailable"
```

---

### Commit 5: Currency REST API Endpoints
**Tasks:** 6.1, 6.2, 6.3

```bash
git add .
git commit -m "feat(currency): add REST endpoints for exchange rates

- Create CurrencyController with CRUD operations
- Add ConvertRequest DTO with validation
- Secure refresh endpoint with admin role"
```

---

### Commit 6: Car Endpoint Integration
**Tasks:** 7.1, 7.2, 7.3

```bash
git add .
git commit -m "feat(car): add currency conversion support

- Add optional currency query parameter
- Return both original and converted prices"
```

---

### Commit 7: Rental Endpoint Integration
**Tasks:** 8.1, 8.2, 8.3

```bash
git add .
git commit -m "feat(rental): add currency conversion support

- Add currency parameter to rental queries
- Include conversion metadata in response"
```

---

### Commit 8: Scheduled Rate Refresh
**Tasks:** 9.1

```bash
git add .
git commit -m "feat(currency): add scheduled hourly rate refresh

- Create scheduler with @Scheduled annotation
- Handle refresh failures gracefully"
```

---

### Commit 9: Documentation
**Tasks:** 11.1, 11.2

```bash
git add .
git commit -m "docs: add currency conversion documentation

- Update README with usage examples
- Document supported currencies"
```

---

## Final Steps

```bash
# 1. Tüm testlerin geçtiğinden emin ol
mvn clean test

# 2. Feature branch'i push et
git push origin feature/currency-conversion

# 3. Pull Request oluştur (GitHub'da)
# Title: feat: Real-Time Currency Conversion
# Description: 
#   - Adds real-time currency conversion using ExchangeRate-API
#   - Caching with 1-hour TTL
#   - Fallback rates for API failures
#   - Integration with car and rental endpoints

# 4. PR onaylandıktan sonra develop'a merge et
git checkout develop
git merge feature/currency-conversion
git push origin develop

# 5. Feature branch'i sil
git branch -d feature/currency-conversion
git push origin --delete feature/currency-conversion
```

---

## Commit Message Convention

```
<type>(<scope>): <subject>

<body>

Refs: <requirements>
```

**Types:**
- `feat`: Yeni özellik
- `fix`: Bug fix
- `docs`: Dokümantasyon
- `test`: Test ekleme/düzeltme
- `refactor`: Kod refactoring
- `chore`: Build, config değişiklikleri

**Scope:** `currency`, `car`, `rental`, `config`

---

## Quick Reference

| Task | Commit | Branch |
|------|--------|--------|
| 1.1-1.3 | Commit 1 | feature/currency-conversion |
| 2.1-2.4 | Commit 2 | feature/currency-conversion |
| 3.1-3.3 | Commit 3 | feature/currency-conversion |
| 4.1-4.3 | Commit 4 | feature/currency-conversion |
| 6.1-6.4 | Commit 5 | feature/currency-conversion |
| 7.1-7.5 | Commit 6 | feature/currency-conversion |
| 8.1-8.3 | Commit 7 | feature/currency-conversion |
| 9.1 | Commit 8 | feature/currency-conversion |
| 11.1-11.2 | Commit 9 | feature/currency-conversion |
