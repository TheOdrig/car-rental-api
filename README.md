# 🚗 Car Rental API

A REST API for car rental management, built with **Spring Boot** and **Spring Modulith** modular architecture.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)](https://spring.io/projects/spring-boot)
[![Tests](https://img.shields.io/badge/Tests-Maven%20verify-blue)](#-testing)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

## ✨ Features

| Feature | Description |
|---------|-------------|
| **Modular Architecture** | 9 Spring Modulith modules with enforced boundaries |
| **Admin Dashboard** | Real-time metrics, alerts, quick actions, revenue analytics |
| **Rental Lifecycle** | Request → Confirm → Pickup → Return workflow |
| **Payment Processing** | Stripe integration with webhooks and reconciliation |
| **Dynamic Pricing** | 5 strategies: season, early booking, duration, weekend, demand |
| **Late Return System** | Automated detection, grace period, penalty calculation |
| **Damage Management** | Photo evidence, assessment, disputes, resolution |
| **Email Notifications** | Event-driven with async processing and retry logic |
| **OAuth2 Login** | Google and GitHub social authentication |
| **Currency Conversion** | Real-time exchange rates with fallback |
| **Availability Search** | Date-based search with similar car recommendations |

## 🏛️ Architecture

This project uses **Spring Modulith** for modular monolith architecture with clear module boundaries.

<details>
<summary>📊 <b>View Module Dependency Diagram</b></summary>

```mermaid
graph TD
    subgraph Core
        shared[🔧 shared]
    end
    
    subgraph Business Modules
        auth[🔐 auth]
        car[🚗 car]
        currency[💱 currency]
        rental[📋 rental]
        damage[🔨 damage]
        payment[💳 payment]
        dashboard[📊 dashboard]
        notification[📧 notification]
    end
    
    auth --> shared
    currency --> shared
    car --> currency
    car --> shared
    payment --> shared
    rental --> car
    rental --> auth
    rental --> currency
    rental --> payment
    damage --> rental
    damage --> car
    damage --> payment
    dashboard --> rental
    dashboard --> car
    dashboard --> payment
    dashboard --> damage
    notification -.->|events| rental
    notification -.->|events| damage
    notification -.->|events| dashboard
```

</details>

### Modules

| Module | Responsibility | Dependencies |
|--------|----------------|--------------|
| `shared` | Cross-cutting: security, exceptions, base classes | - |
| `auth` | JWT authentication, OAuth2, user management | `shared` |
| `currency` | Exchange rates, currency conversion | `shared` |
| `car` | Vehicle catalog, availability, pricing | `currency`, `shared` |
| `payment` | Stripe gateway, webhooks, reconciliation | `shared` |
| `rental` | Rental lifecycle, late returns, penalties | `car`, `auth`, `currency`, `payment` |
| `damage` | Damage reports, assessment, disputes | `rental`, `car`, `payment` |
| `dashboard` | Admin metrics, alerts, quick actions | `rental`, `car`, `payment`, `damage` |
| `notification` | Event-driven email notifications | *(events only)* |

Module boundaries are enforced with Spring Modulith.

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Framework | Spring Boot 3.5, Spring Modulith |
| Security | Spring Security, JWT, OAuth2 |
| Database | PostgreSQL, Flyway migrations |
| Payment | Stripe API |
| Email | SendGrid SMTP, Thymeleaf templates |
| Cache | Caffeine |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| Docs | Swagger/OpenAPI |

## 🚀 Quick Start

### Prerequisites
- Java 17+
- PostgreSQL 15+
- Maven 3.8+

### Setup

```bash
# Clone
git clone https://github.com/TheOdrig/car-rental-api.git
cd car-rental-api

# Configure environment
cp .env.example .env
# Edit .env with your credentials

# Run
mvn spring-boot:run
```

### Access
- **API:** `http://localhost:8082`
- **Swagger UI:** `http://localhost:8082/swagger-ui.html`

## 📚 API Overview

### Authentication
```http
POST /api/v1/auth/register     # Register user
POST /api/v1/auth/login        # Login, get JWT
GET  /api/v1/oauth2/authorize/{provider}  # OAuth2 login
```

### Cars
```http
GET  /api/v1/cars              # List all cars
GET  /api/v1/cars/{id}         # Get car details
POST /api/v1/cars/availability/search  # Search by date range
GET  /api/v1/cars/{id}/similar # Similar car recommendations
```

### Rentals
```http
POST /api/v1/rentals/request   # Request rental
GET  /api/v1/rentals/me        # My rentals
POST /api/v1/rentals/{id}/confirm  # Admin: confirm
POST /api/v1/rentals/{id}/return   # Admin: complete return
```

### Admin Operations
```http
GET  /api/v1/admin/dashboard/summary   # Daily summary
GET  /api/v1/admin/dashboard/fleet     # Fleet status
GET  /api/v1/admin/dashboard/metrics   # Monthly metrics
GET  /api/v1/admin/dashboard/revenue   # Revenue analytics
GET  /api/v1/admin/alerts              # Active alerts
POST /api/v1/admin/alerts/{id}/acknowledge  # Acknowledge alert
POST /api/v1/admin/quick-actions/rentals/{id}/approve  # Approve rental
POST /api/v1/admin/quick-actions/rentals/{id}/pickup   # Process pickup
POST /api/v1/admin/quick-actions/rentals/{id}/return   # Process return
GET  /api/v1/admin/late-returns        # Late return report
POST /api/v1/admin/damages             # Create damage report
POST /api/v1/admin/damages/{id}/assess # Assess damage
```

📖 Full API documentation available at `/swagger-ui.html`

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run modularity verification
mvn test -Dtest=ModularityTests

# Generate coverage report
mvn jacoco:report
```

**Test Coverage:**
- Large automated suite (run `./mvnw verify` for current results)
- Unit, integration, and E2E tests
- Module boundary verification
- Complete rental lifecycle coverage

## 📁 Project Structure

```
src/main/java/com/akif/
├── auth/           # Authentication module
├── car/            # Car management module
├── currency/       # Currency conversion module
├── damage/         # Damage management module
├── dashboard/      # Admin dashboard module
├── notification/   # Email notification module
├── payment/        # Payment processing module
├── rental/         # Rental operations module
├── shared/         # Shared kernel
└── starter/        # Application entry point
```

## 🔧 Configuration

Configuration options include:
- Database setup
- Stripe integration
- Email configuration
- Pricing strategies
- OAuth2 providers

## 📊 Technical Highlights

- **Modular Monolith** with Spring Modulith (9 modules, enforced boundaries)
- **Admin Dashboard** with real-time metrics, 5-level alert system, event-driven cache invalidation
- **Event-Driven Architecture** for decoupled module communication
- **ID Reference + Denormalization** pattern for cross-module entities
- **Automated test suite** with unit, integration and E2E tests
- **Local verification gate** (`scripts/verify`, pre-push hook)
- **5 ADRs** documenting architectural decisions

## 🤝 Contributing

Contributions are welcome.

Quick steps:
1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Run tests (`mvn test`)
4. Commit changes using [Conventional Commits](https://www.conventionalcommits.org/)
5. Push and open Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Mehmet Akif Uludag**

[![GitHub](https://img.shields.io/badge/GitHub-TheOdrig-black?logo=github)](https://github.com/TheOdrig)

---
