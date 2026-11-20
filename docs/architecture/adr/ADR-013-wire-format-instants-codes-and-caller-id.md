# ADR-013: Wire format, caller identity and service structure

**Status:** Accepted


## Context

QA against the Next.js client highlighted ambiguities in time values, display labels and how controllers pass caller identity.

## Decision

- Serialize instants as ISO 8601 UTC with a `Z` suffix. Keep rental start and end as calendar dates. V19 converts stored timestamp columns to `timestamptz`, interpreting prior values as UTC. [V19](../../../src/main/resources/db/migration/V19__instants_with_time_zone.sql) · [Rental response](../../../src/main/java/com/akif/rental/api/RentalResponse.java)
- Serialize domain statuses using stable enum codes such as `IN_USE`. Clients own display labels. [Rental status](../../../src/main/java/com/akif/rental/api/RentalStatus.java)
- Build an `AuthenticatedUser` principal from the signed `userId` claim. Controllers such as `RentalController` pass the caller ID to services. Rental, payment and notification facades delegate to collaborators organized by responsibility. [JWT converter](../../../src/main/java/com/akif/shared/security/JwtUserAuthenticationConverter.java) · [Rental controller](../../../src/main/java/com/akif/rental/web/RentalController.java)

## Consequences

Clients parse instants with their UTC offset, treat date-only values as calendar dates, and use codes for comparisons. Controller tests that supply a principal use `AuthenticatedUser`.
