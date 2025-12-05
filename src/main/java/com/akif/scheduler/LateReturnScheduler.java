package com.akif.scheduler;

import com.akif.service.detection.ILateReturnDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LateReturnScheduler {

    private final ILateReturnDetectionService lateReturnDetectionService;

    @Scheduled(fixedRate = 900000)
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
