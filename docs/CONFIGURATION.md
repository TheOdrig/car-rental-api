# Configuration Guide

This document provides detailed configuration options for the Car Rental API.

## Table of Contents
- [Database](#database)
- [JWT Authentication](#jwt-authentication)
- [OAuth2 Social Login](#oauth2-social-login)
- [Stripe Payment](#stripe-payment)
- [Email Notifications](#email-notifications)
- [Dynamic Pricing](#dynamic-pricing)
- [Late Return Penalties](#late-return-penalties)

---

## Database

### PostgreSQL Setup

```bash
# Create database
createdb car_rental

# Create schema
psql -d car_rental -c "CREATE SCHEMA IF NOT EXISTS gallery;"
```

### Environment Variables

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/car_rental
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

Flyway migrations run automatically on startup.

---

## JWT Authentication

```bash
JWT_SECRET=your-secure-secret-key-here-minimum-256-bits
JWT_ACCESS_TOKEN_EXPIRATION=900000      # 15 minutes
JWT_REFRESH_TOKEN_EXPIRATION=604800000  # 7 days
```

---

## OAuth2 Social Login

### Google

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create OAuth 2.0 credentials
3. Add authorized redirect URI: `http://localhost:8082/api/oauth2/callback/google`

```bash
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

### GitHub

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Create OAuth App
3. Set callback URL: `http://localhost:8082/api/oauth2/callback/github`

```bash
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
```

### Common OAuth2 Settings

```bash
OAUTH2_STATE_SECRET=your-random-secret-key-here
APP_BASE_URL=http://localhost:8082
```

---

## Stripe Payment

### Setup

1. Get API keys from [Stripe Dashboard](https://dashboard.stripe.com/apikeys)
2. Configure webhook endpoint in Stripe Dashboard

```bash
STRIPE_API_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

### Webhook Events

The API handles these Stripe events:
- `checkout.session.completed`
- `checkout.session.expired`
- `payment_intent.payment_failed`

### Webhook Endpoint

```
POST /api/webhooks/stripe
```

---

## Email Notifications

### Development (Mock Sender)

```properties
spring.profiles.active=dev
email.enabled=true
```

Emails are logged but not sent.

### Production (SendGrid)

1. Create account at [SendGrid](https://sendgrid.com/)
2. Generate API key
3. Verify sender email

```bash
EMAIL_FROM=noreply@yourcompany.com
EMAIL_ENABLED=true
SENDGRID_API_KEY=SG.your-api-key
```

```properties
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=${SENDGRID_API_KEY}
```

### Email Types

| Type | Trigger | Schedule |
|------|---------|----------|
| Rental Confirmation | Rental confirmed | Immediate |
| Payment Receipt | Payment captured | Immediate |
| Pickup Reminder | 1 day before pickup | 8 AM |
| Return Reminder | Return date | 9 AM |
| Cancellation | Rental cancelled | Immediate |

---

## Dynamic Pricing

### Strategy Configuration

All pricing strategies are configurable via `application.properties`:

```properties
# Season Pricing
pricing.season.peak.multiplier=1.25
pricing.season.peak.months=6,7,8
pricing.season.offpeak.multiplier=0.90
pricing.season.offpeak.months=11,12,1,2

# Early Booking Discount
pricing.early-booking.tier1.days=30
pricing.early-booking.tier1.multiplier=0.85
pricing.early-booking.tier2.days=14
pricing.early-booking.tier2.multiplier=0.90
pricing.early-booking.tier3.days=7
pricing.early-booking.tier3.multiplier=0.95

# Duration Discount
pricing.duration.tier1.days=7
pricing.duration.tier1.multiplier=0.90
pricing.duration.tier2.days=14
pricing.duration.tier2.multiplier=0.85
pricing.duration.tier3.days=30
pricing.duration.tier3.multiplier=0.80

# Weekend Pricing
pricing.weekend.multiplier=1.15
pricing.strategy.weekend-enabled=true

# Demand Pricing
pricing.demand.high.threshold=0.80
pricing.demand.high.multiplier=1.20
pricing.demand.moderate.threshold=0.50
pricing.demand.moderate.multiplier=1.10
```

### Disable Strategies

```properties
pricing.strategy.season-enabled=true
pricing.strategy.early-booking-enabled=true
pricing.strategy.duration-enabled=true
pricing.strategy.weekend-enabled=true
pricing.strategy.demand-enabled=true
```

---

## Late Return Penalties

```properties
# Grace period (minutes, 0-120)
penalty.grace-period-minutes=60

# Penalty rates
penalty.hourly-penalty-rate=0.10    # 10% per hour for 1-6 hours
penalty.daily-penalty-rate=1.50     # 150% per day for 1+ days
penalty.penalty-cap-multiplier=5.0  # Maximum 5x daily rate

# Severe late threshold
penalty.severely-late-threshold-hours=24
```

### Penalty Calculation

| Duration | Calculation |
|----------|-------------|
| 1-6 hours | 10% × hours × daily rate |
| 7-24 hours | 150% × daily rate (flat) |
| 1+ days | 150% × days × daily rate |
| Maximum | 5× daily rate |

---

## Damage Management

```properties
# Severity thresholds (USD)
damage.threshold.minor=500.00
damage.threshold.moderate=2000.00
damage.threshold.major=5000.00

# Photo limits
damage.photo.max-count=10
damage.photo.max-size-mb=10
```

---

## Cache Configuration

```properties
# Exchange rate cache TTL
currency.cache.ttl-hours=1

# Car search cache
spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=300s
```

---

## Logging

### Correlation ID

All requests include a correlation ID for tracing:

```
X-Correlation-ID: uuid
```

### Log Levels

```properties
logging.level.com.akif=DEBUG
logging.level.org.springframework.security=WARN
logging.level.org.hibernate.SQL=DEBUG
```

---

## Environment Example

See `.env.example` for a complete list of required environment variables.
