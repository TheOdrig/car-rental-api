package com.akif.shared.security.ratelimit;

import com.akif.shared.security.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";

    private final RateLimitProperties properties;
    private final RateLimitEndpointClassifier classifier;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitService rateLimitService;
    private final ApiErrorWriter errorWriter;

    public RateLimitFilter(RateLimitProperties properties,
                           RateLimitEndpointClassifier classifier,
                           ClientIpResolver clientIpResolver,
                           RateLimitService rateLimitService,
                           ApiErrorWriter errorWriter) {
        this.properties = properties;
        this.classifier = classifier;
        this.clientIpResolver = clientIpResolver;
        this.rateLimitService = rateLimitService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<RateLimitCategory> category = classifier.classify(request);
        if (category.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitService.Decision decision = rateLimitService.tryConsume(category.get(), clientIpResolver.resolve(request));
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded for {}", category.get());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        errorWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, ERROR_CODE,
                "Too many requests. Retry after " + decision.retryAfterSeconds() + " seconds.");
    }
}
