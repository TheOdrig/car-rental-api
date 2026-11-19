package com.akif.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";
    private static final Pattern VALID_CORRELATION_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String correlationId = extractOrGenerateCorrelationId(request);
            MDC.put(MDC_CORRELATION_ID_KEY, correlationId);

            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            log.debug("Request started: {} {} [correlationId={}]",
                    request.getMethod(), request.getRequestURI(), correlationId);

            filterChain.doFilter(request, response);

        } finally {
            MDC.remove(MDC_CORRELATION_ID_KEY);
        }
    }

    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || !VALID_CORRELATION_ID.matcher(correlationId).matches()) {
            correlationId = generateCorrelationId();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID from header: {}", correlationId);
        }
        
        return correlationId;
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
