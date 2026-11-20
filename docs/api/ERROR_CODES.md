# API error codes

Every error the Car Rental API returns carries a stable `code`. This page lists all of them and the HTTP status each one answers with.

[ErrorCode](../../src/main/java/com/akif/shared/exception/ErrorCode.java) defines the canonical list. [OpenApiContractTest](../../src/test/java/com/akif/contract/OpenApiContractTest.java) checks that the published schema exposes the same values.

## Response format

Errors are RFC 9457 problem details served as `application/problem+json`. Clients should branch on `code`, not on `detail`, which is a human-readable message.

```json
{
  "type": "urn:car-rental:problem:car-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Car not found with id: 999999",
  "instance": "/api/v1/cars/999999",
  "code": "CAR_NOT_FOUND",
  "timestamp": "2026-11-19T10:15:30.123456Z",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

| Field | Type | Meaning |
|-------|------|---------|
| `type` | string | `urn:car-rental:problem:` followed by the lower-case, hyphenated code |
| `title` | string | The HTTP status reason phrase |
| `status` | integer | The HTTP status code |
| `detail` | string | Human-readable description |
| `instance` | string | The request path |
| `code` | string | The stable error code from the table below |
| `timestamp` | string | UTC instant from the application clock |
| `traceId` | string | The request's correlation id, also sent in the `X-Correlation-ID` response header and written to the logs |
| `errors` | array | `[{field, message}]`, present on `VALIDATION_FAILED`, `CONSTRAINT_VIOLATION`, `INVALID_PARAMETER`, `INVALID_PARAMETER_TYPE` (binding failures) and field-specific `PENALTY_WAIVER_ERROR` |
| `details` | object | Extra context on some errors: `paymentOpensOn` on `PAYMENT_WINDOW_NOT_OPEN`, `provider` on OAuth2 errors, `retryAfterSeconds` on a 429 raised by a controller-level limit |

A 429 also carries a `Retry-After` header with whole seconds. See [Rate limiting](RATE_LIMITING.md).

Responses produced by the security filters (401 `AUTHENTICATION_FAILED`, 403 `ACCESS_DENIED`, 429 from the endpoint buckets) use the same body.

## Codes

A code with several statuses lists each one; the description says which situation maps to which status.

| Code | HTTP status | Meaning |
|------|-------------|---------|
| `ACCESS_DENIED` | 403 | The caller lacks the required authorization. Protected resources such as another customer's rental or local file return 404. |
| `ACCOUNT_BANNED` | 403 | A login with the correct password was refused because an admin banned the account; the message tells the user to contact support. A wrong password still answers 401 `AUTHENTICATION_FAILED`. |
| `ALERT_NOT_FOUND` | 404 | The dashboard alert does not exist. |
| `AUTHENTICATION_FAILED` | 401 | No valid credentials or bearer token were supplied, or a login failed. |
| `BAD_REQUEST` | 400 | A client error raised by the framework that has no more specific code. |
| `CAR_ALREADY_EXISTS` | 409 | A car with the same unique identifier already exists. |
| `CAR_DATES_UNAVAILABLE` | 409 | The car is already booked for an overlapping date range, or another rental of it is still in use. Enforced by the database exclusion constraint as well as by a pre-check. |
| `CAR_NOT_AVAILABLE` | 400 | The car's fleet status is not `AVAILABLE`. |
| `CAR_NOT_FOUND` | 404 | The car does not exist or is soft-deleted. |
| `CAR_VALIDATION_FAILED` | 400 | Car data failed a business validation. |
| `CONCURRENT_MODIFICATION` | 409 | Optimistic locking detected a concurrent update; reload and retry. |
| `CONSTRAINT_VIOLATION` | 400 | Bean Validation rejected a constrained parameter; `errors` lists the fields. |
| `DAMAGE_ASSESSMENT_ERROR` | 400 | A damage assessment or charge operation is not allowed in the report's current state or with the given data (for example charging a report that is not ASSESSED, or one without liability). A declined charge is not an error: `POST /api/v1/admin/damages/{id}/charge` answers 200 with `charged: false`. |
| `DAMAGE_DISPUTE_ERROR` | 400, 403, 502 | A dispute operation is not allowed (400), the caller does not own the rental (403), or the refund required by a resolution failed and nothing was changed (502). |
| `DAMAGE_REPORT_ERROR` | 400, 404 | A damage report operation failed (400), or the report or photo does not exist (404). |
| `DATA_CONFLICT` | 409 | The request violates a database integrity constraint. |
| `EMAIL_SEND_FAILED` | 500 | Sending a notification e-mail failed. |
| `ENDPOINT_NOT_FOUND` | 404 | No handler or static resource matches the path. |
| `FILE_NOT_FOUND` | 404 | The requested local file is missing or inaccessible to the caller. |
| `FILE_UPLOAD_ERROR` | 400 | An uploaded file is missing, too large, of a disallowed type, or could not be stored. |
| `INTERNAL_SERVER_ERROR` | 500 | An unexpected server error. The body does not expose internals. |
| `INVALID_JSON` | 400 | The request body is not valid JSON or does not match the expected shape. |
| `INVALID_MULTIPART_REQUEST` | 400 | The multipart request is malformed. |
| `INVALID_PARAMETER` | 400 | A query, path or body value is not an allowed enum code (exact, case-sensitive) or is otherwise out of range such as a page size outside 1 to 100; `errors[0].field` names it and `detail` lists the allowed values. |
| `INVALID_PARAMETER_TYPE` | 400 | A parameter has the wrong type, or a date is not `yyyy-MM-dd`. |
| `INVALID_PASSWORD` | 400 | The current password is wrong, or the new password equals the current one. |
| `INVALID_PAYMENT_STATE` | 409 | The payment state does not allow the operation. |
| `INVALID_RENTAL_STATE` | 400 | The rental is not in the status the operation requires. |
| `INVALID_STATUS_TRANSITION` | 400 | A car or rental status transition is not allowed. |
| `INVALID_TOKEN` | 401, 400 | A refresh token is unknown or was already rotated (401), or a password reset token is invalid (`Invalid password reset token`), expired (`Password reset token has expired`) or already used (400). |
| `INVALID_USER_STATE` | 409 | The user is already in the requested state, for example banning a banned user. |
| `METHOD_NOT_ALLOWED` | 405 | The HTTP method is not supported for the path. |
| `MISSING_PARAMETER` | 400 | A required request parameter is missing. |
| `NOT_ACCEPTABLE` | 406 | No representation matches the `Accept` header. |
| `OAUTH2_ACCOUNT_ALREADY_LINKED` | 409 | The social account is already linked to another user. |
| `OAUTH2_AUTHENTICATION_FAILED` | 401 | The provider did not authenticate the user. |
| `OAUTH2_AUTHORIZATION_DENIED` | 401 | The user denied authorization at the provider. |
| `OAUTH2_EMAIL_REQUIRED` | 400 | The provider returned no usable e-mail address. |
| `OAUTH2_INVALID_STATE` | 400 | The `state` parameter is missing, malformed, badly signed or expired. |
| `OAUTH2_INVALID_TOKEN` | 401 | The provider rejected the authorization code or token. |
| `OAUTH2_LOCAL_ACCOUNT_UNVERIFIED` | 409 | A local account with this e-mail exists but is not verified, so it is not linked automatically. Log in with the password and link the provider. |
| `OAUTH2_PROVIDER_NOT_SUPPORTED` | 400 | The provider in the path is not `google` or `github`. |
| `OAUTH2_PROVIDER_UNAVAILABLE` | 503 | The provider is temporarily unavailable. |
| `OAUTH2_SOCIAL_LOGIN_REQUIRED` | 401 | The account has no password and must sign in through its provider. |
| `OPEN_RENTAL_REQUEST_LIMIT_EXCEEDED` | 409 | The user already has `rental.max-open-requests-per-user` requests in status `REQUESTED`. |
| `PAYLOAD_TOO_LARGE` | 413 | An upload or request body exceeds the size limit. |
| `PAYMENT_DEADLINE_PASSED` | 409 | The payment-intent call arrived at or after `paymentDueBy`; `details.paymentDueBy` gives the deadline. |
| `PAYMENT_FAILED` | 402 | A payment operation (for example capture at pickup) failed. |
| `PAYMENT_NOT_AUTHORIZED` | 409 | Pickup requires an authorized or captured payment. |
| `PAYMENT_NOT_FOUND` | 404 | No payment exists for the rental. |
| `PAYMENT_WINDOW_NOT_OPEN` | 409 | Payment opens `rental.payment-window-days` before the start date; `details.paymentOpensOn` gives the date. |
| `PENALTY_WAIVER_ERROR` | 400 | The penalty waiver is not allowed or failed. A waiver above the outstanding penalty or without a positive amount carries `errors[0].field` `waiverAmount`. |
| `RATE_LIMIT_EXCEEDED` | 429 | A rate-limit bucket is empty. The response carries a `Retry-After` header. |
| `RECONCILIATION_ERROR` | 500 | The payment reconciliation job failed. |
| `RENTAL_CANCEL_NOT_ALLOWED` | 400 | Only `REQUESTED` and `CONFIRMED` rentals can be cancelled; an `IN_USE` rental ends through return or early return. |
| `RENTAL_NOT_FOUND` | 404 | The rental does not exist. |
| `RENTAL_VALIDATION_FAILED` | 400 | Rental dates or data break a rule: start in the past, end not after start, lead time or length over the limit, pickup outside the permitted dates, including the no-show cutoff. |
| `STRIPE_INTEGRATION_ERROR` | 502 | Stripe returned an error or could not be reached. |
| `TOKEN_EXPIRED` | 401 | The refresh token has expired. |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | The request content type is not supported. |
| `USER_ALREADY_EXISTS` | 409 | The username or e-mail is already registered. |
| `USER_NOT_FOUND` | 404 | The user does not exist. |
| `VALIDATION_FAILED` | 400 | Request body validation failed; `errors` lists `{field, message}` pairs. |
| `WEBHOOK_PAYLOAD_TOO_LARGE` | 413 | The Stripe webhook body exceeds `stripe.webhook.max-body-bytes`. |
| `WEBHOOK_PROCESSING_FAILED` | 500 | Webhook processing failed; Stripe redelivers the event. |
| `WEBHOOK_SIGNATURE_INVALID` | 400 | The `Stripe-Signature` is invalid or its timestamp is outside the tolerance. |
| `WEBHOOK_SIGNATURE_MISSING` | 400 | The `Stripe-Signature` header is absent. |

## Validation error example

```json
{
  "type": "urn:car-rental:problem:validation-failed",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/rentals/request",
  "code": "VALIDATION_FAILED",
  "timestamp": "2026-11-19T10:15:30.123456Z",
  "traceId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "errors": [
    { "field": "carId", "message": "Car ID cannot be null" }
  ]
}
```

## Adding a code

A new code needs an entry in the `ErrorCode` enum and a refresh of `openapi/openapi.json` with `scripts/update-contract.sh`; `OpenApiContractTest` fails until both match. Update this page in the same change.
