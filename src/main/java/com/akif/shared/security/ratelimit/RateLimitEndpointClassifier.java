package com.akif.shared.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class RateLimitEndpointClassifier {

    public record Rule(HttpMethod method, String pattern, RateLimitCategory category) {
    }

    private static final Pattern PATH_VARIABLE = Pattern.compile("[{][^}]+[}]");

    public static final List<Rule> RULES = List.of(
            new Rule(HttpMethod.POST, "/api/v1/auth/login", RateLimitCategory.LOGIN),
            new Rule(HttpMethod.POST, "/api/v1/auth/register", RateLimitCategory.REGISTER),
            new Rule(HttpMethod.POST, "/api/v1/auth/refresh", RateLimitCategory.REFRESH),
            new Rule(HttpMethod.POST, "/api/v1/auth/forgot-password", RateLimitCategory.PASSWORD_RESET),
            new Rule(HttpMethod.POST, "/api/v1/auth/reset-password", RateLimitCategory.PASSWORD_RESET),
            new Rule(HttpMethod.POST, "/api/v1/rentals/request", RateLimitCategory.RENTAL_REQUEST),
            new Rule(HttpMethod.POST, "/api/v1/rentals/*/payment-intent", RateLimitCategory.PAYMENT_INTENT),
            new Rule(HttpMethod.POST, "/api/v1/cars/availability/search", RateLimitCategory.AVAILABILITY_SEARCH),
            new Rule(HttpMethod.GET, "/api/v1/cars/*/similar", RateLimitCategory.AVAILABILITY_SEARCH),
            new Rule(HttpMethod.GET, "/api/v1/oauth2/callback/**", RateLimitCategory.OAUTH2_CALLBACK));

    private final Map<RequestMatcher, RateLimitCategory> rules = new LinkedHashMap<>();

    public RateLimitEndpointClassifier() {
        PathPatternRequestMatcher.Builder matchers = PathPatternRequestMatcher.withDefaults();
        for (Rule rule : RULES) {
            add(matchers, rule.method(), rule.pattern(), rule.category());
        }
    }

    public static Optional<RateLimitCategory> classifyTemplate(String method, String pathTemplate) {
        PathContainer path = PathContainer.parsePath(PATH_VARIABLE.matcher(pathTemplate).replaceAll("1"));
        return RULES.stream()
                .filter(rule -> rule.method().name().equals(method))
                .filter(rule -> PathPatternParser.defaultInstance.parse(rule.pattern()).matches(path))
                .map(Rule::category)
                .findFirst();
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
