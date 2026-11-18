package com.akif.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Production Stripe key validation (test mode only)")
class StripeKeyValidationTest {

    private MockEnvironment environment() {
        return new MockEnvironment()
                .withProperty("jwt.secret", "k7Hq2xVn9Zp4Lm8Rt3Wd6Yb1Cs5Fg0Je")
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
                .withProperty("oauth2.github.client-secret", "gh-secret")
                .withProperty("security.rate-limit.trust-forwarded-headers", "true")
                .withProperty("security.rate-limit.trusted-proxy-hops", "1");
    }

    @Test
    @DisplayName("rejects a live Stripe secret key")
    void rejectsLiveSecretKey() {
        MockEnvironment environment = environment().withProperty("stripe.api.key", "sk_live_51Habcdefghijklmnop");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("live keys are rejected");
    }

    @Test
    @DisplayName("rejects a live restricted Stripe key")
    void rejectsLiveRestrictedKey() {
        MockEnvironment environment = environment().withProperty("stripe.api.key", "rk_live_51Habcdefghijklmnop");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .hasMessageContaining("live keys are rejected");
    }

    @Test
    @DisplayName("rejects a live publishable key")
    void rejectsLivePublishableKey() {
        MockEnvironment environment = environment().withProperty("stripe.publishable-key", "pk_live_51Habcdefghijklmnop");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .hasMessageContaining("stripe.publishable-key must be a Stripe test-mode key");
    }

    @Test
    @DisplayName("accepts a test publishable key and treats it as optional")
    void acceptsTestPublishableKey() {
        MockEnvironment withKey = environment().withProperty("stripe.publishable-key", "pk_test_51Habcdefghijklmnop");

        assertThatCode(() -> new ProdConfigurationValidator(withKey)).doesNotThrowAnyException();
        assertThatCode(() -> new ProdConfigurationValidator(environment())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects a publishable key that is not a test publishable key")
    void rejectsNonTestPublishableKey() {
        MockEnvironment environment = environment().withProperty("stripe.publishable-key", "sk_test_51Habcdefghijklmnop");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .hasMessageContaining("stripe.publishable-key");
    }

    @Test
    @DisplayName("fails at startup when the Stripe key is missing")
    void failsWhenStripeKeyMissing() {
        MockEnvironment environment = environment().withProperty("stripe.api.key", "");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .hasMessageContaining("stripe.api.key is missing");
    }

    @Test
    @DisplayName("fails at startup when the webhook signing secret is missing")
    void failsWhenWebhookSecretMissing() {
        MockEnvironment environment = environment().withProperty("stripe.webhook.secret", "");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .hasMessageContaining("stripe.webhook.secret is missing");
    }

    @Test
    @DisplayName("rejects a Stripe key that does not start with sk_test_")
    void rejectsUnknownKeyPrefix() {
        MockEnvironment environment = environment().withProperty("stripe.api.key", "abc_51Habcdefghijklmnop");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .hasMessageContaining("test-mode secret key");
    }

    @Test
    @DisplayName("rejects a webhook secret that is not a Stripe signing secret")
    void rejectsBadWebhookSecret() {
        MockEnvironment environment = environment().withProperty("stripe.webhook.secret", "not-a-signing-secret");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .hasMessageContaining("stripe.webhook.secret must be a real Stripe signing secret");
    }
}
