package com.akif.shared.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class RateLimitEndpointClassifier {

    private final Map<RequestMatcher, RateLimitCategory> rules = new LinkedHashMap<>();

    public RateLimitEndpointClassifier() {
        PathPatternRequestMatcher.Builder matchers = PathPatternRequestMatcher.withDefaults();
        add(matchers, HttpMethod.POST, "/api/auth/login", RateLimitCategory.LOGIN);
        add(matchers, HttpMethod.POST, "/api/auth/register", RateLimitCategory.REGISTER);
        add(matchers, HttpMethod.POST, "/api/auth/refresh", RateLimitCategory.REFRESH);
        add(matchers, HttpMethod.POST, "/api/auth/forgot-password", RateLimitCategory.PASSWORD_RESET);
        add(matchers, HttpMethod.POST, "/api/auth/reset-password", RateLimitCategory.PASSWORD_RESET);
        add(matchers, HttpMethod.GET, "/api/oauth2/callback/**", RateLimitCategory.OAUTH2_CALLBACK);
    }

    private void add(PathPatternRequestMatcher.Builder matchers, HttpMethod method, String pattern,
                     RateLimitCategory category) {
        rules.put(matchers.matcher(method, pattern), category);
        if (!pattern.endsWith("/**")) {
            rules.put(matchers.matcher(method, pattern + "/"), category);
        }
    }

    public Optional<RateLimitCategory> classify(HttpServletRequest request) {
        for (Map.Entry<RequestMatcher, RateLimitCategory> rule : rules.entrySet()) {
            if (rule.getKey().matches(request)) {
                return Optional.of(rule.getValue());
            }
        }
        return Optional.empty();
    }
}
