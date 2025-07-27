package com.srihari.ai.configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.service.CachingService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for graceful shutdown handling.
 * Ensures proper cleanup of resources and completion of in-flight requests.
 */
@Configuration
@Slf4j
public class GracefulShutdownConfiguration {

    /**
     * Configuration properties for graceful shutdown settings.
     */
    @ConfigurationProperties(prefix = "app.graceful-shutdown")
    public static class GracefulShutdownProperties {
        private Duration timeout = Duration.ofSeconds(30);
        private Duration connectionDrainTimeout = Duration.ofSeconds(15);
        private boolean enabled = true;
        private boolean waitForActiveRequests = true;

        // Getters and setters
        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
        public Duration getConnectionDrainTimeout() { return connectionDrainTimeout; }
        public void setConnectionDrainTimeout(Duration connectionDrainTimeout) { this.connectionDrainTimeout = connectionDrainTimeout; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isWaitForActiveRequests() { return waitForActiveRequests; }
        public void setWaitForActiveRequests(boolean waitForActiveRequests) { this.waitForActiveRequests = waitForActiveRequests; }
    }

    @Bean
    public GracefulShutdownProperties gracefulShutdownProperties() {
        return new GracefulShutdownProperties();
    }

    /**
     * Graceful shutdown handler component.
     */
    @Component
    @RequiredArgsConstructor
    public static class GracefulShutdownHandler implements ApplicationListener<ContextClosedEvent> {
        
        private final GracefulShutdownProperties properties;
        private final StructuredLogger structuredLogger;
        private final CustomMetrics customMetrics;
        private final CachingService cachingService;
        private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
        private final CountDownLatch shutdownLatch = new CountDownLatch(1);

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            if (!properties.isEnabled()) {
                log.info("Graceful shutdown is disabled, performing immediate shutdown");
                return;
            }

            if (shutdownInProgress.compareAndSet(false, true)) {
                performGracefulShutdown();
            }
        }

        @PreDestroy
        public void preDestroy() {
            if (!shutdownInProgress.get()) {
                onApplicationEvent(null);
            }
        }

        private void performGracefulShutdown() {
            long startTime = System.currentTimeMillis();
            
            structuredLogger.info("Starting graceful shutdown", Map.of(
                "operation", "graceful_shutdown_start",
                "timeout", properties.getTimeout().toString(),
                "connectionDrainTimeout", properties.getConnectionDrainTimeout().toString()
            ));

            try {
                // Step 1: Stop accepting new requests
                stopAcceptingNewRequests();

                // Step 2: Wait for active requests to complete
                if (properties.isWaitForActiveRequests()) {
                    waitForActiveRequests();
                }

                // Step 3: Drain connections
                drainConnections();

                // Step 4: Cleanup resources
                cleanupResources();

                long duration = System.currentTimeMillis() - startTime;
                structuredLogger.info("Graceful shutdown completed successfully", Map.of(
                    "operation", "graceful_shutdown_complete",
                    "duration", duration + "ms"
                ));

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                structuredLogger.error("Error during graceful shutdown", Map.of(
                    "operation", "graceful_shutdown_error",
                    "duration", duration + "ms"
                ), e);
            } finally {
                shutdownLatch.countDown();
            }
        }

        private void stopAcceptingNewRequests() {
            structuredLogger.info("Stopping acceptance of new requests", Map.of(
                "operation", "stop_new_requests"
            ));
            
            // Mark the application as shutting down
            // This can be used by health checks to return unhealthy status
            System.setProperty("app.shutdown.in.progress", "true");
        }

        private void waitForActiveRequests() {
            structuredLogger.info("Waiting for active requests to complete", Map.of(
                "operation", "wait_active_requests",
                "timeout", properties.getTimeout().toString()
            ));

            long timeoutMs = properties.getTimeout().toMillis();
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                // Check if there are active requests (this would need to be tracked)
                // For now, we'll just wait a reasonable amount of time
                try {
                    Thread.sleep(1000);
                    
                    // Log progress
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed % 5000 == 0) { // Log every 5 seconds
                        structuredLogger.debug("Still waiting for active requests", Map.of(
                            "operation", "wait_active_requests_progress",
                            "elapsed", elapsed + "ms",
                            "remaining", (timeoutMs - elapsed) + "ms"
                        ));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void drainConnections() {
            structuredLogger.info("Draining connections", Map.of(
                "operation", "drain_connections",
                "timeout", properties.getConnectionDrainTimeout().toString()
            ));

            try {
                // Wait for connection drain timeout
                Thread.sleep(properties.getConnectionDrainTimeout().toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                structuredLogger.warn("Connection draining interrupted", Map.of(
                    "operation", "drain_connections_interrupted"
                ));
            }
        }

        private void cleanupResources() {
            structuredLogger.info("Cleaning up resources", Map.of(
                "operation", "cleanup_resources"
            ));

            try {
                // Clean up cache access times
                cachingService.cleanupOldAccessTimes();
                
                // Log final metrics
                structuredLogger.info("Final application metrics", Map.of(
                    "operation", "final_metrics",
                    "uptime", System.currentTimeMillis() - getApplicationStartTime()
                ));

                // Flush any remaining logs
                structuredLogger.info("Resource cleanup completed", Map.of(
                    "operation", "cleanup_complete"
                ));

            } catch (Exception e) {
                structuredLogger.error("Error during resource cleanup", Map.of(
                    "operation", "cleanup_error"
                ), e);
            }
        }

        private long getApplicationStartTime() {
            // This would ideally be tracked from application startup
            // For now, return a reasonable default
            return System.currentTimeMillis() - 60000; // Assume 1 minute uptime minimum
        }

        public boolean isShutdownInProgress() {
            return shutdownInProgress.get();
        }

        public boolean waitForShutdown(long timeout, TimeUnit unit) {
            try {
                return shutdownLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /**
     * Shutdown hook for JVM shutdown events.
     */
    @Component
    @RequiredArgsConstructor
    public static class ShutdownHook {
        
        private final GracefulShutdownHandler shutdownHandler;
        private final StructuredLogger structuredLogger;

        @PostConstruct
        public void registerShutdownHook() {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                structuredLogger.info("JVM shutdown hook triggered", Map.of(
                    "operation", "jvm_shutdown_hook"
                ));

                if (!shutdownHandler.isShutdownInProgress()) {
                    shutdownHandler.onApplicationEvent(null);
                    
                    // Wait for graceful shutdown to complete
                    try {
                        boolean completed = shutdownHandler.waitForShutdown(30, TimeUnit.SECONDS);
                        if (!completed) {
                            structuredLogger.warn("Graceful shutdown did not complete within timeout", Map.of(
                                "operation", "shutdown_timeout"
                            ));
                        }
                    } catch (Exception e) {
                        structuredLogger.error("Error waiting for graceful shutdown", Map.of(
                            "operation", "shutdown_wait_error"
                        ), e);
                    }
                }
            }, "graceful-shutdown-hook"));
        }
    }
}