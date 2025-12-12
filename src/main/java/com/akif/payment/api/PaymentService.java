package com.akif.payment.api;

import com.akif.payment.internal.dto.CheckoutSessionRequest;
import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaymentService {

    // === Gateway Operations ===
    PaymentResult authorize(BigDecimal amount, CurrencyType currency, String customerId);

    PaymentResult capture(String transactionId, BigDecimal amount);

    PaymentResult refund(String transactionId, BigDecimal amount);

    CheckoutSessionResult createCheckoutSession(CheckoutSessionRequest request);

    // === CRUD Operations (Cross-Module API) ===
    
    /**
     * Create a new payment record for a rental.
     * Used by rental module for penalty payments.
     */
    PaymentDto createPayment(CreatePaymentRequest request);
    
    /**
     * Get payment by ID.
     */
    Optional<PaymentDto> getPaymentById(Long paymentId);
    
    /**
     * Get payment by rental ID.
     */
    Optional<PaymentDto> getPaymentByRentalId(Long rentalId);
    
    /**
     * Update payment status after gateway operations.
     */
    PaymentDto updatePaymentStatus(Long paymentId, PaymentStatus status, String transactionId, String failureReason);
    
    /**
     * Charge a pending payment via gateway.
     * Combines authorize + capture for penalty payments.
     */
    PaymentResult chargePayment(Long paymentId, String customerId);
    
    /**
     * Process a refund for a payment and update the payment record.
     * Returns the refund result and updates refundedAmount on the payment.
     */
    PaymentResult refundPayment(Long paymentId, BigDecimal refundAmount);
    
    /**
     * Add refunded amount to a payment and update status if fully refunded.
     */
    PaymentDto addRefundedAmount(Long paymentId, BigDecimal refundAmount, String refundTransactionId);
}
