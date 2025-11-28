# 🚗 car-rental

A production-ready REST API for car rental management, built with Spring Boot to demonstrate enterprise-level backend development practices.

## 📖 About

This project was developed as a learning exercise to solidify Spring Boot knowledge and implement real-world backend patterns. It showcases a complete car rental system with authentication, authorization, and business logic.

**Key Learning Goals:**
- Spring Boot ecosystem mastery
- RESTful API design
- Security implementation (JWT)
- Database management with JPA/Hibernate
- Testing strategies
- Production deployment practices

## ✨ Features

### Core Functionality
- **User Management** - Registration, authentication with JWT
- **Car Catalog** - Browse available vehicles with filtering and pagination
- **Rental Process** - Request → Confirm → Pickup → Return workflow
- **Role-Based Access** - Separate permissions for users and administrators
- **Admin Operations** - Manage cars, rentals, and users via REST API

### Technical Highlights
- RESTful API design with proper HTTP methods and status codes
- JWT-based stateless authentication
- Role-based authorization (USER, ADMIN)
- Database migrations with Flyway
- Input validation and error handling
- API documentation with Swagger/OpenAPI
- Caffeine cache for performance
- Real-time currency conversion with fallback rates
- Integration test coverage

## 🛠️ Tech Stack

**Backend Framework:**
- Spring Boot 3.5.6
- Spring Security
- Spring Data JPA
- Spring Validation

**Database:**
- PostgreSQL
- Flyway (migrations)

**Security:**
- JWT (JSON Web Tokens)
- BCrypt password hashing

**Documentation:**
- Swagger/OpenAPI 3.0

**Testing:**
- JUnit 5
- JaCoCo (code coverage)
- Integration tests

**Build & Tools:**
- Maven
- Lombok
- MapStruct

## 🚀 Quick Start

### Prerequisites
- Java 17+
- PostgreSQL 15+
- Maven 3.8+

### Environment Variables
Create a `.env` file based on `.env.example`:

```bash
# Application
SPRING_PROFILES_ACTIVE=dev

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/car_rental
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your-secure-secret-key-here-minimum-256-bits
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000
```

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/TheOdrig/car-rental.git
cd car-rental
```

2. **Set up database**
```bash
# Create PostgreSQL database
createdb car_rental

# Create schema
psql -d car_rental -c "CREATE SCHEMA IF NOT EXISTS gallery;"

# Migrations will run automatically on startup via Flyway
```

3. **Build and run**
```bash
mvn clean install
mvn spring-boot:run
```

4. **Access the API**
- API Base URL: `http://localhost:8082`
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- API Docs: `http://localhost:8082/v3/api-docs`

## 📚 API Documentation

### Authentication Endpoints

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
```

### Car Management

```http
GET    /api/cars                    # List all cars
GET    /api/cars/{id}               # Get car details
POST   /api/cars                    # Create car (admin)
PUT    /api/cars/{id}               # Update car (admin)
DELETE /api/cars/{id}               # Delete car (admin)

# Business Operations
POST   /api/cars/{id}/reserve       # Reserve car (admin)
POST   /api/cars/{id}/sell          # Sell car (admin)
POST   /api/cars/{id}/maintenance   # Mark as maintenance (admin)

# Search & Statistics
GET    /api/cars/search             # Advanced search
GET    /api/cars/statistics         # Car statistics (admin)
GET    /api/cars/brand/{brand}      # Filter by brand
```

### Rental Operations

```http
POST   /api/rentals/request           # Request rental (user)
GET    /api/rentals/me                # My rentals (user)
GET    /api/rentals/me?currency=USD   # My rentals with USD prices
POST   /api/rentals/{id}/confirm      # Confirm rental (admin)
POST   /api/rentals/{id}/pickup       # Mark as picked up (admin)
POST   /api/rentals/{id}/return       # Mark as returned (admin)
GET    /api/rentals/admin             # All rentals (admin)
```

### Currency Conversion

```http
GET    /api/exchange-rates              # Get all exchange rates
GET    /api/exchange-rates/{from}/{to}  # Get specific rate (e.g., USD/TRY)
POST   /api/exchange-rates/convert      # Convert amount between currencies
POST   /api/exchange-rates/refresh      # Force refresh rates (admin)

