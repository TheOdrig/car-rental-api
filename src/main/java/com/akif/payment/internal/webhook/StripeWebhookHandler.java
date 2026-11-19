package com.akif.payment.internal.webhook;

import com.akif.payment.domain.Payment;
import com.akif.payment.domain.PaymentRefund;
import com.akif.payment.domain.WebhookEvent;
import com.akif.payment.domain.enums.WebhookEventStatus;
import com.akif.payment.internal.config.InstallationIdentity;
import com.akif.payment.internal.config.StripeConfig;
import com.akif.payment.internal.exception.WebhookRejectedException;
import com.akif.payment.internal.exception.WebhookSignatureException;
import com.akif.payment.internal.repository.PaymentRefundRepository;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.repository.WebhookEventRepository;
import com.akif.payment.internal.service.PaymentStateService;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.IdempotencyKeyGenerator;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.payment.internal.service.gateway.StripeAmounts;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class StripeWebhookHandler {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final Clock clock;
    private final StripeConfig stripeConfig;
    private final InstallationIdentity installation;
    private final PaymentRepository paymentRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final PaymentStateService stateService;
    private final PaymentRefundRepository refundRepository;
    private final PaymentGateway paymentGateway;
    private final IdempotencyKeyGenerator keys;
    private final TransactionTemplate processingTx;
    private final TransactionTemplate isolatedTx;

    public StripeWebhookHandler(
            Clock clock,
            StripeConfig stripeConfig,
            InstallationIdentity installation,
            PaymentRepository paymentRepository,
            WebhookEventRepository webhookEventRepository,
            PaymentStateService stateService,
            PaymentRefundRepository refundRepository,
            PaymentGateway paymentGateway,
            IdempotencyKeyGenerator keys,
            PlatformTransactionManager transactionManager) {
        this.clock = clock;
        this.stripeConfig = stripeConfig;
        this.installation = installation;
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.stateService = stateService;
        this.refundRepository = refundRepository;
        this.paymentGateway = paymentGateway;
        this.keys = keys;
        this.processingTx = new TransactionTemplate(transactionManager);
        this.isolatedTx = new TransactionTemplate(transactionManager);
        this.isolatedTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void handleWebhookEvent(String payload, String signature) {
        Event event = verify(payload, signature);
        String eventId = event.getId();
        String eventType = event.getType();
        log.info("[AUDIT] Webhook verified | Event: {} | Type: {}", eventId, eventType);

        storeIfAbsent(eventId, eventType, payload);

        try {
            processingTx.executeWithoutResult(status -> processStored(event));
        } catch (WebhookRejectedException rejected) {
            log.error("[AUDIT] Webhook rejected | Event: {} | Type: {} | Reason: {}",
                    eventId, eventType, rejected.getMessage());
            recordFailure(eventId, rejected.getMessage());
        } catch (RuntimeException e) {
            log.error("[AUDIT] Webhook processing failed | Event: {} | Type: {} | Error: {}",
                    eventId, eventType, e.getMessage());
            recordFailure(eventId, e.getMessage());
            throw e;
        }
    }

    private Event verify(String payload, String signature) {
        try {
            return Webhook.constructEvent(
                    payload,
                    signature,
                    stripeConfig.getWebhookSecret(),
                    stripeConfig.getWebhookToleranceSeconds(),
                    clock);
        } catch (SignatureVerificationException e) {
            log.warn("[AUDIT] Webhook signature verification failed");
            throw new WebhookSignatureException("unknown", e);
        } catch (RuntimeException e) {
            log.warn("[AUDIT] Webhook payload could not be parsed");
            throw new WebhookSignatureException("unknown", e);
        }
    }

    private void storeIfAbsent(String eventId, String eventType, String payload) {
        try {
            isolatedTx.executeWithoutResult(status -> webhookEventRepository.saveAndFlush(WebhookEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .payload(payload)
                    .status(WebhookEventStatus.RECEIVED)
                    .build()));
        } catch (DataIntegrityViolationException duplicate) {
            log.info("[AUDIT] Webhook already stored | Event: {}", eventId);
        }
    }

    private void processStored(Event event) {
        WebhookEvent stored = webhookEventRepository.findByEventIdForUpdate(event.getId())
                .orElseThrow(() -> new IllegalStateException("Webhook event row missing: " + event.getId()));

        if (stored.getStatus() == WebhookEventStatus.PROCESSED || stored.getStatus() == WebhookEventStatus.DUPLICATE) {
            log.info("[AUDIT] Webhook duplicate skipped | Event: {} | Type: {}", event.getId(), event.getType());
            return;
        }

        dispatch(event);

        stored.markProcessed(Instant.now(clock));
        webhookEventRepository.save(stored);
        log.info("[AUDIT] Webhook processed | Event: {} | Type: {}", event.getId(), event.getType());
    }

    private void dispatch(Event event) {
        switch (event.getType()) {
            case "payment_intent.amount_capturable_updated",
                 "payment_intent.succeeded",
                 "payment_intent.payment_failed",
                 "payment_intent.canceled" -> handleIntentEvent(event.getType(), (PaymentIntent) object(event));
            case "charge.refunded" -> handleChargeRefunded((Charge) object(event));
            case "refund.failed", "refund.updated" -> handleRefundUpdated((Refund) object(event));
            default -> log.info("[AUDIT] Webhook ignored | Event: {} | Type: {}", event.getId(), event.getType());
        }
    }

    private StripeObject object(Event event) {
        return event.getDataObjectDeserializer().getObject().orElseGet(() -> {
            try {
                return event.getDataObjectDeserializer().deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                throw new WebhookRejectedException("Event data could not be deserialized");
            }
        });
    }

    private void handleIntentEvent(String type, PaymentIntent intent) {
        if (!belongsToThisEnvironment(intent)) {
            log.warn("[AUDIT] Webhook ignored, foreign environment tag | Type: {} | Intent: {}", type, intent.getId());
            return;
        }
        if (hasForeignInstallationTag(intent)) {
            log.warn("[AUDIT] Webhook ignored, foreign installation tag | Type: {} | Intent: {}", type, intent.getId());
            return;
        }
        Located located = locate(type, intent);
        if (located.payment() == null) {
            log.warn("[AUDIT] Webhook has no matching payment | Type: {} | Intent: {}", type, intent.getId());
            return;
        }
        Payment payment = located.payment();
        if (located.stale()) {
            log.warn("[AUDIT] Webhook ignored, intent is not the tracked one | Type: {} | Intent: {} | Payment: {}",
                    type, intent.getId(), payment.getId());
            if ("payment_intent.amount_capturable_updated".equals(type)) {
                cancelOrphan(intent.getId());
            }
            return;
        }

        switch (type) {
            case "payment_intent.amount_capturable_updated" -> {
                requireMatchingAmount(payment, intent);
                stateService.applyIntent(payment, toGatewayIntent(intent));
            }
            case "payment_intent.succeeded" -> {
                requireMatchingAmount(payment, intent);
                stateService.applyIntent(payment, toGatewayIntent(intent));
            }
            case "payment_intent.payment_failed" -> stateService.applyFailure(payment, failureReason(intent));
            case "payment_intent.canceled" -> stateService.applyRemoteCancellation(payment);
            default -> {
            }
        }
    }

    private boolean belongsToThisEnvironment(PaymentIntent intent) {
        String expected = stripeConfig.getMetadataEnvironment();
        String actual = intent.getMetadata() != null ? intent.getMetadata().get("env") : null;
        return expected != null && !expected.isBlank() && expected.equals(actual);
    }

    private boolean belongsToThisInstallation(PaymentIntent intent) {
        String actual = intent.getMetadata() != null ? intent.getMetadata().get("installation_id") : null;
        return actual != null && actual.equals(installation.id());
    }

    private boolean hasForeignInstallationTag(PaymentIntent intent) {
        String actual = intent.getMetadata() != null ? intent.getMetadata().get("installation_id") : null;
        return actual != null && !actual.equals(installation.id());
    }

    private void cancelOrphan(String intentId) {
        try {
            paymentGateway.cancel(intentId, keys.forOrphanCancel(intentId));
        } catch (RuntimeException e) {
            log.error("Could not cancel the untracked payment intent {}: {}", intentId, e.getMessage());
        }
    }

    private void handleChargeRefunded(Charge charge) {
        String intentId = charge.getPaymentIntent();
        if (intentId == null) {
            return;
        }
        Optional<Payment> located = paymentRepository.findByIntentIdForUpdate(intentId);
        if (located.isEmpty()) {
            log.warn("[AUDIT] Refund webhook has no matching payment | Intent: {}", intentId);
            return;
        }
        Payment payment = located.get();
        if (charge.getCurrency() == null
                || !charge.getCurrency().equalsIgnoreCase(payment.getCurrency().name())) {
            throw new WebhookRejectedException("Refund currency does not match the payment");
        }
        long refundedMinor = charge.getAmountRefunded() != null ? charge.getAmountRefunded() : 0L;
        stateService.applyRefundTotal(payment, StripeAmounts.fromMinorUnits(refundedMinor, payment.getCurrency()));
    }

    private void handleRefundUpdated(Refund refund) {
        Optional<PaymentRefund> row = refundRepository.findByStripeRefundId(refund.getId());
        if (row.isEmpty()) {
            log.warn("[AUDIT] Refund webhook has no matching refund | Refund: {}", refund.getId());
            return;
        }
        String status = refund.getStatus();
        boolean failed = "failed".equals(status) || "canceled".equals(status);
        if (!failed) {
            return;
        }
        PaymentRefund stored = row.get();
        Optional<Payment> located = paymentRepository.findByIdForUpdate(stored.getPaymentId());
        if (located.isEmpty()) {
            return;
        }
        if (stored.markFailed()) {
            refundRepository.save(stored);
            stateService.reverseRefund(located.get(), stored.getAmount());
            log.warn("[AUDIT] Refund failed at the gateway | Refund: {} | Payment: {}", refund.getId(), stored.getPaymentId());
        }
    }

    private Located locate(String type, PaymentIntent intent) {
        Optional<Payment> byIntent = paymentRepository.findByIntentIdForUpdate(intent.getId());
        if (byIntent.isPresent()) {
            return new Located(byIntent.get(), false);
        }
        String paymentId = intent.getMetadata() != null ? intent.getMetadata().get("payment_id") : null;
        if (paymentId == null || !belongsToThisInstallation(intent)) {
            return new Located(null, false);
        }
        Optional<Payment> byMetadata;
        try {
            byMetadata = paymentRepository.findByIdForUpdate(Long.valueOf(paymentId));
        } catch (NumberFormatException e) {
            return new Located(null, false);
        }
        if (byMetadata.isEmpty()) {
            return new Located(null, false);
        }
        Payment payment = byMetadata.get();
        boolean fallbackAllowed = payment.getStripePaymentIntentId() == null
                && !"payment_intent.canceled".equals(type);
        return new Located(payment, !fallbackAllowed);
    }

    private record Located(Payment payment, boolean stale) {
    }

    private void requireMatchingAmount(Payment payment, PaymentIntent intent) {
        long expected;
        try {
            expected = StripeAmounts.toMinorUnits(payment.getAmount(), payment.getCurrency());
        } catch (IllegalArgumentException e) {
            throw new WebhookRejectedException("Payment amount " + payment.getId() + " cannot be expressed in minor units");
        }
        boolean amountMatches = intent.getAmount() != null && intent.getAmount() == expected;
        boolean currencyMatches = intent.getCurrency() != null
                && intent.getCurrency().equalsIgnoreCase(payment.getCurrency().name());
        if (!amountMatches || !currencyMatches) {
            throw new WebhookRejectedException("Intent amount or currency does not match payment " + payment.getId());
        }
    }

    private GatewayIntent toGatewayIntent(PaymentIntent intent) {
        return new GatewayIntent(
                intent.getId(),
                null,
                GatewayIntentStatus.fromStripe(intent.getStatus()),
                intent.getPaymentMethod(),
                intent.getCustomer(),
                intent.getAmount() != null ? intent.getAmount() : 0L,
                intent.getCurrency());
    }

    private String failureReason(PaymentIntent intent) {
        if (intent.getLastPaymentError() != null && intent.getLastPaymentError().getMessage() != null) {
            return intent.getLastPaymentError().getMessage();
        }
        return "Payment failed";
    }

    private void recordFailure(String eventId, String message) {
        String trimmed = message == null ? "Processing failed"
                : message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
        try {
            isolatedTx.executeWithoutResult(status -> webhookEventRepository.findByEventId(eventId)
                    .ifPresent(stored -> {
                        stored.markFailed(trimmed);
                        webhookEventRepository.save(stored);
                    }));
        } catch (RuntimeException e) {
            log.error("Could not record webhook failure for event {}", eventId);
        }
    }
}
