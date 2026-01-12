# Design Document: Test Documentation

## Overview

Bu design, Car Rental API projesi için kapsamlı test dokümantasyonu oluşturmayı tanımlar. İki ana dokümantasyon dosyası oluşturulacak:

1. **TEST_STRATEGY.md** - Test yaklaşımı, araçlar, coverage hedefleri
2. **CRITICAL_SCENARIOS.md** - İş senaryoları ve test referansları

Her iki dosya da `docs/testing/` dizininde yer alacak.

## Architecture

```
docs/
└── testing/
    ├── TEST_STRATEGY.md      # Ana test stratejisi dokümanı
    └── CRITICAL_SCENARIOS.md # Kritik senaryolar ve test referansları
```

### Mevcut Test Yapısı (Referans)

```
src/test/java/com/akif/
├── {module}/
│   ├── unit/           # Mockito ile unit testler
│   ├── integration/    # @SpringBootTest ile integration testler
│   └── e2e/            # Full context E2E testler
├── e2e/
│   └── infrastructure/ # E2ETestBase, TestFixtures, TestDataBuilder
└── ModularityTests.java
```

## Components and Interfaces

### Component 1: TEST_STRATEGY.md

Ana test stratejisi dokümanı aşağıdaki bölümleri içerecek:

```markdown
# Test Strategy

## Table of Contents
1. Test Philosophy
2. Test Pyramid
3. Coverage Targets
4. Testing Tools & Frameworks
5. Test Directory Structure
6. Naming Conventions
7. Module Coverage Matrix
8. Test Data Management
9. Modularity Testing
10. CI/CD Integration
11. New Module Checklist

## 1. Test Philosophy
- Test piramidi yaklaşımı
- Unit > Integration > E2E önceliği
- Steering files referansı

## 2. Test Pyramid
| Type | Percentage | Purpose |
|------|------------|---------|
| Unit | 70% | Business logic, isolated |
| Integration | 20% | API endpoints, DB |
| E2E | 10% | Full flows |

## 3. Coverage Targets
| Scope | Target |
|-------|--------|
| Overall | 80% |
| Critical Paths | 95% |
| New Code | 80% |

## 4. Testing Tools & Frameworks
- JUnit 5
- Mockito + MockitoExtension
- AssertJ
- Spring Boot Test
- H2 (test database)
- Awaitility (async testing)
- Spring Modulith Test

## 5. Test Directory Structure
[Her modül için yapı]

## 6. Naming Conventions
- Test class: `{ClassName}Test` (unit), `{ClassName}IntegrationTest` (integration)
- Test method: `should{Action}When{Condition}`
- @DisplayName: English, descriptive

## 7. Module Coverage Matrix (Baseline - Jan 2026)
| Metric | Current | Target | Status |
|--------|---------|--------|--------|
| Instruction | 74.68% | 80% | ❌ |
| Branch | 55.17% | 80% | 🚨 |
| Line | 75.45% | 80% | ❌ |
| Method | 78.74% | 85% | ⚠️ |

### Per-Module Status (Top Level)
| Module | Instruction Coverage | Status |
|--------|----------------------|--------|
| rental | ~70-75% | ⚠️ |
| payment | ~60-65% | 🚨 |
| auth | ~75-80% | ⚠️ |
| car | ~80%+ | ✅ |
| ... | ... | ... |

## 8. Test Data Management
- TestFixtures pattern
- TestDataBuilder pattern
- @Transactional usage
- H2 configuration

## 9. Modularity Testing
- ModularityTests explanation
- Spring Modulith verification
- package-info.java updates

## 10. CI/CD Integration
- Maven commands
- JaCoCo reports
- Troubleshooting

## 11. New Module Checklist
- [ ] Unit tests in `unit/` package
- [ ] Integration tests in `integration/` package
- [ ] ModularityTests passes
- [ ] Coverage meets targets
```

### Component 2: CRITICAL_SCENARIOS.md

Kritik iş senaryoları dokümanı aşağıdaki yapıda olacak:

