# Design Document: Operations Documentation

## Overview

Bu design, Car Rental API projesi için kapsamlı operasyon dokümantasyonu oluşturmayı tanımlar. İki ana dokümantasyon dosyası oluşturulacak:

1. **DEPLOYMENT.md** - Deployment prosedürleri, environment config, CI/CD
2. **RUNBOOK.md** - Troubleshooting, backup/restore, rollback

Her iki dosya da `docs/operations/` dizininde yer alacak.

## Architecture

```
docs/
└── operations/
    ├── DEPLOYMENT.md   # Deployment ve configuration
    └── RUNBOOK.md      # Operasyonel prosedürler
```

### Mevcut Operasyon Yapısı (Referans)

```
Proje Kökü:
├── Dockerfile              # Multi-stage build
├── .env.example            # Environment variables template
├── pom.xml                 # Maven build config
└── .github/workflows/
    └── modulith-verify.yml # CI/CD pipeline

src/main/resources/
├── application.properties  # App configuration
└── db/migration/           # Flyway migrations
```

## Components and Interfaces

### Component 1: DEPLOYMENT.md

Deployment dokümanı aşağıdaki bölümleri içerecek:

```markdown
# Deployment Guide

## Table of Contents
1. Prerequisites
2. Local Development Setup
3. Environment Variables
4. Database Setup
5. Docker Deployment
6. CI/CD Pipeline
7. Staging Deployment
8. Production Deployment
9. Health Checks

## 1. Prerequisites

| Requirement | Version | Purpose |
|-------------|---------|---------|
| Java | 17+ | Runtime |
| Maven | 3.8+ | Build |
| PostgreSQL | 15+ | Database |
| Docker | 20+ | Containerization |

## 2. Local Development Setup

### Quick Start
```bash
# 1. Clone repository
git clone https://github.com/TheOdrig/car-rental-api.git
cd car-rental-api

# 2. Configure environment
cp .env.example .env
# Edit .env with your values

# 3. Start PostgreSQL
docker run -d --name postgres \
  -e POSTGRES_DB=car_rental \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 postgres:15

# 4. Run application
mvn spring-boot:run

# 5. Verify
curl http://localhost:8082/health
```

## 3. Environment Variables

### Database
| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| DATABASE_URL | ✅ | - | JDBC connection string |
| DB_USERNAME | ✅ | - | Database username |
| DB_PASSWORD | ✅ | - | Database password |

### JWT Authentication
| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| JWT_SECRET | ✅ | - | 256-bit secret key |
| JWT_ACCESS_TOKEN_EXPIRATION | ❌ | 900000 | Access token TTL (ms) |
| JWT_REFRESH_TOKEN_EXPIRATION | ❌ | 604800000 | Refresh token TTL (ms) |

### OAuth2
| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| GOOGLE_CLIENT_ID | ❌ | - | Google OAuth client ID |
| GOOGLE_CLIENT_SECRET | ❌ | - | Google OAuth secret |
| GITHUB_CLIENT_ID | ❌ | - | GitHub OAuth client ID |
| GITHUB_CLIENT_SECRET | ❌ | - | GitHub OAuth secret |
| OAUTH2_STATE_SECRET | ❌ | default | CSRF protection secret |
| APP_BASE_URL | ❌ | http://localhost:8082 | Application base URL |

### Stripe Payment
| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| STRIPE_API_KEY | ❌ | placeholder | Stripe API key |
| STRIPE_WEBHOOK_SECRET | ❌ | placeholder | Webhook signature secret |

### Email (SendGrid)
| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| EMAIL_ENABLED | ❌ | false | Enable email sending |
| EMAIL_FROM | ❌ | noreply@car-rental.com | Sender address |
| SENDGRID_API_KEY | ❌ | - | SendGrid API key |

### Secrets Management
- Development: .env file (gitignored)
- Production: Environment variables or secrets manager
- Never commit secrets to repository

## 4. Database Setup

### Flyway Migrations
Migrations run automatically on startup.

```bash
# Manual migration
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Repair failed migration
mvn flyway:repair
```

### Schema
- Schema name: `gallery`
- Auto-created by Flyway

## 5. Docker Deployment

### Build
```bash
docker build -t car-rental-api:latest .
```

### Run
```bash
docker run -d \
  --name car-rental-api \
  -p 8082:8082 \
  --env-file .env \
  car-rental-api:latest
```

### Docker Compose (Optional)
```yaml
version: '3.8'
services:
  api:
    build: .
    ports:
      - "8082:8082"
    env_file: .env
    depends_on:
      - db
  db:
    image: postgres:15
    environment:
      POSTGRES_DB: car_rental
      POSTGRES_PASSWORD: password
    volumes:
      - pgdata:/var/lib/postgresql/data
volumes:
  pgdata:
