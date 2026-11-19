package com.akif.payment.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

import java.time.*;
import java.util.List;
import java.util.Optional;

public interface PaymentService {

    PaymentDto createPayment(CreatePaymentRequest request);

    PaymentIntentResult createOrGetIntentForRental(Long rentalId);

    Optional<PaymentSummary> syncAndGetSummaryForRental(Long rentalId);

    Optional<PaymentDto> getPaymentById(Long paymentId);

    Optional<PaymentDto> getPaymentByRentalId(Long rentalId);

    Optional<PaymentDto> getPenaltyPaymentByRentalId(Long rentalId);

    PaymentResult capturePayment(Long paymentId);

    PaymentResult cancelPayment(Long paymentId);

    PaymentResult chargePayment(Long paymentId);

    int closeOutstandingPenaltyPayments(Long rentalId);

    PaymentResult chargeDamage(Long rentalId, Long damageId, BigDecimal amount, CurrencyType currency);

    PaymentResult refundPayment(Long paymentId, BigDecimal refundAmount, String operationKey);

    PaymentResult refund(String transactionId, BigDecimal amount, String operationKey);

    BigDecimal sumCapturedPaymentsBetween(LocalDateTime start, LocalDateTime end);

    List<DailyRevenueProjection> getDailyRevenue(int days);

    List<MonthlyRevenueProjection> getMonthlyRevenue(int months);

    int countFailedPayments();

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
