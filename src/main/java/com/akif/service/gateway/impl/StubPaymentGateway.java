package com.akif.service.gateway.impl;

import com.akif.shared.enums.CurrencyType;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@Profile("!prod")
public class StubPaymentGateway implements IPaymentGateway {

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
