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
- **User Management** - Registration, authentication with JWT, OAuth2 social login (Google, GitHub)
- **Car Catalog** - Browse available vehicles with filtering and pagination
- **Availability Calendar** - Date-based availability search, monthly calendar view, similar car recommendations
- **Rental Process** - Request → Confirm → Pickup → Return workflow
- **Dynamic Pricing** - Intelligent pricing with 5 strategies (season, early booking, duration, weekend, demand)
- **Payment Processing** - Stripe integration with secure checkout, webhook handling, and reconciliation
- **Email Notifications** - Event-driven automated emails (confirmation, receipt, reminders, cancellation)
- **Late Return & Penalty** - Automated detection, grace period, smart penalty calculation, admin waiver system
- **Role-Based Access** - Separate permissions for users and administrators
- **Admin Operations** - Manage cars, rentals, users, penalties via REST API

### Technical Highlights
- RESTful API design with proper HTTP methods and status codes
- JWT-based stateless authentication
- Role-based authorization (USER, ADMIN)
- Dynamic pricing engine with strategy pattern
- Stripe payment gateway with webhook verification
- Idempotent payment operations
- Payment reconciliation with discrepancy detection
- Event-driven email notifications with async processing
- Automatic retry logic with exponential backoff
- Scheduled reminder system with duplicate prevention
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
- Spring Mail
- Spring Retry
- Thymeleaf (email templates)

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

# OAuth2 Social Login (Optional)
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
OAUTH2_STATE_SECRET=your-random-secret-key-here
APP_BASE_URL=http://localhost:8082

# Stripe Payment Gateway
STRIPE_API_KEY=your-stripe-secret-key
STRIPE_PUBLISHABLE_KEY=your-stripe-publishable-key
STRIPE_WEBHOOK_SECRET=your-webhook-signing-secret
```

### Pricing Configuration (Optional)

Default pricing values are pre-configured. Override in `application.properties` if needed:

```properties
# Example: Increase peak season surcharge
pricing.season.peak.multiplier=1.30

# Example: Better early booking discount
pricing.early-booking.tier1.multiplier=0.80

# Example: Disable weekend pricing
pricing.strategy.weekend-enabled=false
```

See `PricingConfig.java` for all available configuration options.

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

### OAuth2 Social Login

```http
GET    /api/oauth2/authorize/{provider}    # Initiate login (google/github)
GET    /api/oauth2/callback/{provider}     # OAuth2 callback
POST   /api/oauth2/link/{provider}         # Link social account (authenticated)
```

**Supported Providers:** Google, GitHub

**Setup:** Get OAuth2 credentials from [Google Cloud Console](https://console.cloud.google.com/) or [GitHub Developer Settings](https://github.com/settings/developers). Add to `.env`:

```bash
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
GITHUB_CLIENT_ID=your-client-id
GITHUB_CLIENT_SECRET=your-client-secret
```

**Features:** Single sign-on, account linking, multi-provider support, automatic user creation

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

### Availability & Smart Search

```http
POST   /api/cars/availability/search           # Search available cars by date range
GET    /api/cars/{id}/availability/calendar    # Get monthly availability calendar
GET    /api/cars/{id}/similar                  # Get similar car recommendations
```

**Availability Search:**
- **Date-based filtering** - Find cars available for specific rental period
- **Smart filtering** - Combine date availability with brand, model, price, body type, seats
- **Dynamic pricing** - Real-time price calculation for selected dates
- **Currency conversion** - View prices in preferred currency (TRY, USD, EUR, GBP, JPY)
- **Pagination** - Efficient result browsing (default 20, max 100 per page)

**Calendar View:**
- **Monthly availability** - Day-by-day availability status for up to 3 months ahead
- **Rental tracking** - See which days are booked vs available
- **Blocking status** - Automatically marks cars in maintenance/damaged as unavailable

**Similar Cars:**
- **Smart recommendations** - Up to 5 similar cars when preferred car is unavailable
- **Similarity scoring** - Body type match (+50), brand match (+30), price match (+20)
- **Availability filtering** - Only shows cars available for selected dates
- **Price comparison** - Compare prices across similar vehicles

**Example Requests:**

```bash
# Search available cars for date range
curl -X POST http://localhost:8082/api/cars/availability/search \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2024-07-15",
    "endDate": "2024-07-22",
    "bodyType": "SUV",
    "minPrice": 100,
    "maxPrice": 300,
    "targetCurrency": "USD",
    "page": 0,
    "size": 20
  }'

