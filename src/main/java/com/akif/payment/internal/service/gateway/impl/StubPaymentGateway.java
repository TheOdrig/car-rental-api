package com.akif.payment.internal.service.gateway.impl;

import com.akif.payment.api.CheckoutSessionResult;
import com.akif.shared.enums.CurrencyType;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.payment.api.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@Profile("!prod")
public class StubPaymentGateway implements PaymentGateway {

    private static final long API_SIMULATION_DELAY_MS = 100;

    @Override
    public PaymentResult authorize(BigDecimal amount, CurrencyType currency, String customerId) {
        log.info("🔒 [STUB] Authorizing payment: {} {}, customerId: {}", amount, currency, customerId);

        simulateApiCall();

        String transactionId = generateTransactionId();
        log.info("✅ [STUB] Payment authorized. TransactionId: {}", transactionId);

        return PaymentResult.success(transactionId, "Payment authorized successfully (STUB)");
    }

    @Override
    public PaymentResult capture(String transactionId, BigDecimal amount) {
        log.info("💰 [STUB] Capturing payment: transactionId={}, amount={}", transactionId, amount);

        simulateApiCall();

        log.info("✅ [STUB] Payment captured. TransactionId: {}", transactionId);
        return PaymentResult.success(transactionId, "Payment captured successfully (STUB)");
    }

    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        log.info("↩️ [STUB] Refunding payment: transactionId={}, amount={}", transactionId, amount);

        simulateApiCall();

        String refundTransactionId = generateTransactionId();
        log.info("✅ [STUB] Payment refunded. RefundTransactionId: {}", refundTransactionId);
        return PaymentResult.success(refundTransactionId, "Payment refunded successfully (STUB)");
    }

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
        log.info("🛒 [STUB] Creating checkout session: rentalId={}, amount={} {}, email={}", 
                rentalId, amount, currency, customerEmail);

        simulateApiCall();

        String sessionId = "cs_test_" + generateTransactionId();
        String sessionUrl = "https://checkout.stripe.com/stub/" + sessionId;
        String idempotencyKey = "idem_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("✅ [STUB] Checkout session created. SessionId: {}, URL: {}", sessionId, sessionUrl);

        return new CheckoutSessionResult(sessionId, sessionUrl, idempotencyKey);
    }

    private void simulateApiCall() {
        try {
            Thread.sleep(API_SIMULATION_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("API call simulation interrupted");
        }
    }

    private String generateTransactionId() {
        return "STUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

