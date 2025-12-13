package com.akif.auth.unit;

import com.akif.auth.domain.enums.OAuth2ErrorType;
import com.akif.auth.internal.exceptipn.OAuth2AuthenticationException;
import com.akif.auth.internal.exceptipn.OAuth2ProviderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuth2 Error Handling Tests")
class OAuth2ErrorHandlingTest {

    @Nested
    @DisplayName("Authorization Denied Error Tests")
    class AuthorizationDeniedErrorTests {

        @Test
        @DisplayName("Should create authorization denied error with correct properties")
        void shouldCreateAuthorizationDeniedError() {
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                    OAuth2ErrorType.AUTHORIZATION_DENIED, "google");

            assertThat(exception.getErrorType()).isEqualTo(OAuth2ErrorType.AUTHORIZATION_DENIED);
            assertThat(exception.getProvider()).isEqualTo("google");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(exception.getMessage()).isEqualTo("User denied authorization");
        }

        @Test
        @DisplayName("Authorization denied error code should be 'authorization_denied'")
        void authorizationDeniedErrorCodeShouldBeCorrect() {
            assertThat(OAuth2ErrorType.AUTHORIZATION_DENIED.getCode()).isEqualTo("authorization_denied");
        }
    }

    @Nested
    @DisplayName("Authentication Failed Error Tests")
    class AuthenticationFailedErrorTests {

        @Test
        @DisplayName("Should create authentication failed error via factory method")
        void shouldCreateAuthenticationFailedError() {
            OAuth2ProviderException exception = OAuth2ProviderException.authenticationFailed("github");

            assertThat(exception.getErrorType()).isEqualTo(OAuth2ErrorType.AUTHENTICATION_FAILED);
            assertThat(exception.getProvider()).isEqualTo("github");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(exception.getMessage()).isEqualTo("Authentication failed");
        }

        @Test
        @DisplayName("Authentication failed error code should be 'authentication_failed'")
        void authenticationFailedErrorCodeShouldBeCorrect() {
            assertThat(OAuth2ErrorType.AUTHENTICATION_FAILED.getCode()).isEqualTo("authentication_failed");
        }
    }

    @Nested
    @DisplayName("Invalid Token Error Tests")
    class InvalidTokenErrorTests {

        @Test
        @DisplayName("Should create invalid token error via factory method")
        void shouldCreateInvalidTokenError() {
            OAuth2ProviderException exception = OAuth2ProviderException.invalidToken("google");

            assertThat(exception.getErrorType()).isEqualTo(OAuth2ErrorType.INVALID_TOKEN);
            assertThat(exception.getProvider()).isEqualTo("google");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(exception.getMessage()).isEqualTo("Invalid or expired token");
        }

        @Test
        @DisplayName("Invalid token error code should be 'invalid_token'")
        void invalidTokenErrorCodeShouldBeCorrect() {
            assertThat(OAuth2ErrorType.INVALID_TOKEN.getCode()).isEqualTo("invalid_token");
        }
    }

    @Nested
    @DisplayName("Provider Unavailable Error Tests")
    class ProviderUnavailableErrorTests {

        @Test
        @DisplayName("Should create provider unavailable error via factory method")
        void shouldCreateProviderUnavailableError() {
            OAuth2ProviderException exception = OAuth2ProviderException.providerUnavailable("github");

            assertThat(exception.getErrorType()).isEqualTo(OAuth2ErrorType.PROVIDER_UNAVAILABLE);
            assertThat(exception.getProvider()).isEqualTo("github");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(exception.getMessage()).isEqualTo("Provider is temporarily unavailable");
        }

        @Test
        @DisplayName("Provider unavailable error code should be 'provider_unavailable'")
        void providerUnavailableErrorCodeShouldBeCorrect() {
            assertThat(OAuth2ErrorType.PROVIDER_UNAVAILABLE.getCode()).isEqualTo("provider_unavailable");
        }
    }

    @Nested
    @DisplayName("Email Required Error Tests")
    class EmailRequiredErrorTests {

        @Test
        @DisplayName("Should create email required error via factory method")
        void shouldCreateEmailRequiredError() {
            OAuth2ProviderException exception = OAuth2ProviderException.emailRequired("github");

            assertThat(exception.getErrorType()).isEqualTo(OAuth2ErrorType.EMAIL_REQUIRED);
            assertThat(exception.getProvider()).isEqualTo("github");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getMessage()).isEqualTo("Email is required but not provided");
        }

        @Test
        @DisplayName("Email required error code should be 'email_required'")
        void emailRequiredErrorCodeShouldBeCorrect() {
            assertThat(OAuth2ErrorType.EMAIL_REQUIRED.getCode()).isEqualTo("email_required");
        }
    }

    @Nested
    @DisplayName("Invalid State Error Tests")
    class InvalidStateErrorTests {

        @Test
        @DisplayName("Should create invalid state error with correct properties")
        void shouldCreateInvalidStateError() {
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                    OAuth2ErrorType.INVALID_STATE);

            assertThat(exception.getErrorType()).isEqualTo(OAuth2ErrorType.INVALID_STATE);
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getMessage()).isEqualTo("Invalid state parameter - possible CSRF attack");
        }

        @Test
        @DisplayName("Invalid state error code should be 'invalid_state'")
        void invalidStateErrorCodeShouldBeCorrect() {
            assertThat(OAuth2ErrorType.INVALID_STATE.getCode()).isEqualTo("invalid_state");
        }
    }

    @Nested
    @DisplayName("Account Already Linked Error Tests")
    class AccountAlreadyLinkedErrorTests {

        @Test
        @DisplayName("Should create account already linked error with correct properties")
        void shouldCreateAccountAlreadyLinkedError() {
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                    OAuth2ErrorType.ACCOUNT_ALREADY_LINKED, "google");

            assertThat(exception.getErrorType()).isEqualTo(OAuth2ErrorType.ACCOUNT_ALREADY_LINKED);
            assertThat(exception.getProvider()).isEqualTo("google");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(exception.getMessage()).isEqualTo("This social account is already linked to another user");
        }
    }

    @Nested
    @DisplayName("Social Login Required Error Tests")
    class SocialLoginRequiredErrorTests {

        @Test
        @DisplayName("Should create social login required error with correct properties")
        void shouldCreateSocialLoginRequiredError() {
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                    OAuth2ErrorType.SOCIAL_LOGIN_REQUIRED, "github");

            assertThat(exception.getErrorType()).isEqualTo(OAuth2ErrorType.SOCIAL_LOGIN_REQUIRED);
            assertThat(exception.getProvider()).isEqualTo("github");
            assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(exception.getMessage()).isEqualTo("This account requires social login");
        }
    }

    @Nested
    @DisplayName("Exception with Custom Message Tests")
    class CustomMessageTests {

        @Test
        @DisplayName("Should create exception with custom message")
        void shouldCreateExceptionWithCustomMessage() {
            String customMessage = "Custom error message for testing";
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                    OAuth2ErrorType.AUTHENTICATION_FAILED, "google", customMessage);

            assertThat(exception.getMessage()).isEqualTo(customMessage);
            assertThat(exception.getErrorType()).isEqualTo(OAuth2ErrorType.AUTHENTICATION_FAILED);
            assertThat(exception.getProvider()).isEqualTo("google");
        }
    }

    @Nested
    @DisplayName("Exception with Cause Tests")
    class ExceptionWithCauseTests {

        @Test
        @DisplayName("Should create exception with cause")
        void shouldCreateExceptionWithCause() {
            RuntimeException cause = new RuntimeException("Original error");
            OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                    OAuth2ErrorType.PROVIDER_UNAVAILABLE, "github", cause);

            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getErrorType()).isEqualTo(OAuth2ErrorType.PROVIDER_UNAVAILABLE);
        }
    }

    @Nested
    @DisplayName("HTTP Status Mapping Tests")
    class HttpStatusMappingTests {

        @Test
        @DisplayName("All error types should map to correct HTTP status")
        void allErrorTypesShouldMapToCorrectHttpStatus() {

            assertThat(new OAuth2AuthenticationException(OAuth2ErrorType.AUTHORIZATION_DENIED).getHttpStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(new OAuth2AuthenticationException(OAuth2ErrorType.AUTHENTICATION_FAILED).getHttpStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(new OAuth2AuthenticationException(OAuth2ErrorType.INVALID_TOKEN).getHttpStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(new OAuth2AuthenticationException(OAuth2ErrorType.SOCIAL_LOGIN_REQUIRED).getHttpStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);

            assertThat(new OAuth2AuthenticationException(OAuth2ErrorType.EMAIL_REQUIRED).getHttpStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(new OAuth2AuthenticationException(OAuth2ErrorType.INVALID_STATE).getHttpStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            assertThat(new OAuth2AuthenticationException(OAuth2ErrorType.PROVIDER_UNAVAILABLE).getHttpStatus())
                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

            assertThat(new OAuth2AuthenticationException(OAuth2ErrorType.ACCOUNT_ALREADY_LINKED).getHttpStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
    }
}
