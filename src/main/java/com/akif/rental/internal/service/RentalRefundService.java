package com.akif.rental.internal.service;

import com.akif.shared.time.UtcDates;
import java.time.Clock;
import com.akif.payment.api.PaymentFailedException;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.api.EarlyReturnPreview;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentResult;
import com.akif.car.api.DynamicPricingService;
import com.akif.shared.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import com.akif.shared.time.RentalDays;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentalRefundService {

    private final Clock clock;
    private final PaymentService paymentService;
    private final DynamicPricingService dynamicPricingService;
    private final RentalLookups lookups;

    RefundInfo refundPayment(Long rentalId) {
        PaymentDto payment = lookups.findPaymentByRentalId(rentalId);

        if (payment.status() == PaymentStatus.CAPTURED) {
            PaymentResult refundResult = paymentService.refundPayment(payment.id(), payment.amount(), cancelRefundKey(rentalId));

            if (!refundResult.success()) {
                throw new PaymentFailedException(
                        payment.transactionId(),
                        "Refund failed: " + refundResult.message());
            }

            return new RefundInfo(true, payment.amount(), refundResult.transactionId());
        } else if (payment.status() == PaymentStatus.AUTHORIZED
                || payment.status() == PaymentStatus.PENDING
                || payment.status() == PaymentStatus.FAILED) {
            PaymentResult cancelResult = paymentService.cancelPayment(payment.id());
            if (!cancelResult.success()) {
                throw new PaymentFailedException(
                        payment.transactionId(),
                        "Payment cancellation failed: " + cancelResult.message());
            }
            boolean holdReleased = payment.status() == PaymentStatus.AUTHORIZED;
            log.info("Payment cancelled before capture. RentalId: {}, HoldReleased: {}", rentalId, holdReleased);

            return holdReleased
                    ? new RefundInfo(true, payment.amount(), payment.transactionId())
                    : new RefundInfo(false, BigDecimal.ZERO, null);
        } else if (payment.status() == PaymentStatus.REFUNDED) {
            return new RefundInfo(true, payment.amount(), payment.transactionId());
        }

        return new RefundInfo(false, BigDecimal.ZERO, null);
    }

    EarlyReturnPreview quoteEarlyReturn(Rental rental, PaymentDto payment) {
        LocalDate today = LocalDate.now(clock);
        long daysUsed = RentalDays.elapsedInclusiveDays(rental.getStartDate(), today);
        int places = rental.getCurrency().getDecimalPlaces();
        BigDecimal none = BigDecimal.ZERO.setScale(places, RoundingMode.DOWN);
        int unusedDays = (int) Math.max(0, rental.getDays() - daysUsed);

        if (payment.status() != PaymentStatus.CAPTURED) {
            log.warn("Cannot refund payment in status: {} for rental: {}", payment.status(), rental.getId());
            return new EarlyReturnPreview(rental.getId(), none, rental.getCurrency(), unusedDays);
        }

        if (unusedDays == 0) {
            log.info("No remaining days for refund. RentalId: {}", rental.getId());
            return new EarlyReturnPreview(rental.getId(), none, rental.getCurrency(), 0);
        }

        BigDecimal usedPrice = BigDecimal.ZERO;
        if (daysUsed > 0) {
            usedPrice = priceOfDaysUsed(rental, payment.amount(), daysUsed, today, places);
        }
        BigDecimal refundAmount = payment.amount()
                .subtract(usedPrice)
                .max(BigDecimal.ZERO)
                .min(payment.amount())
                .setScale(places, RoundingMode.DOWN);

        log.info("Calculating partial refund: totalDays={}, daysUsed={}, usedPrice={}, amount={}",
                rental.getDays(), daysUsed, usedPrice, refundAmount);

        return new EarlyReturnPreview(rental.getId(), refundAmount, rental.getCurrency(), unusedDays);
    }

    EarlyReturnPreview previewEarlyReturn(Rental rental) {
        return quoteEarlyReturn(rental, lookups.findPaymentByRentalId(rental.getId()));
    }

    RefundInfo refundPartialPayment(Rental rental) {
        PaymentDto payment = lookups.findPaymentByRentalId(rental.getId());
        EarlyReturnPreview quote = quoteEarlyReturn(rental, payment);
        BigDecimal refundAmount = quote.amount();

        if (refundAmount.signum() <= 0) {
            return new RefundInfo(false, BigDecimal.ZERO, null);
        }

        PaymentResult refundResult = paymentService.refundPayment(payment.id(), refundAmount, cancelRefundKey(rental.getId()));

        if (!refundResult.success()) {
            throw new PaymentFailedException(
                    payment.transactionId(),
                    "Partial refund failed: " + refundResult.message());
        }

        return new RefundInfo(true, refundAmount, refundResult.transactionId());
    }

    private BigDecimal priceOfDaysUsed(Rental rental, BigDecimal paid, long daysUsed, LocalDate today, int places) {
        BigDecimal prorated = paid.multiply(BigDecimal.valueOf(daysUsed))
                .divide(BigDecimal.valueOf(rental.getDays()), places, RoundingMode.HALF_UP);
        try {
            if (lookups.findCarById(rental.getCarId()).currency() != rental.getCurrency()) {
                log.warn("Car currency changed since booking, prorating the early return of rental {}", rental.getId());
                return prorated;
            }
            LocalDate bookingDate = rental.getCreateTime() != null ? UtcDates.toDate(rental.getCreateTime()) : today;
            BigDecimal repricedWhole = dynamicPricingService.calculatePrice(
                    rental.getCarId(), rental.getStartDate(), rental.getEndDate(), bookingDate, rental.getId()).finalPrice();
            if (repricedWhole == null || repricedWhole.signum() <= 0) {
                return prorated;
            }
            BigDecimal repricedUsed = dynamicPricingService.calculatePrice(
                    rental.getCarId(), rental.getStartDate(), rental.getStartDate().plusDays(daysUsed - 1),
                    bookingDate, rental.getId()).finalPrice();
            BigDecimal drift = rental.getTotalPrice().divide(repricedWhole, 8, RoundingMode.HALF_UP);
            return repricedUsed.multiply(drift).setScale(places, RoundingMode.HALF_UP);
        } catch (BaseException e) {
            log.warn("Could not reprice the used days of rental {}, prorating: {}", rental.getId(), e.getMessage());
            return prorated;
        }
    }

    private static String cancelRefundKey(Long rentalId) {
        return "rental-" + rentalId + ":cancel-refund";
    }
}
