package com.srihari.ai.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.srihari.ai.configuration.GracefulShutdownConfiguration.GracefulShutdownHandler;

import lombok.RequiredArgsConstructor;

/**
 * Health indicator that reports application shutdown status.
 * Returns DOWN when graceful shutdown is in progress to help load balancers
 * stop routing traffic to this instance.
 */
@Component
@RequiredArgsConstructor
public class ShutdownHealthIndicator implements HealthIndicator {

    private final GracefulShutdownHandler shutdownHandler;

    @Override
    public Health health() {
        boolean shutdownInProgress = shutdownHandler.isShutdownInProgress() || 
                                   "true".equals(System.getProperty("app.shutdown.in.progress"));

        if (shutdownInProgress) {
            return Health.down()
                    .withDetail("status", "shutdown_in_progress")
                    .withDetail("message", "Application is shutting down gracefully")
                    .withDetail("accepting_requests", false)
                    .build();
        }

        return Health.up()
                .withDetail("status", "running")
                .withDetail("message", "Application is running normally")
                .withDetail("accepting_requests", true)
                .build();
    }
}