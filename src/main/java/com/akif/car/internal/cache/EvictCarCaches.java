package com.akif.car.internal.cache;

import com.akif.shared.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Caching(evict = {
        @CacheEvict(cacheNames = CacheConfig.CARS_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfig.CAR_STATISTICS_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfig.CAR_STATUS_COUNTS_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfig.CAR_BRAND_COUNTS_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfig.CAR_AVERAGE_PRICES_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfig.FILTER_OPTIONS_CACHE, allEntries = true),
        @CacheEvict(cacheNames = CacheConfig.FLEET_STATUS_CACHE, allEntries = true)
})
public @interface EvictCarCaches {
}
