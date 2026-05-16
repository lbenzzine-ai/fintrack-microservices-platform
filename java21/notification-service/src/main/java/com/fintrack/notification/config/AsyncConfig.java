package com.fintrack.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Java 21 — virtual threads for the async notification dispatcher. Each notification spends most
 * of its time blocked on SMTP / HTTP / DB so virtual threads scale better than a fixed pool.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
