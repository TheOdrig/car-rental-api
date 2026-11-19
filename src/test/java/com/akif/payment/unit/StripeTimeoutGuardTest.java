package com.akif.payment.unit;

import com.akif.payment.internal.config.StripeConfig;
import com.akif.payment.internal.config.StripeTimeoutGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Stripe timeouts versus the idle transaction timeout")
class StripeTimeoutGuardTest {

    private StripeTimeoutGuard guard(int connectMs, int readMs, int retries, String idle) {
        StripeConfig config = new StripeConfig();
        ReflectionTestUtils.setField(config, "connectTimeoutMs", connectMs);
        ReflectionTestUtils.setField(config, "readTimeoutMs", readMs);
        ReflectionTestUtils.setField(config, "maxNetworkRetries", retries);
        StripeTimeoutGuard guard = new StripeTimeoutGuard(config);
        ReflectionTestUtils.setField(guard, "idleInTransactionTimeout", idle);
        return guard;
    }

    @Test
    @DisplayName("the shipped defaults are accepted")
    void defaultsPass() {
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(guard(3000, 10000, 1, "60s"), "verify"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a worst case at or above the idle timeout fails the start")
    void tooSlowFails() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(guard(3000, 27000, 1, "60s"), "verify"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("idle_in_transaction_session_timeout");
    }

    @Test
    @DisplayName("PostgreSQL duration units are understood")
    void unitsAreParsed() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(guard(3000, 10000, 1, "20s"), "verify"))
                .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(guard(3000, 10000, 1, "1min"), "verify"))
                .doesNotThrowAnyException();
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(guard(3000, 10000, 1, "0"), "verify"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("an unparseable value is rejected")
    void garbageFails() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(guard(3000, 10000, 1, "soon"), "verify"))
                .isInstanceOf(IllegalStateException.class);
    }
}
