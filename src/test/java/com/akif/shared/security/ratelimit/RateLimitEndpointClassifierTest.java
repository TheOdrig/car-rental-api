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
        assertThat(classify("POST", "/api/v1/auth/login")).contains(RateLimitCategory.LOGIN);
        assertThat(classify("POST", "/api/v1/auth/register")).contains(RateLimitCategory.REGISTER);
        assertThat(classify("POST", "/api/v1/auth/refresh")).contains(RateLimitCategory.REFRESH);
        assertThat(classify("POST", "/api/v1/auth/forgot-password")).contains(RateLimitCategory.PASSWORD_RESET);
        assertThat(classify("POST", "/api/v1/auth/reset-password")).contains(RateLimitCategory.PASSWORD_RESET);
        assertThat(classify("GET", "/api/v1/oauth2/callback/google")).contains(RateLimitCategory.OAUTH2_CALLBACK);
        assertThat(classify("POST", "/api/v1/rentals/request")).contains(RateLimitCategory.RENTAL_REQUEST);
        assertThat(classify("POST", "/api/v1/rentals/reques%74")).contains(RateLimitCategory.RENTAL_REQUEST);
    }

    @Test
    @DisplayName("throttles starting a rental payment")
    void classifiesPaymentIntent() {
        assertThat(classify("POST", "/api/v1/rentals/12/payment-intent")).contains(RateLimitCategory.PAYMENT_INTENT);
        assertThat(classify("POST", "/api/v1/rentals/12/payment-intent/")).contains(RateLimitCategory.PAYMENT_INTENT);
        assertThat(classify("POST", "/api/v1/cars/availability/search")).contains(RateLimitCategory.AVAILABILITY_SEARCH);
        assertThat(classify("GET", "/api/v1/cars/7/similar")).contains(RateLimitCategory.AVAILABILITY_SEARCH);
        assertThat(classify("GET", "/api/v1/rentals/12/payment")).isEmpty();
    }

    @Test
    @DisplayName("tolerates a trailing slash")
    void toleratesTrailingSlash() {
        assertThat(classify("POST", "/api/v1/auth/login/")).contains(RateLimitCategory.LOGIN);
    }

    @Test
    @DisplayName("classifies percent-encoded, trailing-slash and path-parameter variants")
    void classifiesEncodedVariants() {
        assertThat(classify("POST", "/api/v1/auth/l%6Fgin")).contains(RateLimitCategory.LOGIN);
        assertThat(classify("POST", "/api/v1/auth/%6C%6F%67%69%6E")).contains(RateLimitCategory.LOGIN);
        assertThat(classify("POST", "/api/v1/auth/login/")).contains(RateLimitCategory.LOGIN);
        assertThat(classify("POST", "/api/v1/auth/login;jsessionid=1")).contains(RateLimitCategory.LOGIN);
        assertThat(classify("POST", "/api/v1/auth/refre%73h")).contains(RateLimitCategory.REFRESH);
        assertThat(classify("GET", "/api/v1/oauth2/callback/g%6Fogle")).contains(RateLimitCategory.OAUTH2_CALLBACK);
    }

    @Test
    @DisplayName("leaves every other request unthrottled")
    void ignoresOtherRequests() {
        assertThat(classify("GET", "/api/v1/auth/login")).isEmpty();
        assertThat(classify("POST", "/api/v1/auth/logout")).isEmpty();
        assertThat(classify("GET", "/api/v1/cars")).isEmpty();
                assertThat(classify("GET", "/api/v1/oauth2/authorize/google")).isEmpty();
    }
}
