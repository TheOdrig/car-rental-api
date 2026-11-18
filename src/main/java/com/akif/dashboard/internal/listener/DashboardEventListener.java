package com.akif.dashboard.internal.listener;

import com.akif.damage.api.DamageReportedEvent;
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
