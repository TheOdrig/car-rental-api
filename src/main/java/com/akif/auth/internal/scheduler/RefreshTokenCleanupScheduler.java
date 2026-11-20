package com.akif.auth.internal.scheduler;

import com.akif.auth.internal.config.RefreshTokenProperties;
import com.akif.auth.internal.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenProperties properties;

    @Scheduled(cron = "${auth.refresh-token.cleanup-cron:0 30 3 * * *}")
    public void purgeExpiredTokens() {
        int removed = refreshTokenService.purgeExpired(Duration.ofDays(properties.retentionDays()));
        log.info("Purged {} expired refresh tokens", removed);
    }
}
