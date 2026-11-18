package com.akif.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Production configuration validator")
class ProdConfigurationValidatorTest {

    private static final String STRONG_SECRET = "k7Hq2xVn9Zp4Lm8Rt3Wd6Yb1Cs5Fg0Je";

    private MockEnvironment validEnvironment() {
        return new MockEnvironment()
                .withProperty("jwt.secret", STRONG_SECRET)
                .withProperty("oauth2.state.secret", "Qw3rTy7uIo9pAs2dFg5hJk8lZx1cVb4n")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/car_rental")
                .withProperty("spring.datasource.username", "app")
                .withProperty("spring.datasource.password", "db-password")
                .withProperty("cors.allowed-origins", "https://rent.example.com")
                .withProperty("stripe.api.key", "sk_test_51Habcdefghijklmnop")
                .withProperty("stripe.webhook.secret", "whsec_abcdefghijklmnop")
                .withProperty("r2.endpoint", "https://acc.r2.cloudflarestorage.com")
                .withProperty("r2.bucket-name", "bucket")
                .withProperty("r2.access-key-id", "id")
                .withProperty("r2.secret-access-key", "key")
                .withProperty("email.from", "noreply@example.com")
                .withProperty("spring.mail.password", "mail-key")
                .withProperty("exchange-rate.api.key", "rate-key")
                .withProperty("oauth2.google.client-id", "g-id")
                .withProperty("oauth2.google.client-secret", "g-secret")
                .withProperty("oauth2.github.client-id", "gh-id")
                .withProperty("oauth2.github.client-secret", "gh-secret");
    }

    @Test
    @DisplayName("accepts a complete configuration")
    void acceptsCompleteConfiguration() {
        assertThatCode(() -> new ProdConfigurationValidator(validEnvironment())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("fails when the JWT secret is missing")
    void failsWhenJwtSecretMissing() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("jwt.secret", "");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret is missing");
    }

    @Test
    @DisplayName("fails when the JWT secret is shorter than 32 bytes")
    void failsWhenJwtSecretTooShort() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("jwt.secret", "short-secret");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret must be at least 32 bytes");
    }

    @Test
    @DisplayName("fails when a secret still looks like a placeholder")
    void failsWhenSecretLooksLikePlaceholder() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("oauth2.state.secret", "your-secure-state-secret-minimum-32-chars");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("oauth2.state.secret looks like a placeholder");
    }

    @Test
    @DisplayName("fails when the Stripe key is a placeholder")
    void failsWhenStripeKeyIsPlaceholder() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("stripe.api.key", "sk_test_placeholder");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stripe.api.key");
    }

    @Test
    @DisplayName("fails when CORS allows a wildcard or a non-https origin")
    void failsWhenCorsOriginIsUnsafe() {
        MockEnvironment wildcard = validEnvironment();
        wildcard.withProperty("cors.allowed-origins", "*");
        MockEnvironment insecure = validEnvironment();
        insecure.withProperty("cors.allowed-origins", "http://rent.example.com");

        assertThatThrownBy(() -> new ProdConfigurationValidator(wildcard))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cors.allowed-origins");
        assertThatThrownBy(() -> new ProdConfigurationValidator(insecure))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cors.allowed-origins");
    }

    @Test
    @DisplayName("fails when the database password is absent")
    void failsWhenDatabasePasswordMissing() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("spring.datasource.password", "");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.password is missing");
    }
}
