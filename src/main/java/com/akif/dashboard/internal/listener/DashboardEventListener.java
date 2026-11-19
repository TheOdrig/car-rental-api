package com.akif.dashboard.internal.listener;

import com.akif.damage.api.DamageChargedEvent;
import com.akif.dashboard.api.AlertSeverity;
import com.akif.dashboard.api.AlertType;
import com.akif.dashboard.domain.model.Alert;
import com.akif.dashboard.internal.repository.AlertRepository;
import com.akif.payment.api.PaymentAttentionEvent;
import com.akif.payment.api.PenaltyPaymentCapturedEvent;
import com.akif.rental.api.PenaltyChargeFailedEvent;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.payment.api.PaymentRefundChangedEvent;
import com.akif.rental.api.RentalCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardEventListener {

    private static final String PENALTY_NOT_COLLECTED_TITLE = "Late return penalty not collected";
    private static final String MONTHLY_METRICS_CACHE = "monthlyMetrics";
    private static final String REVENUE_ANALYTICS_CACHE = "revenueAnalytics";

    private final CacheManager cacheManager;
    private final AlertRepository alertRepository;

    @ApplicationModuleListener
    public void handleRentalCancelled(RentalCancelledEvent event) {
        log.debug("Received RentalCancelledEvent for rental: {}. Evicting the monthlyMetrics cache.",
                event.rentalId());
        evictCache(MONTHLY_METRICS_CACHE);
    }

    @ApplicationModuleListener
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        log.debug("Received PaymentCapturedEvent for payment: {}, rental: {}. Evicting revenueAnalytics and monthlyMetrics caches.",
                event.paymentId(), event.rentalId());
        evictCache(REVENUE_ANALYTICS_CACHE);
        evictCache(MONTHLY_METRICS_CACHE);
    }

    @ApplicationModuleListener
    public void handlePenaltyCaptured(PenaltyPaymentCapturedEvent event) {
        evictCache(REVENUE_ANALYTICS_CACHE);
        evictCache(MONTHLY_METRICS_CACHE);
    }

    @ApplicationModuleListener
    public void handleDamageCharged(DamageChargedEvent event) {
        evictCache(REVENUE_ANALYTICS_CACHE);
        evictCache(MONTHLY_METRICS_CACHE);
    }

    @ApplicationModuleListener
    public void handlePaymentRefundChanged(PaymentRefundChangedEvent event) {
        evictCache(REVENUE_ANALYTICS_CACHE);
        evictCache(MONTHLY_METRICS_CACHE);
    }

    @ApplicationModuleListener
    public void handlePenaltyChargeFailed(PenaltyChargeFailedEvent event) {
        String reason = event.reason() != null && event.reason().length() > 300
                ? event.reason().substring(0, 300)
                : event.reason();
        String message = String.format(
                "The penalty of %s %s for rental #%d could not be charged (%s). Collect it manually or waive it.",
                event.amount(), event.currency(), event.rentalId(), reason);
        saveAlertOnce(PENALTY_NOT_COLLECTED_TITLE, message, AlertSeverity.HIGH, event.rentalId(), event.paymentId());
    }

    @ApplicationModuleListener
    public void handlePaymentAttention(PaymentAttentionEvent event) {
        String title;
        String message;
        AlertSeverity severity = AlertSeverity.HIGH;
        switch (event.reason()) {
            case CHARGE_OUTCOME_UNKNOWN -> {
                title = "Charge outcome unknown, do not collect manually";
                message = String.format(
                        "The charge of %s %s for rental #%d timed out and its outcome is unknown. Do not collect it manually: "
                                + "the system checks it with the gateway. A charge that completed is kept and recorded as paid, "
                                + "except on a payment already closed by a waiver or as a duplicate, where it is refunded "
                                + "automatically. If the charge never completed, a \"penalty not collected\" alert follows.",
                        event.amount(), event.currency(), event.rentalId());
            }
            case PENALTY_NOT_COLLECTED -> {
                title = PENALTY_NOT_COLLECTED_TITLE;
                message = String.format(
                        "The penalty of %s %s for rental #%d was not collected: the charge never completed at the gateway. "
                                + "Collect it manually or waive it.",
                        event.amount(), event.currency(), event.rentalId());
            }
            case LATE_CAPTURE_REFUNDED -> {
                title = "Charge captured after the payment was closed";
                message = String.format(
                        "A charge of %s %s for rental #%d was captured after its payment was closed and has been refunded automatically.",
                        event.amount(), event.currency(), event.rentalId());
                severity = AlertSeverity.MEDIUM;
            }
            case DUPLICATE_PENALTY_REFUNDED -> {
                title = "Duplicate penalty charge refunded";
                message = String.format(
                        "A second penalty of %s %s was captured for rental #%d and has been refunded automatically.",
                        event.amount(), event.currency(), event.rentalId());
                severity = AlertSeverity.MEDIUM;
            }
            default -> {
                title = "Automatic refund failed";
                message = String.format(
                        "The automatic refund of %s %s for rental #%d failed. Refund it manually in Stripe.",
                        event.amount(), event.currency(), event.rentalId());
            }
        }
        saveAlertOnce(title, message, severity, event.rentalId(), event.paymentId());
        log.info("Handled payment attention {} for rental {}", event.reason(), event.rentalId());
    }

    private void saveAlertOnce(String title, String message, AlertSeverity severity, Long rentalId, Long paymentId) {
        String marker = paymentId == null ? null : "(payment #" + paymentId + ")";
        boolean exists = marker == null
                ? alertRepository.existsByTypeAndReferenceIdAndTitleAndAcknowledgedFalse(
                        AlertType.FAILED_PAYMENT, rentalId, title)
                : alertRepository.existsOpenAlertForPayment(AlertType.FAILED_PAYMENT, rentalId, title, marker);
        if (exists) {
            return;
        }
        alertRepository.save(Alert.builder()
                .type(AlertType.FAILED_PAYMENT)
                .severity(severity)
                .title(title)
                .message(marker == null ? message : message + " " + marker)
                .actionUrl("/admin/rentals/" + rentalId)
                .referenceId(rentalId)
                .acknowledged(false)
                .build());
        log.info("Created alert '{}' for rental {}", title, rentalId);
    }

    private void evictCache(String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Evicted all entries from cache: {}", cacheName);
        } else {
            log.warn("Cache '{}' not found. Skipping eviction.", cacheName);
        }
    }
}
