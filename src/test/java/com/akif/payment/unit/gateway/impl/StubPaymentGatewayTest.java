package com.akif.payment.unit.gateway.impl;

import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.service.gateway.ChargeCommand;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.IntentCommand;
import com.akif.payment.internal.service.gateway.impl.StubPaymentGateway;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StubPaymentGateway")
class StubPaymentGatewayTest {

    private StubPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new StubPaymentGateway();
    }

    private IntentCommand command() {
        return new IntentCommand(1L, 2L, new BigDecimal("50.00"), CurrencyType.USD, "a@b.c", null, "d", "ck");
    }

    @Test
    @DisplayName("is visibly a stub: ids and secrets carry the stub prefix")
    void idsAreVisiblyStubs() {
        GatewayIntent intent = gateway.createIntent(command(), "key-1");

        assertThat(intent.id()).startsWith("stub_pi_");
        assertThat(intent.clientSecret()).startsWith("stub_secret_");
    }

    @Test
    @DisplayName("reports a new intent as already authorized")
    void newIntentIsAuthorized() {
        assertThat(gateway.createIntent(command(), "key-1").status()).isEqualTo(GatewayIntentStatus.REQUIRES_CAPTURE);
    }

    @Test
    @DisplayName("returns the same intent for the same idempotency key")
    void sameKeySameIntent() {
        GatewayIntent first = gateway.createIntent(command(), "key-1");
        GatewayIntent second = gateway.createIntent(command(), "key-1");

        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    @DisplayName("returns a different intent for a different idempotency key")
    void differentKeyDifferentIntent() {
        assertThat(gateway.createIntent(command(), "key-1").id())
                .isNotEqualTo(gateway.createIntent(command(), "key-2").id());
    }

    @Test
    @DisplayName("tracks capture and cancellation on retrieve")
    void tracksState() {
        GatewayIntent captured = gateway.createIntent(command(), "key-1");
        GatewayIntent cancelled = gateway.createIntent(command(), "key-2");

        gateway.capture(captured.id(), "c1");
        gateway.cancel(cancelled.id(), "c2");

        assertThat(gateway.retrieveIntent(captured.id()).status()).isEqualTo(GatewayIntentStatus.SUCCEEDED);
        assertThat(gateway.retrieveIntent(cancelled.id()).status()).isEqualTo(GatewayIntentStatus.CANCELED);
    }

    @Test
    @DisplayName("labels every result message as a stub")
    void resultMessagesAreLabelled() {
        PaymentResult capture = gateway.capture("stub_pi_x", "k");
        PaymentResult refund = gateway.refund("stub_pi_x", BigDecimal.TEN, CurrencyType.USD, "k");

        assertThat(capture.message()).contains("STUB");
        assertThat(refund.message()).contains("STUB");
    }

    @Test
    @DisplayName("derives refund ids deterministically from the idempotency key")
    void refundIdsAreDeterministic() {
        String first = gateway.refund("stub_pi_x", BigDecimal.TEN, CurrencyType.USD, "k").transactionId();
        String second = gateway.refund("stub_pi_x", BigDecimal.TEN, CurrencyType.USD, "k").transactionId();

        assertThat(first).isEqualTo(second).startsWith("stub_re_");
    }

    @Test
    @DisplayName("charges a saved method successfully")
    void chargesSavedMethod() {
        PaymentResult result = gateway.chargeSavedMethod(
                new ChargeCommand(1L, 2L, BigDecimal.TEN, CurrencyType.USD, "cus", "pm", "d"), "charge-key");

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).startsWith("stub_pi_");
    }
}
