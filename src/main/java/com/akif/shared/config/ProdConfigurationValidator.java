package com.akif.shared.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Profile("prod")
public class ProdConfigurationValidator {

    static final int MIN_SECRET_BYTES = 32;

    private static final String TRUST_KEY = "security.rate-limit.trust-forwarded-headers";
    private static final String HOPS_KEY = "security.rate-limit.trusted-proxy-hops";

    private static final List<String> REQUIRED_SECRETS = List.of(
            "jwt.secret",
            "oauth2.state.secret");

    private static final List<String> REQUIRED_VALUES = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "cors.allowed-origins",
            "stripe.api.key",
            "stripe.webhook.secret",
            "r2.endpoint",
            "r2.bucket-name",
            "r2.access-key-id",
            "r2.secret-access-key",
            "email.from",
            "spring.mail.password",
            "exchange-rate.api.key",
            "oauth2.google.client-id",
            "oauth2.google.client-secret",
            "oauth2.github.client-id",
            "oauth2.github.client-secret");

    private static final List<String> WEAK_MARKERS = List.of(
            "changeme", "change-me", "placeholder", "your-", "dev-only", "secret-key-here");

    public ProdConfigurationValidator(Environment environment) {
        validate(environment);
    }

    static void validate(Environment environment) {
        List<String> problems = new ArrayList<>();

        for (String key : REQUIRED_SECRETS) {
            String value = environment.getProperty(key);
            if (isBlank(value)) {
                problems.add(key + " is missing");
            } else if (value.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                problems.add(key + " must be at least " + MIN_SECRET_BYTES + " bytes");
            } else if (looksWeak(value)) {
                problems.add(key + " looks like a placeholder");
            }
        }

        for (String key : REQUIRED_VALUES) {
            if (isBlank(environment.getProperty(key))) {
                problems.add(key + " is missing");
            }
        }

        String stripeKey = environment.getProperty("stripe.api.key");
        if (!isBlank(stripeKey) && (!stripeKey.startsWith("sk_") || looksWeak(stripeKey))) {
            problems.add("stripe.api.key must be a real Stripe secret key");
        }
        String webhookSecret = environment.getProperty("stripe.webhook.secret");
        if (!isBlank(webhookSecret) && (!webhookSecret.startsWith("whsec_") || looksWeak(webhookSecret))) {
            problems.add("stripe.webhook.secret must be a real Stripe signing secret");
        }

        String origins = environment.getProperty("cors.allowed-origins");
        if (!isBlank(origins)) {
            for (String origin : origins.split(",")) {
                String trimmed = origin.trim();
                if (trimmed.contains("*") || !trimmed.startsWith("https://")) {
                    problems.add("cors.allowed-origins must list explicit https origins, found: " + trimmed);
                }
            }
        }

        validateProxySettings(environment, problems);

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid production configuration: " + String.join("; ", problems));
        }
    }

    private static void validateProxySettings(Environment environment, List<String> problems) {
        String trust = environment.getProperty(TRUST_KEY);
        if (isBlank(trust)) {
            problems.add(TRUST_KEY + " is missing; set TRUST_FORWARDED_HEADERS to true or false explicitly");
        } else if (!"true".equalsIgnoreCase(trust.trim()) && !"false".equalsIgnoreCase(trust.trim())) {
            problems.add(TRUST_KEY + " must be true or false");
        }

        String hops = environment.getProperty(HOPS_KEY);
        if (isBlank(hops)) {
            problems.add(HOPS_KEY + " is missing; set TRUSTED_PROXY_HOPS to the number of proxies in front of the app");
        } else {
            try {
                if (Integer.parseInt(hops.trim()) < 1) {
                    problems.add(HOPS_KEY + " must be at least 1");
                }
            } catch (NumberFormatException e) {
                problems.add(HOPS_KEY + " must be an integer");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean looksWeak(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return WEAK_MARKERS.stream().anyMatch(lower::contains);
    }
}
