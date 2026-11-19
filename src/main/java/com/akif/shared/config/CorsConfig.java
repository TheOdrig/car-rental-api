package com.akif.shared.config;

import com.akif.shared.handler.ProblemFactory;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    static final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type", "Accept", "X-Correlation-ID");
    static final List<String> EXPOSED_HEADERS = List.of("X-Correlation-ID", "Retry-After");

    @Bean
    org.springframework.web.filter.CorsFilter corsFilter(@org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") CorsConfigurationSource source, ProblemFactory problems,
                                                         ObjectMapper objectMapper) {
        org.springframework.web.filter.CorsFilter filter = new org.springframework.web.filter.CorsFilter(source);
        filter.setCorsProcessor(new ProblemCorsProcessor(problems, objectMapper));
        return filter;
    }

    @Bean
    org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.CorsFilter> corsFilterRegistration(
            org.springframework.web.filter.CorsFilter filter) {
        org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.CorsFilter> registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        List<String> origins = properties.allowedOrigins();
        if (origins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalStateException("cors.allowed-origins must list explicit origins; wildcards are not allowed with credentials");
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofSeconds(properties.maxAgeSeconds()));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }
}