```

## 6. CI/CD Pipeline

### GitHub Actions Workflow
File: `.github/workflows/modulith-verify.yml`

### Pipeline Steps
1. Checkout code
2. Setup JDK 17
3. Run ModularityTests
4. Check shared kernel size
5. Upload test results

### ModularityTests
- Verifies Spring Modulith module boundaries
- Fails if modules access internal packages

### Shared Kernel Thresholds
| Status | Count | Action |
|--------|-------|--------|
| ✅ Pass | ≤ 10 | OK |
| ⚠️ Warning | 11-15 | Consider refactoring |
| ❌ Fail | > 25 | Must refactor |

### CI Troubleshooting
| Issue | Solution |
|-------|----------|
| ModularityTests fail | Check package-info.java dependencies |
| Shared kernel too large | Move classes to domain modules |

## 7. Staging Deployment

### Checklist
- [ ] All tests pass locally
- [ ] CI pipeline green
- [ ] Environment variables configured
- [ ] Database migrated
- [ ] Health check passes

## 8. Production Deployment

### Pre-Deployment Checklist
- [ ] Code reviewed and approved
- [ ] CI/CD pipeline green
- [ ] Database backup taken
- [ ] Rollback plan ready
- [ ] Monitoring alerts configured

### Deployment Steps
1. Take database backup
2. Deploy new version
3. Run health checks
4. Monitor logs for errors
5. Verify critical flows

### Post-Deployment Verification
- [ ] /health returns 200
- [ ] Login works
- [ ] Rental creation works
- [ ] Payment processing works

## 9. Health Checks

### Endpoints
| Endpoint | Purpose |
|----------|---------|
| /health | Basic health check |
| /actuator/health | Detailed health status |

### Response Format
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Troubleshooting
| Status | Cause | Solution |
|--------|-------|----------|
| DOWN - db | Database connection failed | Check DATABASE_URL |
| DOWN - diskSpace | Disk full | Free disk space |
```

### Component 2: RUNBOOK.md

Runbook dokümanı aşağıdaki yapıda olacak:

```markdown
# Operations Runbook

## Table of Contents
1. Common Issues
2. Diagnostic Commands
3. Application Management
4. Database Operations
5. Rollback Procedures
6. Logging
7. Escalation

## 1. Common Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| 401 on all requests | JWT_SECRET mismatch | Verify JWT_SECRET matches |
| 500 on startup | Database unreachable | Check DATABASE_URL |
| OAuth2 callback fails | Wrong redirect URI | Update APP_BASE_URL |
| Emails not sending | EMAIL_ENABLED=false | Set EMAIL_ENABLED=true |
| Stripe webhooks fail | Wrong webhook secret | Update STRIPE_WEBHOOK_SECRET |
| Slow queries | Missing indexes | Run EXPLAIN ANALYZE |

## 2. Diagnostic Commands

### Check Application Status
```bash
curl http://localhost:8082/health
```

### Check Database Connection
```bash
psql $DATABASE_URL -c "SELECT 1"
```

### Check Logs
```bash
# Docker
docker logs car-rental-api --tail 100

# Local
tail -f logs/application.log
```

### Check Memory Usage
```bash
docker stats car-rental-api
```

## 3. Application Management

### Restart Application
```bash
# Docker
docker restart car-rental-api

# Local
pkill -f car-rental-api
mvn spring-boot:run
```

### Graceful Shutdown
```bash
curl -X POST http://localhost:8082/actuator/shutdown
```

### Scale (Docker Compose)
```bash
docker-compose up -d --scale api=3
```

## 4. Database Operations

### Backup
```bash
pg_dump $DATABASE_URL > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Restore
```bash
psql $DATABASE_URL < backup_20260110_120000.sql
```

### Check Migration Status
```bash
mvn flyway:info
```

### Repair Failed Migration
```bash
mvn flyway:repair
mvn flyway:migrate
```

## 5. Rollback Procedures

### Application Rollback
```bash
# 1. Stop current version
docker stop car-rental-api

# 2. Start previous version
docker run -d --name car-rental-api \
  car-rental-api:previous-tag

# 3. Verify health
curl http://localhost:8082/health
```

### Database Rollback
```bash
# 1. Stop application
docker stop car-rental-api

# 2. Restore backup
psql $DATABASE_URL < backup_before_deploy.sql

# 3. Start previous version
docker run -d car-rental-api:previous-tag
```

## 6. Logging

### Log Format
```
2026-01-10 12:00:00 - [correlation-id] Message
```

### Log Levels
| Level | Usage |
|-------|-------|
| ERROR | Exceptions, failures |
| WARN | Unexpected but handled |
| INFO | Business events |
| DEBUG | Detailed debugging |

### Correlation ID
- Header: X-Correlation-ID
- Auto-generated if not provided
- Use to trace requests across logs

### Search Logs by Correlation ID
```bash
grep "abc-123-def" logs/application.log
```

### Important Log Patterns
| Pattern | Meaning |
|---------|---------|
| "Payment failed" | Stripe error |
| "JWT token expired" | Auth issue |
| "Database connection" | DB connectivity |
| "Email send failed" | SendGrid error |

## 7. Escalation

### Severity Levels
| Level | Response Time | Examples |
|-------|---------------|----------|
| P1 - Critical | 15 min | Service down, data loss |
| P2 - High | 1 hour | Payment failures |
| P3 - Medium | 4 hours | Feature degraded |
| P4 - Low | 24 hours | Minor issues |

### Contacts
| Role | Contact |
|------|---------|
| On-Call Engineer | [TBD] |
| Tech Lead | [TBD] |
| Database Admin | [TBD] |
```

## Data Models

Bu spec kod değil dokümantasyon oluşturduğu için data model yoktur.

## Error Handling

| Durum | Çözüm |
|-------|-------|
| Environment variable eksik | .env.example'dan kopyala |
| CI workflow değişti | Güncel workflow'u referans al |
| Health endpoint farklı | Gerçek endpoint'i kontrol et |

## Testing Strategy

### Manual Verification Checklist

1. **Completeness Check**
   - [ ] Tüm .env.example değişkenleri dokümante edilmiş
   - [ ] Docker komutları çalışıyor
   - [ ] CI/CD workflow açıklanmış

2. **Accuracy Check**
   - [ ] Port numaraları doğru (8082)
   - [ ] Flyway schema doğru (gallery)
   - [ ] Health endpoint'ler doğru

3. **Consistency Check**
   - [ ] Komutlar test edilmiş
   - [ ] Markdown formatı düzgün
