package com.akif.notification.unit;

import com.akif.notification.internal.config.AsyncConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Async executor MDC propagation")
class AsyncConfigMdcTest {

    @Test
    @DisplayName("async tasks keep the caller's correlation id and do not leak it afterwards")
    void copiesMdcIntoAsyncTasks() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().emailTaskExecutor();
        AtomicReference<String> inside = new AtomicReference<>();
        AtomicReference<String> afterwards = new AtomicReference<>("unset");
        CountDownLatch done = new CountDownLatch(2);
        try {
            MDC.put("correlationId", "abc-123");
            executor.execute(() -> {
                inside.set(MDC.get("correlationId"));
                done.countDown();
            });
            MDC.clear();
            executor.execute(() -> {
                afterwards.set(MDC.get("correlationId"));
                done.countDown();
            });

            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(inside.get()).isEqualTo("abc-123");
            assertThat(afterwards.get()).isNull();
        } finally {
            MDC.clear();
            executor.shutdown();
        }
    }
}
