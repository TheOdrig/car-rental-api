package com.akif.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CORS configuration")
class CorsConfigTest {

    private final CorsConfig config = new CorsConfig();

    private CorsConfiguration resolve(CorsProperties properties) {
        CorsConfigurationSource source = config.corsConfigurationSource(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cars");
        request.setServletPath("/api/v1/cars");
        return source.getCorsConfiguration(request);
    }

    @Test
    @DisplayName("allows only the configured origins and sends credentials")
    void allowsConfiguredOriginsOnly() {
        CorsConfiguration resolved = resolve(new CorsProperties(
                List.of("https://web.example.com", "http://localhost:3000"), 600));

        assertThat(resolved.getAllowedOrigins()).containsExactly("https://web.example.com", "http://localhost:3000");
        assertThat(resolved.getAllowCredentials()).isTrue();
        assertThat(resolved.getAllowedHeaders()).doesNotContain("*");
        assertThat(resolved.getMaxAge()).isEqualTo(600L);
    }

    @Test
    @DisplayName("refuses a wildcard origin")
    void refusesWildcardOrigin() {
        assertThatThrownBy(() -> config.corsConfigurationSource(new CorsProperties(List.of("*"), 600)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> config.corsConfigurationSource(
                new CorsProperties(List.of("https://*.example.com"), 600)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("allows no origin when none is configured")
    void allowsNothingWhenEmpty() {
        CorsConfiguration resolved = resolve(new CorsProperties(List.of(), 600));

        assertThat(resolved.getAllowedOrigins()).isEmpty();
    }
}
