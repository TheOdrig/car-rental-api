package com.akif.shared.security;

import com.akif.shared.exception.InvalidTokenException;
import com.akif.shared.exception.TokenExpiredException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class JwtTokenService {

    public static final String ROLES_CLAIM = "roles";
    public static final String USER_ID_CLAIM = "userId";

    private final JwtEncoder encoder;
    private final JwtDecoder refreshDecoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder encoder,
                           @Qualifier("refreshJwtDecoder") JwtDecoder refreshDecoder,
                           JwtProperties properties,
                           Clock clock) {
        this.encoder = encoder;
        this.refreshDecoder = refreshDecoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String generateAccessToken(Authentication authentication, Long userId) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring("ROLE_".length()) : authority)
                .toList();

        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl()))
                .claim(TokenUse.CLAIM, TokenUse.ACCESS.claimValue())
                .claim(USER_ID_CLAIM, userId)
                .claim(ROLES_CLAIM, roles)
                .build();
        return encode(claims);
    }

    public String generateRefreshToken(Authentication authentication) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMillis(properties.refreshTokenExpiration())))
                .claim(TokenUse.CLAIM, TokenUse.REFRESH.claimValue())
                .build();
        return encode(claims);
    }

    public String usernameFromRefreshToken(String token) {
        try {
            return refreshDecoder.decode(token).getSubject();
        } catch (JwtValidationException e) {
            boolean expired = e.getErrors().stream()
                    .anyMatch(error -> error.getDescription() != null && error.getDescription().contains("expired"));
            if (expired) {
                throw new TokenExpiredException("Refresh token has expired");
            }
            throw new InvalidTokenException("Invalid refresh token");
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid refresh token");
        }
    }

    public Duration accessTokenTtl() {
        return Duration.ofMillis(properties.accessTokenExpiration());
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        Jwt jwt = encoder.encode(JwtEncoderParameters.from(header, claims));
        return jwt.getTokenValue();
    }
}