# Get monthly availability calendar
curl -X GET "http://localhost:8082/api/cars/1/availability/calendar?month=2024-07"

# Get similar car recommendations
curl -X GET "http://localhost:8082/api/cars/1/similar?startDate=2024-07-15&endDate=2024-07-22&limit=5"
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

### Late Return & Penalty Management

```http
GET    /api/admin/late-returns                    # Late return report (admin)
GET    /api/admin/late-returns/statistics         # Late return statistics (admin)
POST   /api/admin/rentals/{id}/penalty/waive      # Waive penalty (admin)
GET    /api/admin/rentals/{id}/penalty/history    # Penalty history (admin)
```

**Late Return System:**
- **Automated Detection** - Scheduled job runs every 15 minutes to detect overdue rentals
- **Grace Period** - Configurable tolerance (default 1 hour, range 0-120 minutes)
- **Smart Penalty Calculation:**
  - 1-6 hours late: 10% per hour × daily rate
  - 7-24 hours late: 150% × daily rate (flat)
  - 1+ days late: 150% per day × daily rate
  - Maximum cap: 5× daily rate
- **Status Tracking** - ON_TIME → GRACE_PERIOD → LATE → SEVERELY_LATE (24+ hours)
- **Automated Notifications** - 4 email types (grace period warning, late notification, severely late alert, penalty summary)
- **Payment Integration** - Automatic penalty charge via Stripe, failed payment handling
- **Admin Waiver** - Full or partial penalty waiver with mandatory reason, refund support
- **Reporting** - Filterable reports, statistics (total late returns, penalty amounts, average late duration)

**Configuration:**
```properties
# Grace period (minutes)
penalty.grace-period-minutes=60

# Penalty rates
penalty.hourly-penalty-rate=0.10
penalty.daily-penalty-rate=1.50
penalty.penalty-cap-multiplier=5.0
penalty.severely-late-threshold-hours=24
```

**Features:**
- Event-driven architecture (4 domain events)
- Pagination for large datasets
- Error recovery and retry logic
- Comprehensive audit trail
- Currency consistency with original rental

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

### Dynamic Pricing

```http
POST   /api/pricing/calculate           # Calculate price for rental period
GET    /api/pricing/preview             # Preview price without booking
GET    /api/pricing/strategies          # List enabled pricing strategies
```

**Pricing Strategies:**
1. **Season Pricing** - Peak season (Jun-Aug): +25%, Off-peak (Nov-Feb): -10%
2. **Early Booking** - Book 30+ days ahead: -15%, 14-29 days: -10%, 7-13 days: -5%
3. **Duration Discount** - 7-13 days: -10%, 14-29 days: -15%, 30+ days: -20%
4. **Weekend Pricing** - Friday-Sunday: +15%
5. **Demand Pricing** - High demand (>80% occupancy): +20%, Moderate (50-80%): +10%

**Configuration:** All multipliers and thresholds are configurable via `application.properties`

### Payment Processing

```http
POST   /api/webhooks/stripe             # Stripe webhook endpoint (public)
```

**Stripe Integration:**
- **Checkout Sessions** - Secure payment flow with Stripe Checkout
- **Webhook Events** - Real-time payment status updates (checkout.session.completed, checkout.session.expired, payment_intent.payment_failed)
- **Signature Verification** - Webhook authenticity validation
- **Idempotency** - Duplicate payment prevention with idempotency keys
- **Reconciliation** - Scheduled daily job for Stripe-database sync with discrepancy detection (runs at midnight)

