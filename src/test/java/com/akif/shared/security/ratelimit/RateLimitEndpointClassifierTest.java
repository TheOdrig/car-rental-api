package com.akif.shared.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitEndpointClassifier")
class RateLimitEndpointClassifierTest {

    private final RateLimitEndpointClassifier classifier = new RateLimitEndpointClassifier();

    private java.util.Optional<RateLimitCategory> classify(String method, String uri) {
        return classifier.classify(new MockHttpServletRequest(method, uri));
    }

    @Test
    @DisplayName("classifies the throttled authentication endpoints")
    void classifiesThrottledEndpoints() {
        assertThat(classify("POST", "/api/auth/login")).contains(RateLimitCategory.LOGIN);
        assertThat(classify("POST", "/api/auth/register")).contains(RateLimitCategory.REGISTER);
        assertThat(classify("POST", "/api/auth/refresh")).contains(RateLimitCategory.REFRESH);
        assertThat(classify("POST", "/api/auth/forgot-password")).contains(RateLimitCategory.PASSWORD_RESET);
        assertThat(classify("POST", "/api/auth/reset-password")).contains(RateLimitCategory.PASSWORD_RESET);
        assertThat(classify("GET", "/api/oauth2/callback/google")).contains(RateLimitCategory.OAUTH2_CALLBACK);
        assertThat(classify("POST", "/api/rentals/request")).contains(RateLimitCategory.RENTAL_REQUEST);
        assertThat(classify("POST", "/api/rentals/reques%74")).contains(RateLimitCategory.RENTAL_REQUEST);
    }

    @Test
    @DisplayName("tolerates a trailing slash")
    void toleratesTrailingSlash() {
        assertThat(classify("POST", "/api/auth/login/")).contains(RateLimitCategory.LOGIN);
    }

    @Test
    @DisplayName("classifies percent-encoded, trailing-slash and path-parameter variants")
    void classifiesEncodedVariants() {
        assertThat(classify("POST", "/api/auth/l%6Fgin")).contains(RateLimitCategory.LOGIN);
        assertThat(classify("POST", "/api/auth/%6C%6F%67%69%6E")).contains(RateLimitCategory.LOGIN);
        assertThat(classify("POST", "/api/auth/login/")).contains(RateLimitCategory.LOGIN);
        assertThat(classify("POST", "/api/auth/login;jsessionid=1")).contains(RateLimitCategory.LOGIN);
        assertThat(classify("POST", "/api/auth/refre%73h")).contains(RateLimitCategory.REFRESH);
        assertThat(classify("GET", "/api/oauth2/callback/g%6Fogle")).contains(RateLimitCategory.OAUTH2_CALLBACK);
    }

    @Test
    @DisplayName("leaves every other request unthrottled")
    void ignoresOtherRequests() {
        assertThat(classify("GET", "/api/auth/login")).isEmpty();
        assertThat(classify("POST", "/api/auth/logout")).isEmpty();
        assertThat(classify("GET", "/api/cars")).isEmpty();
                assertThat(classify("GET", "/api/oauth2/authorize/google")).isEmpty();
    }
}
