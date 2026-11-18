package com.akif.payment.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.TestEventCaptor;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.ReminderScheduler;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.WebhookEvent;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.domain.enums.WebhookEventStatus;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.repository.WebhookEventRepository;
import com.akif.payment.internal.service.PaymentStateService;
import com.akif.payment.internal.service.gateway.StripeAmounts;
import com.akif.testsupport.TestcontainersConfiguration;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Stripe webhook with real signatures")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StripeWebhookSignedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestEventCaptor eventCaptor;

    @Autowired
    private RentalRepository rentalRepository;

    @MockitoBean
    private ReminderScheduler reminderScheduler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @MockitoSpyBean
    private PaymentStateService stateService;

    @Value("${stripe.webhook.secret}")
    private String secret;

    private Payment payment;
    private String intentId;

    @BeforeEach
    void createPayment() {
        User customer = userRepository.save(TestDataBuilder.createTestUser());
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        Rental rental = rentalRepository.save(TestDataBuilder.createConfirmedRental(car, customer));
        intentId = "pi_wh_" + UUID.randomUUID().toString().substring(0, 8);
        payment = paymentRepository.save(Payment.builder()
                .rentalId(rental.getId())
                .kind(PaymentKind.RENTAL)
                .userEmail(customer.getEmail())
                .carLicensePlate(car.getLicensePlate())
                .amount(rental.getTotalPrice())
                .currency(rental.getCurrency())
                .status(PaymentStatus.PENDING)
                .paymentMethod("CARD")
                .stripePaymentIntentId(intentId)
                .transactionId(intentId)
                .isDeleted(false)
                .build());
        eventCaptor.clear();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM car_rental.webhook_events");
        paymentRepository.deleteAll();
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private long minor() {
        return StripeAmounts.toMinorUnits(payment.getAmount(), payment.getCurrency());
    }

    private String currency() {
        return payment.getCurrency().name().toLowerCase();
    }

    private String intentEvent(String eventId, String type, String status, long amountMinor, String currency, String intent) {
        return "{\"id\":\"" + eventId + "\",\"object\":\"event\",\"type\":\"" + type + "\",\"data\":{\"object\":{"
                + "\"id\":\"" + intent + "\",\"object\":\"payment_intent\",\"amount\":" + amountMinor
                + ",\"currency\":\"" + currency + "\",\"status\":\"" + status + "\",\"customer\":\"cus_wh\","
                + "\"payment_method\":\"pm_wh\",\"metadata\":{\"payment_id\":\"" + payment.getId() + "\"},"
                + "\"last_payment_error\":{\"message\":\"Your card was declined.\",\"code\":\"card_declined\"}}}}";
    }

    private String intentEvent(String eventId, String type, String status) {
        return intentEvent(eventId, type, status, minor(), currency(), intentId);
    }

    private String refundEvent(String eventId, long refundedMinor) {
        return "{\"id\":\"" + eventId + "\",\"object\":\"event\",\"type\":\"charge.refunded\",\"data\":{\"object\":{"
                + "\"id\":\"ch_wh\",\"object\":\"charge\",\"amount\":" + minor() + ",\"amount_refunded\":" + refundedMinor
                + ",\"currency\":\"" + currency() + "\",\"payment_intent\":\"" + intentId + "\"}}}";
    }

    private String header(String payload, long timestamp, String signingSecret) throws Exception {
        String signature = Webhook.Util.computeHmacSha256(signingSecret, timestamp + "." + payload);
        return "t=" + timestamp + ",v1=" + signature;
    }

    private ResultActions deliver(String payload) throws Exception {
        return deliver(payload, header(payload, clock.instant().getEpochSecond(), secret));
    }

    private ResultActions deliver(String payload, String signatureHeader) throws Exception {
        return mockMvc.perform(post("/api/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", signatureHeader)
                .content(payload));
    }

    private Payment reload() {
        return paymentRepository.findById(payment.getId()).orElseThrow();
    }

    @Test
    @DisplayName("authorizes the payment on amount_capturable_updated and remembers the card for later charges")
    void authorizesOnCapturableUpdate() throws Exception {
        deliver(intentEvent("evt_a1", "payment_intent.amount_capturable_updated", "requires_capture"))
                .andExpect(status().isOk());

        Payment updated = reload();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(updated.getStripePaymentMethodId()).isEqualTo("pm_wh");
        assertThat(updated.getStripeCustomerId()).isEqualTo("cus_wh");
        assertThat(webhookEventRepository.findByEventId("evt_a1").orElseThrow().getStatus())
                .isEqualTo(WebhookEventStatus.PROCESSED);
    }

    @Test
    @DisplayName("captures on payment_intent.succeeded and publishes the receipt event once")
    void capturesOnSucceeded() throws Exception {
        eventCaptor.clear();

        deliver(intentEvent("evt_s1", "payment_intent.succeeded", "succeeded")).andExpect(status().isOk());

        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(eventCaptor.getEventsOfType(PaymentCapturedEvent.class)).hasSize(1);
    }

    @Test
    @DisplayName("a replayed event id is acknowledged but processed only once")
    void replayIsIdempotent() throws Exception {
        String payload = intentEvent("evt_r1", "payment_intent.succeeded", "succeeded");
        eventCaptor.clear();

        deliver(payload).andExpect(status().isOk());
        deliver(payload).andExpect(status().isOk());
        deliver(payload).andExpect(status().isOk());

        assertThat(eventCaptor.getEventsOfType(PaymentCapturedEvent.class)).hasSize(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.webhook_events WHERE event_id = 'evt_r1'", Integer.class)).isEqualTo(1);
        Mockito.verify(stateService, Mockito.times(1)).applyIntent(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("an old authorization event cannot downgrade a captured payment")
    void lateAuthorizationDoesNotDowngrade() throws Exception {
        deliver(intentEvent("evt_l1", "payment_intent.succeeded", "succeeded")).andExpect(status().isOk());
        deliver(intentEvent("evt_l2", "payment_intent.amount_capturable_updated", "requires_capture")).andExpect(status().isOk());

        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    @DisplayName("marks a pending payment FAILED on payment_intent.payment_failed with the decline reason")
    void failsPendingPayment() throws Exception {
        deliver(intentEvent("evt_f1", "payment_intent.payment_failed", "requires_payment_method")).andExpect(status().isOk());

        Payment updated = reload();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(updated.getFailureReason()).isEqualTo("Your card was declined.");
    }

    @Test
    @DisplayName("a late failure event does not downgrade an authorized payment")
    void failureDoesNotDowngradeAuthorized() throws Exception {
        deliver(intentEvent("evt_f2", "payment_intent.amount_capturable_updated", "requires_capture")).andExpect(status().isOk());
        deliver(intentEvent("evt_f3", "payment_intent.payment_failed", "requires_payment_method")).andExpect(status().isOk());

        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    @DisplayName("cancels the payment on payment_intent.canceled")
    void cancelsPayment() throws Exception {
        deliver(intentEvent("evt_c1", "payment_intent.canceled", "canceled")).andExpect(status().isOk());

        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("applies refund totals from charge.refunded and flips to REFUNDED when fully refunded")
    void appliesRefunds() throws Exception {
        deliver(intentEvent("evt_x0", "payment_intent.succeeded", "succeeded")).andExpect(status().isOk());

        deliver(refundEvent("evt_x1", 500)).andExpect(status().isOk());
        assertThat(reload().getRefundedAmount()).isEqualByComparingTo("5.00");
        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.CAPTURED);

        deliver(refundEvent("evt_x2", minor())).andExpect(status().isOk());
        assertThat(reload().getRefundedAmount()).isEqualByComparingTo(payment.getAmount());
        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("a replayed refund event does not add the refund twice")
    void refundReplayDoesNotDoubleCount() throws Exception {
        deliver(intentEvent("evt_y0", "payment_intent.succeeded", "succeeded")).andExpect(status().isOk());
        String payload = refundEvent("evt_y1", 500);

        deliver(payload).andExpect(status().isOk());
        deliver(payload).andExpect(status().isOk());

        assertThat(reload().getRefundedAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("rejects an unsigned-looking header and stores nothing")
    void rejectsInvalidSignature() throws Exception {
        String payload = intentEvent("evt_bad1", "payment_intent.succeeded", "succeeded");

        deliver(payload, "t=" + clock.instant().getEpochSecond() + ",v1=deadbeef").andExpect(status().isBadRequest());

        assertThat(webhookEventRepository.findByEventId("evt_bad1")).isEmpty();
        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("rejects a payload signed with a different secret")
    void rejectsWrongSecret() throws Exception {
        String payload = intentEvent("evt_bad2", "payment_intent.succeeded", "succeeded");

        deliver(payload, header(payload, clock.instant().getEpochSecond(), "whsec_attacker")).andExpect(status().isBadRequest());

        assertThat(webhookEventRepository.findByEventId("evt_bad2")).isEmpty();
        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("rejects a body that was modified after signing")
    void rejectsTamperedBody() throws Exception {
        String signed = intentEvent("evt_bad3", "payment_intent.succeeded", "succeeded");
        String tampered = signed.replace("\"succeeded\"", "\"succeeded\",\"x\":1");

        deliver(tampered, header(signed, clock.instant().getEpochSecond(), secret)).andExpect(status().isBadRequest());

        assertThat(webhookEventRepository.findByEventId("evt_bad3")).isEmpty();
    }

    @Test
    @DisplayName("rejects a correctly signed event whose timestamp is outside the tolerance, so old captures cannot be replayed")
    void rejectsStaleTimestamp() throws Exception {
        String payload = intentEvent("evt_old", "payment_intent.succeeded", "succeeded");
        long stale = clock.instant().getEpochSecond() - 3600;

        deliver(payload, header(payload, stale, secret)).andExpect(status().isBadRequest());

        assertThat(webhookEventRepository.findByEventId("evt_old")).isEmpty();
        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("rejects a request without a Stripe-Signature header")
    void rejectsMissingHeader() throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intentEvent("evt_none", "payment_intent.succeeded", "succeeded")))
                .andExpect(status().isBadRequest());

        assertThat(webhookEventRepository.findByEventId("evt_none")).isEmpty();
    }

    @Test
    @DisplayName("rejects a signed body that is not a Stripe event")
    void rejectsNonEventBody() throws Exception {
        String payload = "not json at all";

        deliver(payload, header(payload, clock.instant().getEpochSecond(), secret)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("refuses an intent whose amount differs from the server-side payment and records the event as failed")
    void rejectsAmountMismatch() throws Exception {
        deliver(intentEvent("evt_m1", "payment_intent.amount_capturable_updated", "requires_capture",
                minor() - 1, currency(), intentId)).andExpect(status().isOk());

        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.PENDING);
        WebhookEvent stored = webhookEventRepository.findByEventId("evt_m1").orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(WebhookEventStatus.FAILED);
        assertThat(stored.getErrorMessage()).contains("amount or currency");
    }

    @Test
    @DisplayName("refuses an intent whose currency differs from the server-side payment")
    void rejectsCurrencyMismatch() throws Exception {
        String other = currency().equals("jpy") ? "usd" : "jpy";

        deliver(intentEvent("evt_m2", "payment_intent.succeeded", "succeeded", minor(), other, intentId))
                .andExpect(status().isOk());

        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(webhookEventRepository.findByEventId("evt_m2").orElseThrow().getStatus())
                .isEqualTo(WebhookEventStatus.FAILED);
    }

    @Test
    @DisplayName("acknowledges an event for an unknown intent without changing anything")
    void ignoresUnknownIntent() throws Exception {
        String payload = "{\"id\":\"evt_u1\",\"object\":\"event\",\"type\":\"payment_intent.succeeded\",\"data\":{\"object\":{"
                + "\"id\":\"pi_unknown\",\"object\":\"payment_intent\",\"amount\":100,\"currency\":\"usd\",\"status\":\"succeeded\"}}}";

        deliver(payload).andExpect(status().isOk());

        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(webhookEventRepository.findByEventId("evt_u1").orElseThrow().getStatus())
                .isEqualTo(WebhookEventStatus.PROCESSED);
    }

    @Test
    @DisplayName("acknowledges event types it does not handle")
    void ignoresUnsupportedType() throws Exception {
        String payload = "{\"id\":\"evt_t1\",\"object\":\"event\",\"type\":\"customer.created\",\"data\":{\"object\":{\"id\":\"cus_1\",\"object\":\"customer\"}}}";

        deliver(payload).andExpect(status().isOk());

        assertThat(webhookEventRepository.findByEventId("evt_t1").orElseThrow().getStatus())
                .isEqualTo(WebhookEventStatus.PROCESSED);
    }

    @Test
    @DisplayName("falls back to the payment id in the intent metadata when the intent id is not stored yet")
    void locatesByMetadata() throws Exception {
        payment.nextAttempt();
        payment = paymentRepository.saveAndFlush(payment);

        deliver(intentEvent("evt_md1", "payment_intent.amount_capturable_updated", "requires_capture"))
                .andExpect(status().isOk());

        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    @DisplayName("a processing failure answers 500, leaves the event FAILED and is processed on Stripe's retry")
    void failedProcessingIsRetried() throws Exception {
        Mockito.doThrow(new IllegalStateException("database unavailable"))
                .doCallRealMethod()
                .when(stateService).applyIntent(Mockito.any(), Mockito.any());
        String payload = intentEvent("evt_retry", "payment_intent.amount_capturable_updated", "requires_capture");

        deliver(payload).andExpect(status().isInternalServerError());
        assertThat(webhookEventRepository.findByEventId("evt_retry").orElseThrow().getStatus())
                .isEqualTo(WebhookEventStatus.FAILED);

        deliver(payload).andExpect(status().isOk());

        assertThat(webhookEventRepository.findByEventId("evt_retry").orElseThrow().getStatus())
                .isEqualTo(WebhookEventStatus.PROCESSED);
        assertThat(reload().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    @DisplayName("a refund event whose currency differs is rejected without changing the payment")
    void rejectsRefundCurrencyMismatch() throws Exception {
        deliver(intentEvent("evt_z0", "payment_intent.succeeded", "succeeded")).andExpect(status().isOk());
        String other = currency().equals("jpy") ? "usd" : "jpy";
        String payload = refundEvent("evt_z1", 500).replace("\"currency\":\"" + currency() + "\"", "\"currency\":\"" + other + "\"");

        deliver(payload).andExpect(status().isOk());

        assertThat(reload().getRefundedAmount()).isNull();
        assertThat(webhookEventRepository.findByEventId("evt_z1").orElseThrow().getStatus())
                .isEqualTo(WebhookEventStatus.FAILED);
    }

    @Test
    @DisplayName("keeps the stored payload for audit and links it to the verified event id")
    void storesPayload() throws Exception {
        String payload = intentEvent("evt_p1", "payment_intent.canceled", "canceled");

        deliver(payload).andExpect(status().isOk());

        WebhookEvent stored = webhookEventRepository.findByEventId("evt_p1").orElseThrow();
        assertThat(stored.getEventType()).isEqualTo("payment_intent.canceled");
        assertThat(stored.getPayload()).isEqualTo(payload);
        assertThat(stored.getProcessedAt()).isNotNull();
        assertThat(new BigDecimal(String.valueOf(webhookEventRepository.count()))).isGreaterThanOrEqualTo(BigDecimal.ONE);
    }
}
