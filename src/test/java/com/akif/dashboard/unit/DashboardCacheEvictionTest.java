package com.akif.dashboard.unit;

import com.akif.dashboard.internal.listener.DashboardEventListener;
import com.akif.dashboard.internal.repository.AlertRepository;
import com.akif.payment.api.PaymentRefundChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Dashboard cache eviction")
class DashboardCacheEvictionTest {

    @Test
    @DisplayName("a refund evicts the revenue caches so refunded money stops counting as collected")
    void refundEvictsRevenueCaches() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("monthlyMetrics", "revenueAnalytics");
        cacheManager.getCache("monthlyMetrics").put("current", "stale");
        cacheManager.getCache("revenueAnalytics").put("current", "stale");
        DashboardEventListener listener = new DashboardEventListener(cacheManager, mock(AlertRepository.class));

        listener.handlePaymentRefundChanged(new PaymentRefundChangedEvent(1L, 2L));

        assertThat(cacheManager.getCache("monthlyMetrics").get("current")).isNull();
        assertThat(cacheManager.getCache("revenueAnalytics").get("current")).isNull();
    }
}
