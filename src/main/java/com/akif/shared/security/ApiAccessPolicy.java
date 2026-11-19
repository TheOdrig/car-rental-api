package com.akif.shared.security;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.regex.Pattern;

public final class ApiAccessPolicy {

    public record Rule(HttpMethod method, String pattern, Access access) {

        boolean matches(String requestMethod, PathContainer path) {
            if (method != null && !method.name().equals(requestMethod)) {
                return false;
            }
            return PATTERNS.parse(pattern).matches(path);
        }
    }

    private static final PathPatternParser PATTERNS = PathPatternParser.defaultInstance;
    private static final Pattern PATH_VARIABLE = Pattern.compile("[{][^}]+[}]");

    public static final List<String> DOCUMENTATION_PATHS = List.of("/swagger-ui/**", "/v3/api-docs/**");
    public static final String SWAGGER_UI_ENTRY = "/swagger-ui.html";

    public static final List<Rule> RULES = List.of(
            any("/api/v1/auth/**", Access.PUBLIC),
            any("/api/v1/oauth2/authorize/**", Access.PUBLIC),
            any("/api/v1/oauth2/callback/**", Access.PUBLIC),
            any("/api/v1/oauth2/link/**", Access.AUTHENTICATED),
            any("/api/v1/files/**", Access.PUBLIC),
            any("/api/v1/webhooks/**", Access.PUBLIC),

            rule(HttpMethod.POST, "/api/v1/rentals/request", Access.USER),
            rule(HttpMethod.POST, "/api/v1/rentals/*/confirm", Access.ADMIN),
            rule(HttpMethod.POST, "/api/v1/rentals/*/pickup", Access.ADMIN),
            rule(HttpMethod.POST, "/api/v1/rentals/*/return", Access.ADMIN),
            rule(HttpMethod.POST, "/api/v1/rentals/*/early-return", Access.ADMIN),
            rule(HttpMethod.POST, "/api/v1/rentals/*/cancel", Access.AUTHENTICATED),
            rule(HttpMethod.POST, "/api/v1/rentals/*/payment-intent", Access.USER),
            rule(HttpMethod.GET, "/api/v1/rentals/*/payment", Access.AUTHENTICATED),
            rule(HttpMethod.GET, "/api/v1/rentals/me", Access.USER),
            rule(HttpMethod.GET, "/api/v1/rentals/admin", Access.ADMIN),
            rule(HttpMethod.GET, "/api/v1/rentals/*", Access.AUTHENTICATED),

            any("/api/v1/cars/business/**", Access.ADMIN),

            rule(HttpMethod.GET, "/api/v1/cars", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/v1/cars/{id}", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/v1/cars/search/**", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/v1/cars/statistics/**", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/v1/cars/{id}/availability/**", Access.PUBLIC),
            rule(HttpMethod.POST, "/api/v1/cars/availability/search", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/v1/cars/{id}/similar", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/v1/cars/active", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/v1/cars/filter-options", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/v1/cars/search", Access.PUBLIC),

            rule(HttpMethod.POST, "/api/v1/cars", Access.ADMIN),
            rule(HttpMethod.PUT, "/api/v1/cars/**", Access.ADMIN),
            rule(HttpMethod.PATCH, "/api/v1/cars/**", Access.ADMIN),
            rule(HttpMethod.DELETE, "/api/v1/cars/**", Access.ADMIN),

            rule(HttpMethod.GET, "/api/v1/exchange-rates/**", Access.PUBLIC),
            rule(HttpMethod.POST, "/api/v1/exchange-rates/convert", Access.PUBLIC),
            rule(HttpMethod.POST, "/api/v1/exchange-rates/refresh", Access.ADMIN),

            rule(HttpMethod.GET, "/api/v1/pricing/preview", Access.PUBLIC),
            rule(HttpMethod.GET, "/api/v1/pricing/strategies", Access.PUBLIC),
            rule(HttpMethod.POST, "/api/v1/pricing/calculate", Access.AUTHENTICATED),

            any("/api/v1/admin/late-returns/**", Access.ADMIN),
            any("/api/v1/admin/rentals/*/penalty/**", Access.ADMIN),
            any("/api/v1/admin/quick-actions/**", Access.ADMIN),
            any("/api/v1/admin/dashboard/**", Access.ADMIN),
            any("/api/v1/admin/alerts/**", Access.ADMIN),
            any("/api/v1/admin/**", Access.ADMIN));

    public static final Access DEFAULT_ACCESS = Access.AUTHENTICATED;

    private ApiAccessPolicy() {
    }

    public static Access resolve(String method, String path) {
        PathContainer container = PathContainer.parsePath(path);
        for (Rule rule : RULES) {
            if (rule.matches(method, container)) {
                return rule.access();
            }
        }
        return DEFAULT_ACCESS;
    }

    public static Access resolveTemplate(String method, String pathTemplate) {
        return resolve(method, PATH_VARIABLE.matcher(pathTemplate).replaceAll("1"));
    }

    private static Rule any(String pattern, Access access) {
        return new Rule(null, pattern, access);
    }

    private static Rule rule(HttpMethod method, String pattern, Access access) {
        return new Rule(method, pattern, access);
    }
}
