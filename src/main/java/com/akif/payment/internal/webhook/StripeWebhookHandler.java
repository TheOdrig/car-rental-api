package com.akif.payment.internal.webhook;

import com.akif.payment.internal.config.StripeConfig;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.enums.WebhookEventStatus;
import com.akif.payment.internal.exception.WebhookSignatureException;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.WebhookEvent;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.repository.WebhookEventRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookHandler {

    private final StripeConfig stripeConfig;
    private final PaymentRepository paymentRepository;
    private final WebhookEventRepository webhookEventRepository;

    @Transactional
    public void handleWebhookEvent(String payload, String signature) {
        LocalDateTime receivedTimestamp = LocalDateTime.now();
        
        log.info("[AUDIT] Webhook Event: RECEIVED | Timestamp: {}", receivedTimestamp);

        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, stripeConfig.getWebhookSecret());
            log.info("[AUDIT] Webhook Event: SIGNATURE_VERIFIED | Event ID: {} | Event Type: {} | Timestamp: {}",
                    event.getId(), event.getType(), LocalDateTime.now());
        } catch (SignatureVerificationException e) {
            log.error("[AUDIT] Webhook Event: SIGNATURE_VERIFICATION_FAILED | Error: {} | Timestamp: {}",
                    e.getMessage(), LocalDateTime.now());
            throw new WebhookSignatureException("unknown", e);
        }

        String eventId = event.getId();
        String eventType = event.getType();

        log.info("[AUDIT] Webhook Event: PROCESSING_STARTED | Event ID: {} | Event Type: {} | Timestamp: {}",
                eventId, eventType, LocalDateTime.now());

        if (isEventAlreadyProcessed(eventId)) {
            log.info("[AUDIT] Webhook Event: DUPLICATE_DETECTED | Event ID: {} | Event Type: {} | Processing Result: SKIPPED | Timestamp: {}",
                    eventId, eventType, LocalDateTime.now());
            return;
        }

        WebhookEvent webhookEvent = WebhookEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .payload(payload)
                .status(WebhookEventStatus.PROCESSING)
                .build();
        webhookEventRepository.save(webhookEvent);

        try {
            switch (eventType) {
                case "checkout.session.completed":
                    Session completedSession = (Session) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new IllegalStateException("Failed to deserialize session"));
                    processCheckoutSessionCompleted(completedSession);
                    break;

                case "checkout.session.expired":
                    Session expiredSession = (Session) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new IllegalStateException("Failed to deserialize session"));
                    processCheckoutSessionExpired(expiredSession);
                    break;

                case "payment_intent.payment_failed":
                    PaymentIntent failedIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new IllegalStateException("Failed to deserialize payment intent"));
                    processPaymentIntentFailed(failedIntent);
                    break;

                default:
                    log.warn("Unhandled event type: {}", eventType);
            }

            webhookEvent.setStatus(WebhookEventStatus.PROCESSED);
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);

            log.info("[AUDIT] Webhook Event: PROCESSING_COMPLETED | Event ID: {} | Event Type: {} | Processing Result: SUCCESS | Timestamp: {}",
                    eventId, eventType, LocalDateTime.now());

        } catch (Exception e) {
            log.error("[AUDIT] Webhook Event: PROCESSING_FAILED | Event ID: {} | Event Type: {} | Processing Result: FAILED | Error: {} | Timestamp: {}",
                    eventId, eventType, e.getMessage(), LocalDateTime.now());
            webhookEvent.setStatus(WebhookEventStatus.FAILED);
            webhookEvent.setErrorMessage(e.getMessage());
            webhookEventRepository.save(webhookEvent);
            throw e;
        }
    }

    public void processCheckoutSessionCompleted(Session session) {
        String sessionId = session.getId();
        String paymentIntentId = session.getPaymentIntent();
        
        log.info("[AUDIT] Webhook Event: checkout.session.completed | Session ID: {} | Payment Intent ID: {} | Processing: STARTED | Timestamp: {}",
                sessionId, paymentIntentId, LocalDateTime.now());

        Payment payment = findPaymentBySessionId(sessionId);
        Long paymentId = payment.getId();
        Long rentalId = payment.getRentalId();
        
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setStripePaymentIntentId(paymentIntentId);
        payment.setTransactionId(paymentIntentId);
        
        paymentRepository.save(payment);
        
        log.info("[AUDIT] Webhook Event: checkout.session.completed | Session ID: {} | Payment ID: {} | Rental ID: {} | Status Updated: CAPTURED | Payment Intent ID: {} | Timestamp: {}",
                sessionId, paymentId, rentalId, paymentIntentId, LocalDateTime.now());
    }

    public void processCheckoutSessionExpired(Session session) {
        String sessionId = session.getId();
        
        log.info("[AUDIT] Webhook Event: checkout.session.expired | Session ID: {} | Processing: STARTED | Timestamp: {}",
                sessionId, LocalDateTime.now());

        Payment payment = findPaymentBySessionId(sessionId);
        Long paymentId = payment.getId();
        Long rentalId = payment.getRentalId();
        
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Checkout session expired");
        
        paymentRepository.save(payment);
        
        log.info("[AUDIT] Webhook Event: checkout.session.expired | Session ID: {} | Payment ID: {} | Rental ID: {} | Status Updated: FAILED | Failure Reason: Checkout session expired | Timestamp: {}",
                sessionId, paymentId, rentalId, LocalDateTime.now());
    }

    public void processPaymentIntentFailed(PaymentIntent paymentIntent) {
        String paymentIntentId = paymentIntent.getId();
        
        log.info("[AUDIT] Webhook Event: payment_intent.payment_failed | Payment Intent ID: {} | Processing: STARTED | Timestamp: {}",
                paymentIntentId, LocalDateTime.now());

        Payment payment = findPaymentByPaymentIntentId(paymentIntentId);
        Long paymentId = payment.getId();
        Long rentalId = payment.getRentalId();
        
        payment.setStatus(PaymentStatus.FAILED);

        String failureReason = "Payment failed";
        String stripeErrorCode = null;
        if (paymentIntent.getLastPaymentError() != null) {
            failureReason = paymentIntent.getLastPaymentError().getMessage();
            stripeErrorCode = paymentIntent.getLastPaymentError().getCode();
        }
        payment.setFailureReason(failureReason);
        
        paymentRepository.save(payment);
        
        log.info("[AUDIT] Webhook Event: payment_intent.payment_failed | Payment Intent ID: {} | Payment ID: {} | Rental ID: {} | Status Updated: FAILED | Stripe Error Code: {} | Failure Reason: {} | Timestamp: {}",
                paymentIntentId, paymentId, rentalId, stripeErrorCode, failureReason, LocalDateTime.now());
    }

    public boolean isEventAlreadyProcessed(String eventId) {
        Optional<WebhookEvent> existingEvent = webhookEventRepository.findByEventId(eventId);
        
        if (existingEvent.isPresent()) {
            WebhookEventStatus status = existingEvent.get().getStatus();

            if (status == WebhookEventStatus.PROCESSED || 
                status == WebhookEventStatus.PROCESSING ||
                status == WebhookEventStatus.DUPLICATE) {

                if (status == WebhookEventStatus.PROCESSED) {
                    WebhookEvent event = existingEvent.get();
                    event.setStatus(WebhookEventStatus.DUPLICATE);
                    webhookEventRepository.save(event);
                }
                
                return true;
            }
        }
        
        return false;
    }

    private Payment findPaymentBySessionId(String sessionId) {
        return paymentRepository.findByStripeSessionIdAndIsDeletedFalse(sessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found for session: " + sessionId));
    }

    private Payment findPaymentByPaymentIntentId(String paymentIntentId) {
        return paymentRepository.findByStripePaymentIntentIdAndIsDeletedFalse(paymentIntentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found for payment intent: " + paymentIntentId));
    }
}
