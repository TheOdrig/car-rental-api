package com.akif.shared.security.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RateLimitEndpointClassifier {

    private static final String OAUTH2_CALLBACK_PREFIX = "/api/oauth2/callback/";

    public Optional<RateLimitCategory> classify(String method, String path) {
        if (method == null || path == null) {
            return Optional.empty();
        }
        String normalized = path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

        if ("POST".equalsIgnoreCase(method)) {
            return switch (normalized) {
                case "/api/auth/login" -> Optional.of(RateLimitCategory.LOGIN);
                case "/api/auth/register" -> Optional.of(RateLimitCategory.REGISTER);
                case "/api/auth/refresh" -> Optional.of(RateLimitCategory.REFRESH);
                case "/api/auth/forgot-password", "/api/auth/reset-password" ->
                        Optional.of(RateLimitCategory.PASSWORD_RESET);
                default -> Optional.empty();
            };
        }

        if ("GET".equalsIgnoreCase(method) && normalized.startsWith(OAUTH2_CALLBACK_PREFIX)) {
            return Optional.of(RateLimitCategory.OAUTH2_CALLBACK);
        }

        return Optional.empty();
    }
}
