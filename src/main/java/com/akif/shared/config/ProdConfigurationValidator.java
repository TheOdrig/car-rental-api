package com.akif.shared.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProdConfigurationValidator implements EnvironmentPostProcessor {

    static final int MIN_SECRET_BYTES = 32;

    private static final String TRUST_KEY = "security.rate-limit.trust-forwarded-headers";
    private static final String CIDRS_KEY = "security.rate-limit.trusted-proxy-cidrs";
    private static final String HEADER_KEY = "security.rate-limit.trusted-client-ip-header";
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
            "stripe.metadata.environment",
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

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            validate(environment);
        }
    }

    static void validate(Environment environment) {
        List<String> problems = new ArrayList<>();
        Lookup lookup = new Lookup(environment, problems);

        for (String key : REQUIRED_SECRETS) {
            String value = lookup.get(key);
            if (isBlank(value)) {
                lookup.reportMissing(key);
            } else if (value.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
                problems.add(key + " must be at least " + MIN_SECRET_BYTES + " bytes");
            } else if (looksWeak(value)) {
                problems.add(key + " looks like a placeholder");
            }
        }

        for (String key : REQUIRED_VALUES) {
            if (isBlank(lookup.get(key))) {
                lookup.reportMissing(key);
            }
        }

        String stripeKey = lookup.get("stripe.api.key");
        if (!isBlank(stripeKey)) {
            if (isLiveKey(stripeKey)) {
                problems.add("stripe.api.key must be a Stripe test-mode key; live keys are rejected");
            } else if (!isTestSecretKey(stripeKey) || looksWeak(stripeKey)) {
                problems.add("stripe.api.key must be a real Stripe test-mode secret key");
            }
        }
        String publishableKey = lookup.get("stripe.publishable-key");
        if (!isBlank(publishableKey)) {
            if (isLiveKey(publishableKey)) {
                problems.add("stripe.publishable-key must be a Stripe test-mode key; live keys are rejected");
            } else if (!publishableKey.startsWith("pk_test_")) {
                problems.add("stripe.publishable-key must be a Stripe test-mode publishable key");
            }
        }
        String webhookSecret = lookup.get("stripe.webhook.secret");
        if (!isBlank(webhookSecret) && (!webhookSecret.startsWith("whsec_") || looksWeak(webhookSecret))) {
            problems.add("stripe.webhook.secret must be a real Stripe signing secret");
        }

        String origins = lookup.get("cors.allowed-origins");
        if (!isBlank(origins)) {
            for (String origin : origins.split(",")) {
                String trimmed = origin.trim();
                if (trimmed.contains("*") || !trimmed.startsWith("https://")) {
                    problems.add("cors.allowed-origins must list explicit https origins, found: " + trimmed);
                }
            }
        }

        validateProxySettings(lookup, problems);

        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid production configuration: " + String.join("; ", problems));
        }
    }

    private static void validateProxySettings(Lookup lookup, List<String> problems) {
        String trust = lookup.get(TRUST_KEY);
        if (isBlank(trust)) {
            lookup.reportMissing(TRUST_KEY, "set TRUST_FORWARDED_HEADERS to true or false explicitly");
        } else if (!"true".equalsIgnoreCase(trust.trim()) && !"false".equalsIgnoreCase(trust.trim())) {
            problems.add(TRUST_KEY + " must be true or false");
        }

        String header = lookup.get(HEADER_KEY);
        boolean trustsProxy = "true".equalsIgnoreCase(trust == null ? "" : trust.trim()) || !isBlank(header);
        if (trustsProxy) {
            String cidrs = lookup.get(CIDRS_KEY);
            if (isBlank(cidrs)) {
                lookup.reportMissing(CIDRS_KEY, "set TRUSTED_PROXY_CIDRS when forwarded headers or a client IP header are trusted");
            } else {
                for (String cidr : cidrs.split(",")) {
                    String trimmed = cidr.trim();
                    if (trimmed.endsWith("/0")) {
                        problems.add(CIDRS_KEY + " must not trust every address, found: " + trimmed);
                    } else if (trimmed.isEmpty() || !isCidr(trimmed)) {
                        problems.add(CIDRS_KEY + " must list valid CIDR ranges, found: " + trimmed);
                    }
                }
            }
        }

        String hops = lookup.get(HOPS_KEY);
        if (isBlank(hops)) {
            lookup.reportMissing(HOPS_KEY, "set TRUSTED_PROXY_HOPS to the number of proxies in front of the app");
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

    private static final class Lookup {

        private final Environment environment;
        private final List<String> problems;
        private final Set<String> unresolved = new HashSet<>();

        private Lookup(Environment environment, List<String> problems) {
            this.environment = environment;
            this.problems = problems;
        }

        private String get(String key) {
            try {
                return environment.getProperty(key);
            } catch (IllegalArgumentException e) {
                if (unresolved.add(key)) {
                    problems.add(key + " references an unset environment variable");
                }
                return null;
            }
        }

        private void reportMissing(String key) {
            if (!unresolved.contains(key)) {
                problems.add(key + " is missing");
            }
        }

        private void reportMissing(String key, String hint) {
            if (!unresolved.contains(key)) {
                problems.add(key + " is missing; " + hint);
            }
        }
    }

    private static boolean isLiveKey(String key) {
        return key.startsWith("sk_live_") || key.startsWith("pk_live_") || key.startsWith("rk_live_");
    }

    private static boolean isTestSecretKey(String key) {
        return key.startsWith("sk_test_") || key.startsWith("rk_test_");
    }

    private static boolean isCidr(String value) {
        if (!value.matches("^[0-9a-fA-F:.]+/\\d{1,3}$")) {
            return false;
        }
        try {
            new org.springframework.security.web.util.matcher.IpAddressMatcher(value);
            return true;
        } catch (RuntimeException e) {
            return false;
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
