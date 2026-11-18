package com.akif.shared.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitEndpointClassifier")
class RateLimitEndpointClassifierTest {

    private final RateLimitEndpointClassifier classifier = new RateLimitEndpointClassifier();

    @Test
    @DisplayName("classifies the throttled authentication endpoints")
    void classifiesThrottledEndpoints() {
        assertThat(classifier.classify("POST", "/api/auth/login")).contains(RateLimitCategory.LOGIN);
        assertThat(classifier.classify("POST", "/api/auth/register")).contains(RateLimitCategory.REGISTER);
        assertThat(classifier.classify("POST", "/api/auth/refresh")).contains(RateLimitCategory.REFRESH);
        assertThat(classifier.classify("POST", "/api/auth/forgot-password")).contains(RateLimitCategory.PASSWORD_RESET);
        assertThat(classifier.classify("POST", "/api/auth/reset-password")).contains(RateLimitCategory.PASSWORD_RESET);
        assertThat(classifier.classify("GET", "/api/oauth2/callback/google")).contains(RateLimitCategory.OAUTH2_CALLBACK);
    }

    @Test
    @DisplayName("tolerates a trailing slash")
    void toleratesTrailingSlash() {
        assertThat(classifier.classify("POST", "/api/auth/login/")).contains(RateLimitCategory.LOGIN);
    }

    @Test
    @DisplayName("leaves every other request unthrottled")
    void ignoresOtherRequests() {
        assertThat(classifier.classify("GET", "/api/auth/login")).isEmpty();
        assertThat(classifier.classify("POST", "/api/auth/logout")).isEmpty();
        assertThat(classifier.classify("GET", "/api/cars")).isEmpty();
        assertThat(classifier.classify("POST", "/api/rentals/request")).isEmpty();
        assertThat(classifier.classify("GET", "/api/oauth2/authorize/google")).isEmpty();
    }
}
