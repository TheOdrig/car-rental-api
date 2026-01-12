# API Conventions

This document describes the conventions and standards used throughout the Car Rental API.

## Table of Contents

- [Base URL](#base-url)
- [Authentication](#authentication)
- [Request Format](#request-format)
- [Response Format](#response-format)
- [Pagination](#pagination)
- [Date and Time Format](#date-and-time-format)
- [Naming Conventions](#naming-conventions)
- [HTTP Status Codes](#http-status-codes)

---

## Base URL

All API endpoints are prefixed with:

```
http://localhost:8080/api
```

**Production:**
```
https://api.example.com/api
```

### API Structure

| Path Prefix | Description | Authentication |
|-------------|-------------|----------------|
| `/api/auth` | Authentication (login, register) | Public |
| `/api/oauth2` | OAuth2 social login | Public |
| `/api/cars` | Car management and search | Mixed |
| `/api/rentals` | Rental management | Required |
| `/api/pricing` | Dynamic pricing | Public (read) |
| `/api/exchange-rates` | Currency conversion | Public |
| `/api/admin/*` | Admin operations | Admin only |
| `/api/webhooks` | External webhooks | Signature verified |

---

## Authentication

The API uses JWT (JSON Web Token) Bearer authentication.

### Header Format

```http
Authorization: Bearer <access_token>
```

### Token Lifecycle

| Token Type | Lifetime | Usage |
|------------|----------|-------|
| Access Token | 15 minutes* | API requests |
| Refresh Token | 7 days | Get new access token |

> *Default configuration. May vary by environment.

### Example Request

```bash
curl -X GET "http://localhost:8080/api/rentals/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
  -H "Accept: application/json"
```

### Public Endpoints (No Auth Required)

**Authentication:**
- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/oauth2/authorize/*`
- `GET /api/oauth2/callback/*`

**Cars (Read-Only):**
- `GET /api/cars` - List all cars
- `GET /api/cars/{id}` - Car details
- `GET /api/cars/search/*` - Search endpoints
- `GET /api/cars/filter-options` - Filter options
- `GET /api/cars/featured` - Featured cars
- `GET /api/cars/active` - Active cars
- `GET /api/cars/{id}/similar` - Similar cars
- `GET /api/cars/{id}/availability/*` - Availability check
- `POST /api/cars/availability/search` - Search available cars
- `GET /api/cars/statistics/*` - Statistics

**Other:**
- `GET /api/exchange-rates/*` - Currency rates
- `POST /api/exchange-rates/convert` - Convert currency
- `GET /api/pricing/preview` - Price preview
- `GET /api/pricing/strategies` - Pricing strategies
- `POST /api/webhooks/*` - External webhooks (signature verified)

---

## Request Format

### Content-Type

All POST/PUT/PATCH requests must include:

```http
Content-Type: application/json
```

### Accept Header

Clients should specify:

```http
Accept: application/json
```

### Request Body Example

```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Query Parameters

- Use `camelCase` for parameter names
- Multiple values: repeat the parameter or use comma-separated values
- Boolean: use `true` or `false` (lowercase)

```http
GET /api/cars/search?brand=Toyota&fuelType=ELECTRIC&available=true
```

---

## Response Format

### Success Response

```json
{
  "id": 1,
  "brand": "Toyota",
  "model": "Corolla",
  "dailyPrice": 45.00,
  "status": "AVAILABLE"
}
```

### Error Response

See [ERROR_CODES.md](./ERROR_CODES.md) for complete error documentation.

```json
{
  "errorCode": "CAR_NOT_FOUND",
  "message": "Car not found with id: 123",
  "timestamp": "2026-01-10T14:30:00",
  "status": 404,
  "path": "/api/cars/123",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

## Pagination

Paginated endpoints use Spring Data's `Pageable` interface.

### Request Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 20 | Number of items per page (max: 100) |
| `sort` | string | varies | Sort field and direction |

### Sort Format

```http
# Single field, ascending (default)
GET /api/cars?sort=brand

# Single field, descending
GET /api/cars?sort=brand,desc

# Multiple fields
GET /api/cars?sort=brand,asc&sort=dailyPrice,desc
```

### Paginated Response

```json
{
  "content": [
    { "id": 1, "brand": "Toyota", ... },
    { "id": 2, "brand": "Honda", ... }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "direction": "ASC"
    }
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false,
  "size": 20,
  "number": 0,
  "numberOfElements": 20
}
```

### Pagination cURL Example

```bash
curl -X GET "http://localhost:8080/api/cars?page=0&size=10&sort=brand,asc" \
  -H "Accept: application/json"
```

---

## Date and Time Format

All dates and times follow **ISO 8601** standard.

### Formats

| Type | Format | Example |
|------|--------|---------|
| Date | `YYYY-MM-DD` | `2026-01-10` |
| DateTime | `YYYY-MM-DDTHH:mm:ss` | `2026-01-10T14:30:00` |
| DateTime (with timezone) | `YYYY-MM-DDTHH:mm:ssZ` | `2026-01-10T14:30:00Z` |

### Timezone

- All timestamps in responses are in **UTC**
- Clients should convert to local timezone as needed
- Request dates (e.g., rental dates) are interpreted as **local dates** (no timezone)

### Request Example

```json
{
  "startDate": "2026-01-15",
  "endDate": "2026-01-20"
}
```

### Response Example

```json
{
  "createTime": "2026-01-10T14:30:00",
  "updateTime": "2026-01-10T15:45:00"
}
```

---

## Naming Conventions

### Summary Table

| Context | Convention | Example |
|---------|------------|---------|
| JSON fields | camelCase | `firstName`, `totalPrice`, `createTime` |
| URL paths | kebab-case | `/api/late-returns`, `/api/quick-actions` |
| Query parameters | camelCase | `?startDate=2026-01-10&carId=5` |
| Enum values | UPPER_SNAKE_CASE | `AVAILABLE`, `IN_MAINTENANCE`, `RENTED` |
| HTTP headers | Title-Case | `Authorization`, `Content-Type` |

### JSON Field Rules

- Use `camelCase` for all field names
- Boolean fields: prefix with `is` or `has` when appropriate (`isActive`, `hasDiscount`)
- Temporal fields: suffix with `Time`, `Date`, or `At` (`createTime`, `startDate`, `expiresAt`)
- ID fields: suffix with `Id` (`carId`, `rentalId`)

### Enum Values

All enum values are returned as uppercase strings:

```json
{
  "status": "AVAILABLE",
  "fuelType": "ELECTRIC",
  "transmissionType": "AUTOMATIC"
}
```

---

## HTTP Status Codes

### Success Codes

| Code | Name | Usage |
|------|------|-------|
| 200 | OK | Successful GET, PUT, PATCH |
| 201 | Created | Successful POST (resource created) |
| 204 | No Content | Successful DELETE |

### Client Error Codes

| Code | Name | Usage |
|------|------|-------|
| 400 | Bad Request | Validation error, malformed request |
| 401 | Unauthorized | Missing or invalid authentication |
| 402 | Payment Required | Payment processing failed |
| 403 | Forbidden | Authenticated but insufficient permissions |
| 404 | Not Found | Resource does not exist |
| 409 | Conflict | Resource already exists, date overlap |
| 429 | Too Many Requests | Rate limit exceeded |

### Server Error Codes

| Code | Name | Usage |
|------|------|-------|
| 500 | Internal Server Error | Unexpected server error |
| 502 | Bad Gateway | External service error (Stripe, etc.) |
| 503 | Service Unavailable | External provider unavailable |

### Status Code Selection Guide

```
Creating a new resource?
  └── Success → 201 Created
  └── Already exists → 409 Conflict

Updating a resource?
  └── Success → 200 OK
  └── Not found → 404 Not Found

Deleting a resource?
  └── Success → 204 No Content
  └── Not found → 404 Not Found

Authentication issue?
  └── No token → 401 Unauthorized
  └── Invalid token → 401 Unauthorized
  └── No permission → 403 Forbidden
```

---

## Additional Notes

### CORS

Cross-Origin Resource Sharing (CORS) is configured for frontend applications. Default allowed origin is `http://localhost:3000`.

### Compression

Responses are gzip-compressed when `Accept-Encoding: gzip` header is present.

### API Versioning

Currently, the API does not use versioning. When breaking changes are introduced, consider:
- URL versioning: `/api/v2/cars`
- Header versioning: `Accept: application/vnd.carrental.v2+json`
