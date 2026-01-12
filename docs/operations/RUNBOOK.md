# Operations Runbook

> **Car Rental API** - Operational procedures for troubleshooting, recovery, and maintenance.

---

## Table of Contents

1. [Common Issues](#1-common-issues)
2. [Diagnostic Commands](#2-diagnostic-commands)
3. [Application Management](#3-application-management)
4. [Database Operations](#4-database-operations)
5. [Rollback Procedures](#5-rollback-procedures)
6. [Logging](#6-logging)
7. [Issue Severity Reference](#7-issue-severity-reference)

---

## 1. Common Issues

### Authentication Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| `401 Unauthorized` on all requests | JWT_SECRET mismatch | Verify `JWT_SECRET` matches between token generation and validation |
| `401` after token refresh | Token expired | Check `JWT_ACCESS_TOKEN_EXPIRATION` value |
| OAuth2 callback fails | Wrong redirect URI | Update `APP_BASE_URL` in environment |
| "Invalid token signature" | Secret key changed | Users need to re-login after secret rotation |

### Database Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| `Connection refused` on startup | Database not running | Start PostgreSQL: `docker compose up -d` |
| `FATAL: database "car_rental" does not exist` | Database not created | Create DB: `createdb car_rental` |
| `Schema "gallery" does not exist` | First run without Flyway | Flyway creates schema automatically, restart app |
| `Checksum mismatch` on migration | Migration file modified | Run `mvn flyway:repair` then restart |
| Slow queries | Missing indexes | Check `V15__fix_data_and_add_indexes.sql` applied |

### Application Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| `Port 8082 already in use` | Another process on port | Kill process or change port |
| `OutOfMemoryError` | Heap too small | Increase `-Xmx` in `JAVA_OPTS` |
| App starts but endpoints fail | Missing env vars | Check required variables in `.env` |
| File upload fails | R2 not configured | Falls back to local `uploads/` directory |

### Payment Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| Stripe webhooks fail | Wrong webhook secret | Update `STRIPE_WEBHOOK_SECRET` |
| `Invalid API key` | Wrong Stripe key | Check `STRIPE_API_KEY` (test vs live) |
| Payment succeeds but rental not updated | Webhook not received | Check Stripe webhook logs |

### Email Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| Emails not sent | `EMAIL_ENABLED=false` | Set `EMAIL_ENABLED=true` |
| SendGrid errors | Invalid API key | Verify `SENDGRID_API_KEY` |
| Emails logged but not sent | Dev mode | Expected behavior when `EMAIL_ENABLED=false` |

---

## 2. Diagnostic Commands

### Application Health

```bash
# Basic health check
curl http://localhost:8082/health

# Check with status code
curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/health

# Root endpoint (more info)
curl http://localhost:8082/
```

### Database Diagnostics

```bash
# Check PostgreSQL is running
docker compose ps

# Check PostgreSQL is ready
pg_isready -h localhost -p 5432

# Connect to database
psql -h localhost -U postgres -d car_rental

# Check migration status
mvn flyway:info

# Check table exists
psql -h localhost -U postgres -d car_rental -c "\dt gallery.*"
```

### Container Diagnostics

```bash
# Check running containers
docker ps

# Check container logs
docker logs car-rental-db --tail 100

# Check container resource usage
docker stats car-rental-db

# Get container details
docker inspect car-rental-db
```

### Memory Diagnostics

```bash
# Check Java heap (if JMX enabled)
jcmd <pid> GC.heap_info

# Check process memory
# Windows
tasklist /FI "IMAGENAME eq java.exe"

# Linux/Mac
ps aux | grep java
```

### Network Diagnostics

```bash
# Check if port is listening
# Windows
netstat -ano | findstr :8082

# Linux/Mac
lsof -i :8082

# Test endpoint connectivity
curl -v http://localhost:8082/health
```

---

## 3. Application Management

### Start Application

```bash
# Local development (Maven)
mvn spring-boot:run

# Run JAR directly
java -jar target/car-rental-api-0.0.1-SNAPSHOT.jar

# Docker
docker run -d --name car-rental-api -p 8082:8082 --env-file .env car-rental-api:latest

# Docker Compose (with database)
docker compose up -d
```

### Stop Application

```bash
# Local development
# Press Ctrl+C in terminal, or:
# Windows
taskkill /F /IM java.exe

# Docker
docker stop car-rental-api

# Docker Compose
docker compose down
```

### Restart Application

```bash
# Docker
docker restart car-rental-api

# Docker Compose
docker compose restart

# Full restart (stop, rebuild, start)
docker compose down
docker compose build --no-cache
docker compose up -d
```

### View Logs

```bash
# Local (Maven output in terminal)

# Docker
docker logs car-rental-api --tail 100 -f

# Docker Compose
docker compose logs -f
docker compose logs api --tail 100
```

### Check Application Status

```bash
# Health check
curl http://localhost:8082/health

# Process running check (Windows)
tasklist | findstr java

# Process running check (Linux/Mac)
pgrep -f car-rental-api
```

---

## 4. Database Operations

### Backup Database

```bash
# Full backup
pg_dump -h localhost -U postgres -d car_rental > backup_$(date +%Y%m%d_%H%M%S).sql

# Backup specific schema
pg_dump -h localhost -U postgres -d car_rental -n gallery > backup_gallery.sql

# Compressed backup
pg_dump -h localhost -U postgres -d car_rental | gzip > backup_$(date +%Y%m%d).sql.gz

# Docker: backup from container
docker exec car-rental-db pg_dump -U postgres car_rental > backup.sql
```

### Restore Database

```bash
# Restore from backup
psql -h localhost -U postgres -d car_rental < backup.sql

# Restore compressed backup
gunzip -c backup.sql.gz | psql -h localhost -U postgres -d car_rental

# Docker: restore to container
docker exec -i car-rental-db psql -U postgres car_rental < backup.sql
```

### Migration Management

```bash
# Check migration status
mvn flyway:info

# Run pending migrations
mvn flyway:migrate

# Repair failed migration (reset checksum)
mvn flyway:repair

# Validate migrations
mvn flyway:validate

# Clean database (⚠️ DESTRUCTIVE - dev only!)
mvn flyway:clean
```

### Common Database Queries

```sql
-- Check current schema
SELECT current_schema();

-- List tables in gallery schema
\dt gallery.*

-- Check user count
SELECT COUNT(*) FROM gallery.users;

-- Check rental status distribution
SELECT status, COUNT(*) FROM gallery.rentals GROUP BY status;

-- Find user by email
SELECT id, email, role FROM gallery.users WHERE email = 'user@example.com';
```

---

## 5. Rollback Procedures

### Application Rollback

**Scenario:** New deployment introduced bugs, need to revert.

```bash
# 1. Stop current version
docker stop car-rental-api

# 2. Start previous version
docker run -d --name car-rental-api \
  -p 8082:8082 \
  --env-file .env \
  car-rental-api:previous-tag

# 3. Verify rollback
curl http://localhost:8082/health
```

### Git Rollback

```bash
# Revert last commit
git revert HEAD

# Revert specific commit
git revert <commit-hash>

# Hard reset (⚠️ DESTRUCTIVE)
git reset --hard <commit-hash>
git push --force  # Only if absolutely necessary
```

### Database Rollback

**Scenario:** Migration caused data issues.

```bash
# 1. Stop application
docker stop car-rental-api

# 2. Restore from backup
psql -h localhost -U postgres -d car_rental < backup_before_migration.sql

# 3. Start previous application version
docker run -d car-rental-api:previous-tag

# 4. Verify
curl http://localhost:8082/health
```

### Rollback Checklist

- [ ] Identify the problem version/commit
- [ ] Ensure backup exists before rollback
- [ ] Stop current application
- [ ] Rollback database if schema changed
- [ ] Deploy previous version
- [ ] Verify health check passes
- [ ] Test critical user flows
- [ ] Monitor logs for errors
- [ ] Communicate to stakeholders (if applicable)

---

## 6. Logging

### Log Format

The application uses SLF4J with Logback. Logs include correlation ID for request tracing.

```
2026-01-12 01:45:00.123 INFO  [correlationId=abc-123-def] c.a.rental.RentalService - Rental created
```

### Log Levels

| Level | Usage | Example |
|-------|-------|---------|
| `ERROR` | Exceptions, failures, critical issues | Payment failed, database connection lost |
| `WARN` | Unexpected but handled situations | Retry succeeded, fallback used |
| `INFO` | Business events, important actions | Rental created, user registered |
| `DEBUG` | Detailed debugging information | Request details, SQL queries |
| `TRACE` | Very detailed tracing | Method entry/exit, variable values |

### Correlation ID

Every request has a unique correlation ID for tracing:

| Aspect | Value |
|--------|-------|
| **Header Name** | `X-Correlation-ID` |
| **MDC Key** | `correlationId` |
| **Generation** | Auto-generated UUID if not provided |
| **Response** | Returned in response header |

**How it works:**
1. Client sends request (optionally with `X-Correlation-ID` header)
2. If no header, server generates UUID
3. Correlation ID added to MDC (Mapped Diagnostic Context)
4. All logs in that request include the correlation ID
5. Response returns `X-Correlation-ID` header

### Searching Logs

```bash
# Search by correlation ID
grep "abc-123-def" application.log

# Search for errors
grep "ERROR" application.log

# Search for specific user
grep "userId=user@example.com" application.log

# Tail recent logs with filter
tail -f application.log | grep "ERROR\|WARN"

# Docker logs
docker logs car-rental-api 2>&1 | grep "abc-123-def"
```

### Important Log Patterns

| Pattern | Meaning | Action |
|---------|---------|--------|
| `Payment failed` | Stripe payment error | Check Stripe Dashboard |
| `JWT token expired` | Authentication issue | User needs to re-login |
| `Database connection` | DB connectivity issue | Check PostgreSQL status |
| `Email send failed` | SendGrid error | Check SendGrid API key |
| `Flyway migration` | Schema change issue | Check migration SQL |
| `OutOfMemoryError` | Heap exhausted | Increase `JAVA_OPTS` |
| `Connection refused` | External service down | Check dependent services |

### Configuring Log Levels

```properties
# application.properties - reduce verbosity
logging.level.root=WARN
logging.level.com.akif=INFO
logging.level.org.springframework=WARN
logging.level.org.hibernate=WARN

# Enable debug for troubleshooting
logging.level.com.akif.rental=DEBUG
```

---

## 7. Issue Severity Reference

| Level | Examples |
|-------|----------|
| **P1 - Critical** | Service completely down, data loss, security breach |
| **P2 - High** | Payment failures, authentication broken, major feature broken |
| **P3 - Medium** | Feature degraded, intermittent errors |
| **P4 - Low** | Minor issues, non-critical bugs |

> **Note:** For solo development, prioritize based on impact. Add escalation contacts when team grows.

---

> **Last Updated:** 2026-01-12
