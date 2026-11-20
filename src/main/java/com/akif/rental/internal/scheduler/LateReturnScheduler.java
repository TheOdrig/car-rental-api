package com.akif.rental.internal.scheduler;

import com.akif.rental.internal.service.detection.LateReturnDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LateReturnScheduler {

    private final LateReturnDetectionService lateReturnDetectionService;

    @Scheduled(fixedRateString = "${rental.late-return.detection-interval-ms:900000}")
    public void detectLateReturns() {
        log.info("Starting scheduled late return detection job");
        
        try {
            lateReturnDetectionService.detectLateReturns();
            log.info("Scheduled late return detection job completed successfully");
        } catch (Exception e) {
            log.error("Scheduled late return detection job failed: {}", e.getMessage(), e);
        }
    }
}
