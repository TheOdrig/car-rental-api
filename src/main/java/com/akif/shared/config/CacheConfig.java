package com.akif.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String EXCHANGE_RATES_CACHE = "exchangeRates";
    public static final String CARS_CACHE = "cars";
    public static final String CAR_STATISTICS_CACHE = "car-statistics";
    public static final String CAR_STATUS_COUNTS_CACHE = "car-status-counts";
    public static final String CAR_BRAND_COUNTS_CACHE = "car-brand-counts";
    public static final String CAR_AVERAGE_PRICES_CACHE = "car-average-prices";
    public static final String MOST_VIEWED_CARS_CACHE = "most-viewed-cars";
    public static final String MOST_LIKED_CARS_CACHE = "most-liked-cars";

    public static final String DAILY_SUMMARY_CACHE = "dailySummary";
    public static final String FLEET_STATUS_CACHE = "fleetStatus";
    public static final String MONTHLY_METRICS_CACHE = "monthlyMetrics";
    public static final String REVENUE_ANALYTICS_CACHE = "revenueAnalytics";

    private static final Map<String, Integer> CACHE_TTL_MINUTES = Map.ofEntries(
            Map.entry(EXCHANGE_RATES_CACHE, 60),
            Map.entry(CARS_CACHE, 10),
            Map.entry(CAR_STATISTICS_CACHE, 30),
            Map.entry(CAR_STATUS_COUNTS_CACHE, 30),
            Map.entry(CAR_BRAND_COUNTS_CACHE, 30),
            Map.entry(CAR_AVERAGE_PRICES_CACHE, 30),
            Map.entry(MOST_VIEWED_CARS_CACHE, 15),
            Map.entry(MOST_LIKED_CARS_CACHE, 15),

            Map.entry(DAILY_SUMMARY_CACHE, 5),
            Map.entry(FLEET_STATUS_CACHE, 5),
            Map.entry(MONTHLY_METRICS_CACHE, 15),
            Map.entry(REVENUE_ANALYTICS_CACHE, 15)
    );

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager() {
            @Override
            @NonNull
            protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache(@NonNull String name) {
                Integer ttlMinutes = CACHE_TTL_MINUTES.getOrDefault(name, 10);
                return Caffeine.newBuilder()
                        .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .recordStats()
                        .build();
            }
        };
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}
