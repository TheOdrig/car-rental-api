package com.akif.payment.unit.gateway.impl;

import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.config.StripeConfig;
import com.akif.payment.internal.exception.StripeIntegrationException;
import com.akif.payment.internal.service.gateway.ChargeCommand;
import com.akif.payment.internal.service.gateway.ChargeResult;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.IntentCommand;
import com.akif.payment.internal.service.gateway.impl.StripePaymentGateway;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.FakeStripeServer;
import com.akif.testsupport.FakeStripeServer.Recorded;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StripePaymentGateway against a fake Stripe HTTP server")
class StripePaymentGatewayTest {

    private FakeStripeServer stripe;
    private StripePaymentGateway gateway;

    @BeforeEach
    void setUp() {
        stripe = new FakeStripeServer();
        StripeConfig config = new StripeConfig();
        ReflectionTestUtils.setField(config, "apiKey", "sk_test_unit");
        ReflectionTestUtils.setField(config, "apiBaseUrl", stripe.baseUrl());
        ReflectionTestUtils.setField(config, "maxNetworkRetries", 0);
        gateway = new StripePaymentGateway(config);
    }

    @AfterEach
    void tearDown() {
        stripe.close();
    }

    private IntentCommand command(BigDecimal amount, CurrencyType currency) {
        return new IntentCommand(7L, 3L, amount, currency, "customer@example.com", null,
                "Car rental #3", "customer-key");
    }

    @Test
    @DisplayName("creates a manual-capture intent with the idempotency key and a server-computed amount")
    void createsManualCaptureIntent() {
        GatewayIntent intent = gateway.createIntent(command(new BigDecimal("120.50"), CurrencyType.USD), "intent-key-1");

        assertThat(intent.id()).startsWith("pi_fake_");
        assertThat(intent.clientSecret()).endsWith("_secret_fake");
        assertThat(intent.status()).isEqualTo(GatewayIntentStatus.REQUIRES_PAYMENT_METHOD);
        assertThat(intent.customerId()).isEqualTo("cus_fake");

        Recorded request = stripe.requestsTo("POST", "/v1/payment_intents").get(0);
        assertThat(request.header("Idempotency-Key")).isEqualTo("intent-key-1");
        assertThat(request.header("Authorization")).isEqualTo("Bearer sk_test_unit");
        assertThat(request.form()).containsEntry("amount", "12050")
                .containsEntry("currency", "usd")
                .containsEntry("capture_method", "manual")
                .containsEntry("setup_future_usage", "off_session")
                .containsEntry("metadata[payment_id]", "7")
                .containsEntry("metadata[rental_id]", "3");
    }

    @Test
    @DisplayName("sends zero-decimal currencies without a decimal shift")
    void zeroDecimalCurrencyAmount() {
        gateway.createIntent(command(new BigDecimal("15000"), CurrencyType.JPY), "intent-key-jpy");

        Recorded request = stripe.requestsTo("POST", "/v1/payment_intents").get(0);
        assertThat(request.form()).containsEntry("amount", "15000").containsEntry("currency", "jpy");
    }

    @Test
    @DisplayName("creates the customer with its own idempotency key before the intent")
    void createsCustomerWithOwnKey() {
        gateway.createIntent(command(new BigDecimal("10.00"), CurrencyType.EUR), "intent-key-2");

        Recorded customer = stripe.requestsTo("POST", "/v1/customers").get(0);
        assertThat(customer.header("Idempotency-Key")).isEqualTo("customer-key");
        assertThat(customer.form()).containsEntry("email", "customer@example.com");
    }

    @Test
    @DisplayName("reuses a known customer instead of creating a new one")
    void reusesKnownCustomer() {
        IntentCommand known = new IntentCommand(7L, 3L, new BigDecimal("10.00"), CurrencyType.USD,
                "customer@example.com", "cus_existing", "Car rental #3", "customer-key");

        gateway.createIntent(known, "intent-key-3");

        assertThat(stripe.requestsTo("POST", "/v1/customers")).isEmpty();
        assertThat(stripe.requestsTo("POST", "/v1/payment_intents").get(0).form())
                .containsEntry("customer", "cus_existing");
    }

