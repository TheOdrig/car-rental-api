# Configuration

The [base properties](../src/main/resources/application.properties), [development profile](../src/main/resources/application-dev.properties), [production profile](../src/main/resources/application-prod.properties) and [environment template](../.env.example) define application settings. This portfolio project runs locally.

## Profiles

| Profile | Used for | What differs |
|---------|----------|--------------|
| `dev` | Local work and `docker compose up` | Sample data from `db/seed`, stub payment gateway, mock e-mail sender (logs instead of sending), local file storage, Swagger UI and `/v3/api-docs`, dev-only defaults for secrets, `spring.flyway.clean-disabled=false` |
| `test` | The test suite | Stub payment gateway, mock e-mail sender, local file storage, rate limiting off, PostgreSQL from Testcontainers |
| `prod` | Deployment | No defaults for secrets and endpoints, real Stripe gateway (test-mode keys only), SendGrid SMTP, Cloudflare R2 storage, Swagger and API docs off, startup validation |

Select the profile with `SPRING_PROFILES_ACTIVE`. In a deployment the value must be exactly `prod`: another value such as `production` selects no `prod` beans and no validator.

In `prod`, an unset `${VAR}` placeholder or an invalid value stops startup. `ProdConfigurationValidator` reports every problem in one message: missing or short (under 32 bytes) `JWT_SECRET` and `OAUTH2_STATE_SECRET`, placeholder-looking secrets, missing required values, live Stripe keys, a webhook secret that does not start with `whsec_`, CORS origins that are not explicit `https` origins, and invalid proxy settings.

## Server and management port

| Variable | Default | Meaning |
|----------|---------|---------|
| `PORT` | `8082` | API port |
| `MANAGEMENT_PORT` | `8081` | Actuator port. Serves `health` (with liveness and readiness probes), `info` and `prometheus` only. Do not expose it publicly. |

Actuator paths do not exist on the API port, and unexposed actuator paths can return 401 or 404. Health details are never shown, and the mail health indicator is disabled so an SMTP outage does not change readiness. Readiness includes the database; liveness does not.

## Database

| Variable | Default | Meaning |
|----------|---------|---------|
| `DATABASE_URL` | `dev`: `jdbc:postgresql://localhost:5432/car_rental`; `prod`: required | JDBC URL |
| `DB_USERNAME` | `dev`: `postgres`; `prod`: required | Database user |
| `DB_PASSWORD` | `dev`: a local placeholder; `prod`: required | Database password |
| `DB_SCHEMA` | `car_rental` | Schema that Flyway creates and Hibernate uses |

PostgreSQL 18 is the tested version. Flyway runs on startup (`classpath:db/migration`; `dev` also loads `classpath:db/seed`), Hibernate runs with `ddl-auto=validate`, and `spring.flyway.clean-disabled=true` outside `dev`. Every pooled session runs in UTC and with `statement_timeout` 30 s, `lock_timeout` 5 s and `idle_in_transaction_session_timeout` 60 s (set by the Hikari init SQL; override with `DB_STATEMENT_TIMEOUT`, `DB_LOCK_TIMEOUT` and `DB_IDLE_IN_TRANSACTION_TIMEOUT`). The pool holds at most 10 connections, waits 5 s for one (`connection-timeout`) and logs a stack trace when a connection is held for more than 60 s (`leak-detection-threshold`). Flyway migrates on its own non-pooled connection (`FlywayDataSourceConfig`, a non-default `DataSource` bean so the Hikari pool stays the primary one; `PooledDataSourceWiringTest` boots from plain `spring.datasource.*` properties and asserts it), so migrations are not subject to those timeouts and never leave a changed session in the pool. Refunds raise or lower the timeouts per transaction with `SET LOCAL` (see Payments). Migration `V4` runs `CREATE EXTENSION IF NOT EXISTS btree_gist`, which needs a privilege on managed servers; see [the overlap migration](../src/main/resources/db/migration/V4__rental_overlap_exclusion.sql).

