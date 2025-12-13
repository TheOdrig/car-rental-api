package com.akif.payment.internal.service.gateway.impl;

import com.akif.payment.internal.config.StripeConfig;
import com.akif.shared.enums.CurrencyType;
import com.akif.payment.internal.exception.PaymentFailedException;
import com.akif.payment.api.CheckoutSessionResult;
import com.akif.payment.internal.service.gateway.IdempotencyKeyGenerator;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.payment.api.PaymentResult;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class StripePaymentGateway implements PaymentGateway {

    private final StripeConfig stripeConfig;
    private final IdempotencyKeyGenerator idempotencyKeyGenerator;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    @Override
    public CheckoutSessionResult createCheckoutSession(
            Long rentalId,
            BigDecimal amount,
            CurrencyType currency,
            String customerEmail,
            String description,
            String successUrl,
            String cancelUrl
    ) {
        LocalDateTime operationTimestamp = LocalDateTime.now();
        
        log.info("[AUDIT] Payment Operation: CREATE_CHECKOUT_SESSION | Rental ID: {} | Amount: {} {} | Customer Email: {} | Timestamp: {}",
                rentalId, amount, currency, customerEmail, operationTimestamp);

        String idempotencyKey = idempotencyKeyGenerator.generateForCheckout(
                rentalId,
                operationTimestamp
        );

        String effectiveSuccessUrl = (successUrl != null) ? successUrl : stripeConfig.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}";
        String effectiveCancelUrl = (cancelUrl != null) ? cancelUrl : stripeConfig.getCancelUrl();
        String effectiveDescription = (description != null) ? description : "Rental ID: " + rentalId;

        return executeWithRetry(() -> {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("rental_id", rentalId.toString());
            metadata.put("amount", amount.toPlainString());
            metadata.put("currency", currency.name());

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(effectiveSuccessUrl)
                    .setCancelUrl(effectiveCancelUrl)
                    .setCustomerEmail(customerEmail)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency.name().toLowerCase())
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Car Rental Payment")
                                                                    .setDescription(effectiveDescription)
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .build();

            Session session = Session.create(params);

            log.info("[AUDIT] Payment Operation: CREATE_CHECKOUT_SESSION | Status: SUCCESS | Rental ID: {} | Session ID: {} | Idempotency Key: {} | Timestamp: {}",
                    rentalId, session.getId(), idempotencyKey, LocalDateTime.now());

            return new CheckoutSessionResult(session.getId(), session.getUrl(), idempotencyKey);

        }, "createCheckoutSession");
    }


    @Override
    public PaymentResult authorize(BigDecimal amount, CurrencyType currency, String customerId) {
        LocalDateTime operationTimestamp = LocalDateTime.now();
        
        log.info("[AUDIT] Payment Operation: AUTHORIZE | Amount: {} {} | Customer ID: {} | Timestamp: {}",
                amount, currency, customerId, operationTimestamp);

        log.warn("Direct authorize() called - Stripe Checkout handles authorization automatically");

        String transactionId = "STRIPE-AUTH-" + System.currentTimeMillis();
        
        log.info("[AUDIT] Payment Operation: AUTHORIZE | Status: SUCCESS | Transaction ID: {} | Timestamp: {}",
                transactionId, LocalDateTime.now());

        return PaymentResult.success(transactionId, "Authorization handled by Stripe Checkout");
    }

    @Override
    public PaymentResult capture(String transactionId, BigDecimal amount) {
        LocalDateTime operationTimestamp = LocalDateTime.now();
        
        log.info("[AUDIT] Payment Operation: CAPTURE | Transaction ID: {} | Amount: {} | Timestamp: {}",
                transactionId, amount, operationTimestamp);

        log.warn("Direct capture() called - Stripe Checkout auto-captures payments");

        log.info("[AUDIT] Payment Operation: CAPTURE | Status: SUCCESS | Transaction ID: {} | Timestamp: {}",
                transactionId, LocalDateTime.now());

        return PaymentResult.success(transactionId, "Payment captured automatically by Stripe Checkout");
    }

    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        LocalDateTime operationTimestamp = LocalDateTime.now();
        
        log.info("[AUDIT] Payment Operation: REFUND | Transaction ID: {} | Amount: {} | Timestamp: {}",
                transactionId, amount, operationTimestamp);

        return executeWithRetry(() -> {
            try {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(transactionId)
                        .setAmount(amountInCents)
                        .build();

                Refund refund = Refund.create(params);

                log.info("[AUDIT] Payment Operation: REFUND | Status: SUCCESS | Transaction ID: {} | Refund ID: {} | Amount: {} | Stripe Status: {} | Timestamp: {}",
                        transactionId, refund.getId(), amount, refund.getStatus(), LocalDateTime.now());

                return PaymentResult.success(refund.getId(), "Refund processed successfully");

            } catch (StripeException e) {
                log.error("[AUDIT] Payment Operation: REFUND | Status: FAILED | Transaction ID: {} | Amount: {} | Stripe Error Code: {} | Error Message: {} | Timestamp: {}",
                        transactionId, amount, e.getCode(), e.getMessage(), LocalDateTime.now());
                throw new PaymentFailedException(transactionId, e.getMessage());
            }
        }, "refund");
    }

    public PaymentResult partialRefund(String transactionId, BigDecimal amount) {
        LocalDateTime operationTimestamp = LocalDateTime.now();
        
        log.info("[AUDIT] Payment Operation: PARTIAL_REFUND | Transaction ID: {} | Amount: {} | Timestamp: {}",
                transactionId, amount, operationTimestamp);

        return executeWithRetry(() -> {
            try {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(transactionId)
                        .setAmount(amountInCents)
                        .build();

                Refund refund = Refund.create(params);

                log.info("[AUDIT] Payment Operation: PARTIAL_REFUND | Status: SUCCESS | Transaction ID: {} | Refund ID: {} | Amount: {} | Stripe Status: {} | Timestamp: {}",
                        transactionId, refund.getId(), amount, refund.getStatus(), LocalDateTime.now());

                return PaymentResult.success(refund.getId(), "Partial refund processed successfully");

            } catch (StripeException e) {
                log.error("[AUDIT] Payment Operation: PARTIAL_REFUND | Status: FAILED | Transaction ID: {} | Amount: {} | Stripe Error Code: {} | Error Message: {} | Timestamp: {}",
                        transactionId, amount, e.getCode(), e.getMessage(), LocalDateTime.now());
                throw new PaymentFailedException(transactionId, e.getMessage());
            }
        }, "partialRefund");
    }

    private <T> T executeWithRetry(StripeOperation<T> operation, String operationName) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            try {
                return operation.execute();
            } catch (StripeException e) {
                attempt++;

                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    log.error("[AUDIT] Payment Operation: {} | Status: FAILED_AFTER_RETRIES | Attempts: {} | Stripe Error Code: {} | Error Message: {} | Timestamp: {}",
                            operationName.toUpperCase(), MAX_RETRY_ATTEMPTS, e.getCode(), e.getMessage(), LocalDateTime.now());
                    throw new PaymentFailedException(
                            String.format("Stripe operation '%s' failed after %d attempts: %s",
                                    operationName, MAX_RETRY_ATTEMPTS, e.getMessage())
                    );
                }

                if (isRetryableError(e)) {
                    log.warn("[AUDIT] Payment Operation: {} | Status: RETRY | Attempt: {}/{} | Retry Delay: {}ms | Stripe Error Code: {} | Error Message: {} | Timestamp: {}",
                            operationName.toUpperCase(), attempt, MAX_RETRY_ATTEMPTS, backoffMs, e.getCode(), e.getMessage(), LocalDateTime.now());

                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new PaymentFailedException("Operation interrupted during retry: " + ie.getMessage());
                    }

                    backoffMs *= 2;
                } else {
                    log.error("[AUDIT] Payment Operation: {} | Status: FAILED_NON_RETRYABLE | Stripe Error Code: {} | Error Message: {} | Timestamp: {}",
                            operationName.toUpperCase(), e.getCode(), e.getMessage(), LocalDateTime.now());
                    throw new PaymentFailedException(operationName + " failed: " + e.getMessage());
                }
            }
        }
    }

    private boolean isRetryableError(StripeException e) {

        return e.getStatusCode() == null ||
                e.getStatusCode() == 429 ||
                e.getStatusCode() >= 500;
    }

    @FunctionalInterface
    private interface StripeOperation<T> {
        T execute() throws StripeException;
    }
}
