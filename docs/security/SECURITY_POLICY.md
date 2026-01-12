# Security Policy

> Car Rental API Security Posture and OWASP Compliance

## Table of Contents

1. [Security Contact](#1-security-contact)
2. [OWASP Top 10 (2025 RC) Compliance](#2-owasp-top-10-2025-rc-compliance)

---

## 1. Security Contact

If you discover a security vulnerability, please open a GitHub issue or contact the repository owner.

**For sensitive issues:** Please use GitHub's private vulnerability reporting feature.

---

## 2. OWASP Top 10 (2025 RC) Compliance

This section documents our security posture against the OWASP Top 10 2025 Release Candidate.

### Compliance Overview

| # | Category | Status | Mitigation |
|---|----------|--------|------------|
| A01 | Broken Access Control | ✅ | Role-based access (USER/ADMIN), @PreAuthorize, URL-based security |
| A02 | Security Misconfiguration | ✅ | Secure defaults, CORS whitelist, stateless sessions |
| A03 | Software Supply Chain Failures | ⚠️ | Dependabot enabled, regular updates |
| A04 | Cryptographic Failures | ✅ | BCrypt passwords, HMAC-SHA256 JWT, HTTPS |
| A05 | Injection | ✅ | JPA parameterized queries, @Valid input validation |
| A06 | Insecure Design | ✅ | Spring Modulith boundaries, domain-driven design |
| A07 | Authentication Failures | ✅ | JWT with expiration, OAuth2 state parameter |
| A08 | Software or Data Integrity Failures | ✅ | Stripe webhook signatures, event idempotency |
| A09 | Logging & Alerting Failures | ✅ | Structured logging, correlation IDs, no PII |
| A10 | Mishandling of Exceptional Conditions | ✅ | GlobalExceptionHandler, fail-secure defaults |

### Detailed Mitigations

#### A01 - Broken Access Control

**Implementation:**
- URL-based security rules in `SecurityConfig.java`
- Method-level security with `@PreAuthorize("hasRole('ADMIN')")`
- Two roles: `USER` and `ADMIN` (defined in `Role.java`)

**Code Reference:**
```java
// SecurityConfig.java
.requestMatchers(HttpMethod.POST, "/api/rentals/*/confirm").hasRole("ADMIN")
.requestMatchers(HttpMethod.GET, "/api/rentals/me").hasRole("USER")
```

#### A02 - Security Misconfiguration

**Implementation:**
- CSRF disabled (stateless JWT API)
- CORS configured with specific origins
- Session management: `STATELESS`
- No default credentials

**Code Reference:**
```java
// SecurityConfig.java
.csrf(AbstractHttpConfigurer::disable)
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

#### A03 - Software Supply Chain Failures

**Implementation:**
- GitHub Dependabot alerts enabled
- Regular dependency updates via Dependabot PRs

**Known Limitation:**
- No SBOM (Software Bill of Materials) generation yet

#### A04 - Cryptographic Failures

**Implementation:**
- Passwords: BCrypt with default work factor (10 rounds)
- JWT Signing: HMAC-SHA256 via `Keys.hmacShaKeyFor()`
- OAuth2 State: HMAC-SHA256 with timestamp expiration

**Code Reference:**
```java
// JwtTokenProvider.java
private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes());
}

// SecurityConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

#### A05 - Injection

**Implementation:**
- Spring Data JPA with parameterized queries (no raw SQL)
- Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`)
- No dynamic query construction

#### A06 - Insecure Design

**Implementation:**
- Spring Modulith for module isolation
- Clear module boundaries (auth, car, rental, payment, etc.)
- Application events for cross-module communication

#### A07 - Authentication Failures

**Implementation:**
- JWT access tokens with configurable expiration
- Refresh tokens for token rotation
- OAuth2 state parameter with HMAC signature and expiration
- Token validation in `JwtAuthenticationFilter`

**See:** [AUTHENTICATION.md](./AUTHENTICATION.md) for detailed documentation

#### A08 - Software or Data Integrity Failures

**Implementation:**
- Stripe webhook signature verification
- Event ID-based idempotency (prevents replay attacks)
- WebhookEvent entity tracks processing status

**Code Reference:**
```java
// StripeWebhookHandler.java
event = Webhook.constructEvent(payload, signature, stripeConfig.getWebhookSecret());

// Idempotency check
if (isEventAlreadyProcessed(eventId)) {
    return; // Skip duplicate
}
```

#### A09 - Logging & Alerting Failures

**Implementation:**
- Structured logging with SLF4J/Logback
- Correlation IDs via `CorrelationIdFilter`
- Audit logs for webhook events with timestamps
- No PII (passwords, tokens) in logs

**Code Reference:**
```java
// StripeWebhookHandler.java
log.info("[AUDIT] Webhook Event: SIGNATURE_VERIFIED | Event ID: {} | Event Type: {}",
        event.getId(), event.getType());
```

#### A10 - Mishandling of Exceptional Conditions

**Implementation:**
- `GlobalExceptionHandler` for centralized error handling
- Fail-secure: Invalid tokens return 401, not 500
- Proper error responses without sensitive details
