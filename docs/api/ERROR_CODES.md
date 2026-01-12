# API Error Codes

Comprehensive documentation of all error codes returned by the Car Rental API.

## Table of Contents

- [Overview](#overview)
- [Error Response Format](#error-response-format)
- [Error Codes by Module](#error-codes-by-module)
  - [Auth Module](#auth-module)
  - [Car Module](#car-module)
  - [Rental Module](#rental-module)
  - [Payment Module](#payment-module)
  - [Damage Module](#damage-module)
  - [Dashboard Module](#dashboard-module)
  - [Notification Module](#notification-module)
  - [Shared/Common](#sharedcommon)
  - [Framework/Validation Errors](#frameworkvalidation-errors)

---

## Overview

The Car Rental API uses structured error responses to help clients handle errors gracefully. Each error includes:

- **HTTP Status Code**: Standard HTTP status indicating the error category
- **Error Code**: Unique identifier for the specific error type
- **Message**: Human-readable description of the error
- **Path**: The API endpoint that generated the error

All error codes are defined as constants in the codebase and follow a consistent naming pattern: `MODULE_ERROR_TYPE` (e.g., `CAR_NOT_FOUND`, `RENTAL_DATE_OVERLAP`).

---

## Error Response Format

All API errors return a consistent JSON structure:

```json
{
  "errorCode": "CAR_NOT_FOUND",
  "message": "Car with id 123 not found",
  "timestamp": "2026-01-10T14:30:00",
  "status": 404,
  "path": "/api/cars/123",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "validationErrors": null,
  "details": null
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `errorCode` | string | Unique error identifier (e.g., `CAR_NOT_FOUND`) |
| `message` | string | Human-readable error description |
| `timestamp` | string | ISO 8601 datetime when error occurred |
| `status` | integer | HTTP status code |
| `path` | string | Request path that caused the error |
| `traceId` | string | UUID for tracking/debugging (correlates with logs) |
| `validationErrors` | object | Field-level validation errors (present on validation failures) |
| `details` | object | Additional error context (optional) |

### Validation Error Example

When validation fails, `validationErrors` contains field-specific messages:

```json
{
  "errorCode": "VALIDATION_FAILED",
  "message": "Validation failed",
  "timestamp": "2026-01-10T14:30:00",
  "status": 400,
  "path": "/api/rentals",
  "traceId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "validationErrors": {
    "startDate": "Start date must be in the future",
    "endDate": "End date must be after start date"
  },
  "details": null
}
```

### cURL Example

```bash
# Request a non-existent car
curl -X GET "http://localhost:8080/api/cars/999999" \
  -H "Authorization: Bearer <your_jwt_token>" \
  -H "Accept: application/json"

# Response (404 Not Found)
{
  "errorCode": "CAR_NOT_FOUND",
  "message": "Car with id 999999 not found",
  "timestamp": "2026-01-10T14:30:00",
  "status": 404,
  "path": "/api/cars/999999",
  "traceId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "validationErrors": null,
  "details": null
}
```

---

## Error Codes by Module

### Auth Module

Authentication and authorization related errors.

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `USER_ALREADY_EXISTS` | 409 Conflict | Email or username is already registered | Use a different email address |
| `authorization_denied` | 401 Unauthorized | User denied OAuth2 authorization | Retry OAuth flow, user must grant access |
| `authentication_failed` | 401 Unauthorized | OAuth2 authentication failed | Check credentials and retry |
| `invalid_token` | 401 Unauthorized | Invalid or expired OAuth2 token | Re-authenticate with provider |
| `provider_unavailable` | 503 Service Unavailable | OAuth2 provider is temporarily unavailable | Retry later |
| `email_required` | 400 Bad Request | Email is required but not provided by OAuth2 | Ensure email permission is granted |
| `invalid_state` | 400 Bad Request | Invalid state parameter (possible CSRF attack) | Restart OAuth flow from beginning |
| `account_already_linked` | 409 Conflict | Social account is already linked to another user | Use different social account or unlink first |
| `social_login_required` | 401 Unauthorized | Account requires social login (no password set) | Use the original OAuth2 provider to login |

**Note:** OAuth2 errors use lowercase `snake_case` codes as per OAuth2 specification.

### Car Module

Car management related errors.

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `CAR_NOT_FOUND` | 404 Not Found | Car with given ID does not exist | Verify the car ID is correct |
| `CAR_NOT_AVAILABLE` | 400 Bad Request | Car is not available for rental | Choose a different car or date range |
| `CAR_ALREADY_EXISTS` | 409 Conflict | Car with same identifier already exists | Use different license plate or VIN |
| `CAR_CANNOT_BE_RESERVED` | 400 Bad Request | Car cannot be reserved in current state | Check car availability status |
| `CAR_CANNOT_BE_SOLD` | 400 Bad Request | Car cannot be sold (has active rentals) | Complete or cancel active rentals first |
| `CAR_OPERATION_NOT_ALLOWED` | 403 Forbidden | Operation not allowed on this car | Check car state and your permissions |
| `CAR_VALIDATION_FAILED` | 400 Bad Request | Car data validation failed | Fix the validation errors in request |

### Rental Module

Rental booking and management related errors.

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `RENTAL_NOT_FOUND` | 404 Not Found | Rental with given ID does not exist | Verify the rental ID is correct |
| `INVALID_RENTAL_STATE` | 400 Bad Request | Invalid rental state transition attempted | Check current state, see valid transitions |
| `RENTAL_DATE_OVERLAP` | 409 Conflict | Requested dates overlap with existing rental | Choose different dates or different car |
| `RENTAL_VALIDATION_FAILED` | 400 Bad Request | Rental data validation failed | Fix the validation errors in request |
| `LATE_RETURN_ERROR` | 400 Bad Request | Error processing late return | Check rental dates and contact support |
| `PENALTY_CALCULATION_ERROR` | 500 Internal Server Error | Failed to calculate penalty amount | Contact support |
| `PENALTY_WAIVER_ERROR` | 400 Bad Request | Failed to waive penalty | Check waiver eligibility requirements |
| `INVALID_PENALTY_CONFIG` | 400 Bad Request | Penalty configuration is invalid | Contact admin to fix configuration |

### Payment Module

Payment processing related errors.

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `PAYMENT_FAILED` | 402 Payment Required | Payment processing failed | Retry with a valid payment method |
| `STRIPE_INTEGRATION_ERROR` | 502 Bad Gateway | Stripe API communication error | Retry later, contact support if persists |
| `WEBHOOK_SIGNATURE_INVALID` | 400 Bad Request | Invalid Stripe webhook signature | Check webhook endpoint configuration |
| `RECONCILIATION_ERROR` | 500 Internal Server Error | Payment reconciliation failed | Contact support |

### Damage Module

Damage reporting and dispute related errors.

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `DAMAGE_REPORT_ERROR` | 400/404 Bad Request/Not Found | Damage report operation failed | Check damage report data or ID |
| `DAMAGE_ASSESSMENT_ERROR` | 400 Bad Request | Damage assessment operation failed | Check assessment data and damage status |
| `DAMAGE_DISPUTE_ERROR` | 400/403 Bad Request/Forbidden | Damage dispute operation failed | Check dispute eligibility and ownership |

### Dashboard Module

Admin dashboard related errors.

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `ALERT_NOT_FOUND` | 404 Not Found | Alert with given ID does not exist | Verify the alert ID is correct |

### Notification Module

Email and notification related errors.

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `EMAIL_SEND_FAILED` | 500 Internal Server Error | Failed to send email notification | Check SMTP configuration, retry later |

### Shared/Common

General errors that can occur across any module.

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `INVALID_STATUS_TRANSITION` | 400 Bad Request | Invalid state machine transition | Check valid state transitions in docs |
| `FILE_UPLOAD_ERROR` | 400 Bad Request | File upload failed | Check file size and type restrictions |
| `INVALID_TOKEN` | 401 Unauthorized | JWT token is invalid or malformed | Re-authenticate to get a new token |
| `TOKEN_EXPIRED` | 401 Unauthorized | JWT token has expired | Use refresh token or re-login |

### Framework/Validation Errors

These errors are handled by the global exception handler and not specific to any module.

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `AUTHENTICATION_FAILED` | 401 Unauthorized | Authentication failed | Check credentials |
| `ACCESS_DENIED` | 403 Forbidden | Insufficient permissions | Check required role/permissions |
| `VALIDATION_FAILED` | 400 Bad Request | Request body validation failed | Fix field-specific errors in response |
| `CONSTRAINT_VIOLATION` | 400 Bad Request | Database constraint violation | Check unique/required fields |
| `INVALID_JSON` | 400 Bad Request | Malformed JSON in request body | Fix JSON syntax |
| `MISSING_PARAMETER` | 400 Bad Request | Required query parameter missing | Include required parameter |
| `INVALID_PARAMETER_TYPE` | 400 Bad Request | Wrong parameter type | Check expected type in docs |
| `ENDPOINT_NOT_FOUND` | 404 Not Found | Requested endpoint does not exist | Check API documentation |
| `INTERNAL_SERVER_ERROR` | 500 Internal Server Error | Unexpected server error | Contact support with traceId |
