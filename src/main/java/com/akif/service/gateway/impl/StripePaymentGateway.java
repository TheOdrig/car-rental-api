package com.akif.service.gateway.impl;

import com.akif.config.StripeConfig;
import com.akif.enums.CurrencyType;
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
        log.info("Creating Stripe Checkout Session for payment ID: {}, amount: {} {}",
                payment.getId(), payment.getAmount(), payment.getCurrency());

        String idempotencyKey = idempotencyKeyGenerator.generateForCheckout(
                payment.getRental().getId(),
                LocalDateTime.now()
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

            log.info("✅ Stripe Checkout Session created. SessionId: {}, URL: {}",
                    session.getId(), session.getUrl());

            return new CheckoutSessionResult(session.getId(), session.getUrl(), idempotencyKey);

        }, "createCheckoutSession");
    }

    @Override
    public PaymentResult authorize(BigDecimal amount, CurrencyType currency, String customerId) {
        log.info("🔒 Authorizing payment via Stripe: {} {}, customerId: {}", amount, currency, customerId);

        log.warn("Direct authorize() called - Stripe Checkout handles authorization automatically");

        return PaymentResult.success(
                "STRIPE-AUTH-" + System.currentTimeMillis(),
                "Authorization handled by Stripe Checkout"
        );
    }

    @Override
    public PaymentResult capture(String transactionId, BigDecimal amount) {
        log.info("💰 Capturing payment via Stripe: transactionId={}, amount={}", transactionId, amount);

        log.warn("Direct capture() called - Stripe Checkout auto-captures payments");

        return PaymentResult.success(transactionId, "Payment captured automatically by Stripe Checkout");
    }

    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        log.info("↩️ Processing full refund via Stripe: transactionId={}, amount={}", transactionId, amount);

        return executeWithRetry(() -> {
            try {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(transactionId)
                        .setAmount(amountInCents)
                        .build();

                Refund refund = Refund.create(params);

                log.info("✅ Stripe refund processed. RefundId: {}, Status: {}",
                        refund.getId(), refund.getStatus());

                return PaymentResult.success(refund.getId(), "Refund processed successfully");

            } catch (StripeException e) {
                log.error("❌ Stripe refund failed: {}", e.getMessage(), e);
                throw new PaymentFailedException(transactionId, e.getMessage());
            }
        }, "refund");
    }

    public PaymentResult partialRefund(String transactionId, BigDecimal amount) {
        log.info("↩️ Processing partial refund via Stripe: transactionId={}, amount={}", transactionId, amount);

        return executeWithRetry(() -> {
            try {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(transactionId)
                        .setAmount(amountInCents)
                        .build();

                Refund refund = Refund.create(params);

                log.info("✅ Stripe partial refund processed. RefundId: {}, Amount: {}, Status: {}",
                        refund.getId(), amount, refund.getStatus());

                return PaymentResult.success(refund.getId(), "Partial refund processed successfully");

            } catch (StripeException e) {
                log.error("❌ Stripe partial refund failed: {}", e.getMessage(), e);
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
                    log.error("❌ Stripe operation '{}' failed after {} attempts: {}",
                            operationName, MAX_RETRY_ATTEMPTS, e.getMessage());
                    throw new PaymentFailedException(
                            String.format("Stripe operation '%s' failed after %d attempts: %s",
                                    operationName, MAX_RETRY_ATTEMPTS, e.getMessage())
                    );
                }

                if (isRetryableError(e)) {
                    log.warn("⚠️ Stripe operation '{}' failed (attempt {}/{}), retrying in {}ms: {}",
                            operationName, attempt, MAX_RETRY_ATTEMPTS, backoffMs, e.getMessage());

                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new PaymentFailedException("Operation interrupted during retry: " + ie.getMessage());
                    }

                    backoffMs *= 2;
                } else {
                    log.error("❌ Stripe operation '{}' failed with non-retryable error: {}",
                            operationName, e.getMessage());
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
