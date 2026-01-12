# Contributing to Car Rental API

Thank you for considering contributing to the Car Rental API! This document provides guidelines and instructions for contributing to this project.

## Table of Contents

- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Development Setup](#development-setup)
- [Development Guidelines](#development-guidelines)
  - [Code Style](#code-style)
  - [Project Structure](#project-structure)
  - [Testing Requirements](#testing-requirements)
- [Making Changes](#making-changes)
  - [Branch Naming](#branch-naming)
  - [Commit Messages](#commit-messages)
  - [Pull Request Process](#pull-request-process)
- [Reporting Issues](#reporting-issues)
  - [Bug Reports](#bug-reports)
  - [Feature Requests](#feature-requests)
- [Code Review](#code-review)
  - [Review Process](#review-process)
  - [Approval Requirements](#approval-requirements)
- [Additional Resources](#additional-resources)

---

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- **Java 17+** - Required for Spring Boot 3.x
- **Maven 3.8+** - Build and dependency management
- **PostgreSQL 15+** - Primary database
- **Docker** (optional) - For running local database
- **IDE with Lombok support** - IntelliJ IDEA or VS Code with Lombok plugin

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/TheOdrig/car-rental-api.git
   cd car-rental-api
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env with your credentials
   ```

3. **Start PostgreSQL with Docker**
   ```bash
   docker run -d --name car-rental-db \
     -e POSTGRES_DB=gallery \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 postgres:15
   ```

4. **Build and test**
   ```bash
   mvn clean verify
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

The API will be available at `http://localhost:8080`. Access Swagger UI at `http://localhost:8080/swagger-ui.html`.

---

## Development Guidelines

### Code Style

Follow the existing code patterns and conventions:

- **Lombok**: Use annotations to reduce boilerplate
  - `@RequiredArgsConstructor` for dependency injection
  - `@Getter`, `@Setter` for entity fields
  - `@Builder` for complex object construction

- **DTOs**: Use Java Records for immutable data transfer objects
  ```java
  public record CarResponse(Long id, String brand, String model, BigDecimal dailyRate) {}
  ```

- **Package Structure**: Follow Spring Modulith conventions
  - `api/` - Public API (controllers, public services, events)
  - `internal/` - Internal implementation (repositories, internal services)

- **Reference**: See `.kiro/steering/java-spring-boot-main-standards.md` for detailed standards

### Project Structure

This project uses **Spring Modulith** with 9 domain modules:

```
src/main/java/com/carrental/
├── user/          # User management and authentication
├── car/           # Vehicle inventory and availability
├── rental/        # Rental lifecycle management
├── payment/       # Stripe integration and transactions
├── pricing/       # Dynamic pricing strategies
├── damage/        # Damage reporting and assessment
├── notification/  # Email notifications
├── currency/      # Currency conversion
├── dashboard/     # Admin metrics and alerts
└── shared/        # Shared kernel (common types, events)
```

For detailed architecture information, see `docs/development/DEVELOPER_GUIDE.md`.

### Testing Requirements

- **All new features must have tests**
- **Maintain or improve code coverage**

**Run the full test suite:**
```bash
mvn test
```

**Run modularity verification:**
```bash
mvn test -Dtest=ModularityTests
```

> ⚠️ **Important**: ModularityTests must pass before any PR can be merged. These tests enforce module boundaries and prevent architectural violations.

---

## Making Changes

### Branch Naming

Use the following branch naming conventions:

| Prefix | Purpose | Example |
|--------|---------|--------|
| `feature/` | New features | `feature/late-return-penalty` |
| `bugfix/` | Bug fixes | `bugfix/123-payment-timeout` |
| `hotfix/` | Critical production fixes | `hotfix/security-vulnerability` |
| `docs/` | Documentation updates | `docs/api-error-codes` |

### Commit Messages

This project follows the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Formatting, no code change |
| `refactor` | Code refactoring |
| `test` | Adding tests |
| `chore` | Maintenance tasks |

**Examples:**
```
feat(rental): add late return penalty calculation
fix(payment): handle Stripe webhook timeout
docs(api): add error codes documentation
test(car): add availability search tests
```

### Pull Request Process

1. **Create a feature branch** from `main`
2. **Make your changes** following the code style guidelines
3. **Write/update tests** for your changes
4. **Ensure all tests pass** locally
5. **Create a Pull Request** with a clear description

**PR Checklist:**

```markdown
- [ ] Code follows project style guidelines
- [ ] Tests added/updated for changes
- [ ] All tests pass (`mvn test`)
- [ ] Modularity tests pass (`mvn test -Dtest=ModularityTests`)
- [ ] Documentation updated if needed
- [ ] No new warnings introduced
- [ ] PR description explains changes
```

---

## Reporting Issues

### Bug Reports

When reporting a bug, please include:

1. **Summary** - A clear and descriptive title
2. **Environment** - Java version, OS, deployment type
3. **Steps to Reproduce** - Detailed steps to reproduce the issue
4. **Expected Behavior** - What you expected to happen
5. **Actual Behavior** - What actually happened
6. **Logs/Screenshots** - Relevant error messages or screenshots

**Example:**
```
Title: Payment webhook fails with 500 error on retry

Environment: Java 17, Ubuntu 22.04, Railway deployment
Steps: 1. Create rental  2. Complete payment  3. Webhook times out  4. Stripe retries
Expected: Webhook handles retry gracefully
Actual: 500 Internal Server Error on retry
Logs: [attached stack trace]
```

### Feature Requests

When requesting a feature, please include:

1. **Problem Statement** - What problem does this solve?
2. **Proposed Solution** - How would you like it to work?
3. **Alternatives Considered** - Other approaches you've thought about
4. **Additional Context** - Any other relevant information

> 💡 **Tip**: Check existing issues first to avoid duplicates.

---

## Code Review

### Review Process

1. **Automated Checks** - CI runs tests and modularity verification
2. **Code Review** - I will review your code
3. **Feedback** - I'll provide feedback or request changes if needed
4. **Approval** - I approve if all criteria are met
5. **Merge** - I merge the PR into `main`

### Approval Requirements

- All CI checks must pass
- My approval on the PR
- No unresolved conversations
- Branch is up-to-date with `main`

> ⏱️ **Response Time**: I aim to review PRs within 48-72 hours.

---

## Additional Resources

- **[README.md](README.md)** - Project overview and quick start
- **[Developer Guide](docs/development/DEVELOPER_GUIDE.md)** - In-depth development documentation
- **[API Documentation](http://localhost:8080/swagger-ui.html)** - Interactive Swagger UI
- **[Architecture Decisions](docs/architecture/)** - ADRs explaining design choices

---

## Questions?

If you have questions that aren't covered in this guide, feel free to open an issue with the `question` label.

**Thank you for contributing! 🚗💨**
