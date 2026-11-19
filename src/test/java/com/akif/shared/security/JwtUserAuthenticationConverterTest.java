package com.akif.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Bearer token to caller identity")
class JwtUserAuthenticationConverterTest {

    private final JwtUserAuthenticationConverter converter = new JwtUserAuthenticationConverter();

    private Jwt.Builder token() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("akif")
                .issuedAt(Instant.parse("2031-03-01T08:00:00Z"))
                .expiresAt(Instant.parse("2031-03-01T08:15:00Z"))
                .claim(JwtTokenService.ROLES_CLAIM, List.of("USER", "ADMIN"));
    }

    @Test
    @DisplayName("the principal carries the signed user id, username and roles")
    void principalCarriesTheUserId() {
        UsernamePasswordAuthenticationToken authentication =
                converter.convert(token().claim(JwtTokenService.USER_ID_CLAIM, 42L).build());

        assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(AuthenticatedUser.class, user -> {
            assertThat(user.id()).isEqualTo(42L);
            assertThat(user.getUsername()).isEqualTo("akif");
        });
        assertThat(authentication.getAuthorities()).extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("a token without a user id claim is rejected")
    void tokenWithoutUserIdIsRejected() {
        assertThatThrownBy(() -> converter.convert(token().build()))
                .isInstanceOf(InvalidBearerTokenException.class);
    }
}
