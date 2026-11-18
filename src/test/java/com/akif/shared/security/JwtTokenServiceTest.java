package com.akif.shared.security;

import com.akif.shared.exception.InvalidTokenException;
import com.akif.shared.exception.TokenExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenService")
class JwtTokenServiceTest {

    private static final String SECRET = "k7Hq2xVn9Zp4Lm8Rt3Wd6Yb1Cs5Fg0JeXx";
    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    private JwtProperties properties;
    private Clock clock;
    private JwtConfig config;
    private SecretKey key;
    private JwtTokenService service;
    private JwtDecoder accessDecoder;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties(SECRET, 900_000L, 604_800_000L, "car-rental-api");
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        config = new JwtConfig();
        key = config.jwtSigningKey(properties);
        JwtEncoder encoder = config.jwtEncoder(key);
        service = new JwtTokenService(encoder, config.refreshJwtDecoder(key, properties, clock), properties, clock);
        accessDecoder = config.jwtDecoder(key, properties, clock);
        authentication = new UsernamePasswordAuthenticationToken("alice", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("access token carries subject, roles, user id, token use and expiry")
    void accessTokenClaims() {
        Jwt jwt = accessDecoder.decode(service.generateAccessToken(authentication, 42L));

        assertThat(jwt.getSubject()).isEqualTo("alice");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactlyInAnyOrder("USER", "ADMIN");
        assertThat(((Number) jwt.getClaim("userId")).longValue()).isEqualTo(42L);
        assertThat(jwt.getClaimAsString("token_use")).isEqualTo("access");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("car-rental-api");
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    @DisplayName("a refresh token is rejected when used as an access token")
    void refreshTokenRejectedAsAccessToken() {
        String refreshToken = service.generateRefreshToken(authentication);

        assertThatThrownBy(() -> accessDecoder.decode(refreshToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("not an access token");
    }

    @Test
    @DisplayName("an access token is rejected when used as a refresh token")
    void accessTokenRejectedAsRefreshToken() {
        String accessToken = service.generateAccessToken(authentication, 42L);

        assertThatThrownBy(() -> service.usernameFromRefreshToken(accessToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("a refresh token resolves to its subject")
    void refreshTokenResolvesSubject() {
        assertThat(service.usernameFromRefreshToken(service.generateRefreshToken(authentication)))
                .isEqualTo("alice");
    }

    @Test
    @DisplayName("an expired access token is rejected")
    void expiredAccessTokenRejected() {
        String token = service.generateAccessToken(authentication, 42L);
        JwtDecoder later = config.jwtDecoder(key, properties,
                Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

        assertThatThrownBy(() -> later.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("an expired refresh token raises TokenExpiredException")
    void expiredRefreshTokenRaisesExpired() {
        String token = service.generateRefreshToken(authentication);
        JwtTokenService later = new JwtTokenService(config.jwtEncoder(key),
                config.refreshJwtDecoder(key, properties, Clock.fixed(NOW.plus(Duration.ofDays(8)), ZoneOffset.UTC)),
                properties, clock);

        assertThatThrownBy(() -> later.usernameFromRefreshToken(token)).isInstanceOf(TokenExpiredException.class);
    }

    @Test
    @DisplayName("a token signed with another key is rejected")
    void foreignSignatureRejected() {
        JwtProperties other = new JwtProperties("Zq9Wx8Ec7Rv6Tb5Yn4Um3Ij2Ok1Pl0AaSs", 900_000L, 604_800_000L,
                "car-rental-api");
        JwtConfig otherConfig = new JwtConfig();
        JwtTokenService foreign = new JwtTokenService(otherConfig.jwtEncoder(otherConfig.jwtSigningKey(other)),
                config.refreshJwtDecoder(key, properties, clock), other, clock);

        String forged = foreign.generateAccessToken(authentication, 1L);

        assertThatThrownBy(() -> accessDecoder.decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("an unsigned token is rejected")
    void unsignedTokenRejected() {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(
                ("{\"sub\":\"alice\",\"iss\":\"car-rental-api\",\"token_use\":\"access\",\"exp\":"
                        + NOW.plusSeconds(600).getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> accessDecoder.decode(header + "." + payload + "."))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a token from another issuer is rejected")
    void foreignIssuerRejected() {
        JwtProperties otherIssuer = new JwtProperties(SECRET, 900_000L, 604_800_000L, "someone-else");
        JwtTokenService foreign = new JwtTokenService(config.jwtEncoder(key),
                config.refreshJwtDecoder(key, properties, clock), otherIssuer, clock);

        String token = foreign.generateAccessToken(authentication, 1L);

        assertThatThrownBy(() -> accessDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a signing secret shorter than 32 bytes is refused")
    void shortSecretRefused() {
        JwtProperties weak = new JwtProperties("too-short", 900_000L, 604_800_000L, "car-rental-api");

        assertThatThrownBy(() -> config.jwtSigningKey(weak))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