**Setup:** Get API keys from [Stripe Dashboard](https://dashboard.stripe.com/apikeys). Add to `.env`:

```bash
STRIPE_API_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

**Features:** Secure checkout, automatic payment sync, webhook handling, daily reconciliation reports (logged)

### Email Notifications

```http
# No direct endpoints - emails are sent automatically via event-driven system
```

**Email Types:**
1. **Rental Confirmation** - Sent when rental is confirmed (includes rental ID, car details, dates, total price, pickup location)
2. **Payment Receipt** - Sent after payment capture (includes transaction ID, amount, currency, payment date, rental reference)
3. **Pickup Reminder** - Sent 1 day before pickup date at 8 AM (includes pickup date, time window, location, car details)
4. **Return Reminder** - Sent on return date at 9 AM (includes return date, location, late penalty information)
5. **Cancellation Confirmation** - Sent when rental is cancelled (includes cancellation date, refund details if applicable)

**Architecture:**
- **Event-Driven** - Uses Spring Events for decoupled notification system
- **Async Processing** - Non-blocking email delivery with dedicated thread pool (2-10 threads)
- **Retry Logic** - Automatic retry up to 3 times with exponential backoff (1s, 2s, 4s)
- **Template Engine** - Thymeleaf HTML templates for professional email formatting
- **Scheduled Reminders** - Daily cron jobs for pickup (8 AM) and return (9 AM) reminders

**Configuration:**

Development mode (logs emails without sending):
```properties
spring.profiles.active=dev
email.enabled=true
```

Production mode (SendGrid SMTP):
```properties
spring.profiles.active=prod
email.enabled=true
email.from=noreply@yourcompany.com

# SendGrid SMTP Configuration
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=${SENDGRID_API_KEY}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**Environment Variables:**
```bash
# Email Configuration
EMAIL_FROM=noreply@yourcompany.com
EMAIL_ENABLED=true

# SendGrid (Production only)
SENDGRID_API_KEY=SG.your-sendgrid-api-key
SENDGRID_HOST=smtp.sendgrid.net
SENDGRID_PORT=587
SENDGRID_USERNAME=apikey
```

**SendGrid Setup (Production):**
1. Create account at [SendGrid](https://sendgrid.com/)
2. Generate API key in Settings → API Keys
3. Verify sender email in Settings → Sender Authentication
4. Add API key to `.env` file as `SENDGRID_API_KEY`
5. Configure `email.from` with verified sender email

**Features:**
- Automatic email sending on rental lifecycle events
- Professional HTML email templates
- Retry mechanism for failed deliveries
- Development mode with mock sender (logs only)
- Production mode with SendGrid SMTP
- Scheduled daily reminders with duplicate prevention
- Comprehensive logging for troubleshooting

**Email Flow:**
```
Rental Confirmed → RentalConfirmedEvent → EmailEventListener → EmailNotificationService → SendGrid
Payment Captured → PaymentCapturedEvent → EmailEventListener → EmailNotificationService → SendGrid
Scheduler (8 AM) → PickupReminderEvent → EmailEventListener → EmailNotificationService → SendGrid
Scheduler (9 AM) → ReturnReminderEvent → EmailEventListener → EmailNotificationService → SendGrid
Rental Cancelled → RentalCancelledEvent → EmailEventListener → EmailNotificationService → SendGrid
```

### Example Requests

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

# Calculate rental price
curl -X POST http://localhost:8082/api/pricing/calculate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "carId": 1,
    "startDate": "2024-07-15",
    "endDate": "2024-07-22"
  }'
```

## 🧪 Testing

```bash
# Run all tests
mvn clean test

# Run only E2E tests
mvn test -Dtest="com.akif.e2e.**"

# Generate coverage report
mvn jacoco:report

# View coverage report
# Open: target/site/jacoco/index.html
```

**Test Types:**
- **Unit Tests** - Service layer, mappers, utilities (JUnit 5, Mockito)
- **Integration Tests** - REST controllers with MockMvc
- **E2E Tests** - Complete user scenarios across all system components

**E2E Test Coverage:**
- Complete rental lifecycle (request → confirm → pickup → return)
- Cancellation and refund flows (REQUESTED, CONFIRMED, IN_USE states)
- Availability search and calendar generation
- Similar car recommendations with similarity scoring
- Dynamic pricing integration (all 5 strategies combined)
- Currency conversion with fallback rates
- Payment gateway operations (authorize, capture, refund)
- Late return & penalty flow (detection, calculation, payment, waiver)
- Email event publishing verification
- Role-based authorization (USER vs ADMIN)
- Date overlap prevention and availability
- Error handling and edge cases
- Concurrent operations and idempotency

**Test Infrastructure:**
- `E2ETestBase` - Base class with MockMvc, JWT token generation
- `TestDataBuilder` - Utility for creating test data
- `TestEventCaptor` - Captures domain events for verification
- `TestFixtures` - Common test constants

**Test Configuration:**
- H2 in-memory database (isolated per test)
- MockEmailSender (logs only, no actual sending)
- StubPaymentGateway (no Stripe API key required)
- Mocked scheduler (no scheduled tasks during tests)

**Current Coverage:** ~70-90% across controllers and services

## 🏗️ Project Structure

```
src/
├── main/
│   ├── java/com/akif/
│   │   ├── config/          # Security, Swagger, CORS, Async configs
│   │   ├── controller/      # REST endpoints
│   │   ├── dto/             # Request/Response objects
│   │   │   ├── request/     # Request DTOs
│   │   │   ├── response/    # Response DTOs
│   │   │   └── email/       # Email message DTOs
│   │   ├── enums/           # Status types, currencies, roles, email types
│   │   ├── event/           # Domain events (rental, payment)
│   │   ├── exception/       # Custom exceptions
│   │   ├── handler/         # Global exception handler
│   │   ├── listener/        # Event listeners (email)
│   │   ├── mapper/          # Entity-DTO mappers (MapStruct)
│   │   ├── model/           # JPA entities
│   │   ├── repository/      # Data access layer
│   │   ├── scheduler/       # Scheduled tasks (reminders, reconciliation)
│   │   ├── security/        # JWT, UserDetails, filters
│   │   ├── service/         # Business logic
│   │   │   ├── email/       # Email notification services
│   │   │   ├── gateway/     # Payment gateway interfaces
│   │   │   └── impl/        # Service implementations
│   │   └── starter/         # Application entry point
│   └── resources/
│       ├── db/migration/    # Flyway SQL scripts
│       ├── templates/email/ # Thymeleaf email templates
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
- `linked_accounts` - OAuth2 social account links
- `cars` - Vehicle inventory
- `rentals` - Rental transactions with reminder tracking and late return fields (late_return_status, late_detected_at, penalty_amount)
- `payments` - Payment records with Stripe integration
- `penalty_waivers` - Penalty waiver records with audit trail
- `webhook_events` - Idempotent webhook event processing

**Relationships:**
- User → Rentals (one-to-many)
- User → LinkedAccounts (one-to-many)
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
- **Design Patterns** - Strategy pattern for dynamic pricing, event-driven architecture, dependency injection, service composition
- **Event-Driven Systems** - Spring Events for decoupled communication between components
- **Async Processing** - Non-blocking operations with thread pools and @Async
- **Retry Logic** - Implementing resilience with Spring Retry and exponential backoff
- **JPA/Hibernate** - Entity relationships, query optimization, N+1 problem, complex queries with NOT EXISTS subqueries
- **RESTful Design** - Proper HTTP methods, status codes, and API versioning
- **Database Migrations** - Managing schema changes with Flyway
- **Template Engines** - Thymeleaf for dynamic HTML email generation
- **Scheduled Tasks** - Cron-based scheduling for automated reminders
- **Testing Strategies** - Unit tests, integration tests, API testing
- **Error Handling** - Global exception handling and meaningful error responses
- **Documentation** - API documentation with Swagger/OpenAPI
- **Production Readiness** - Logging, monitoring, security best practices

## 🎯 Project Status

**✅ Completed Features:**
- Core rental functionality
- JWT authentication
- OAuth2 social login (Google, GitHub)
- Availability calendar & smart search (date-based filtering, similar car recommendations)
- Dynamic pricing engine with 5 strategies
- Stripe payment gateway integration
- Real-time currency conversion (TRY, USD, EUR, GBP, JPY)
- Email notification system (event-driven, async, with retry logic)
- Late return & penalty system (automated detection, grace period, smart calculation, admin waiver)
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