Docker Compose publishes the database on `localhost:${DB_PORT:-5433}` and reads optional `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `APP_PORT` and `MANAGEMENT_HOST_PORT` from a UTF-8 `.env`.

## JWT

| Variable | Default | Meaning |
|----------|---------|---------|
| `JWT_SECRET` | `dev`: a dev-only value; `prod`: required | HS256 signing key, at least 32 bytes |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `900000` | Access token lifetime in milliseconds (15 minutes) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | `604800000` | Refresh token lifetime in milliseconds (7 days) |
| `JWT_REFRESH_REUSE_GRACE_SECONDS` | `10` | Window in which presenting a just-rotated refresh token is rejected without revoking its family |

The issuer is the fixed value `car-rental-api`. See [Authentication](security/AUTHENTICATION.md).

## Client address and proxies

| Variable | Default | Meaning |
|----------|---------|---------|
| `CORS_ORIGINS` | `dev`: `http://localhost:3000,http://localhost:4200`; `prod`: required | Comma-separated allowed origins. `prod` accepts only explicit `https` origins, never `*`. Credentials are allowed; CORS applies to `/api/v1/**`. |
| `TRUST_FORWARDED_HEADERS` | `dev`: `false`; `prod`: required | Whether `X-Forwarded-For` may identify the client for rate limiting |
| `TRUSTED_PROXY_HOPS` | `1` outside `prod`; `prod`: required | Number of trusted proxies that append to `X-Forwarded-For` (at least 1) |
| `TRUSTED_CLIENT_IP_HEADER` | empty | Header that carries the end-user address, for example `X-Client-IP` |
| `TRUSTED_PROXY_CIDRS` | empty | Comma-separated IPv4 and IPv6 CIDRs of the proxies allowed to set those headers. `prod` requires it when `TRUST_FORWARDED_HEADERS=true` or `TRUSTED_CLIENT_IP_HEADER` is set, and rejects any `/0` range. |

