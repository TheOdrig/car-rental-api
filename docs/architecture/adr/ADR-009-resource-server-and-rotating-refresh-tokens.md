# ADR-009: Resource-server JWT access tokens and rotating opaque refresh tokens

**Status:** Accepted


## Context

The API needs signed access credentials and a way to rotate and revoke refresh credentials per session.

## Decision

Spring Security's resource server validates HS256 JWTs with the application clock and requires the access-token marker. Refresh tokens are opaque. The database stores their hashes and the service rotates them within a token family. Reuse of an already-rotated token is rejected. Within the configured grace window the family remains usable, while reuse outside that window revokes it. [JWT configuration](../../../src/main/java/com/akif/shared/security/JwtConfig.java) · [Refresh token service](../../../src/main/java/com/akif/auth/internal/service/RefreshTokenService.java) · [Authentication](../../security/AUTHENTICATION.md)

## Consequences

Bearer tokens remain valid until expiry unless another enforced access rule denies the request. The OAuth2 flow uses signed, time-limited state. The default token lifetimes and grace setting are in [application properties](../../../src/main/resources/application.properties).

History. ADR-009 superseded the original authentication strategy recorded in ADR-008.
