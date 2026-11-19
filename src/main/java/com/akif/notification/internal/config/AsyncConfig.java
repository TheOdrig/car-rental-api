package com.akif.notification.internal.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

@Getter
@Setter
@Configuration
@EnableAsync
@EnableResilientMethods
@ConfigurationProperties(prefix = "email.async")
public class AsyncConfig implements AsyncConfigurer {

    private int corePoolSize = 2;
    private int maxPoolSize = 4;
    private int queueCapacity = 100;

    @Override
    public Executor getAsyncExecutor() {
        return emailTaskExecutor();
    }

    @Bean("emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("event-");
        executor.setTaskDecorator(runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                if (context == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(context);
                }
                try {
                    runnable.run();
                } finally {
                    if (previous == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(previous);
                    }
                }
            };
        });
        executor.initialize();
        return executor;
    }
}
