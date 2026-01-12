# Implementation Plan: Architecture Decisions Documentation

## Overview

Bu plan, Car Rental API projesi için 3 kritik ADR (006, 007, 008) oluşturmayı adım adım tanımlar. Her ADR mevcut format (ADR-003) ile tutarlı olacak.

## Tasks

- [x] 1. Create ADR-006 skeleton ✅ (2026-01-12)
  - Create `docs/architecture/adr/ADR-006-dynamic-pricing-strategy.md`
  - Add Status, Context, Decision sections
  - Reference existing pricing code structure
  - _Requirements: Story 1_

- [x] 2. Document ADR-006 Rationale ✅ (2026-01-12)
  - [x] 2.1 Add Alternatives Evaluated table
    - Strategy Pattern, Rule Engine, ML-based, Hardcoded
    - Pros/Cons for each
    - _Requirements: Story 1 - Alternatives evaluated_
  - [x] 2.2 Add "Why Strategy Pattern?" section
    - Extensibility, testability, SRP benefits
    - _Requirements: Story 1 - Strategy Pattern rationale_

- [x] 3. Document ADR-006 Implementation Details ✅ (2026-01-12)
  - [x] 3.1 List all 5 strategies with execution order
    - Season (1), EarlyBooking (2), Duration (3), Weekend (4), Demand (5)
    - _Requirements: Story 1 - 5 strategies explained_
  - [x] 3.2 Add price calculation flow diagram (Mermaid)
    - Chain of multipliers
    - _Requirements: Story 1 - Chain of responsibility_
  - [x] 3.3 Document price caps mechanism
    - minDailyPrice, maxDailyPrice
    - _Requirements: Story 1 - Price caps_
  - [x] 3.4 Add code references
    - PricingStrategy, DynamicPricingServiceImpl, PricingConfig
    - _Requirements: Story 1 - PricingConfig reference_

- [x] 4. Complete ADR-006 Consequences ✅ (2026-01-12)
  - Add Positive consequences (extensibility, testability)
  - Add Negative consequences (complexity, more classes)
  - Add Related ADRs section
  - _Requirements: FR-1_

- [x] 5. Checkpoint - Review ADR-006 ✅ (2026-01-12)
  - Verify all Story 1 acceptance criteria met
  - Ensure format consistency with ADR-003

- [x] 6. Create ADR-007 skeleton ✅ (2026-01-12)
  - Create `docs/architecture/adr/ADR-007-late-return-penalty.md`
  - Add Status, Context, Decision sections
  - Reference penalty calculation code
  - _Requirements: Story 2_

- [x] 7. Document ADR-007 Penalty Tiers ✅ (2026-01-12)
  - [x] 7.1 Add Grace Period section
    - 60 minutes default, rationale
    - _Requirements: Story 2 - Grace period rationale_
  - [x] 7.2 Add Hourly Penalty section
    - 10% rate, 1-6 hours threshold
    - _Requirements: Story 2 - Hourly penalty documented_
  - [x] 7.3 Add Daily Penalty section
    - 150% rate, >6 hours threshold
    - _Requirements: Story 2 - Daily penalty documented_
  - [x] 7.4 Add Penalty Cap section
    - 5× daily rate, rationale
    - _Requirements: Story 2 - Cap rationale_

- [x] 8. Document ADR-007 Rationale ✅ (2026-01-12)
  - [x] 8.1 Add Alternatives Evaluated table
    - Tiered, Flat Fee, Progressive, No Cap
    - _Requirements: Story 2 - Alternative models_
  - [x] 8.2 Add penalty decision flow diagram (Mermaid)
    - Grace → Hourly → Daily → Cap
    - _Requirements: Story 2 - LateReturnStatus explained_
  - [x] 8.3 Document PenaltyConfig validation rules
    - Valid ranges for each parameter
    - _Requirements: Story 2 - PenaltyConfig validation_

- [x] 9. Complete ADR-007 Consequences ✅ (2026-01-12)
  - Add Positive consequences (fair, predictable)
  - Add Negative consequences (complexity)
  - Add code references and Related ADRs
  - _Requirements: FR-1, FR-2_

- [x] 10. Checkpoint - Review ADR-007 ✅ (2026-01-12)
  - Verify all Story 2 acceptance criteria met
  - Ensure format consistency

- [x] 11. Create ADR-008 skeleton ✅ (2026-01-12)
  - Create `docs/architecture/adr/ADR-008-authentication-strategy.md`
  - Add Status, Context, Decision sections
  - Reference JWT and OAuth2 code
  - _Requirements: Story 3_

- [x] 12. Document ADR-008 Token Structure ✅ (2026-01-12)
  - [x] 12.1 Add Access Token section
    - Claims: subject, userId, roles
    - Expiration configuration
    - _Requirements: Story 3 - JWT structure, Token claims_
  - [x] 12.2 Add Refresh Token section
    - Purpose and expiration
    - _Requirements: Story 3 - Token expiration rationale_
  - [x] 12.3 Add authentication flow diagram (Mermaid)
    - Login → Token generation → Request validation
    - _Requirements: Story 3 - JWT structure_

- [x] 13. Document ADR-008 OAuth2 Strategy ✅ (2026-01-12)
  - [x] 13.1 Add OAuth2 Provider Selection table
    - Google, GitHub included with rationale
    - Facebook, Twitter excluded with rationale
    - _Requirements: Story 3 - OAuth2 provider rationale_
  - [x] 13.2 Add Alternatives Evaluated table
    - JWT, Session-based, Opaque, PASETO
    - _Requirements: Story 3 - Alternatives evaluated_

- [x] 14. Document ADR-008 Security Considerations ✅ (2026-01-12)
  - [x] 14.1 Add signing strategy section
    - HMAC-SHA256 with secret key
    - _Requirements: Story 3 - HMAC signing_
  - [x] 14.2 Add token storage recommendations
    - HttpOnly cookies, refresh rotation
    - _Requirements: Story 3 - Security considerations_
  - [x] 14.3 Add code references
    - JwtTokenProvider, SecurityConfig, OAuth2Controller, OAuth2AuthService
    - _Requirements: FR-2_

- [x] 15. Complete ADR-008 Consequences ✅ (2026-01-12)
  - Add Positive consequences (stateless, scalable)
  - Add Negative consequences (token size, revocation)
  - Add Related ADRs section
  - _Requirements: FR-1_

- [x] 16. Final Checkpoint - Review all ADRs ✅ (2026-01-12)
  - Verify all 3 stories' acceptance criteria met
  - Ensure format consistency across all ADRs
  - Verify code references are accurate
  - Ask user for final review

## Notes

- Tüm ADR'ler mevcut format (ADR-003) ile tutarlı olacak
- Mermaid diyagramları GitHub'da render edilebilir formatta
- Code references gerçek dosya yollarını içerecek
- Default değerler mevcut koddan alınacak (kafadan uydurulmayacak)