# Currency parameter on other endpoints
GET    /api/cars?currency=USD           # Car prices in USD
GET    /api/cars/{id}?currency=EUR      # Car details with EUR prices
GET    /api/rentals/me?currency=GBP     # Rental prices in GBP
```

**Supported Currencies:** TRY, USD, EUR, GBP, JPY

### Example Request

```bash
# Register a new user
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "securepass123"
  }'

# Login
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "securepass123"
  }'

# List cars (with token)
curl -X GET http://localhost:8082/api/cars \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🧪 Testing

### Run Tests
```bash
# Run all tests
mvn clean test

# Generate coverage report
mvn jacoco:report

# View coverage report
# Open: target/site/jacoco/index.html
```

**Current Coverage:** ~70-90% across controllers and services

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/com/akif/
│   │   ├── config/          # Security, Swagger, CORS configs
│   │   ├── controller/      # REST endpoints
│   │   ├── dto/             # Request/Response objects
│   │   │   ├── request/     # Request DTOs
│   │   │   └── response/    # Response DTOs
│   │   ├── enums/           # Status types, currencies, roles
│   │   ├── exception/       # Custom exceptions
│   │   ├── handler/         # Global exception handler
│   │   ├── mapper/          # Entity-DTO mappers (MapStruct)
│   │   ├── model/           # JPA entities
│   │   ├── repository/      # Data access layer
│   │   ├── security/        # JWT, UserDetails, filters
│   │   ├── service/         # Business logic
│   │   │   ├── gateway/     # Payment gateway interfaces
│   │   │   └── impl/        # Service implementations
│   │   └── starter/         # Application entry point
│   └── resources/
│       ├── db/migration/    # Flyway SQL scripts
│       └── application.properties
└── test/                    # Integration tests
```

## 🔒 Security

- **Authentication:** JWT tokens with configurable expiration
- **Authorization:** Role-based access control (RBAC)
- **Password Storage:** BCrypt hashing
- **Input Validation:** Bean Validation annotations
- **SQL Injection:** Prevented via JPA/Hibernate
- **CORS:** Configurable allowed origins

## 📊 Database Schema

**Main Tables:**
- `users` - User accounts and credentials
- `user_roles` - Role assignments
- `cars` - Vehicle inventory
- `rentals` - Rental transactions
- `payments` - Payment records

**Relationships:**
- User → Rentals (one-to-many)
- Car → Rentals (one-to-many)
- Rental → Payments (one-to-many)
- User → Roles (one-to-many)

## 🐳 Docker Support

Containerized deployment with multi-stage Dockerfile for optimized image size and production readiness.

## 📈 Performance

- **Caching:** Caffeine cache for frequently accessed data
- **Pagination:** All list endpoints support pagination
- **Connection Pooling:** HikariCP (default in Spring Boot)
- **Query Optimization:** Proper JPA relationships and fetch strategies

## 🤝 Contributing

This is a learning project, but suggestions are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is open source and available under the MIT License.

## 👤 Author

**Mehmet Akif Uludag**

- Learning Spring Boot through hands-on development
- Focus: Backend development, REST APIs, Spring ecosystem
- GitHub: [@TheOdrig](https://github.com/TheOdrig)

## 📚 What I Learned

Through this project, I gained practical experience with:

- **Spring Boot Architecture** - Understanding auto-configuration, starters, and conventions
- **Spring Security** - Implementing JWT authentication from scratch
- **JPA/Hibernate** - Entity relationships, query optimization, N+1 problem
- **RESTful Design** - Proper HTTP methods, status codes, and API versioning
- **Database Migrations** - Managing schema changes with Flyway
- **Testing Strategies** - Unit tests, integration tests, API testing
- **Error Handling** - Global exception handling and meaningful error responses
- **Documentation** - API documentation with Swagger/OpenAPI
- **Production Readiness** - Logging, monitoring, security best practices

## 🎯 Project Status

**✅ Completed Features:**
- Core rental functionality
- JWT authentication
- Real-time currency conversion (TRY, USD, EUR, GBP, JPY)
- Integration tests
- API documentation
- Docker support
- Cloud deployment (Railway)

**📋 Possible Future Enhancements:**
- Redis caching
- CI/CD pipeline
- Advanced monitoring

> This project achieved its learning goals. Additional features may be added based on interest.
> 
**⭐ If you find this project helpful for learning Spring Boot, please give it a star!**
