# Design: Project Basics Documentation

## Overview

Bu tasarım, 2 temel proje dosyası için içerik yapısını tanımlar: CONTRIBUTING.md ve CHANGELOG.md.

## File Structure

```
/
├── README.md           (mevcut)
├── LICENSE             (mevcut)
├── CONTRIBUTING.md     (YENİ)
└── CHANGELOG.md        (YENİ)
```

---

## Document 1: CONTRIBUTING.md

### Structure

```markdown
# Contributing to Car Rental API

## Welcome
[Hoş geldin mesajı]

## Table of Contents
[İçindekiler]

## Getting Started
### Prerequisites
### Development Setup

## Development Guidelines
### Code Style
### Project Structure
### Testing Requirements

## Making Changes
### Branch Naming
### Commit Messages
### Pull Request Process

## Reporting Issues
### Bug Reports
### Feature Requests

## Code Review
### Review Process
### Approval Requirements

## Additional Resources
[Linkler]
```

### Content Details

#### Prerequisites
```markdown
- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Docker (optional, for local database)
- IDE with Lombok support
```

#### Development Setup
```bash
# Clone repository
git clone https://github.com/TheOdrig/car-rental-api.git
cd car-rental-api

# Copy environment file
cp .env.example .env
# Edit .env with your credentials

# Start PostgreSQL (Docker)
docker run -d --name car-rental-db \
  -e POSTGRES_DB=gallery \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:15

# Build and test
mvn clean verify

# Run application
mvn spring-boot:run
```

#### Code Style Guidelines
- Follow existing code patterns
- Use Lombok annotations (@RequiredArgsConstructor, @Getter, @Setter)
- Records for DTOs
- Public API in `api/` package, implementation in `internal/`
- Reference: `.kiro/steering/java-spring-boot-main-standards.md`

#### Branch Naming Convention
```
feature/short-description
bugfix/issue-number-description
hotfix/critical-fix-description
docs/documentation-update
```

#### Commit Message Format (Conventional Commits)
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Maintenance

Examples:
```
feat(rental): add late return penalty calculation
fix(payment): handle Stripe webhook timeout
docs(api): add error codes documentation
test(car): add availability search tests
```

#### Pull Request Checklist
```markdown
- [ ] Code follows project style guidelines
- [ ] Tests added/updated for changes
- [ ] All tests pass (`mvn test`)
- [ ] Modularity tests pass (`mvn test -Dtest=ModularityTests`)
- [ ] Documentation updated if needed
- [ ] No new warnings introduced
- [ ] PR description explains changes
```

#### Testing Requirements
```markdown
- All new features must have tests
- Maintain or improve code coverage
- Run full test suite before PR: `mvn test`
- Run modularity verification: `mvn test -Dtest=ModularityTests`
```

---

## Document 2: CHANGELOG.md

### Structure (Keep a Changelog Format)

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Documentation improvements

### Changed
- ...

### Fixed
- ...

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

## [1.0.0] - 2025-11-28

### Added
- Initial release with traditional layered architecture
- Railway deployment configuration
- JWT authentication with OAuth2 (Google, GitHub)
- Car management (CRUD, availability search)
- Rental lifecycle (request → confirm → pickup → return)
- Dynamic pricing with 5 strategies
- Late return detection and penalty calculation
- Stripe payment integration with webhooks
- Damage reporting and assessment
- Admin dashboard with metrics and alerts
- Email notifications with SendGrid
- Currency conversion with ExchangeRate-API
- PostgreSQL database with Flyway migrations
- Swagger/OpenAPI documentation
- Automated test suite

[Unreleased]: https://github.com/TheOdrig/car-rental-api/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/TheOdrig/car-rental-api/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/TheOdrig/car-rental-api/releases/tag/v1.0.0
```

### Section Definitions

| Section | Description | Example |
|---------|-------------|---------|
| Added | New features | "Add late return penalty system" |
| Changed | Changes in existing functionality | "Update pricing algorithm" |
| Deprecated | Soon-to-be removed features | "Deprecate v1 API endpoints" |
| Removed | Removed features | "Remove legacy payment gateway" |
| Fixed | Bug fixes | "Fix rental date overlap validation" |
| Security | Security fixes | "Fix JWT token validation vulnerability" |

---

## Acceptance Criteria Mapping

| Requirement | Document Section |
|-------------|-----------------|
| Story 1: Dev environment setup | CONTRIBUTING.md Getting Started |
| Story 1: Code style guidelines | CONTRIBUTING.md Code Style |
| Story 1: PR process | CONTRIBUTING.md Pull Request Process |
| Story 1: Commit message format | CONTRIBUTING.md Commit Messages |
| Story 1: Testing requirements | CONTRIBUTING.md Testing Requirements |
| Story 1: Issue reporting | CONTRIBUTING.md Reporting Issues |
| Story 1: Code review process | CONTRIBUTING.md Code Review |
| Story 2: SemVer format | CHANGELOG.md header |
| Story 2: Keep a Changelog format | CHANGELOG.md structure |
| Story 2: Sections | CHANGELOG.md Added/Changed/Fixed |
| Story 2: Unreleased section | CHANGELOG.md [Unreleased] |
| Story 2: Initial release (v1.0.0) | CHANGELOG.md [1.0.0] - Layered architecture |
| Story 2: Major release (v2.0.0) | CHANGELOG.md [2.0.0] - Spring Modulith migration |
| Story 2: Version comparison links | CHANGELOG.md footer links |
