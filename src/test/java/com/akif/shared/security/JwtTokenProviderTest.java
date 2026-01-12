package com.akif.shared.security;

import com.akif.shared.security.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Unit Tests")
public class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private Authentication authentication;
    @SuppressWarnings("FieldCanBeLocal")
    private final String testSecret = "testSecretKeyForTestingPurposesOnlyDoNotUseInProduction1234567890";
    private final long testAccessTokenExpiration = 900000L;
    @SuppressWarnings("FieldCanBeLocal")
    private final long testRefreshTokenExpiration = 604800000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", testAccessTokenExpiration);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", testRefreshTokenExpiration);

        authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password",
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate valid access token")
        void shouldGenerateValidAccessToken() {
            String token = jwtTokenProvider.generateAccessToken(authentication, 1L);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);

            boolean isValid = jwtTokenProvider.validateToken(token);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should generate valid refresh token")
        void shouldGenerateValidRefreshToken() {
            String token = jwtTokenProvider.generateRefreshToken(authentication);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);

            boolean isValid = jwtTokenProvider.validateToken(token);
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should generate different tokens for same authentication")
        void shouldGenerateDifferentTokensForSameAuthentication() {
            String accessToken1 = jwtTokenProvider.generateAccessToken(authentication, 1L);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String accessToken2 = jwtTokenProvider.generateAccessToken(authentication, 1L);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            assertThat(accessToken1).isNotEqualTo(accessToken2);
            assertThat(accessToken1).isNotEqualTo(refreshToken);
            assertThat(accessToken2).isNotEqualTo(refreshToken);
        }

        @Test
        @DisplayName("Should extract correct username from generated token")
        void shouldExtractCorrectUsernameFromGeneratedToken() {
            String token = jwtTokenProvider.generateAccessToken(authentication, 1L);

            String username = jwtTokenProvider.getUsernameFromToken(token);

            assertThat(username).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate valid token")
        void shouldValidateValidToken() {
            String token = jwtTokenProvider.generateAccessToken(authentication, 1L);

            boolean isValid = jwtTokenProvider.validateToken(token);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid token")
        void shouldRejectInvalidToken() {
            String invalidToken = "invalid.token.here";

            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject null token")
        void shouldRejectNullToken() {
            boolean isValid = jwtTokenProvider.validateToken(null);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject empty token")
        void shouldRejectEmptyToken() {
            boolean isValid = jwtTokenProvider.validateToken("");

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject malformed token")
        void shouldRejectMalformedToken() {
            String malformedToken = "not.a.valid.jwt.token.with.too.many.parts";

            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Expiration Tests")
    class TokenExpirationTests {

        @Test
        @DisplayName("Should detect expired token")
        void shouldDetectExpiredToken() {

            ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 1L);
            String token = jwtTokenProvider.generateAccessToken(authentication, 1L);
            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            boolean isExpired = jwtTokenProvider.isTokenExpired(token);

            assertThat(isExpired).isTrue();
        }

        @Test
        @DisplayName("Should detect non-expired token")
        void shouldDetectNonExpiredToken() {
            String token = jwtTokenProvider.generateAccessToken(authentication, 1L);

            boolean isExpired = jwtTokenProvider.isTokenExpired(token);

            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("Should return true for invalid token when checking expiration")
        void shouldReturnTrueForInvalidTokenWhenCheckingExpiration() {
            String invalidToken = "invalid.token";

            boolean isExpired = jwtTokenProvider.isTokenExpired(invalidToken);

            assertThat(isExpired).isTrue();
        }

        @Test
        @DisplayName("Should get correct expiration time")
        void shouldGetCorrectExpirationTime() {
            String token = jwtTokenProvider.generateAccessToken(authentication, 1L);
            long currentTime = System.currentTimeMillis();

            long expirationTime = jwtTokenProvider.getExpirationTime(token);

            assertThat(expirationTime).isGreaterThan(currentTime);
            assertThat(expirationTime).isLessThanOrEqualTo(currentTime + testAccessTokenExpiration + 1000);
        }
    }

    @Nested
    @DisplayName("Username Extraction Tests")
    class UsernameExtractionTests {

        @Test
        @DisplayName("Should extract username from valid token")
        void shouldExtractUsernameFromValidToken() {
            String token = jwtTokenProvider.generateAccessToken(authentication, 1L);

            String username = jwtTokenProvider.getUsernameFromToken(token);

            assertThat(username).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should throw exception for invalid token when extracting username")
        void shouldThrowExceptionForInvalidTokenWhenExtractingUsername() {
            String invalidToken = "invalid.token";

            assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(invalidToken))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("Should throw exception for null token when extracting username")
        void shouldThrowExceptionForNullTokenWhenExtractingUsername() {

            assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle null authentication")
        void shouldHandleNullAuthentication() {

            assertThatThrownBy(() -> jwtTokenProvider.generateAccessToken(null, 1L))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle authentication with null name")
        void shouldHandleAuthenticationWithNullName() {
            Authentication authWithNullName = new UsernamePasswordAuthenticationToken(null, "password");

            String token = jwtTokenProvider.generateAccessToken(authWithNullName, 1L);

            assertThat(token).isNotNull();
            assertThat(jwtTokenProvider.getUsernameFromToken(token)).isNull();
        }

        @Test
        @DisplayName("Should handle very long username")
        void shouldHandleVeryLongUsername() {
            String longUsername = "a".repeat(1000);
            Authentication longUsernameAuth = new UsernamePasswordAuthenticationToken(longUsername, "password");

            String token = jwtTokenProvider.generateAccessToken(longUsernameAuth, 1L);

            assertThat(token).isNotNull();
            assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(longUsername);
        }
    }
}
