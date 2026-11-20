# API conventions

Conventions shared by every endpoint of the Car Rental API: URLs, authentication, request and response shapes, paging, dates and enums. The list of endpoints and their schemas is not repeated here: the committed contract [`openapi/openapi.json`](../../openapi/openapi.json) describes the operations, and [`openapi/endpoint-inventory.txt`](../../openapi/endpoint-inventory.txt) lists each operation with its required access level (`PUBLIC`, `AUTHENTICATED`, `USER` or `ADMIN`). Both are compared with the running application by `OpenApiContractTest` and `EndpointInventoryTest`.

## Base URL and versioning

Versioned API endpoints are served under `/api/v1`, for example `http://localhost:8082/api/v1/cars` in development. Swagger UI (`/swagger-ui/index.html`) and `/v3/api-docs` are unversioned and exist only outside `prod`. Actuator endpoints are on a separate management port and are not part of the API.

| Path prefix | Area | Access |
|-------------|------|--------|
| `/api/v1/auth` | Register, login, refresh, logout, password reset | Public |
| `/api/v1/oauth2` | Social login (account linking requires authentication) | Public, except `link` |
| `/api/v1/cars` | Catalogue, search, availability, statistics; writes under the same prefix | Public catalogue, statistics and availability reads. Admin writes |
| `/api/v1/rentals` | Request, own list, payment intent | User |
| `/api/v1/rentals` | Detail, cancellation, payment status | Authenticated owner or admin |
| `/api/v1/rentals` | Admin list, confirm, pickup, return, early return | Admin |
| `/api/v1/pricing`, `/api/v1/exchange-rates` | Price preview and currency conversion | Mostly public |
| `/api/v1/admin/**` | Damages, dashboard, alerts, users, late returns, quick actions | Admin |
| `/api/v1/webhooks` | Stripe webhook | Signature verified |

