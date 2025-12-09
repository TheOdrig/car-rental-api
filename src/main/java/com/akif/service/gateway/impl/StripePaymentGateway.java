package com.akif.service.gateway.impl;

import com.akif.config.StripeConfig;
import com.akif.shared.enums.CurrencyType;
import com.akif.exception.PaymentFailedException;
import com.akif.model.Payment;
import com.akif.service.gateway.CheckoutSessionResult;
import com.akif.service.gateway.IdempotencyKeyGenerator;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
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
public class StripePaymentGateway implements IPaymentGateway {

    private final StripeConfig stripeConfig;
    private final IdempotencyKeyGenerator idempotencyKeyGenerator;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    public CheckoutSessionResult createCheckoutSession(Payment payment, String customerEmail) {
        LocalDateTime operationTimestamp = LocalDateTime.now();
        Long rentalId = payment.getRental().getId();
        
        log.info("[AUDIT] Payment Operation: CREATE_CHECKOUT_SESSION | Rental ID: {} | Payment ID: {} | Amount: {} {} | Customer Email: {} | Timestamp: {}",
                rentalId, payment.getId(), payment.getAmount(), payment.getCurrency(), customerEmail, operationTimestamp);

        String idempotencyKey = idempotencyKeyGenerator.generateForCheckout(
                rentalId,
                operationTimestamp
        );

        return executeWithRetry(() -> {

            long amountInCents = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("payment_id", payment.getId().toString());
            metadata.put("rental_id", payment.getRental().getId().toString());
            metadata.put("amount", payment.getAmount().toPlainString());
            metadata.put("currency", payment.getCurrency().name());

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(stripeConfig.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(stripeConfig.getCancelUrl())
                    .setCustomerEmail(customerEmail)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(payment.getCurrency().name().toLowerCase())
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Car Rental Payment")
                                                                    .setDescription("Rental ID: " + payment.getRental().getId())
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

            log.info("[AUDIT] Payment Operation: CREATE_CHECKOUT_SESSION | Status: SUCCESS | Rental ID: {} | Payment ID: {} | Session ID: {} | Idempotency Key: {} | Timestamp: {}",
                    rentalId, payment.getId(), session.getId(), idempotencyKey, LocalDateTime.now());

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
