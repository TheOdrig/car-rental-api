package com.akif.dashboard.internal.listener;

import com.akif.damage.api.DamageChargedEvent;
import com.akif.damage.api.DamageReportedEvent;
import com.akif.dashboard.api.AlertSeverity;
import com.akif.dashboard.api.AlertType;
import com.akif.dashboard.domain.model.Alert;
import com.akif.dashboard.internal.repository.AlertRepository;
import com.akif.payment.api.PenaltyPaymentCapturedEvent;
import com.akif.rental.api.PenaltyChargeFailedEvent;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.rental.api.RentalCancelledEvent;
import com.akif.rental.api.RentalConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardEventListener {

    private static final String DAILY_SUMMARY_CACHE = "dailySummary";
    private static final String FLEET_STATUS_CACHE = "fleetStatus";
    private static final String MONTHLY_METRICS_CACHE = "monthlyMetrics";
    private static final String REVENUE_ANALYTICS_CACHE = "revenueAnalytics";

    private final CacheManager cacheManager;
    private final AlertRepository alertRepository;

    @ApplicationModuleListener
    public void handleRentalConfirmed(RentalConfirmedEvent event) {
        log.debug("Received RentalConfirmedEvent for rental: {}. Evicting dailySummary and fleetStatus caches.",
                event.rentalId());
        evictCache(DAILY_SUMMARY_CACHE);
        evictCache(FLEET_STATUS_CACHE);
    }

    @ApplicationModuleListener
    public void handleRentalCancelled(RentalCancelledEvent event) {
        log.debug("Received RentalCancelledEvent for rental: {}. Evicting dailySummary, fleetStatus, and monthlyMetrics caches.",
                event.rentalId());
        evictCache(DAILY_SUMMARY_CACHE);
        evictCache(FLEET_STATUS_CACHE);
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
    public void handlePenaltyChargeFailed(PenaltyChargeFailedEvent event) {
        if (alertRepository.existsByTypeAndReferenceIdAndAcknowledgedFalse(AlertType.FAILED_PAYMENT, event.rentalId())) {
            return;
        }
        String reason = event.reason() != null && event.reason().length() > 300
                ? event.reason().substring(0, 300)
                : event.reason();
        alertRepository.save(Alert.builder()
                .type(AlertType.FAILED_PAYMENT)
                .severity(AlertSeverity.HIGH)
                .title("Late return penalty not collected")
                .message(String.format(
                        "The penalty of %s %s for rental #%d could not be charged (%s). Collect it manually or waive it.",
                        event.amount(), event.currency(), event.rentalId(), reason))
                .actionUrl("/admin/rentals/" + event.rentalId())
                .referenceId(event.rentalId())
                .acknowledged(false)
                .build());
        log.info("Created alert for the uncollected penalty of rental {}", event.rentalId());
    }

    @ApplicationModuleListener
    public void handleDamageReported(DamageReportedEvent event) {
        log.debug("Received DamageReportedEvent for damage: {}, rental: {}. Evicting dailySummary cache.",
                event.damageReportId(), event.rentalId());
        evictCache(DAILY_SUMMARY_CACHE);
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
