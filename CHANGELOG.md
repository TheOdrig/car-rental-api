# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Documentation improvements (CONTRIBUTING.md, CHANGELOG.md)

### Changed

### Fixed

## [2.0.0] - 2025-12-14

### ⚠️ BREAKING CHANGES
- Complete architecture migration from layered to modular

### Changed
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

### Added
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

## [1.0.0] - 2025-11-28

### Added
- Initial release with traditional layered architecture
- Railway deployment configuration
- JWT authentication
- Car management (CRUD, availability search)
- Rental lifecycle (request → confirm → pickup → return)
- PostgreSQL database with Flyway migrations
- Swagger/OpenAPI documentation
- Basic test suite

---

[Unreleased]: https://github.com/TheOdrig/car-rental-api/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/TheOdrig/car-rental-api/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/TheOdrig/car-rental-api/releases/tag/v1.0.0
