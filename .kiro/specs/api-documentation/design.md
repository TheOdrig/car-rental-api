# Design: API Documentation

## Overview

Bu tasarım, 3 API dokümantasyon dosyası için içerik yapısını tanımlar. Error codes, conventions ve rate limiting dokümantasyonu.

## File Structure

```
docs/api/
├── ERROR_CODES.md      (YENİ)
├── API_CONVENTIONS.md  (YENİ)
└── RATE_LIMITING.md    (YENİ)
```

---

## Document 1: ERROR_CODES.md

### Structure

```markdown
# API Error Codes

## Overview
[Error handling açıklaması]

## Error Response Format
[JSON örneği]

## Error Codes by Module

### Auth Module
| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| ... | ... | ... | ... |

### Car Module
[Tablo]

### Rental Module
[Tablo]

### Payment Module
[Tablo]

### Damage Module
[Tablo]

### Dashboard Module
[Tablo]

### Shared/Common
[Tablo]
```

### Error Response Format
```json
{
  "timestamp": "2026-01-10T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "errorCode": "CAR_NOT_FOUND",
  "message": "Car with id 123 not found",
  "path": "/api/cars/123"
}
```

### Error Codes by Module

#### Auth Module
| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| USER_ALREADY_EXISTS | 409 Conflict | Email/username already registered | Use different email |
| INVALID_TOKEN | 401 Unauthorized | JWT token is invalid | Re-authenticate |
| TOKEN_EXPIRED | 401 Unauthorized | JWT token has expired | Refresh token |
| OAUTH2_ERROR | 401 Unauthorized | OAuth2 authentication failed | Retry OAuth flow |

#### Car Module
| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| CAR_NOT_FOUND | 404 Not Found | Car with given ID not found | Check car ID |
| CAR_NOT_AVAILABLE | 400 Bad Request | Car is not available for rental | Choose different car |
| CAR_ALREADY_EXISTS | 409 Conflict | Car with license plate exists | Use different plate |
| CAR_CANNOT_BE_RESERVED | 400 Bad Request | Car cannot be reserved | Check car status |
| CAR_CANNOT_BE_SOLD | 400 Bad Request | Car cannot be sold | Check active rentals |
| CAR_OPERATION_NOT_ALLOWED | 400 Bad Request | Operation not allowed | Check car state |
| CAR_VALIDATION_FAILED | 400 Bad Request | Car data validation failed | Fix validation errors |

#### Rental Module
| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| RENTAL_NOT_FOUND | 404 Not Found | Rental with given ID not found | Check rental ID |
| INVALID_RENTAL_STATE | 400 Bad Request | Invalid rental state transition | Check current state |
| RENTAL_DATE_OVERLAP | 400 Bad Request | Dates overlap with existing rental | Choose different dates |
| RENTAL_VALIDATION_FAILED | 400 Bad Request | Rental data validation failed | Fix validation errors |
| LATE_RETURN_ERROR | 400 Bad Request | Late return processing error | Contact support |
| PENALTY_CALCULATION_ERROR | 500 Internal | Penalty calculation failed | Contact support |
| PENALTY_WAIVER_ERROR | 400 Bad Request | Penalty waiver failed | Check waiver eligibility |

#### Payment Module
| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| PAYMENT_FAILED | 400 Bad Request | Payment processing failed | Retry with valid card |
| STRIPE_ERROR | 502 Bad Gateway | Stripe API error | Retry later |
| WEBHOOK_SIGNATURE_INVALID | 400 Bad Request | Invalid webhook signature | Check webhook config |
| RECONCILIATION_ERROR | 500 Internal | Payment reconciliation failed | Contact support |

#### Damage Module
| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| DAMAGE_REPORT_ERROR | 400 Bad Request | Damage report creation failed | Check report data |
| DAMAGE_ASSESSMENT_ERROR | 400 Bad Request | Damage assessment failed | Check assessment data |
| DAMAGE_DISPUTE_ERROR | 400 Bad Request | Damage dispute failed | Check dispute eligibility |

#### Dashboard Module
| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| ALERT_NOT_FOUND | 404 Not Found | Alert with given ID not found | Check alert ID |

#### Shared/Common
| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| INVALID_STATUS_TRANSITION | 400 Bad Request | Invalid status transition | Check valid transitions |
| FILE_UPLOAD_ERROR | 400 Bad Request | File upload failed | Check file size/type |

---

## Document 2: API_CONVENTIONS.md

### Structure

```markdown
# API Conventions

## Base URL
[Base URL bilgisi]

## Authentication
[JWT Bearer token format]

## Request Format
[Content-Type, Accept headers]

## Response Format
[JSON structure]

## Pagination
[Page, size, sort parameters]

## Date/Time Format
[ISO 8601]

## Naming Conventions
[camelCase, kebab-case]

## HTTP Status Codes
[Status code mapping]
```

### Content Details

#### Authentication
```http
Authorization: Bearer <jwt_token>
```

#### Pagination
```http
GET /api/cars?page=0&size=20&sort=brand,asc
```

Response:
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "direction": "ASC" }
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

#### Date/Time Format
- Date: `YYYY-MM-DD` (e.g., `2026-01-10`)
- DateTime: `YYYY-MM-DDTHH:mm:ssZ` (ISO 8601)
- Timezone: UTC (Z suffix)

#### Naming Conventions
| Context | Convention | Example |
|---------|------------|---------|
| JSON fields | camelCase | `firstName`, `totalPrice` |
| URL paths | kebab-case | `/api/late-returns` |
| Query params | camelCase | `?startDate=2026-01-10` |
| Enum values | UPPER_SNAKE | `RENTAL_STATUS` |

#### HTTP Status Codes
| Status | Meaning | Usage |
|--------|---------|-------|
| 200 | OK | Successful GET, PUT |
| 201 | Created | Successful POST |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Validation error |
| 401 | Unauthorized | Missing/invalid token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Duplicate resource |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 502 | Bad Gateway | External service error |

---

## Document 3: RATE_LIMITING.md

### Structure

```markdown
# Rate Limiting

## Overview
[Rate limiting açıklaması]

## Current Status
[Mevcut durum - implemented/planned]

## Rate Limit Headers
[X-RateLimit-* headers]

## Handling 429 Responses
[Retry strategy]

## Best Practices
[Rate limit avoidance tips]
```

### Content Details

#### Rate Limit Headers (Standard)
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1704902400
Retry-After: 60
```

#### 429 Response Handling
```json
{
  "timestamp": "2026-01-10T14:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please retry after 60 seconds.",
  "path": "/api/cars"
}
```

#### Best Practices
1. Implement exponential backoff
2. Cache responses where possible
3. Use bulk endpoints when available
4. Monitor rate limit headers
5. Implement request queuing

---

## Acceptance Criteria Mapping

| Requirement | Document Section |
|-------------|-----------------|
| Story 1: All error codes | ERROR_CODES.md tables |
| Story 1: HTTP status mapping | ERROR_CODES.md tables |
| Story 1: Example response | ERROR_CODES.md format |
| Story 2: Pagination format | API_CONVENTIONS.md Pagination |
| Story 2: Date/time format | API_CONVENTIONS.md Date/Time |
| Story 2: Error response format | API_CONVENTIONS.md Response |
| Story 2: Naming conventions | API_CONVENTIONS.md Naming |
| Story 3: Rate limit headers | RATE_LIMITING.md Headers |
| Story 3: 429 handling | RATE_LIMITING.md Handling |
| Story 3: Best practices | RATE_LIMITING.md Best Practices |
