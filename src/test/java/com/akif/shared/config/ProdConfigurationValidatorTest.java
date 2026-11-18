package com.akif.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
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
                .withProperty("stripe.metadata.environment", "prod")
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
                .withProperty("security.rate-limit.trusted-proxy-hops", "1")
                .withProperty("security.rate-limit.trusted-proxy-cidrs", "172.16.0.0/12,fd00::/8");
    }

    @Test
    @DisplayName("accepts a complete configuration")
    void acceptsCompleteConfiguration() {
        assertThatCode(() -> new ProdConfigurationValidator(validEnvironment())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("fails when the Stripe metadata environment tag is missing")
    void failsWhenStripeEnvironmentTagMissing() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("stripe.metadata.environment", "");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stripe.metadata.environment is missing");
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

    @Test
    @DisplayName("fails when the forwarded-header trust flag is not set explicitly")
    void failsWhenTrustFlagMissing() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("security.rate-limit.trust-forwarded-headers", "");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.rate-limit.trust-forwarded-headers is missing");
    }

    @Test
    @DisplayName("rejects trusted proxy CIDRs that cover every address")
    void failsWhenCidrsTrustEverything() {
        for (String cidrs : new String[] {"0.0.0.0/0", "::/0", "172.16.0.0/12,0.0.0.0/0"}) {
            MockEnvironment environment = validEnvironment();
            environment.withProperty("security.rate-limit.trusted-proxy-cidrs", cidrs);

            assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must not trust every address");
        }
    }

    @Test
    @DisplayName("requires trusted proxy CIDRs when forwarded headers are trusted")
    void failsWhenCidrsMissingWithForwardedTrust() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("security.rate-limit.trusted-proxy-cidrs", "");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.rate-limit.trusted-proxy-cidrs is missing");
    }

    @Test
    @DisplayName("requires trusted proxy CIDRs when a trusted client IP header is configured")
    void failsWhenCidrsMissingWithHeaderTrust() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("security.rate-limit.trust-forwarded-headers", "false");
        environment.withProperty("security.rate-limit.trusted-client-ip-header", "X-Client-IP");
        environment.withProperty("security.rate-limit.trusted-proxy-cidrs", "");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("security.rate-limit.trusted-proxy-cidrs is missing");
    }

    @Test
    @DisplayName("does not require CIDRs when no forwarded header is trusted")
    void cidrsOptionalWithoutTrust() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("security.rate-limit.trust-forwarded-headers", "false");
        environment.withProperty("security.rate-limit.trusted-proxy-cidrs", "");

        assertThatCode(() -> new ProdConfigurationValidator(environment)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects malformed trusted proxy CIDRs")
    void failsOnMalformedCidrs() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("security.rate-limit.trusted-proxy-cidrs", "10.0.0.0/8,proxy.example.com,10.0.0.1");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must list valid CIDR ranges, found: proxy.example.com")
                .hasMessageContaining("found: 10.0.0.1");
    }

    @Test
    @DisplayName("fails when the trusted proxy hop count is missing or invalid")
    void failsWhenProxyHopsInvalid() {
        MockEnvironment missing = validEnvironment();
        missing.withProperty("security.rate-limit.trusted-proxy-hops", "");
        MockEnvironment zero = validEnvironment();
        zero.withProperty("security.rate-limit.trusted-proxy-hops", "0");
        MockEnvironment text = validEnvironment();
        text.withProperty("security.rate-limit.trusted-proxy-hops", "two");

        assertThatThrownBy(() -> new ProdConfigurationValidator(missing))
                .hasMessageContaining("security.rate-limit.trusted-proxy-hops is missing");
        assertThatThrownBy(() -> new ProdConfigurationValidator(zero))
                .hasMessageContaining("trusted-proxy-hops must be at least 1");
        assertThatThrownBy(() -> new ProdConfigurationValidator(text))
                .hasMessageContaining("trusted-proxy-hops must be an integer");
    }

    @Test
    @DisplayName("reports every problem at once even when properties hold unresolved placeholders")
    void reportsAllProblemsDespiteUnresolvedPlaceholders() {
        MockEnvironment environment = validEnvironment();
        environment.withProperty("jwt.secret", "${PROD_VALIDATOR_UNSET_JWT}");
        environment.withProperty("stripe.api.key", "${PROD_VALIDATOR_UNSET_STRIPE}");
        environment.withProperty("r2.bucket-name", "${PROD_VALIDATOR_UNSET_BUCKET}");
        environment.withProperty("oauth2.state.secret", "short");
        environment.withProperty("spring.datasource.password", "");

        assertThatThrownBy(() -> new ProdConfigurationValidator(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret references an unset environment variable")
                .hasMessageContaining("stripe.api.key references an unset environment variable")
                .hasMessageContaining("r2.bucket-name references an unset environment variable")
                .hasMessageContaining("oauth2.state.secret must be at least 32 bytes")
                .hasMessageContaining("spring.datasource.password is missing")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain("jwt.secret is missing"));
    }
}
