# ADR-012: Request bounds and query design

**Status:** Accepted


## Context

Catalogue and admin queries need bounded request inputs and stable result ordering. Public catalogue responses expose only the fields needed for browsing.

## Decision

- Validate paging for handlers using `Pageable`. Negative pages, malformed values and size outside 1 through 100 return 400 `INVALID_PARAMETER`. Sort fields must appear in the endpoint's allowlist. [Paging interceptor](../../../src/main/java/com/akif/shared/web/PageableGuardInterceptor.java)
- Bound JSON request bodies to 1 MiB by default. Oversized JSON receives 413. Multipart uploads have separate limits. [Request body filter](../../../src/main/java/com/akif/shared/security/RequestBodyLimitFilter.java)
- Return VINs on admin fleet reads and omit them from public car responses. Batch availability pricing avoids a separate pricing query for each result. The availability and similar-car routes share a [rate limit](../../api/RATE_LIMITING.md#endpoint-limits).
- Migration V18 removes indexes assessed as redundant or unused at the time. The migration records the schema change. [V18](../../../src/main/resources/db/migration/V18__drop_unused_indexes.sql)

## Consequences

Clients must stay within the documented bounds and use admin routes for VINs. Caller identity is passed into services using the signed user ID described in [ADR-013](ADR-013-wire-format-instants-codes-and-caller-id.md).