Client address resolution is described in [Rate limiting](api/RATE_LIMITING.md#client-address). The rate limits themselves are properties under `security.rate-limit.limits.*` and are listed there.

## OAuth2 social login

| Variable | Default | Meaning |
|----------|---------|---------|
| `APP_BASE_URL` | `dev`: `http://localhost:3000`; `prod`: required | Public URL of the web application |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | `dev`: empty; `prod`: required | Google OAuth2 client |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | `dev`: empty; `prod`: required | GitHub OAuth2 client |
| `OAUTH2_STATE_SECRET` | `dev`: a dev-only value; `prod`: required | HMAC key for the `state` parameter, at least 32 bytes |

The redirect URIs are `${APP_BASE_URL}/api/auth/callback/google` and `${APP_BASE_URL}/api/auth/callback/github`: routes of the web application, not of this API. Register exactly these with each provider. `oauth2.state.expiration-minutes` (default 10) limits the age of a `state` value. The same `APP_BASE_URL` builds the links in the password-reset e-mail (`/reset-password?token=...`) and the pay-again e-mail (`/rentals/{id}/pay`).

## Stripe

| Variable | Default | Meaning |
|----------|---------|---------|
| `STRIPE_API_KEY` | `dev`: a placeholder; `prod`: required | Test-mode secret key |
| `STRIPE_WEBHOOK_SECRET` | `dev`: a placeholder; `prod`: required | Signing secret of the webhook endpoint |
| `STRIPE_PUBLISHABLE_KEY` | empty | Test-mode publishable key, returned to the web client with the PaymentIntent |
| `STRIPE_METADATA_ENVIRONMENT` | `dev` outside `prod`; `prod`: required | Value of the `env` metadata tag on every PaymentIntent. Webhook events with a different or missing tag are ignored, so give each environment that shares a Stripe account its own value. |

Only test mode is supported: `prod` startup fails for `sk_live_`, `pk_live_` and `rk_live_` keys. `dev` and `test` use a stub gateway and never call Stripe.

| Property | Default | Meaning |
|----------|---------|---------|
| `stripe.webhook.tolerance-seconds` | `300` | Allowed skew of the signed webhook timestamp |
| `stripe.webhook.max-body-bytes` | `262144` | Largest accepted webhook body; larger bodies answer 413 |
| `stripe.api.max-network-retries` | `1` | Stripe SDK retries |
| `stripe.api.connect-timeout-ms`, `stripe.api.read-timeout-ms` | `3000`, `10000` | Stripe HTTP timeouts. Some transactions hold row locks across a Stripe call, so (connect + read) x (`stripe.api.max-network-retries` + 1) must stay below `DB_IDLE_IN_TRANSACTION_TIMEOUT` (60 s); `StripeTimeoutGuard` fails the start otherwise |
| `payment.refund.lock-timeout-ms`, `payment.refund.statement-timeout-ms` | `5000`, `30000` | Lock wait and statement timeout for refunds, which hold the payment row lock through the Stripe call |
| `payment.sync-min-interval-seconds` | `10` | Minimum interval between refreshes of a pending payment from Stripe |
| `payment.repair.enabled` | `true` | Runs the repair of PENDING and AUTHORIZED payments that are out of step with Stripe |
| `payment.repair.min-age-minutes` | `30` | A payment is read from Stripe again only when it was last synced or updated longer ago than this |
| `payment.repair.interval-ms`, `payment.repair.initial-delay-ms` | `1800000`, `300000` | Fixed delay between repair runs and the delay before the first one (at most 100 payments per run) |
| `payment.reconciliation.max-individual-lookups` | `200` | Most single-intent Stripe lookups per nightly reconciliation for payments outside the listing window; the rest are reported as `LOOKUP_FAILED` |

The webhook endpoint is `POST /api/v1/webhooks/stripe`. It handles `payment_intent.amount_capturable_updated`, `payment_intent.succeeded`, `payment_intent.payment_failed`, `payment_intent.canceled`, `charge.refunded`, `refund.updated` and `refund.failed`, and acknowledges other event types without acting on them. Events tagged with another installation's `installation_id` are ignored; see [Stripe webhook security](security/AUTHENTICATION.md#stripe-webhook-security).

## Rentals

| Property | Default | Meaning |
|----------|---------|---------|
| `rental.max-rental-days` | `30` | Longest rental, counting both end dates |
| `rental.max-lead-days` | `180` | Furthest start date from today |
| `rental.max-open-requests-per-user` | `3` | Requests in status `REQUESTED` per user; more answer 409 |
| `rental.request-expiry-hours` | `24` | `REQUESTED` rentals older than this are cancelled |
| `rental.payment-grace-hours` | `12` | Valid from 1 to 72 hours. The deadline is the later of midnight UTC at the start of `startDate` and approval time plus this grace. See [ADR-014](architecture/adr/ADR-014-payment-deadline-after-approval.md). |
| `rental.payment-window-days` | `5` | Valid from 1 to 6 days. The payment intent can be created this many days before the start date. Earlier calls return 409 `PAYMENT_WINDOW_NOT_OPEN`. |
| `rental.no-show-days` | `1` | Cancels a no-show with an authorization this many days after the start date and releases the hold. Valid from 1 through `max(1, 6 - rental.payment-window-days)` using the application's seven-day hold assumption. The last pickup day is the earlier of the end date and `startDate + no-show-days - 1`. |
| `rental.no-show-cron` | `0 */30 * * * *` | Schedule of the no-show release |
| `rental.request-expiry-cron`, `rental.payment-expiry-cron` | `0 */10 * * * *` | Schedules of the two expiry jobs. The payment job checks for authorization or capture before cancelling an unpaid approval at or after its payment deadline. |

Out-of-range rental settings and incompatible no-show values fail startup. [Rental properties](../src/main/java/com/akif/rental/internal/config/RentalProperties.java)

## Pricing

Defaults live in `PricingConfig` and are overridden under `pricing.*`.

| Property | Default | Meaning |
|----------|---------|---------|
| `pricing.min-daily-price`, `pricing.max-daily-price` | `10`, `10000` | Daily price floor and ceiling in the car's currency, applied to the final price |
| `pricing.daily-limits.<currency>.min`, `.max` | `jpy`: `1000`, `1500000` | Per-currency override of the two limits above |
| `pricing.strategy.season-enabled`, `early-booking-enabled`, `duration-enabled`, `weekend-enabled`, `demand-enabled` | `true` | Switch a strategy on or off |
| `pricing.season.peak`, `pricing.season.offpeak` | `06-01` to `08-31` at 1.25; `11-01` to `02-28` at 0.90 | Season windows and multipliers |
| `pricing.early-booking.tier1..3` | 30 days at 0.85, 14 days at 0.90, 7 days at 0.95 | Lead-time discounts |
| `pricing.duration.tier1..3` | 7 days at 0.90, 14 days at 0.85, 30 days at 0.80 | Rental-length discounts |
| `pricing.weekend.multiplier`, `pricing.weekend.days` | `1.15`, Friday to Sunday | Weekend surcharge |
| `pricing.demand.high`, `pricing.demand.moderate` | threshold 80 at 1.20, threshold 50 at 1.10 | Demand surcharges; thresholds are fleet occupancy percentages (occupied bookable car-days divided by all bookable car-days, counting committed bookings) |

A multiplier outside its expected range (discounts 0.50 to 1.00, surcharges 1.00 to 2.00) is logged as a warning at startup. Amounts are rounded to the currency's decimal places (JPY has none).

## Late-return penalties

| Property | Default | Valid range |
|----------|---------|-------------|
| `penalty.grace-period-minutes` | `60` | 0 to 120 |
| `penalty.hourly-penalty-rate` | `0.10` | 0.05 to 0.25 |
| `penalty.daily-penalty-rate` | `1.50` | 1.00 to 2.00 |
| `penalty.penalty-cap-multiplier` | `5.0` | 3.0 to 10.0 |
| `penalty.hourly-tier-max-hours` | `6` | At least 1 and below both other hour thresholds |
| `penalty.single-day-tier-max-hours` | `24` | Above the hourly tier maximum |
| `penalty.severely-late-threshold-hours` | `24` | Above the hourly tier maximum |

Invalid values fail startup. From 23:59:59 UTC on the end date, the application truncates elapsed time to whole minutes, subtracts the grace and rounds any positive remainder up to hours. The table gives the default tiers. [Penalty settings](../src/main/java/com/akif/rental/internal/config/PenaltyConfig.java)

| Hours late | Penalty |
|------------|---------|
| within the grace period | none |
| 1 to 6 | `hourly-penalty-rate` x hours x daily price |
| 7 to 24 | `daily-penalty-rate` x daily price (one day) |
| over 24 | `daily-penalty-rate` x late days rounded up x daily price |

The result is capped at `penalty-cap-multiplier` x the daily price and rounded to the currency's decimal places.

## Damage management

| Property | Default | Meaning |
|----------|---------|---------|
| `damage.minor-threshold`, `damage.moderate-threshold`, `damage.major-threshold` | `500`, `2000`, `10000` | A repair cost below the first is `MINOR`, below the second `MODERATE`, below the third `MAJOR`, otherwise `TOTAL_LOSS` |
| `damage.default-insurance-deductible` | `1000.00` | Deductible applied when insurance covers a damage and none is given |
| `damage.max-photos-per-report` | `10` | Photos per report |
| `damage.max-photo-size-bytes` | `10485760` | Largest photo; also the multipart file limit |
| `damage.allowed-photo-types` | `image/jpeg,image/png,image/heic` | Accepted photo types |
| `damage.photo-url-expiration-minutes` | `60` | Lifetime of a photo access URL |

Set `damage.currency-limits.<currency>` to override thresholds and the insurance deductible field by field. The bundled JPY override sets the thresholds to 2000, 8000 and 40000, with a deductible of 4000. [Damage settings](../src/main/java/com/akif/damage/internal/config/DamageConfig.java)

The multipart request limit is 100 MB. Avatar uploads accept at most 5 MB.

## Currency

| Property | Default | Meaning |
|----------|---------|---------|
| `EXCHANGE_RATE_API_KEY` | `dev`: empty; `prod`: required | Key for the exchange-rate provider |
| `exchange-rate.api.connect-timeout`, `exchange-rate.api.read-timeout` | `5000` | Provider timeouts in milliseconds |
| `currency.refresh.interval` | `3600000` | Rate refresh interval in milliseconds |
| `currency.fallback.*` | USD 1, TRY 42.49, EUR 0.87, GBP 0.76, JPY 156 | Rates used when the provider call fails |

Supported currencies are `TRY`, `USD`, `EUR`, `GBP` and `JPY`.

## E-mail

| Variable | Default | Meaning |
|----------|---------|---------|
| `EMAIL_FROM` | `dev`: local placeholder; `prod`: required | Sender address |
| `EMAIL_ENABLED` | `dev`: `false`; `prod`: fixed `true` | Master switch |
| `SENDGRID_HOST`, `SENDGRID_PORT`, `SENDGRID_USERNAME` | `smtp.sendgrid.net`, `587`, `apikey` | SMTP endpoint |
| `SENDGRID_API_KEY` | `dev`: empty; `prod`: required | SMTP password |

`dev` and `test` use a mock sender that logs each message; only `prod` sends through SMTP. SMTP timeouts are 5 s to connect and 10 s to read and write. Listeners send e-mail from an executor of 2 to 4 threads (`email.async.*`) after the triggering transaction commits.

### Notification e-mails

Each application event triggers one template under `src/main/resources/templates/email/`. Templates share the fragments in `email/shared/`. The variables each template receives are set in `EmailTemplateServiceImpl`.

| Template | Triggered by |
|----------|--------------|
| `rental/rental-confirmation` | `RentalConfirmedEvent` |
| `rental/pickup-reminder`, `rental/return-reminder` | `PickupReminderEvent`, `ReturnReminderEvent`, published by `ReminderScheduler` (daily at 08:00 and 09:00) |
| `rental/cancellation-confirmation` | `RentalCancelledEvent` |
| `payment/payment-receipt` | `PaymentCapturedEvent` |
| `payment/payment-retry` | `PaymentRetryRequestedEvent` |
| `penalty/grace-period-warning`, `penalty/late-return-notification`, `penalty/severely-late-notification` | `GracePeriodWarningEvent`, `LateReturnNotificationEvent`, `SeverelyLateNotificationEvent` |
| `penalty/penalty-summary` | `PenaltySummaryEvent` |
| `damage/damage-reported`, `damage-assessed`, `damage-charged`, `damage-charge-failed`, `damage-disputed`, `damage-resolved` | the matching `Damage*Event` |
| `auth/password-reset` | `PasswordResetRequestedEvent`; the link is valid for one hour |

## File storage

`prod` stores avatars and damage photos in Cloudflare R2 (S3-compatible). `dev` and `test` use local storage under `file.upload.base-dir` (default `uploads`).

| Variable | Meaning |
|----------|---------|
| `R2_ENDPOINT` | R2 endpoint URL |
| `R2_BUCKET_NAME` | Bucket name |
| `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` | Access credentials |

All four are required in `prod`.

## Scheduled jobs

| Job | Schedule | Purpose |
|-----|----------|---------|
| Refresh token purge | daily at 03:30 | Deletes refresh tokens that expired more than 7 days ago |
| Payment reconciliation | daily at 02:00 | Compares the previous day's payments with Stripe |
| Pickup and return reminders | daily at 08:00 and 09:00 | Publishes the reminder events |
| Late-return detection | every 15 minutes | Flags late rentals and publishes warnings |
| Request expiry | `rental.request-expiry-cron` | Cancels stale `REQUESTED` rentals |
| Unpaid rental expiry | `rental.payment-expiry-cron` | Cancels unpaid `CONFIRMED` rentals at or after the approval-based payment deadline, after checking payment state |
| No-show release | `rental.no-show-cron` | Cancels `CONFIRMED` rentals still holding an authorization `rental.no-show-days` after the start date and releases the hold |
| Payment repair | `payment.repair.interval-ms` | Applies the gateway state to PENDING and AUTHORIZED payments that missed a webhook |
| Exchange-rate refresh | `currency.refresh.interval` | Refreshes cached rates |
| Dashboard alerts | every 5 minutes | Recomputes dashboard alerts |

Cron expressions use the JVM time zone, which is UTC in the container image. Jobs run in every application instance; there is no distributed lock.

## Logging

Every request carries a correlation id taken from the `X-Correlation-ID` request header (only `[A-Za-z0-9-]{1,64}` is accepted, otherwise one is generated). It is echoed in the response header and as `traceId` in problem details. `LogSafetyTest` and `SecretRedactionTest` guard against secrets in logs. The `prod` profile logs everything at `INFO` (the root logger and `com.akif` are `INFO`, and there is no per-package `DEBUG` entry); `dev`, `test` and the default profile log `com.akif` at `DEBUG`. `LogbackProfileTest` fails when a `DEBUG` or `TRACE` logger sits outside a non-prod `springProfile`. E-mail addresses, typed login names and admin search terms are not logged (`LogSafetyTest` fails on log arguments that mention e-mail, `loginRequest` or `registerRequest`). The `[<user>]` field is filled after bearer authentication and is carried into the e-mail executor threads together with the correlation id.

Production configuration is validated while the environment is prepared, before any bean or Flyway runs (`ProdConfigurationValidator` is an `EnvironmentPostProcessor`), so an invalid `prod` setup fails with a list of problems and leaves the database untouched.