    @Test
    @DisplayName("rejects amounts with more precision than the currency allows")
    void rejectsExcessPrecision() {
        assertThatThrownBy(() -> gateway.createIntent(command(new BigDecimal("10.55"), CurrencyType.JPY), "k"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(stripe.requests()).isEmpty();
    }

    @Test
    @DisplayName("maps a Stripe error on create to an integration exception without leaking the key")
    void createErrorMapsToIntegrationException() {
        stripe.enqueueError(402, "card_error", "card_declined", "Your card was declined.");

        assertThatThrownBy(() -> gateway.createIntent(command(new BigDecimal("10.00"), CurrencyType.USD), "k"))
                .isInstanceOf(StripeIntegrationException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("sk_test_unit"));
    }

    @Test
    @DisplayName("retrieves an intent and maps its status")
    void retrievesIntent() {
        stripe.setIntentStatus("pi_known", "requires_capture");

        GatewayIntent intent = gateway.retrieveIntent("pi_known");

        assertThat(intent.id()).isEqualTo("pi_known");
        assertThat(intent.status()).isEqualTo(GatewayIntentStatus.REQUIRES_CAPTURE);
        assertThat(intent.paymentMethodId()).isEqualTo("pm_fake");
        assertThat(stripe.requestsTo("GET", "/v1/payment_intents/pi_known").get(0).header("Idempotency-Key")).isNull();
    }

    @Test
    @DisplayName("captures with the idempotency key")
    void capturesWithKey() {
        PaymentResult result = gateway.capture("pi_cap", "capture-key");

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).isEqualTo("pi_cap");
        assertThat(stripe.requestsTo("POST", "/v1/payment_intents/pi_cap/capture").get(0).header("Idempotency-Key"))
                .isEqualTo("capture-key");
    }

    @Test
    @DisplayName("returns a failure result instead of throwing when capture is rejected")
    void captureFailureIsAResult() {
        stripe.enqueueError(400, "invalid_request_error", "payment_intent_unexpected_state", "Not capturable");

        PaymentResult result = gateway.capture("pi_cap", "capture-key");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isNotBlank();
    }

    @Test
    @DisplayName("cancels with the idempotency key")
    void cancelsWithKey() {
        PaymentResult result = gateway.cancel("pi_can", "cancel-key");

        assertThat(result.success()).isTrue();
        assertThat(stripe.requestsTo("POST", "/v1/payment_intents/pi_can/cancel").get(0).header("Idempotency-Key"))
                .isEqualTo("cancel-key");
    }

    @Test
    @DisplayName("refunds a payment intent for the exact minor-unit amount with the idempotency key")
    void refundsWithKey() {
        PaymentResult result = gateway.refund("pi_ref", new BigDecimal("33.30"), CurrencyType.TRY, "refund-key");

        assertThat(result.success()).isTrue();
        Recorded request = stripe.requestsTo("POST", "/v1/refunds").get(0);
        assertThat(request.header("Idempotency-Key")).isEqualTo("refund-key");
        assertThat(request.form()).containsEntry("payment_intent", "pi_ref").containsEntry("amount", "3330");
    }

    @Test
    @DisplayName("refuses a refund amount the currency cannot express without calling Stripe")
    void refundPrecisionIsRejectedLocally() {
        PaymentResult result = gateway.refund("pi_ref", new BigDecimal("1.50"), CurrencyType.JPY, "refund-key");

        assertThat(result.success()).isFalse();
        assertThat(stripe.requests()).isEmpty();
    }

    @Test
    @DisplayName("charges a saved payment method off session and confirms immediately")
    void offSessionCharge() {
        ChargeCommand command = new ChargeCommand(9L, 3L, new BigDecimal("40.00"), CurrencyType.USD,
                "cus_saved", "pm_saved", "Penalty");

        ChargeResult result = gateway.chargeSavedMethod(command, "charge-key");

        assertThat(result.success()).isTrue();
        Recorded request = stripe.requestsTo("POST", "/v1/payment_intents").get(0);
        assertThat(request.header("Idempotency-Key")).isEqualTo("charge-key");
        assertThat(request.form()).containsEntry("off_session", "true")
                .containsEntry("confirm", "true")
                .containsEntry("payment_method", "pm_saved")
                .containsEntry("customer", "cus_saved")
                .containsEntry("amount", "4000");
    }

    @Test
    @DisplayName("reports a declined off-session charge as a failed result")
    void declinedOffSessionCharge() {
        stripe.enqueueError(402, "card_error", "card_declined", "Your card was declined.");
        ChargeCommand command = new ChargeCommand(9L, 3L, new BigDecimal("40.00"), CurrencyType.USD,
                "cus_saved", "pm_saved", "Penalty");

        ChargeResult result = gateway.chargeSavedMethod(command, "charge-key");

        assertThat(result.success()).isFalse();
        assertThat(result.definitiveDecline()).isTrue();
        assertThat(result.result().message()).contains("declined");
    }

    @Test
    @DisplayName("keeps the idempotency key usable when Stripe answers with a server error")
    void serverErrorIsAmbiguous() {
        stripe.enqueueError(500, "api_error", "internal", "Stripe had a problem.");
        ChargeCommand command = new ChargeCommand(9L, 3L, new BigDecimal("40.00"), CurrencyType.USD,
                "cus_saved", "pm_saved", "Penalty");

        ChargeResult result = gateway.chargeSavedMethod(command, "charge-key");

        assertThat(result.success()).isFalse();
        assertThat(result.definitiveDecline()).isFalse();
    }

    @Test
    @DisplayName("keeps the idempotency key usable when the connection to Stripe fails")
    void connectionErrorIsAmbiguous() {
        stripe.close();
        ChargeCommand command = new ChargeCommand(9L, 3L, new BigDecimal("40.00"), CurrencyType.USD,
                "cus_saved", "pm_saved", "Penalty");

        ChargeResult result = gateway.chargeSavedMethod(command, "charge-key");

        assertThat(result.success()).isFalse();
        assertThat(result.definitiveDecline()).isFalse();
    }

}
