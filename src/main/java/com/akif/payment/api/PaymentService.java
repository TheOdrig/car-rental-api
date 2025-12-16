package com.akif.payment.api;

import com.akif.payment.internal.dto.CheckoutSessionRequest;
import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

import java.time.*;
import java.util.List;
import java.util.Optional;

public interface PaymentService {

    PaymentResult authorize(BigDecimal amount, CurrencyType currency, String customerId);

    PaymentResult capture(String transactionId, BigDecimal amount);

    PaymentResult refund(String transactionId, BigDecimal amount);

    CheckoutSessionResult createCheckoutSession(CheckoutSessionRequest request);

    PaymentDto createPayment(CreatePaymentRequest request);

    Optional<PaymentDto> getPaymentById(Long paymentId);

    Optional<PaymentDto> getPaymentByRentalId(Long rentalId);

    PaymentDto updatePaymentStatus(Long paymentId, PaymentStatus status, String transactionId, String failureReason);

    PaymentResult chargePayment(Long paymentId, String customerId);

    PaymentResult refundPayment(Long paymentId, BigDecimal refundAmount);

    PaymentDto addRefundedAmount(Long paymentId, BigDecimal refundAmount, String refundTransactionId);


    BigDecimal sumCapturedPaymentsBetween(LocalDateTime start, LocalDateTime end);

    List<DailyRevenueProjection> getDailyRevenue(int days);

    List<MonthlyRevenueProjection> getMonthlyRevenue(int months);

    interface DailyRevenueProjection {
       LocalDate getDate();
        BigDecimal getRevenue();
        int getRentalCount();
    }

    interface MonthlyRevenueProjection {
        YearMonth getMonth();
        BigDecimal getRevenue();
        int getRentalCount();
    }
}

