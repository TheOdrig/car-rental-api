# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

[Compare v2.0.0 with main](https://github.com/TheOdrig/car-rental-api/compare/v2.0.0...main)

This change set rebaselines the database into `V1__baseline.sql` and renames the schema to `car_rental`. Start with a fresh database when moving from an earlier schema.

### Changed (breaking)
- Every public endpoint is served under `/api/v1`; the Stripe webhook moved to `/api/v1/webhooks/stripe`
- Errors are RFC 9457 problem details with a stable `code` from the `ErrorCode` enum; OAuth2 failures use `OAUTH2_*` codes, and client errors that used to answer 500 answer 400, 409 or 413
- Lists are returned as a `PageResponse` record (`content`, `page`, `size`, `totalElements`, `totalPages`)
- An unauthenticated or invalid token answers 401 (it was 403)
- Refresh tokens are opaque, hashed and rotated; access tokens carry `token_use=access`; `expiresIn` is the seconds to expiry on every path
- Car status is fleet condition only (`AVAILABLE`, `MAINTENANCE`, `DAMAGED`, `INSPECTION`); availability comes from rentals
- A rental in `IN_USE` can no longer be cancelled; admins end it with `POST /api/v1/rentals/{id}/early-return`, which refunds the unused days
- `GET /api/v1/admin/users/{id}` reports `statistics.spentByCurrency`, and damage statistics and user damage liability are reported per currency instead of a mixed sum
- `GET /api/v1/cars/search` is the single catalogue search (the duplicate POST was removed) and applies sorting; `GET /api/v1/cars/search/criteria` matches brand, model and search term as case-insensitive substrings and takes `page` and `size`
- Rental date rules (past start, end after start, lead time, maximum length) are checked once by a domain policy from `rental.max-lead-days` and `rental.max-rental-days`
- Schema `gallery` renamed to `car_rental`; Java 21, Spring Boot 4.1.2, Spring Modulith 2.1.2 and PostgreSQL 18
- Public car responses no longer contain `vinNumber`; admins read it from `GET /api/v1/admin/cars` and `/api/v1/admin/cars/{id}` (server-side search, status and brand filters, paging, allowlisted sort)
- `size` above 100 or below 1, a malformed `page`/`size` and an unknown `sort` field answer 400 `INVALID_PARAMETER` instead of falling back silently or answering 500; JSON bodies above 1 MiB answer 413
- Customers asking for another customer's rental, payment or damage photo get 404 instead of 403
- Late-return statistics report penalties per currency (`penaltiesByCurrency`); pending-rental items carry `currency` and the customer's name; the damage assessment currency must equal the rental currency; a dispute cannot raise the charged liability and the damage detail exposes dispute and resolution data
- Dashboard `sort` parameters are no longer accepted (the lists have a fixed order); alert action URLs point at existing web routes

### Added
- Stripe PaymentIntents with manual capture, signed and replay-safe webhooks, refunds recorded per operation, and deterministic idempotency keys tagged with a per-database installation identity
- A payment window that opens before the start date and an approval-based payment deadline. The API expires stale requests and overdue unpaid approvals and enforces an open-request cap, maximum rental length and lead time.
- A database exclusion constraint that prevents overlapping rentals of one car
- Rate limiting (Bucket4j) on authentication, rental request, payment intent and OAuth2 callback endpoints, with per-username limits on login and forgot-password, and trusted-proxy handling of the client address
- A `prod` profile with fail-fast configuration validation; stub payment, e-mail and storage implementations for `dev` and `test`
- Persistent cross-module events (Spring Modulith event publication registry) delivered after commit
- A management port for actuator health, liveness, readiness and Prometheus metrics
- A machine-checked OpenAPI contract and endpoint inventory in `openapi/`, a local verification gate with JaCoCo line and branch floors, and a pre-push hook
- [Flyway migrations through V27](src/main/resources/db/migration), including refresh tokens, booking constraints, payment persistence, UTC timestamp changes and stable pricing adjustment codes.

### Removed
- Car-gallery features: selling, featured cars, test drives, view and like counters, the `SOLD`, `RENTED` and `RESERVED` statuses, reserve and cancel-reservation endpoints, `damagePrice` and `totalPrice` on cars, and ten response fields that were never populated
- Placeholder pickup and return locations from reminder and confirmation e-mails
- spring-retry, H2 test database, and the hand-written JWT filter

### Fixed
- Soft-deleted cars are no longer listed, shown, searched or bookable
- Money is rounded by the currency's decimal places, and daily price limits are per currency
- Refunds serialize per payment; a failed dispute refund changes nothing and answers 502
- OAuth2 accounts auto-link only to verified local e-mails; Google must report a verified e-mail
- Time uses one injected UTC clock, and the JVM, Hibernate and database sessions run in UTC
- Return reminders state the penalty from the configuration in the rental currency
- Secrets, e-mail addresses and presigned URLs are kept out of logs; `prod` logs at `INFO` only

## History

### 2.1.0 changes recorded on 2026-01-25

#### Added
- **User Account Settings**: Profile management, avatar upload (R2), password change/reset
- **Admin Detail Pages**: Customer and vehicle detail pages with statistics
- **Ban/Unban System**: User ban workflow with reason tracking and audit fields
- **Admin Notes**: Internal communication system for customer management
- **User Statistics**: Rental/damage aggregation (totalRentals, completedRentals, lateReturns, totalSpent)
- **Avatar Storage**: R2 integration with presigned URLs (JPEG/PNG/GIF/WebP, max 5MB)
- **Password Reset**: Token-based flow with 1-hour validity and email notification

#### Technical
- New endpoints (profile, avatar, password, admin detail)
- Database migration V2 (phone field, password_reset_tokens table)
- Email template for password reset (Thymeleaf)

### [2.0.0] - 2025-12-14

#### ⚠️ BREAKING CHANGES
- Complete architecture migration from layered to modular

#### Changed
- **Architecture**: Migrated from traditional layered architecture to Spring Modulith
- Reorganized codebase into 9 domain modules:
  - `user` - User management and authentication
  - `car` - Vehicle inventory and availability
  - `rental` - Rental lifecycle management
  - `payment` - Stripe integration and transactions
  - `pricing` - Dynamic pricing strategies
  - `damage` - Damage reporting and assessment
  - `notification` - Email notifications
  - `currency` - Currency conversion
  - `dashboard` - Admin metrics and alerts
- Inter-module communication via Spring Application Events
- Module boundaries enforced via ArchUnit/Modularity tests

#### Added
- Spring Modulith dependency management
- `@ApplicationModuleListener` for event-driven communication
- Shared kernel (`shared` module) for common types
- Module documentation generation
- Cross-module integration tests
- OAuth2 integration (Google, GitHub)
- Dynamic pricing with 5 strategies
- Late return detection and penalty calculation
- Stripe payment integration with webhooks
- Damage reporting and assessment
- Admin dashboard with metrics and alerts
- Email notifications with SendGrid
- Currency conversion with ExchangeRate-API

### [1.0.0] - 2025-11-28

#### Added
- Initial release with traditional layered architecture
- Railway deployment configuration
- JWT authentication
- Car management (CRUD, availability search)
- Rental lifecycle (request → confirm → pickup → return)
- PostgreSQL database with Flyway migrations
- Swagger/OpenAPI documentation
- Basic test suite

---


[2.0.0]: https://github.com/TheOdrig/car-rental-api/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/TheOdrig/car-rental-api/releases/tag/v1.0.0
