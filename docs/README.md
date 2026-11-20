# Car Rental API documentation

This portfolio backend runs locally. The Stripe integration supports test mode.

## Architecture and decisions

- [Developer guide](architecture/DEVELOPER_GUIDE.md) maps module responsibilities, dependencies and extension points.
- [Architecture diagrams](architecture/DIAGRAMS.md) show rental states, the payment flow and Stripe webhook processing.

| Number | Title | Status |
| --- | --- | --- |
| 001 | [Spring Modulith over Maven multi-module](architecture/adr/ADR-001-spring-modulith-over-maven-multimodule.md) | Accepted |
| 002 | [Cross-module entity references](architecture/adr/ADR-002-cross-module-entity-strategy.md) | Accepted |
| 003 | [Event-driven inter-module communication](architecture/adr/ADR-003-event-driven-communication.md) | Accepted |
| 006 | [Dynamic pricing](architecture/adr/ADR-006-dynamic-pricing-strategy.md) | Accepted |
| 007 | [Late-return penalties](architecture/adr/ADR-007-late-return-penalty.md) | Accepted |
| 009 | [Resource-server JWT access tokens and rotating opaque refresh tokens](architecture/adr/ADR-009-resource-server-and-rotating-refresh-tokens.md) | Accepted |
| 010 | [Booking integrity and fleet status](architecture/adr/ADR-010-booking-integrity-and-fleet-status.md) | Accepted |
| 011 | [Stripe manual-capture PaymentIntents](architecture/adr/ADR-011-stripe-manual-capture-payment-intents.md) | Accepted |
| 012 | [Request bounds and query design](architecture/adr/ADR-012-request-bounds-and-query-hygiene.md) | Accepted |
| 013 | [Wire format, caller identity and service structure](architecture/adr/ADR-013-wire-format-instants-codes-and-caller-id.md) | Accepted |
| 014 | [Payment deadline after approval](architecture/adr/ADR-014-payment-deadline-after-approval.md) | Accepted |

History. ADR-004 was merged into 001, ADR-005 into 011, and ADR-008 was superseded by 009.

## API

- [API conventions](api/API_CONVENTIONS.md) explains access levels, payloads, paging and dates.
- [Error codes](api/ERROR_CODES.md) maps problem codes to HTTP statuses and meanings.
- [Rate limiting](api/RATE_LIMITING.md) lists buckets, client identity rules and retry behavior.

## Security

- [Authentication and authorization](security/AUTHENTICATION.md) covers tokens, OAuth2, ownership and webhook verification.
- [Security policy](../SECURITY.md) explains how to report a vulnerability privately.

## Testing

- [Test strategy](testing/TEST_STRATEGY.md) describes test layers, infrastructure and local verification.

## Operations and configuration

- [Configuration](CONFIGURATION.md) lists profiles, settings and scheduled jobs.
- [Contributing](../CONTRIBUTING.md) covers local setup and change verification.
- [Changelog](../CHANGELOG.md) records breaking changes and project history.
