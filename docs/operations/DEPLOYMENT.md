# Deployment Guide

> **Car Rental API** - Comprehensive deployment documentation for local, Docker, and production environments.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Local Development Setup](#2-local-development-setup)
3. [Environment Variables](#3-environment-variables)
4. [Database Setup](#4-database-setup)
5. [Docker Deployment](#5-docker-deployment)
6. [CI/CD Pipeline](#6-cicd-pipeline)
7. [Staging Deployment](#7-staging-deployment)
8. [Production Deployment](#8-production-deployment)
9. [Health Checks](#9-health-checks)

---

## 1. Prerequisites

| Requirement | Version | Purpose | Installation |
|-------------|---------|---------|--------------|
| **Java** | 17 (Temurin) | Runtime environment | [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17) |
| **Maven** | 3.9+ | Build tool | [Maven](https://maven.apache.org/download.cgi) |
| **PostgreSQL** | 15+ | Primary database | [PostgreSQL](https://www.postgresql.org/download/) |
| **Docker** | 20+ | Containerization | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| **Git** | 2.30+ | Version control | [Git](https://git-scm.com/downloads) |

### Verify Installation

```bash
# Check Java version
java -version
# Expected: openjdk version "17.x.x" or higher

# Check Maven version
mvn -version
# Expected: Apache Maven 3.9.x or higher

# Check PostgreSQL
psql --version
# Expected: psql (PostgreSQL) 15.x or higher

# Check Docker
docker --version
# Expected: Docker version 20.x.x or higher
```

### IDE Recommendations

| IDE | Plugins/Extensions |
|-----|-------------------|
| **IntelliJ IDEA** | Spring Boot, Lombok, Database Navigator |
| **VS Code** | Extension Pack for Java, Spring Boot Extension Pack |

---

## 2. Local Development Setup

### Quick Start (TL;DR)

```bash
# 1. Clone repository
git clone https://github.com/TheOdrig/car-rental-api.git
cd car-rental-api

# 2. Configure environment
cp .env.example .env
# Edit .env with your values (see Section 3 for details)

# 3. Start PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_DB=car_rental \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:15-alpine

# 4. Run application
mvn spring-boot:run

# 5. Verify
curl http://localhost:8082/health
# Expected: {"status":"UP"}
```

### Step-by-Step Guide

#### Step 1: Clone Repository

```bash
git clone https://github.com/TheOdrig/car-rental-api.git
cd car-rental-api
```

#### Step 2: Configure Environment

```bash
# Copy example configuration
cp .env.example .env
```

**Minimum required configuration for local development:**

```properties
# .env (minimum for local dev)
DATABASE_URL=jdbc:postgresql://localhost:5432/car_rental
DB_USERNAME=postgres
DB_PASSWORD=password
JWT_SECRET=local-dev-secret-key-minimum-256-bits-for-testing
```

> **Note:** OAuth2, Stripe, and Email settings are optional for local development.

#### Step 3: Start PostgreSQL

**Option A: Docker Compose (Recommended)**
```bash
# Start PostgreSQL with persistent volume
docker compose up -d

# Verify it's running
docker compose ps
```

> This uses `docker-compose.yml` in project root with health checks and data persistence.

**Option B: Docker Run (Quick)**
```bash
docker run -d --name postgres \
  -e POSTGRES_DB=car_rental \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:15-alpine
```

> ⚠️ Data is lost when container is removed. Use Option A for persistent storage.

**Option C: Local PostgreSQL Installation**
```sql
-- Connect to PostgreSQL and run:
CREATE DATABASE car_rental;
```

#### Step 4: Run Application

```bash
# Development mode (with hot reload)
mvn spring-boot:run

# Or build and run JAR
mvn clean package -DskipTests
java -jar target/car-rental-api-0.0.1-SNAPSHOT.jar
```

**Application starts on:** `http://localhost:8082`

#### Step 5: Verify Installation

```bash
# Health check
curl http://localhost:8082/health

# Swagger UI
open http://localhost:8082/swagger-ui.html

# API Docs
curl http://localhost:8082/v3/api-docs
```

### Local Development Troubleshooting

| Problem | Symptom | Solution |
|---------|---------|----------|
| **Database connection failed** | `Connection refused` on startup | Verify PostgreSQL is running: `docker ps` or `pg_isready` |
| **Port already in use** | `Port 8082 already in use` | Kill process: `netstat -ano \| findstr :8082` then `taskkill /PID <pid> /F` |
| **Flyway migration failed** | `Migration checksum mismatch` | Run `mvn flyway:repair` then restart |
| **JWT token invalid** | `401 Unauthorized` on all requests | Check `JWT_SECRET` in .env matches your token |
| **Out of memory** | `java.lang.OutOfMemoryError` | Increase heap: `MAVEN_OPTS="-Xmx1024m"` |
| **.env not loaded** | Environment variables not found | Ensure `.env` is in project root, restart IDE |

### Useful Development Commands

```bash
# Run only unit tests
mvn test

# Run specific test class
mvn test -Dtest=CarServiceTest

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Check code compilation
mvn compile

# Clean build artifacts
mvn clean
```

---

## 3. Environment Variables

> All environment variables are configured via `.env` file. Copy `.env.example` to `.env` and modify as needed.

### 3.1 Database Configuration

| Variable | Required | Default | Description |
|----------|:--------:|---------|-------------|
| `DATABASE_URL` | ✅ | - | JDBC connection string (e.g., `jdbc:postgresql://localhost:5432/car_rental`) |
| `DB_USERNAME` | ✅ | - | Database username |
| `DB_PASSWORD` | ✅ | - | Database password |

### 3.2 JWT Authentication

| Variable | Required | Default | Description |
|----------|:--------:|---------|-------------|
| `JWT_SECRET` | ✅ | - | Secret key for JWT signing (minimum 256 bits) |
| `JWT_ACCESS_TOKEN_EXPIRATION` | ❌ | `900000` | Access token TTL in milliseconds (15 min) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | ❌ | `604800000` | Refresh token TTL in milliseconds (7 days) |

> ⚠️ **Security:** `JWT_SECRET` must be at least 32 characters for HS256 algorithm.

### 3.3 OAuth2 Configuration

| Variable | Required | Default | Description |
|----------|:--------:|---------|-------------|
| `APP_BASE_URL` | ❌ | `http://localhost:8082` | Application base URL for OAuth callbacks |
| `GOOGLE_CLIENT_ID` | ❌ | - | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | ❌ | - | Google OAuth2 client secret |
| `GITHUB_CLIENT_ID` | ❌ | - | GitHub OAuth2 client ID |
| `GITHUB_CLIENT_SECRET` | ❌ | - | GitHub OAuth2 client secret |
| `OAUTH2_STATE_SECRET` | ❌ | `default` | CSRF protection secret (minimum 32 chars) |

> **Note:** OAuth2 is optional. Application works with email/password authentication without these.

### 3.4 Stripe Payment

| Variable | Required | Default | Description |
|----------|:--------:|---------|-------------|
| `STRIPE_API_KEY` | ❌ | `placeholder` | Stripe API secret key (`sk_test_...` or `sk_live_...`) |
| `STRIPE_WEBHOOK_SECRET` | ❌ | `placeholder` | Webhook signature verification secret (`whsec_...`) |

> **Note:** Payment features are disabled when Stripe is not configured.

### 3.5 Email Configuration (SendGrid)

| Variable | Required | Default | Description |
|----------|:--------:|---------|-------------|
| `EMAIL_ENABLED` | ❌ | `false` | Enable/disable email sending |
| `EMAIL_FROM` | ❌ | `noreply@rentacar.com` | Sender email address |
| `SENDGRID_HOST` | ❌ | `smtp.sendgrid.net` | SMTP host |
| `SENDGRID_PORT` | ❌ | `587` | SMTP port |
| `SENDGRID_USERNAME` | ❌ | `apikey` | SMTP username (always `apikey` for SendGrid) |
| `SENDGRID_API_KEY` | ❌ | - | SendGrid API key |

> **Note:** When `EMAIL_ENABLED=false`, emails are logged but not sent.

### 3.6 File Storage (Cloudflare R2)

| Variable | Required | Default | Description |
|----------|:--------:|---------|-------------|
| `R2_ENDPOINT` | ❌ | - | R2 endpoint URL |
| `R2_BUCKET_NAME` | ❌ | - | S3-compatible bucket name |
| `R2_ACCESS_KEY_ID` | ❌ | - | R2 access key ID |
| `R2_SECRET_ACCESS_KEY` | ❌ | - | R2 secret access key |

> **Note:** File uploads fall back to local storage when R2 is not configured.

### 3.7 Secrets Management

#### Development Environment
```bash
# Use .env file (gitignored)
cp .env.example .env
# Edit .env with development values
```

#### Production Environment

| Option | Description |
|--------|-------------|
| **Environment Variables** | Set directly in deployment platform (Railway, Render, etc.) |
| **Secrets Manager** | AWS Secrets Manager, HashiCorp Vault, etc. |
| **CI/CD Secrets** | GitHub Secrets, GitLab CI Variables |

> ⚠️ **Never commit secrets to the repository!**

### 3.8 Missing Variable Behavior

| Variable Category | If Missing | Behavior |
|-------------------|------------|----------|
| **Database** | ❌ Application fails to start | Required for core functionality |
| **JWT_SECRET** | ❌ Application fails to start | Required for authentication |
| **OAuth2** | ⚠️ OAuth buttons hidden | Email/password auth still works |
| **Stripe** | ⚠️ Payment features disabled | Rental creation works, payment skipped |
| **Email** | ⚠️ Emails logged only | No actual emails sent |
| **R2 Storage** | ⚠️ Local storage used | Files stored in `uploads/` directory |

### Example .env for Development

```properties
# Minimum configuration for local development
DATABASE_URL=jdbc:postgresql://localhost:5432/car_rental
DB_USERNAME=postgres
DB_PASSWORD=password
JWT_SECRET=dev-secret-key-for-local-development-only-32chars

# Optional: Enable features as needed
EMAIL_ENABLED=false
STRIPE_API_KEY=sk_test_...
```

---

## 4. Database Setup

### Schema Configuration

| Environment | Schema | Configuration |
|-------------|--------|---------------|
| Development | `gallery` | `application.properties` |
| Production | `public` | `application-production.properties` |

### Flyway Migrations

Migrations are located in `src/main/resources/db/migration/` and run automatically on startup.

**Current Migrations (V1-V18):**

| Version | Description |
|---------|-------------|
| V1 | Initial schema (cars table) |
| V2 | Seed car data |
| V3 | Users table |
| V4 | Rentals table |
| V5 | OAuth2 support |
| V6 | Stripe payment fields |
| V7 | Reminder tracking |
| V8 | Late return fields |
| V9 | Penalty waivers table |
| V10 | Damage tables |
| V11 | Damage tracking for rentals |
| V13 | Dashboard alerts |
| V14 | Missing payment columns |
| V15 | Data fixes and indexes |
| V16 | Seed images |
| V17 | Update rental prices |
| V18 | Seed admin user |

### Migration Commands

```bash
# Check migration status
mvn flyway:info

# Run pending migrations manually
mvn flyway:migrate

# Repair failed migration (reset checksum)
mvn flyway:repair

# Clean database (⚠️ DESTRUCTIVE - dev only)
mvn flyway:clean
```

### Troubleshooting Migrations

| Problem | Solution |
|---------|----------|
| `Checksum mismatch` | Run `mvn flyway:repair` then restart |
| `Migration failed` | Fix the SQL, run `mvn flyway:repair`, then `mvn flyway:migrate` |
| `Schema does not exist` | Flyway creates schema automatically on first run |
| `Out of order` | Set `spring.flyway.out-of-order=true` in properties |

---

## 5. Docker Deployment

### Build Docker Image

```bash
# Build image
docker build -t car-rental-api:latest .

# Build with specific tag
docker build -t car-rental-api:1.0.0 .

# Build with no cache
docker build --no-cache -t car-rental-api:latest .
```

### Run with Docker

```bash
# Run with environment file
docker run -d \
  --name car-rental-api \
  -p 8082:8082 \
  --env-file .env \
  car-rental-api:latest

# Run with inline environment variables
docker run -d \
  --name car-rental-api \
  -p 8082:8082 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/car_rental \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=password \
  -e JWT_SECRET=your-secret-key \
  car-rental-api:latest
```

> **Note:** Use `host.docker.internal` to connect to PostgreSQL running on host machine.

### Docker Compose (Full Stack)

```bash
# Start all services (PostgreSQL + API)
docker compose up -d

# View logs
docker compose logs -f

# Stop all services
docker compose down

# Stop and remove volumes (⚠️ deletes data)
docker compose down -v
```

### Docker Resource Limits

JVM memory is configurable via `JAVA_OPTS` environment variable:

```dockerfile
# Default configuration in Dockerfile
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

| Setting | Description |
|---------|-------------|
| `-XX:+UseContainerSupport` | JVM respects container memory limits |
| `-XX:MaxRAMPercentage=75.0` | Use 75% of container RAM for JVM |

#### Override Memory Settings

```bash
# Low memory (512MB container - e.g., Railway free tier)
docker run -e JAVA_OPTS="-Xmx400m -Xms256m" car-rental-api:latest

# Medium memory (1GB container)
docker run -e JAVA_OPTS="-Xmx768m -Xms512m" car-rental-api:latest

# High memory (2GB+ container)
docker run -e JAVA_OPTS="-Xmx1536m -Xms512m" car-rental-api:latest

# Use default (75% of container RAM)
docker run car-rental-api:latest
```

### Production Docker Considerations

- [ ] Use specific image tags, not `latest`
- [ ] Set resource limits in orchestrator (K8s, Docker Swarm)
- [ ] Configure health check endpoints
- [ ] Use secrets management for sensitive env vars
- [ ] Enable container logging to external system
- [ ] Tune `JAVA_OPTS` based on container memory limit

---

## 6. CI/CD Pipeline

### GitHub Actions Workflow

The project uses GitHub Actions for continuous integration. Workflow file: `.github/workflows/modulith-verify.yml`

#### Trigger Events

```yaml
on:
  push:
    branches: [main, develop, 'refactor/**']
  pull_request:
    branches: [main, develop]
```

| Event | Branches | Description |
|-------|----------|-------------|
| Push | main, develop, refactor/* | Runs on every push |
| Pull Request | main, develop | Runs on PR creation/update |

### Pipeline Steps

```
┌─────────────────────────────────────────────────────────────┐
│  1. Checkout code                                            │
│     └── actions/checkout@v4                                  │
├─────────────────────────────────────────────────────────────┤
│  2. Set up JDK 17 (Temurin)                                  │
│     └── actions/setup-java@v4                                │
│     └── Maven cache enabled                                  │
├─────────────────────────────────────────────────────────────┤
│  3. Verify Module Structure                                  │
│     └── mvn test -Dtest=ModularityTests                      │
├─────────────────────────────────────────────────────────────┤
│  4. Check Shared Kernel Size                                 │
│     └── Count classes in shared/*                            │
│     └── Fail if > 25 classes                                 │
├─────────────────────────────────────────────────────────────┤
│  5. Upload Test Results                                      │
│     └── target/surefire-reports/                             │
└─────────────────────────────────────────────────────────────┘
```

### ModularityTests

`ModularityTests` verifies Spring Modulith module boundaries:

```java
@Test
void verifyModuleStructure() {
    ApplicationModules.of(CarRentalApiApplication.class)
        .verify();
}
```

**What it checks:**
- Module dependencies follow allowed patterns
- No cyclic dependencies between modules
- Internal packages are not accessed from outside
- API boundaries are respected

### Shared Kernel Thresholds

The pipeline counts classes in `src/main/java/**/shared/*`:

| Count | Status | Action |
|-------|--------|--------|
| ≤ 10 | ✅ Pass | Acceptable |
| 11-15 | ⚠️ Warning | Consider refactoring |
| 16-25 | ⚠️ Warning | Should refactor |
| > 25 | ❌ Fail | **Pipeline fails** - must refactor |

**Current shared kernel includes:**
- Security infrastructure (JWT, filters)
- Configuration classes
- File upload services
- Common exceptions

> These are intentionally shared across all modules.

### CI Troubleshooting

| Problem | Cause | Solution |
|---------|-------|----------|
| `ModularityTests` fail | Module accesses internal package | Check `@NamedInterface` or move to API |
| Shared kernel > 25 | Too many shared classes | Move domain-specific classes to their modules |
| Maven cache miss | First run or pom.xml changed | Cache rebuilds automatically |
| Test timeout | Long Integration tests | Increase timeout or skip specific tests |
| Build fails with OOM | Not enough memory | Add `-Xmx1g` to Maven opts |

### Running CI Locally

```bash
# Run exactly what CI runs
mvn test -Dtest=ModularityTests -DfailIfNoTests=false

# Check shared kernel size
find src/main/java -path "*/shared/*" -name "*.java" ! -name "package-info.java" | wc -l

# Full test suite
mvn test
```

### Extending the Pipeline

Future additions to consider:
- [ ] Add `mvn verify` for integration tests
- [ ] Add SonarQube/SonarCloud analysis
- [ ] Add dependency vulnerability scanning (Dependabot)
- [ ] Add Docker image build and push
- [ ] Add deployment to staging/production

---

## 7. Staging Deployment

> **Note:** Staging environment is not currently implemented.

For solo development, the following workflow is sufficient:
1. Test locally with `dev` profile
2. Run `mvn test` (CI pipeline)
3. Deploy directly to production

**When to add staging:**
- Team grows beyond 1-2 developers
- Production has real users with real data
- Database migrations become risky
- QA team needs a separate environment

---

## 8. Production Deployment

> ⚠️ **Production deployments require extra caution.** Follow checklists carefully.

### Production Environment

| Component | Configuration |
|-----------|---------------|
| Profile | `SPRING_PROFILES_ACTIVE=production` |
| Schema | `public` (not `gallery`) |
| Stripe | Live mode (`sk_live_...`) |
| Email | Enabled with real SendGrid |
| Storage | Cloudflare R2 |

### Pre-Deployment Checklist

**Code Quality:**

- [ ] All tests pass (unit + integration)
- [ ] CI pipeline is green
- [ ] Code reviewed by at least 1 person
- [ ] No critical/high Dependabot alerts

**Database:**

- [ ] Migrations tested in staging
- [ ] **Database backup taken**
- [ ] Rollback migration prepared (if schema changes)

**Configuration:**

- [ ] Environment variables verified
- [ ] Secrets rotated if needed
- [ ] `SPRING_PROFILES_ACTIVE=production`

**Monitoring:**

- [ ] Alerting configured
- [ ] Log aggregation ready
- [ ] Health check endpoint verified

### Deployment Steps

```bash
# 1. Take database backup
pg_dump $DATABASE_URL > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. Build and tag image
docker build -t car-rental-api:v1.0.0 .

# 3. Deploy (platform-specific)
# Railway: railway up
# Render: git push origin main
# Fly.io: fly deploy

# 4. Verify health
curl https://your-domain.com/health
```

### Post-Deployment Verification

**Immediate (0-5 minutes):**

- [ ] `/health` returns `UP`
- [ ] Application logs show no errors
- [ ] Login/logout works
- [ ] Homepage loads correctly

**Short-term (5-30 minutes):**

- [ ] Create a test rental (if possible)
- [ ] Check payment flow (Stripe)
- [ ] Verify email delivery
- [ ] Monitor error rates

**Extended (1-24 hours):**

- [ ] No memory leaks (stable heap)
- [ ] Response times normal
- [ ] No customer complaints

### Rollback Procedure

**If deployment fails:**

```bash
# 1. Stop new deployment
# Platform-specific: railway down, fly scale count 0, etc.

# 2. Restore previous version
docker run -d car-rental-api:previous-tag

# 3. Restore database (if needed)
psql $DATABASE_URL < backup_before_deploy.sql

# 4. Verify rollback
curl https://your-domain.com/health
```

### Production Platform Reference

| Platform | Free Tier | PostgreSQL | Notes |
|----------|-----------|------------|-------|
| **Railway** | $5/mo credit | Included | Previously used |
| **Render** | 750 hrs/mo | Separate service | Good alternative |
| **Fly.io** | 3 shared VMs | Fly Postgres | CLI-based |
| **DigitalOcean** | None | DO Database | $5/mo droplet |
| **Heroku** | None (deprecated) | Heroku Postgres | Eco dyno $5/mo |

> **Current Status:** No active production deployment

---

## 9. Health Checks

### Available Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/` | GET | ❌ Public | Root endpoint with status and message |
| `/health` | GET | ❌ Public | Simple health check |

> **Note:** This project uses a custom `HealthController` instead of Spring Actuator.

### Response Format

**Root Endpoint (`/`):**
```json
{
  "status": "UP",
  "message": "Car Rental API is running"
}
```

**Health Endpoint (`/health`):**
```json
{
  "status": "UP"
}
```

### Using Health Checks

```bash
# Basic health check
curl http://localhost:8082/health
# Expected: {"status":"UP"}

# Root endpoint (more verbose)
curl http://localhost:8082/
# Expected: {"status":"UP","message":"Car Rental API is running"}

# Check with HTTP status code
curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/health
# Expected: 200
```

### Health Check Troubleshooting

| Response | Cause | Solution |
|----------|-------|----------|
| `Connection refused` | Application not running | Check if JAR is running, check port 8082 |
| `502 Bad Gateway` | Reverse proxy issue | Check nginx/load balancer config |
| `503 Service Unavailable` | App starting up | Wait for startup to complete |
| No JSON response | Wrong endpoint | Verify URL (no trailing slash issues) |

### Docker Health Check

The `docker-compose.yml` includes a PostgreSQL health check:

```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres -d car_rental"]
  interval: 10s
  timeout: 5s
  retries: 5
```

To add application health check in Docker:

```yaml
# Optional: add to docker-compose.yml under api service
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8082/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

### Future: Spring Actuator

To get more detailed health information, consider adding Spring Actuator:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

This would provide:
- `/actuator/health` - Detailed health with components (db, diskSpace)
- `/actuator/info` - Application info
- `/actuator/metrics` - Performance metrics

---

## Quick Reference

| Action | Command |
|--------|---------|
| Start database | `docker compose up -d` |
| Stop database | `docker compose down` |
| Build | `mvn clean package -DskipTests` |
| Run locally | `mvn spring-boot:run` |
| Run tests | `mvn test` |
| Docker build | `docker build -t car-rental-api:latest .` |

---

> **Last Updated:** 2026-01-11  
