package com.akif.shared.security.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private boolean trustForwardedHeaders = false;
    private int trustedProxyHops = 1;
    private String trustedClientIpHeader = "";
    private int maxTrackedClients = 100_000;
    private Map<RateLimitCategory, Limit> limits = new EnumMap<>(RateLimitCategory.class);

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
        };
    }

    @Getter
    @Setter
    public static class Limit {

        private int capacity;
        private int periodSeconds;

        public Limit() {
        }

        public Limit(int capacity, int periodSeconds) {
            this.capacity = capacity;
            this.periodSeconds = periodSeconds;
        }
    }
}
