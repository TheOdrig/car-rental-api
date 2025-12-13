package com.akif.rental.scheduler;

import com.akif.rental.internal.scheduler.LateReturnScheduler;
import com.akif.rental.internal.service.detection.LateReturnDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LateReturnScheduler Unit Tests")
class LateReturnSchedulerTest {

    @Mock
    private LateReturnDetectionService lateReturnDetectionService;

    @InjectMocks
    private LateReturnScheduler scheduler;

    @Nested
    @DisplayName("Scheduler Execution")
    class SchedulerExecution {

        @Test
        @DisplayName("Should call detection service when scheduler runs")
        void shouldCallDetectionServiceWhenSchedulerRuns() {
            doNothing().when(lateReturnDetectionService).detectLateReturns();

            scheduler.detectLateReturns();

            verify(lateReturnDetectionService, times(1)).detectLateReturns();
        }

        @Test
        @DisplayName("Should handle successful detection")
        void shouldHandleSuccessfulDetection() {
            doNothing().when(lateReturnDetectionService).detectLateReturns();

            scheduler.detectLateReturns();

            verify(lateReturnDetectionService, times(1)).detectLateReturns();
            verifyNoMoreInteractions(lateReturnDetectionService);
        }
    }

    @Nested
    @DisplayName("Error Recovery")
    class ErrorRecovery {

        @Test
        @DisplayName("Should handle exception from detection service gracefully")
        void shouldHandleExceptionFromDetectionServiceGracefully() {
            doThrow(new RuntimeException("Detection failed"))
                    .when(lateReturnDetectionService).detectLateReturns();

            scheduler.detectLateReturns();

            verify(lateReturnDetectionService, times(1)).detectLateReturns();
        }

        @Test
        @DisplayName("Should continue scheduling after error")
        void shouldContinueSchedulingAfterError() {
            doThrow(new RuntimeException("First failure"))
                    .doNothing()
                    .when(lateReturnDetectionService).detectLateReturns();

            scheduler.detectLateReturns();
            scheduler.detectLateReturns();

            verify(lateReturnDetectionService, times(2)).detectLateReturns();
        }

        @Test
        @DisplayName("Should handle null pointer exception")
        void shouldHandleNullPointerException() {
            doThrow(new NullPointerException("Null value"))
                    .when(lateReturnDetectionService).detectLateReturns();

            scheduler.detectLateReturns();

            verify(lateReturnDetectionService, times(1)).detectLateReturns();
        }
    }
}
