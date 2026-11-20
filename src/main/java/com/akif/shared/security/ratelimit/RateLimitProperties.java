package com.akif.shared.security.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "security.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("false") boolean trustForwardedHeaders,
        @DefaultValue("1") @Min(1) int trustedProxyHops,
        @DefaultValue("") String trustedClientIpHeader,
        List<String> trustedProxyCidrs,
        @DefaultValue("100000") @Min(1) int maxTrackedClients,
        Map<RateLimitCategory, @Valid @NotNull Limit> limits) {

    public RateLimitProperties {
        trustedClientIpHeader = trustedClientIpHeader == null ? "" : trustedClientIpHeader;
        trustedProxyCidrs = trustedProxyCidrs == null ? List.of() : List.copyOf(trustedProxyCidrs);
        limits = limits == null ? Map.of() : Map.copyOf(limits);
    }

    public Limit limitFor(RateLimitCategory category) {
        Limit configured = limits.get(category);
        return configured != null ? configured : defaultLimit(category);
    }

    private static Limit defaultLimit(RateLimitCategory category) {
        return switch (category) {
            case LOGIN -> new Limit(10, 60);
            case REGISTER -> new Limit(5, 600);
            case REFRESH -> new Limit(30, 60);
            case PASSWORD_RESET -> new Limit(5, 900);
            case OAUTH2_CALLBACK -> new Limit(20, 60);
            case RENTAL_REQUEST -> new Limit(10, 600);
            case PAYMENT_INTENT -> new Limit(10, 60);
            case AVAILABILITY_SEARCH -> new Limit(60, 60);
            case LOGIN_USERNAME -> new Limit(50, 900);
            case LOGIN_USERNAME_IP -> new Limit(5, 300);
            case PASSWORD_RESET_USERNAME -> new Limit(20, 900);
            case PASSWORD_RESET_EMAIL_IP -> new Limit(3, 900);
        };
    }

    public record Limit(@Min(1) int capacity, @Min(1) int periodSeconds) {
    }
}
