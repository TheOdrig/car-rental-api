# Rate limiting

How the Car Rental API throttles abusive or expensive requests, how it decides who a client is, and what a throttled client sees.

## How it works

`RateLimitFilter` runs before bearer-token authentication. It asks `RateLimitEndpointClassifier` whether the request matches a rule. A matching request consumes one token from a Bucket4j bucket keyed by category and client address; an empty bucket answers 429. Requests that match no rule are not limited, and the filter does nothing when `security.rate-limit.enabled=false` (the `test` profile sets this).

Buckets live in an in-memory Caffeine cache: at most `security.rate-limit.max-tracked-clients` entries (default 100000), each evicted one hour after its last access. Limits are therefore per application instance and reset on restart. Bucket time comes from the application `Clock`.

Paths are matched after decoding and normalization, with and without a trailing slash, so an encoded path such as `/api/v1/auth/l%6Fgin` is throttled like `/api/v1/auth/login`.

## Endpoint limits

Each limit is a token bucket of `capacity` tokens that refills greedily over `period-seconds`. Defaults are set in `application.properties`. Override a category with `security.rate-limit.limits.<category>.capacity` and `.period-seconds`, using the kebab-case category name (for example `login` or `password-reset`).

| Category | Applies to | Capacity | Period (s) | Keyed by |
|----------|------------|----------|------------|----------|
| `LOGIN` | `POST /api/v1/auth/login` | 10 | 60 | client address |
| `REGISTER` | `POST /api/v1/auth/register` | 5 | 600 | client address |
| `REFRESH` | `POST /api/v1/auth/refresh` | 30 | 60 | client address |
| `PASSWORD_RESET` | `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password` | 5 | 900 | client address |
| `OAUTH2_CALLBACK` | `GET /api/v1/oauth2/callback/**` | 20 | 60 | client address |
| `RENTAL_REQUEST` | `POST /api/v1/rentals/request` | 10 | 600 | client address |
| `PAYMENT_INTENT` | `POST /api/v1/rentals/*/payment-intent` | 10 | 60 | client address |
| `AVAILABILITY_SEARCH` | `POST /api/v1/cars/availability/search`, `GET /api/v1/cars/{id}/similar` | 60 | 60 | client address, shared across both routes |

All other endpoints, including the Stripe webhook, are not rate limited. A new limited endpoint needs an entry in `RateLimitEndpointClassifier.RULES`; `OpenApiContractTest` checks that every limited operation documents its 429 response.

## Per-account limits

Login and forgot-password have additional buckets that are checked inside the controller. They stop one address from locking a victim out and many addresses from guessing one account.

| Category | Applies to | Capacity | Period (s) | Keyed by | Consumed when |
|----------|------------|----------|------------|----------|---------------|
| `LOGIN_USERNAME_IP` | login | 5 | 300 | account and client address | reserved before the password is checked, returned after a success |
| `LOGIN_USERNAME` | login | 50 | 900 | account, any address | reserved before the password is checked, returned after a success |
| `PASSWORD_RESET_EMAIL_IP` | forgot-password | 3 | 900 | e-mail and client address | every request |
| `PASSWORD_RESET_USERNAME` | forgot-password | 20 | 900 | e-mail, any address | every request |

For login, each attempt reserves one token from both buckets before the credentials are verified (an atomic take, so a burst of parallel requests cannot all slip through) and returns the tokens after a successful login. Once a bucket is empty the request is answered 429 without checking the password. The account key is the user id when the typed username or e-mail resolves to an account, so the username and the e-mail of one account share a bucket; an unknown name is keyed by its lower-cased text. A social-only account answered with a social login hint keeps its reservation like any other failure. The forgot-password response is identical for registered and unknown e-mail addresses. These four categories have no entry in `application.properties`; their defaults are in `RateLimitProperties` and can be overridden like the others.

## Client address

The client key is the address `ClientIpResolver` returns. It resolves in this order:

1. If the socket address (`request.getRemoteAddr()`) is not inside `security.rate-limit.trusted-proxy-cidrs`, it is the client address and no header is read.
2. Otherwise, if `security.rate-limit.trusted-client-ip-header` is set and the request carries that header with a valid IP literal, that value is the client address.
3. Otherwise, if `security.rate-limit.trust-forwarded-headers` is `true`, all `X-Forwarded-For` lines are joined and the entry at position `size - trusted-proxy-hops` is used, provided it is a valid IP literal.
4. Otherwise the socket address is used.

IPv6 clients are bucketed by their /64 prefix, because one client normally controls a whole /64 and could otherwise rotate through addresses to get a fresh bucket each time. IPv4-mapped IPv6 addresses count as the IPv4 address.

| Property | Environment variable | Meaning |
|----------|----------------------|---------|
| `security.rate-limit.trust-forwarded-headers` | `TRUST_FORWARDED_HEADERS` | Honor `X-Forwarded-For`. `false` in `dev`; required, with no default, in `prod`. |
| `security.rate-limit.trusted-proxy-hops` | `TRUSTED_PROXY_HOPS` | Number of trusted proxies that append to `X-Forwarded-For`; at least 1. Required in `prod`. |
| `security.rate-limit.trusted-client-ip-header` | `TRUSTED_CLIENT_IP_HEADER` | Header that carries the end-user address, for example one set by a web front end or an edge network. Optional. |
| `security.rate-limit.trusted-proxy-cidrs` | `TRUSTED_PROXY_CIDRS` | Comma-separated IPv4 and IPv6 CIDR ranges of the proxies allowed to set those headers. Required in `prod` when forwarded headers or a client IP header are trusted. |

In `prod`, startup fails when `TRUSTED_PROXY_CIDRS` is empty or malformed in those cases, or contains a `/0` range (`ProdConfigurationValidator`). See [Configuration](../CONFIGURATION.md#client-address-and-proxies).

If the configured CIDRs omit the real proxy, the resolver uses the socket address. An incorrect hop count can group users into one bucket or select an untrusted entry. Use a trusted proxy that overwrites client-supplied headers and keep trusted CIDRs narrow. The resolver checks the immediate remote against those CIDRs before it uses a forwarded header. [Client address resolver](../../src/main/java/com/akif/shared/security/ratelimit/ClientIpResolver.java)

## The 429 response

An empty bucket answers HTTP 429 with a `Retry-After` header (whole seconds, at least 1) and a problem-details body with `code` `RATE_LIMIT_EXCEEDED`:

```json
{
  "type": "urn:car-rental:problem:rate-limit-exceeded",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Too many requests. Retry after 42 seconds.",
  "instance": "/api/v1/auth/login",
  "code": "RATE_LIMIT_EXCEEDED",
  "timestamp": "2026-11-19T10:15:30.123456Z",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

A limit enforced in the controller (the per-account buckets) returns the same body plus `details.retryAfterSeconds`. The API sends no `X-RateLimit-*` headers.

Clients should wait for `Retry-After` before retrying, should not retry in a tight loop, and should not retry login or register automatically after a 429.

## Tests

`RateLimitServiceTest`, `RateLimitEndpointClassifierTest`, `ClientIpResolverTest` and `RateLimitIntegrationTest` cover the buckets, the classifier, the client address rules and the filter. `ProdConfigurationValidatorTest` covers the production validation.
