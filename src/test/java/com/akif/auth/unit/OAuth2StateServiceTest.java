package com.akif.auth.unit;

import com.akif.auth.domain.enums.OAuth2ErrorType;
import com.akif.auth.internal.config.OAuth2Properties;
import com.akif.auth.domain.exception.OAuth2AuthenticationException;
import com.akif.auth.internal.service.oauth2.impl.OAuth2StateService;
import com.akif.testsupport.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OAuth2StateService")
class OAuth2StateServiceTest {

    private static final String SECRET = "unit-test-oauth2-state-secret-0123456789";
    private static final Instant NOW = Instant.parse("2026-03-10T10:00:00Z");

    private MutableClock clock;
    private OAuth2StateService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Clock.fixed(NOW, ZoneOffset.UTC));
        service = new OAuth2StateService(properties(SECRET), clock);
    }

    @Test
    @DisplayName("accepts a state it generated")
    void acceptsGeneratedState() {
        assertThat(service.validateState(service.generateState())).isTrue();
    }

    @Test
    @DisplayName("accepts a state until the expiry window has passed")
    void acceptsStateInsideWindow() {
        String state = service.generateState();
        clock.advance(Duration.ofMinutes(10));

        assertThat(service.validateState(state)).isTrue();
    }

    @Test
    @DisplayName("rejects a state older than the expiry window")
    void rejectsExpiredState() {
        String state = service.generateState();
        clock.advance(Duration.ofMinutes(10).plusSeconds(1));

        assertInvalid(state);
    }

    @Test
    @DisplayName("rejects a well-formed state whose signature was forged")
    void rejectsForgedSignature() {
        String forged = encode(NOW.getEpochSecond() + ".random-value.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

        assertInvalid(forged);
    }

    @Test
    @DisplayName("rejects a state signed with a different secret")
    void rejectsOtherSecret() {
        OAuth2StateService other = new OAuth2StateService(properties("another-secret-value-0123456789abcdef"), clock);

        assertInvalid(other.generateState());
    }

    @Test
    @DisplayName("rejects a state whose payload was changed after signing")
    void rejectsTamperedPayload() {
        String decoded = new String(Base64.getUrlDecoder().decode(service.generateState()), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\.");
        String tampered = encode((Long.parseLong(parts[0]) + 3600) + "." + parts[1] + "." + parts[2]);

        assertInvalid(tampered);
    }

    @Test
    @DisplayName("rejects a correctly signed state with a non-numeric timestamp")
    void rejectsNonNumericTimestamp() {
        String data = "not-a-number.random-value";
        String state = encode(data + "." + sign(data));

        assertInvalid(state);
    }

    @Test
    @DisplayName("rejects empty, malformed and non-base64 states")
    void rejectsMalformedStates() {
        assertInvalid(null);
        assertInvalid(" ");
        assertInvalid("%%%not-base64%%%");
        assertInvalid(encode("only.two"));
    }

    private void assertInvalid(String state) {
        assertThatThrownBy(() -> service.validateState(state))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(error -> assertThat(((OAuth2AuthenticationException) error).getErrorType())
                        .isEqualTo(OAuth2ErrorType.INVALID_STATE));
    }

    private static OAuth2Properties properties(String secret) {
        OAuth2Properties properties = new OAuth2Properties();
        properties.getState().setSecret(secret);
        return properties;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