Update the OpenAPI snapshot and endpoint inventory when changing the HTTP contract. [Contribution guide](../../CONTRIBUTING.md#what-a-change-should-respect)

## Authentication

Send the access token from login or refresh as a bearer token:

```http
Authorization: Bearer <access_token>
```

An access token lasts 15 minutes by default and a refresh token 7 days (`JWT_ACCESS_TOKEN_EXPIRATION`, `JWT_REFRESH_TOKEN_EXPIRATION`). A missing or invalid token answers 401 and a valid token without the required role answers 403, both as problem details. `/api/v1/auth/**` and the webhook ignore any bearer header. See [Authentication](../security/AUTHENTICATION.md).

## Requests

- Bodies are JSON (`Content-Type: application/json`); multipart is used only for file uploads.
- JSON field names and query parameter names are camelCase; URL paths are kebab-case.
- Booleans are `true` or `false`.
- Request dates such as rental dates are local calendar dates in the form `yyyy-MM-dd`, without a time zone.
- A client can send `X-Correlation-ID` (letters, digits and `-`, up to 64 characters) to trace a request; otherwise the server generates one. It is echoed in the response header.

## Responses

A successful response is the resource, a `PageResponse` for lists, or an empty body (204 for logout, for example). Create operations answer 201.

Errors are RFC 9457 problem details (`application/problem+json`) with a stable `code`. The fields, every code and its status are in [Error codes](ERROR_CODES.md).

```json
{
  "type": "urn:car-rental:problem:car-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Car not found with id: 123",
  "instance": "/api/v1/cars/123",
  "code": "CAR_NOT_FOUND",
  "timestamp": "2026-11-19T10:15:30.123456Z",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

Status codes in use: 200, 201, 204, 400 (validation, malformed requests), 401, 402 (payment failed), 403 (including `ACCOUNT_BANNED` on a banned login), 404, 405, 406, 409 (conflict, overlapping dates), 413 (`PAYLOAD_TOO_LARGE`: request body over the limit), 415, 429, 500, 502 (Stripe error) and 503 (OAuth2 provider unavailable).

A request that fails validation answers 400 with `code` `VALIDATION_FAILED` and an `errors` array of `{ "field", "message" }`. Two more 400 codes name the offending parameter:

| Code | When | Shape |
|------|------|-------|
| `INVALID_PARAMETER` | A query, path or body value is not one of the allowed enum codes, or is otherwise out of range (unknown sort field, bad page size) | `detail` names the field and the allowed values; `errors[0].field` is the parameter or body property |
| `INVALID_PARAMETER_TYPE` | A value has the wrong type (for example text where a number is expected) or a date is not `yyyy-MM-dd` | `detail` names the parameter |

`detail` never contains a Java class name. Business-rule failures that belong to one request field (for example a penalty waiver above the outstanding penalty, `PENALTY_WAIVER_ERROR`) also carry `errors[0].field`.

## Paging and sorting

Paged endpoints return a `PageResponse`; `page` is zero-based:

```json
{
  "content": [ { "id": 1, "brand": "Toyota" } ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

Most list endpoints take Spring Data parameters:

| Parameter | Default | Meaning |
|-----------|---------|---------|
| `page` | `0` | Page number, zero-based |
| `size` | `20` for most lists, `10` on some admin and dashboard lists | Items per page, 1 to 100; anything else answers 400 `INVALID_PARAMETER` |
| `sort` | varies by endpoint | `field` or `field,direction`; repeat the parameter for several fields |

```http
GET /api/v1/cars/active?page=0&size=10&sort=price,asc
```

The sortable fields depend on the endpoint and are listed in its OpenAPI description. `GET /api/v1/cars/search` is different: it takes its filters, `page` and `size` (1 to 100) as query parameters, plus `sortBy` (`id`, `licensePlate`, `brand`, `model`, `productionYear`, `price`, `createTime` or `rating`; anything else answers 400 with field `sortBy`) and `sortDirection` (`asc` or `desc`). Rows with a null sort value come last and `id` breaks ties, so pages are stable.

## Dates, times and numbers

| Kind | Format | Example |
|------|--------|---------|
| Calendar date (rental start and end, `paymentOpensOn`) | `yyyy-MM-dd` | `2026-11-19` |
| Instant (`createTime`, `paymentDueBy`, `lastLoginAt`, `timestamp` in problem details) | ISO 8601 UTC with a `Z` suffix | `2026-11-19T10:15:30.123456Z` |

Instants always end in `Z`; clients must not read them as local time. The server runs in UTC: the application clock is injected, never read from the machine's zone. Money amounts are JSON numbers in the resource's currency; `currency` is one of `TRY`, `USD`, `EUR`, `GBP` or `JPY`, and amounts are rounded to that currency's decimal places (none for `JPY`). Totals that mix currencies are never produced: reports list amounts per currency.

## Enum values

Enums are exchanged as their constant codes, never as display labels, in responses, query parameters, path variables and request bodies. Matching is exact and case-sensitive: `confirmed`, `Confirmed` and `In Use` are rejected with 400 `INVALID_PARAMETER`; list parameters (`status` on `GET /rentals/admin`) take repeated parameters or a comma separated list of codes.

| Enum | JSON values |
|------|-------------|
| Rental status | `REQUESTED`, `CONFIRMED`, `IN_USE`, `RETURNED`, `CANCELLED` |
| Cancelled by | `CUSTOMER`, `ADMIN`, `SYSTEM` |
| Late-return status | `ON_TIME`, `GRACE_PERIOD`, `LATE`, `SEVERELY_LATE` |
| Car status | `AVAILABLE`, `MAINTENANCE`, `DAMAGED`, `INSPECTION` |
| Payment status | `PENDING`, `AUTHORIZED`, `CAPTURED`, `FAILED`, `REFUNDED`, `CANCELLED` |
| Currency | `TRY`, `USD`, `EUR`, `GBP`, `JPY` |
| Damage status, severity, category | the constant names, for example `UNDER_ASSESSMENT`, `TOTAL_LOSS`, `GLASS_DAMAGE` |

## Rental payment fields

`RentalResponse` carries the payment state so clients do not have to call `GET /rentals/{id}/payment` per row:

- `paymentStatus`: the status of the rental's own payment (absent before approval).
- `paymentOpensOn`: the first day the payment intent can be created.
- `paymentDueBy`: the instant by which an approved rental must be paid; present only while the rental is `CONFIRMED` and its payment is neither `AUTHORIZED` nor `CAPTURED`. The rule is in [ADR-014](../architecture/adr/ADR-014-payment-deadline-after-approval.md).
- `cancelledBy` and `cancellationReason`: who cancelled and the optional reason (absent when unknown or not given).

## Files

Local storage returns relative paths under `/api/v1/files/`. Downloads require an authenticated owner or admin. The damage photo URL lookup also checks ownership or admin access. R2 storage returns presigned absolute HTTPS URLs. [Endpoint inventory](../../openapi/endpoint-inventory.txt)

## CORS

CORS applies to `/api/v1/**` and allows the origins in `CORS_ORIGINS` (explicit `https` origins in `prod`) with credentials. Allowed methods are `GET`, `POST`, `PUT`, `PATCH`, `DELETE` and `OPTIONS`; allowed request headers are `Authorization`, `Content-Type`, `Accept` and `X-Correlation-ID`; `X-Correlation-ID` and `Retry-After` are exposed to browsers.
