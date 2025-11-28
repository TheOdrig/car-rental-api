package com.akif.config;

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

    private static final Map<String, Integer> CACHE_TTL_MINUTES = Map.of(
            EXCHANGE_RATES_CACHE, 60,
            CARS_CACHE, 10,
            CAR_STATISTICS_CACHE, 30,
            CAR_STATUS_COUNTS_CACHE, 30,
            CAR_BRAND_COUNTS_CACHE, 30,
            CAR_AVERAGE_PRICES_CACHE, 30,
            MOST_VIEWED_CARS_CACHE, 15,
            MOST_LIKED_CARS_CACHE, 15
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
