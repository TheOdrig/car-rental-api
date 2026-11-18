package com.akif.shared.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

@Service
public class RateLimitService {

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private final RateLimitProperties properties;
    private final TimeMeter timeMeter;
    private final Cache<String, Bucket> buckets;

    public RateLimitService(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.timeMeter = new ClockTimeMeter(clock);
        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.getMaxTrackedClients())
                .expireAfterAccess(Duration.ofHours(1))
                .build();
    }

    public Decision tryConsume(RateLimitCategory category, String clientKey) {
        Bucket bucket = buckets.get(category.name() + ":" + clientKey, key -> newBucket(category));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return new Decision(true, 0);
        }
        long retryAfter = Math.max(1L, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
        return new Decision(false, retryAfter);
    }

    public void clear() {
        buckets.invalidateAll();
    }

    private Bucket newBucket(RateLimitCategory category) {
        RateLimitProperties.Limit limit = properties.limitFor(category);
        return Bucket.builder()
                .withCustomTimePrecision(timeMeter)
                .addLimit(bandwidth -> bandwidth.capacity(limit.getCapacity())
                        .refillGreedy(limit.getCapacity(), Duration.ofSeconds(limit.getPeriodSeconds())))
                .build();
    }

    private record ClockTimeMeter(Clock clock) implements TimeMeter {

        @Override
        public long currentTimeNanos() {
            return clock.millis() * 1_000_000L;
        }

        @Override
        public boolean isWallClockBased() {
            return true;
        }
    }
}
