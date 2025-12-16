package com.akif.payment.internal.service;

import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.internal.dto.CheckoutSessionRequest;
import com.akif.payment.api.CheckoutSessionResult;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.mapper.PaymentMapper;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.shared.enums.CurrencyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.stream.Collectors;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PaymentServiceImpl implements PaymentService {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;


    @Override
    public PaymentResult authorize(BigDecimal amount, CurrencyType currency, String customerId) {
        log.info("Authorizing payment: amount={}, currency={}, customerId={}", amount, currency, customerId);
        return paymentGateway.authorize(amount, currency, customerId);
    }

    @Override
    public PaymentResult capture(String transactionId, BigDecimal amount) {
        log.info("Capturing payment: transactionId={}, amount={}", transactionId, amount);
        return paymentGateway.capture(transactionId, amount);
    }

    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        log.info("Refunding payment: transactionId={}, amount={}", transactionId, amount);
        return paymentGateway.refund(transactionId, amount);
    }

    @Override
    public CheckoutSessionResult createCheckoutSession(CheckoutSessionRequest request) {
        log.info("Creating checkout session for rental: rentalId={}, amount={} {}", 
                request.rentalId(), request.amount(), request.currency());
        
        return paymentGateway.createCheckoutSession(
                request.rentalId(),
                request.amount(),
                request.currency(),
                request.customerEmail(),
                request.description(),
                request.successUrl(),
                request.cancelUrl()
        );
    }


    @Override
    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        log.debug("Creating payment for rental: {} with amount: {} {}", 
                request.rentalId(), request.amount(), request.currency());

        Payment payment = Payment.builder()
                .rentalId(request.rentalId())
                .userEmail(request.userEmail())
                .carLicensePlate(request.carLicensePlate())
                .amount(request.amount())
                .currency(request.currency())
                .status(PaymentStatus.PENDING)
                .paymentMethod(request.paymentMethod())
                .isDeleted(false)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        
        log.info("Created payment: ID={}, Rental ID={}, Amount={} {}", 
                savedPayment.getId(), request.rentalId(), request.amount(), request.currency());

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    public Optional<PaymentDto> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .filter(p -> !p.getIsDeleted())
                .map(paymentMapper::toDto);
    }

    @Override
    public Optional<PaymentDto> getPaymentByRentalId(Long rentalId) {
        return paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                .map(paymentMapper::toDto);
    }

    @Override
    @Transactional
    public PaymentDto updatePaymentStatus(Long paymentId, PaymentStatus status, String transactionId, String failureReason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        payment.updateStatus(status);
        if (transactionId != null) {
            payment.setTransactionId(transactionId);
        }
        if (failureReason != null) {
            payment.setFailureReason(failureReason);
        }
        
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Updated payment status: ID={}, Status={}", paymentId, status);
        
        return paymentMapper.toDto(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResult chargePayment(Long paymentId, String customerId) {
        log.debug("Charging payment: paymentId={}, customerId={}", paymentId, customerId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        try {
            PaymentResult authorizeResult = paymentGateway.authorize(
                    payment.getAmount(),
                    payment.getCurrency(),
                    customerId
            );

            if (!authorizeResult.success()) {
                log.warn("Failed to authorize payment: ID={}, Reason: {}", paymentId, authorizeResult.message());
                payment.updateStatus(PaymentStatus.FAILED);
                payment.setFailureReason(authorizeResult.message());
                payment.setGatewayResponse(authorizeResult.message());
                paymentRepository.save(payment);
                return authorizeResult;
            }

            PaymentResult captureResult = paymentGateway.capture(
                    authorizeResult.transactionId(),
                    payment.getAmount()
            );

            if (captureResult.success()) {
                payment.updateStatus(PaymentStatus.CAPTURED);
                payment.setTransactionId(captureResult.transactionId());
                payment.setGatewayResponse(captureResult.message());
                paymentRepository.save(payment);
                log.info("Successfully charged payment: ID={}, Transaction ID={}", paymentId, captureResult.transactionId());
            } else {
                payment.updateStatus(PaymentStatus.FAILED);
                payment.setFailureReason(captureResult.message());
                payment.setGatewayResponse(captureResult.message());
                paymentRepository.save(payment);
                log.warn("Failed to capture payment: ID={}, Reason: {}", paymentId, captureResult.message());
            }
            
            return captureResult;
            
        } catch (Exception e) {
            log.error("Exception while charging payment: ID={}, Error: {}", paymentId, e.getMessage(), e);
            payment.updateStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);
            return PaymentResult.failure("Payment gateway error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResult refundPayment(Long paymentId, BigDecimal refundAmount) {
        log.debug("Processing refund for payment: paymentId={}, amount={}", paymentId, refundAmount);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (!payment.canRefund()) {
            log.warn("Payment {} cannot be refunded, status: {}", paymentId, payment.getStatus());
            return PaymentResult.failure("Payment cannot be refunded, current status: " + payment.getStatus());
        }

        if (payment.getTransactionId() == null || payment.getTransactionId().isEmpty()) {
            log.warn("Payment {} has no transaction ID, cannot process refund", paymentId);
            return PaymentResult.failure("Payment has no transaction ID");
        }

        try {
            PaymentResult refundResult = paymentGateway.refund(payment.getTransactionId(), refundAmount);
            
            if (refundResult.success()) {
                BigDecimal currentRefunded = payment.getRefundedAmount() != null 
                        ? payment.getRefundedAmount() 
                        : BigDecimal.ZERO;
                payment.setRefundedAmount(currentRefunded.add(refundAmount));

                if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
                    payment.updateStatus(PaymentStatus.REFUNDED);
                }
                
                paymentRepository.save(payment);
                log.info("Successfully processed refund for payment: ID={}, amount={}, transaction={}", 
                        paymentId, refundAmount, refundResult.transactionId());
            } else {
                log.error("Refund failed for payment: ID={}, reason: {}", paymentId, refundResult.message());
            }
            
            return refundResult;
            
        } catch (Exception e) {
            log.error("Error processing refund for payment: ID={}", paymentId, e);
            return PaymentResult.failure("Refund error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentDto addRefundedAmount(Long paymentId, BigDecimal refundAmount, String refundTransactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        BigDecimal currentRefunded = payment.getRefundedAmount() != null 
                ? payment.getRefundedAmount() 
                : BigDecimal.ZERO;
        payment.setRefundedAmount(currentRefunded.add(refundAmount));

        if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
            payment.updateStatus(PaymentStatus.REFUNDED);
        }
        
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Added refunded amount to payment: ID={}, refundAmount={}, totalRefunded={}", 
                paymentId, refundAmount, savedPayment.getRefundedAmount());
        
        return paymentMapper.toDto(savedPayment);
    }


    @Override
    public BigDecimal sumCapturedPaymentsBetween(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.sumCapturedPaymentsBetween(start, end);
    }

    @Override
    public List<DailyRevenueProjection> getDailyRevenue(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Payment> payments = paymentRepository.findByStatusAndCreateTimeAfter(
                PaymentStatus.CAPTURED, startDate);

        Map<LocalDate, List<Payment>> grouped = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCreateTime().toLocalDate()));
        
        return grouped.entrySet().stream()
                .map(entry -> new DailyRevenueProjectionImpl(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size()))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MonthlyRevenueProjection> getMonthlyRevenue(int months) {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
        List<Payment> payments = paymentRepository.findByStatusAndCreateTimeAfter(
                PaymentStatus.CAPTURED, startDate);

        Map<YearMonth, List<Payment>> grouped = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> YearMonth.from(p.getCreateTime())));
        
        return grouped.entrySet().stream()
                .map(entry -> new MonthlyRevenueProjectionImpl(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size()))
                .sorted((a, b) -> a.getMonth().compareTo(b.getMonth()))
                .collect(Collectors.toList());
    }

    private record DailyRevenueProjectionImpl(
            LocalDate date,
            BigDecimal revenue, 
            int rentalCount) implements DailyRevenueProjection {
        @Override
        public LocalDate getDate() { return date; }
        @Override
        public BigDecimal getRevenue() { return revenue; }
        @Override
        public int getRentalCount() { return rentalCount; }
    }

    private record MonthlyRevenueProjectionImpl(
            YearMonth month,
            BigDecimal revenue, 
            int rentalCount) implements MonthlyRevenueProjection {
        @Override
        public YearMonth getMonth() { return month; }
        @Override
        public BigDecimal getRevenue() { return revenue; }
        @Override
        public int getRentalCount() { return rentalCount; }
    }
}