```markdown
# Critical Test Scenarios

## Table of Contents
1. Rental Lifecycle
2. Payment Processing
3. Authentication & Authorization
4. Late Return & Penalties
5. Damage Management
6. Edge Cases
7. Error Handling
8. Coverage Gaps

## 1. Rental Lifecycle

### Happy Path Scenarios
| # | Scenario | Test Reference |
|---|----------|----------------|
| 1.1 | User requests rental | RentalServiceTest#shouldSuccessfullyCreateRentalRequest |
| 1.2 | Admin confirms rental | RentalServiceTest#shouldSuccessfullyConfirmRentalAndAuthorizePayment |
| 1.3 | Admin processes pickup | RentalServiceTest#shouldSuccessfullyProcessPickupAndCapturePayment |
| 1.4 | Admin processes return | RentalServiceTest#shouldSuccessfullyProcessReturn |
| 1.5 | User cancels rental | RentalServiceTest#shouldSuccessfullyCancelRequestedRentalWithoutRefund |

### Edge Cases
| # | Scenario | Test Reference |
|---|----------|----------------|
| 1.6 | Date overlap detection | RentalServiceTest#shouldThrowExceptionWhenDateOverlapExists |
| 1.7 | Past date validation | RentalServiceTest#shouldThrowExceptionWhenStartDateIsInPast |
| 1.8 | Concurrent booking | ConcurrencyE2ETest |

## 2. Payment Processing
[Similar structure]

## 3. Authentication & Authorization
[Similar structure]

## 4. Late Return & Penalties
[Similar structure]

## 5. Damage Management
[Similar structure]

## 6. Edge Cases
[Cross-cutting edge cases]

## 7. Error Handling
[Error scenarios]

## 8. Coverage Gaps
| Gap | Priority | Notes |
|-----|----------|-------|
| ... | ... | ... |
```

## Data Models

Bu spec kod değil dokümantasyon oluşturduğu için data model yoktur. Ancak dokümantasyonda referans verilecek mevcut test sınıfları:

### Mevcut Test Infrastructure

| Class | Location | Purpose |
|-------|----------|---------|
| E2ETestBase | e2e/infrastructure/ | Base class for E2E tests |
| TestFixtures | e2e/infrastructure/ | Static test data |
| TestDataBuilder | e2e/infrastructure/ | Fluent test data builder |
| TestEventCaptor | e2e/infrastructure/ | Event capture for async tests |
| ModularityTests | root | Spring Modulith verification |

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Bu spec dokümantasyon oluşturduğu için, properties doküman içeriğinin doğruluğunu kontrol eder:

### Property 1: Module Structure Consistency

*For any* module documented in TEST_STRATEGY.md, the documented test directory structure (unit/, integration/, e2e/) should match the actual directory structure in the codebase.

**Validates: Requirements 1.4**

### Property 2: Scenario Test Reference Validity

*For any* scenario documented in CRITICAL_SCENARIOS.md, the referenced test file should exist in the codebase and the test method should be present.

**Validates: Requirements 2.7**

## Error Handling

Dokümantasyon oluşturma sürecinde karşılaşılabilecek durumlar:

| Durum | Çözüm |
|-------|-------|
| Test dosyası bulunamadı | Gap olarak işaretle |
| Coverage verisi yok | "TBD" olarak işaretle, JaCoCo çalıştırıldıktan sonra güncelle |
| Modül yapısı değişti | Dokümantasyonu güncelle |

## Testing Strategy

Bu spec'in kendisi dokümantasyon oluşturduğu için, test stratejisi manuel review'a dayanır:

### Manual Verification Checklist

1. **Completeness Check**
   - [ ] Tüm 9 modül TEST_STRATEGY.md'de listelenmiş
   - [ ] Tüm kritik senaryolar CRITICAL_SCENARIOS.md'de var
   - [ ] Her senaryo için test referansı mevcut

2. **Accuracy Check**
   - [ ] Dokümante edilen test yapısı gerçek yapıyla eşleşiyor
   - [ ] Referans verilen test dosyaları mevcut
   - [ ] Coverage hedefleri makul ve ulaşılabilir

3. **Consistency Check**
   - [ ] Naming conventions steering file ile tutarlı
   - [ ] Terminoloji tutarlı kullanılmış
   - [ ] Markdown formatı düzgün


