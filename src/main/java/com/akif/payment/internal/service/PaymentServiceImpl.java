package com.akif.payment.internal.service;

import com.akif.payment.api.CollectedRevenue;
import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.DailyRevenue;
import com.akif.payment.api.MonthlyRevenue;
import com.akif.payment.api.NetPaid;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentIntentResult;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentSummary;
import com.akif.shared.enums.CurrencyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class PaymentServiceImpl implements PaymentService {

    private final PaymentLifecycleService lifecycle;
    private final PaymentIntentService intents;
    private final PaymentChargeService charges;
    private final PaymentRefundService refunds;
    private final PaymentRevenueService revenue;

    @Override
    public PaymentDto createPayment(CreatePaymentRequest request) {
        return lifecycle.createPayment(request);
    }

    @Override
    public PaymentIntentResult createOrGetIntentForRental(Long rentalId) {
        return intents.createOrGetIntentForRental(rentalId);
    }

    @Override
    public Optional<PaymentSummary> syncAndGetSummaryForRental(Long rentalId) {
        return intents.syncAndGetSummaryForRental(rentalId);
    }

    @Override
    public Optional<PaymentDto> getPaymentById(Long paymentId) {
        return lifecycle.getPaymentById(paymentId);
    }

    @Override
    public Optional<PaymentDto> getPaymentByRentalId(Long rentalId) {
        return lifecycle.getPaymentByRentalId(rentalId);
    }

    @Override
    public Optional<PaymentDto> getPenaltyPaymentByRentalId(Long rentalId) {
        return lifecycle.getPenaltyPaymentByRentalId(rentalId);
    }

    @Override
    public PaymentResult capturePayment(Long paymentId) {
        return lifecycle.capturePayment(paymentId);
    }

    @Override
    public PaymentResult cancelPayment(Long paymentId) {
        return lifecycle.cancelPayment(paymentId);
    }

    @Override
    public PaymentResult chargePayment(Long paymentId) {
        return charges.chargePayment(paymentId);
    }

    @Override
    public int closeOutstandingPenaltyPayments(Long rentalId) {
        return lifecycle.closeOutstandingPenaltyPayments(rentalId);
    }

    @Override
    public PaymentResult chargeDamage(Long rentalId, Long damageId, BigDecimal amount, CurrencyType currency) {
        return charges.chargeDamage(rentalId, damageId, amount, currency);
    }

    @Override
    public PaymentResult refundPayment(Long paymentId, BigDecimal refundAmount, String operationKey) {
        return refunds.refundPayment(paymentId, refundAmount, operationKey);
    }

    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount, String operationKey) {
        return refunds.refund(transactionId, amount, operationKey);
    }

    @Override
    public List<CollectedRevenue> sumCollectedBetween(Instant start, Instant end) {
        return revenue.sumCollectedBetween(start, end);
    }

    @Override
    public List<NetPaid> sumNetPaidForRentals(Collection<Long> rentalIds) {
        return revenue.sumNetPaidForRentals(rentalIds);
    }

    @Override
    public List<DailyRevenue> getDailyRevenue(int days) {
        return revenue.getDailyRevenue(days);
    }

    @Override
    public List<MonthlyRevenue> getMonthlyRevenue(int months) {
        return revenue.getMonthlyRevenue(months);
    }

    @Override
    public int countFailedPayments() {
        return revenue.countFailedPayments();
    }
}
