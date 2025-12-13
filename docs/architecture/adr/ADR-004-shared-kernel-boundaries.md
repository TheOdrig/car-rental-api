# ADR-004: Shared Kernel Boundaries and Rules

## Status
**Accepted**

## Context

In Spring Modulith, the `shared` module hosts common code accessible to all other modules. However, uncontrolled growth risks creating a "big ball of mud".

**Question:** What should be in the shared kernel, and what shouldn't?

## Decision

The shared kernel contains **only cross-cutting concerns**. Business logic or domain-specific code is **never** in shared.

## Rationale

### Shared Kernel Should Contain ✅

| Category | Example | Reason |
|----------|---------|--------|
| **Base Classes** | `BaseEntity` | All entities extend it |
| **Global Enums** | `CurrencyType`, `Role` | Used in multiple modules |
| **Exceptions** | `BaseException` | Caught by global handler |
| **Security** | `JwtTokenProvider`, `SecurityConfig` | Cross-cutting concern |
| **Infrastructure** | `FileUploadService` | Multiple modules use it |
| **Config** | `CacheConfig`, `CorsConfig` | Application-wide |

### Shared Kernel Should NOT Contain ❌

| Category | Example | Where It Belongs |
|----------|---------|------------------|
| **Domain Enums** | `RentalStatus`, `DamageStatus` | Own module |
| **Business DTOs** | `RentalRequest` | Own module |
| **Domain Services** | `PenaltyCalculationService` | Own module |
| **Entities** | `Car`, `User`, `Rental` | Own module |
| **Events** | `RentalConfirmedEvent` | Publisher module |

### Size Thresholds

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| Class count | ≤ 10 | > 15 | > 25 |

### Current State

Current shared kernel contains **21 classes**. Distribution:

```
shared/
├── config/        (4) - CacheConfig, CorsConfig, OpenApiConfig, R2Config
├── domain/        (1) - BaseEntity
├── enums/         (2) - CurrencyType, Role
├── exception/     (5) - BaseException + 4 specialized
├── handler/       (2) - GlobalExceptionHandler, ErrorResponseDto
├── infrastructure/(3) - FileUploadService + 2 impl
└── security/      (4) - SecurityConfig, JWT, Filters
```

**Assessment:** 21 > 10, but these are genuinely cross-cutting:
- Security (4): Every endpoint is protected
- Config (4): Application-wide
- Exception (5): Global handling
- Infrastructure (3): Multi-module upload

**Decision:** Raise CI threshold to 25, accept current structure.

## Implementation

### package-info.java

```java
@org.springframework.modulith.ApplicationModule(
    type = Type.OPEN  // All packages public
)
package com.akif.shared;
```

### CI Check

```yaml
# .github/workflows/modulith-verify.yml
- name: Check Shared Kernel Size
  run: |
    count=$(find src/main/java -path "*/shared/*" -name "*.java" ! -name "package-info.java" | wc -l)
    if [ $count -gt 25 ]; then
      echo "❌ FAIL: Shared kernel has $count classes (> 25)!"
      exit 1
    fi
```

## Consequences

### Positive
- Shared kernel under control
- Clear guidelines for code placement
- CI enforcement

### Negative
- Manual review required (CI only checks count)
- Business logic could leak into shared (discipline needed)

### Governance

1. **PR Review:** Every class added to shared should be scrutinized
2. **Weekly Check:** `ModularityTests` in weekly CI job
3. **Quarterly Review:** Shared kernel audit

## Related ADRs
- ADR-001: Spring Modulith decision
