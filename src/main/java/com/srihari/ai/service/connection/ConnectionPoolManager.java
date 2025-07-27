package com.srihari.ai.service.connection;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.metrics.CustomMetrics;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.netty.resources.ConnectionProvider;

/**
 * Manages connection pool lifecycle, monitoring, and optimization.
 * Provides connection pool metrics, health checks, and resource management.
 */
@Component
@RequiredArgsConstructor
public class ConnectionPoolManager implements HealthIndicator {
    
    private final StructuredLogger structuredLogger;
    private final CustomMetrics customMetrics;
    
    // Connection pool metrics
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger idleConnections = new AtomicInteger(0);
    private final AtomicLong totalConnectionsCreated = new AtomicLong(0);
    private final AtomicLong totalConnectionsDestroyed = new AtomicLong(0);
    private final AtomicLong connectionAcquisitionFailures = new AtomicLong(0);
    private final AtomicLong connectionTimeouts = new AtomicLong(0);
    
    // Pool configuration
    private static final int DEFAULT_MAX_CONNECTIONS = 100;
    private static final Duration DEFAULT_MAX_IDLE_TIME = Duration.ofSeconds(30);
    private static final Duration DEFAULT_MAX_LIFE_TIME = Duration.ofMinutes(10);
    private static final Duration DEFAULT_PENDING_ACQUIRE_TIMEOUT = Duration.ofSeconds(10);
    
    /**
     * Creates an optimized connection provider with monitoring
     * 
     * @param poolName Name of the connection pool
     * @param maxConnections Maximum number of connections
     * @param maxIdleTime Maximum idle time for connections
     * @param maxLifeTime Maximum lifetime for connections
     * @return Configured connection provider
     */
    public ConnectionProvider createOptimizedConnectionProvider(
            String poolName, 
            int maxConnections, 
            Duration maxIdleTime, 
            Duration maxLifeTime) {
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        structuredLogger.info("Creating optimized connection provider", Map.of(
            "operation", "connection_provider_creation",
            "poolName", poolName,
            "maxConnections", maxConnections,
            "maxIdleTime", maxIdleTime.toString(),
            "maxLifeTime", maxLifeTime.toString(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        return ConnectionProvider.builder(poolName)
                .maxConnections(maxConnections)
                .maxIdleTime(maxIdleTime)
                .maxLifeTime(maxLifeTime)
                .pendingAcquireTimeout(DEFAULT_PENDING_ACQUIRE_TIMEOUT)
                .evictInBackground(Duration.ofSeconds(120))
                .metrics(true) // Enable Micrometer metrics
                .build();
    }
    
    /**
     * Creates a default optimized connection provider
     * 
     * @param poolName Name of the connection pool
     * @return Configured connection provider with default settings
     */
    public ConnectionProvider createDefaultConnectionProvider(String poolName) {
        return createOptimizedConnectionProvider(
            poolName, 
            DEFAULT_MAX_CONNECTIONS, 
            DEFAULT_MAX_IDLE_TIME, 
            DEFAULT_MAX_LIFE_TIME
        );
    }
    
    /**
     * Records connection acquisition
     */
    public void recordConnectionAcquired(String poolName) {
        activeConnections.incrementAndGet();
        totalConnectionsCreated.incrementAndGet();
        
        customMetrics.incrementActiveConnections();
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        structuredLogger.debug("Connection acquired", Map.of(
            "operation", "connection_acquired",
            "poolName", poolName,
            "activeConnections", activeConnections.get(),
            "totalCreated", totalConnectionsCreated.get(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
    }
    
    /**
     * Records connection release
     */
    public void recordConnectionReleased(String poolName) {
        activeConnections.decrementAndGet();
        idleConnections.incrementAndGet();
        
        customMetrics.decrementActiveConnections();
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        structuredLogger.debug("Connection released", Map.of(
            "operation", "connection_released",
            "poolName", poolName,
            "activeConnections", activeConnections.get(),
            "idleConnections", idleConnections.get(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
    }
    
    /**
     * Records connection destruction
     */
    public void recordConnectionDestroyed(String poolName, String reason) {
        totalConnectionsDestroyed.incrementAndGet();
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        structuredLogger.debug("Connection destroyed", Map.of(
            "operation", "connection_destroyed",
            "poolName", poolName,
            "reason", reason,
            "totalDestroyed", totalConnectionsDestroyed.get(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
    }
    
    /**
     * Records connection acquisition failure
     */
    public void recordConnectionAcquisitionFailure(String poolName, String reason) {
        connectionAcquisitionFailures.incrementAndGet();
        
        customMetrics.incrementConnectionAcquisitionFailures(poolName, reason);
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        structuredLogger.warn("Connection acquisition failed", Map.of(
            "operation", "connection_acquisition_failed",
            "poolName", poolName,
            "reason", reason,
            "totalFailures", connectionAcquisitionFailures.get(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
    }
    
    /**
     * Records connection timeout
     */
    public void recordConnectionTimeout(String poolName, Duration timeout) {
        connectionTimeouts.incrementAndGet();
        
        customMetrics.incrementConnectionTimeouts(poolName);
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        structuredLogger.warn("Connection timeout", Map.of(
            "operation", "connection_timeout",
            "poolName", poolName,
            "timeout", timeout.toString(),
            "totalTimeouts", connectionTimeouts.get(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
    }
    
    /**
     * Gets current connection pool statistics
     * 
     * @return Connection pool statistics
     */
    public ConnectionPoolStats getConnectionPoolStats() {
        return ConnectionPoolStats.builder()
                .activeConnections(activeConnections.get())
                .idleConnections(idleConnections.get())
                .totalConnectionsCreated(totalConnectionsCreated.get())
                .totalConnectionsDestroyed(totalConnectionsDestroyed.get())
                .connectionAcquisitionFailures(connectionAcquisitionFailures.get())
                .connectionTimeouts(connectionTimeouts.get())
                .build();
    }
    
    /**
     * Monitors connection pool health and performance
     * 
     * @return Mono that completes when monitoring is done
     */
    public Mono<Void> monitorConnectionPool() {
        return Mono.fromRunnable(() -> {
            ConnectionPoolStats stats = getConnectionPoolStats();
            
            // Record metrics
            customMetrics.recordConnectionPoolStats(stats);
            
            // Log statistics periodically
            String correlationId = CorrelationIdHolder.getCorrelationId();
            structuredLogger.info("Connection pool statistics", Map.of(
                "operation", "connection_pool_monitoring",
                "activeConnections", stats.getActiveConnections(),
                "idleConnections", stats.getIdleConnections(),
                "totalCreated", stats.getTotalConnectionsCreated(),
                "totalDestroyed", stats.getTotalConnectionsDestroyed(),
                "acquisitionFailures", stats.getConnectionAcquisitionFailures(),
                "timeouts", stats.getConnectionTimeouts(),
                "correlationId", correlationId != null ? correlationId : "unknown"
            ));
            
            // Check for potential issues
            checkConnectionPoolHealth(stats);
        });
    }
    
    /**
     * Optimizes connection pool based on current usage patterns
     * 
     * @param stats Current connection pool statistics
     * @return Optimization recommendations
     */
    public ConnectionPoolOptimizationRecommendations optimizeConnectionPool(ConnectionPoolStats stats) {
        ConnectionPoolOptimizationRecommendations.Builder recommendations = 
            ConnectionPoolOptimizationRecommendations.builder();
        
        // Analyze connection usage patterns
        double utilizationRate = (double) stats.getActiveConnections() / DEFAULT_MAX_CONNECTIONS;
        double failureRate = stats.getConnectionAcquisitionFailures() > 0 ? 
            (double) stats.getConnectionAcquisitionFailures() / stats.getTotalConnectionsCreated() : 0.0;
        
        // High utilization - recommend increasing pool size
        if (utilizationRate > 0.8) {
            recommendations.increaseMaxConnections(true)
                    .recommendedMaxConnections((int) (DEFAULT_MAX_CONNECTIONS * 1.5))
                    .reason("High connection utilization detected: " + String.format("%.2f%%", utilizationRate * 100));
        }
        
        // High failure rate - recommend increasing timeout
        if (failureRate > 0.05) {
            recommendations.increaseAcquireTimeout(true)
                    .recommendedAcquireTimeout(DEFAULT_PENDING_ACQUIRE_TIMEOUT.multipliedBy(2))
                    .reason("High connection acquisition failure rate: " + String.format("%.2f%%", failureRate * 100));
        }
        
        // Too many idle connections - recommend reducing pool size
        if (stats.getIdleConnections() > DEFAULT_MAX_CONNECTIONS * 0.5 && utilizationRate < 0.3) {
            recommendations.decreaseMaxConnections(true)
                    .recommendedMaxConnections((int) (DEFAULT_MAX_CONNECTIONS * 0.8))
                    .reason("Too many idle connections with low utilization");
        }
        
        // Frequent timeouts - recommend increasing timeout
        if (stats.getConnectionTimeouts() > stats.getTotalConnectionsCreated() * 0.1) {
            recommendations.increaseAcquireTimeout(true)
                    .recommendedAcquireTimeout(DEFAULT_PENDING_ACQUIRE_TIMEOUT.multipliedBy(3))
                    .reason("Frequent connection timeouts detected");
        }
        
        ConnectionPoolOptimizationRecommendations result = recommendations.build();
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        structuredLogger.info("Connection pool optimization analysis", Map.of(
            "operation", "connection_pool_optimization",
            "utilizationRate", String.format("%.2f%%", utilizationRate * 100),
            "failureRate", String.format("%.2f%%", failureRate * 100),
            "hasRecommendations", result.hasRecommendations(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        return result;
    }
    
    @Override
    public Health health() {
        ConnectionPoolStats stats = getConnectionPoolStats();
        
        Health.Builder healthBuilder = Health.up()
                .withDetail("activeConnections", stats.getActiveConnections())
                .withDetail("idleConnections", stats.getIdleConnections())
                .withDetail("totalConnectionsCreated", stats.getTotalConnectionsCreated())
                .withDetail("totalConnectionsDestroyed", stats.getTotalConnectionsDestroyed())
                .withDetail("connectionAcquisitionFailures", stats.getConnectionAcquisitionFailures())
                .withDetail("connectionTimeouts", stats.getConnectionTimeouts());
        
        // Check for health issues
        if (stats.getConnectionAcquisitionFailures() > stats.getTotalConnectionsCreated() * 0.1) {
            healthBuilder.down().withDetail("issue", "High connection acquisition failure rate");
        }
        
        if (stats.getConnectionTimeouts() > stats.getTotalConnectionsCreated() * 0.05) {
            healthBuilder.down().withDetail("issue", "High connection timeout rate");
        }
        
        return healthBuilder.build();
    }
    
    private void checkConnectionPoolHealth(ConnectionPoolStats stats) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        // Check for high failure rate
        if (stats.getConnectionAcquisitionFailures() > 0) {
            double failureRate = (double) stats.getConnectionAcquisitionFailures() / 
                Math.max(1, stats.getTotalConnectionsCreated());
            
            if (failureRate > 0.1) {
                structuredLogger.warn("High connection acquisition failure rate detected", Map.of(
                    "operation", "connection_pool_health_check",
                    "failureRate", String.format("%.2f%%", failureRate * 100),
                    "totalFailures", stats.getConnectionAcquisitionFailures(),
                    "totalCreated", stats.getTotalConnectionsCreated(),
                    "correlationId", correlationId != null ? correlationId : "unknown"
                ));
            }
        }
        
        // Check for high timeout rate
        if (stats.getConnectionTimeouts() > 0) {
            double timeoutRate = (double) stats.getConnectionTimeouts() / 
                Math.max(1, stats.getTotalConnectionsCreated());
            
            if (timeoutRate > 0.05) {
                structuredLogger.warn("High connection timeout rate detected", Map.of(
                    "operation", "connection_pool_health_check",
                    "timeoutRate", String.format("%.2f%%", timeoutRate * 100),
                    "totalTimeouts", stats.getConnectionTimeouts(),
                    "totalCreated", stats.getTotalConnectionsCreated(),
                    "correlationId", correlationId != null ? correlationId : "unknown"
                ));
            }
        }
    }
    
    /**
     * Connection pool statistics data class
     */
    public static class ConnectionPoolStats {
        private final int activeConnections;
        private final int idleConnections;
        private final long totalConnectionsCreated;
        private final long totalConnectionsDestroyed;
        private final long connectionAcquisitionFailures;
        private final long connectionTimeouts;
        
        private ConnectionPoolStats(Builder builder) {
            this.activeConnections = builder.activeConnections;
            this.idleConnections = builder.idleConnections;
            this.totalConnectionsCreated = builder.totalConnectionsCreated;
            this.totalConnectionsDestroyed = builder.totalConnectionsDestroyed;
            this.connectionAcquisitionFailures = builder.connectionAcquisitionFailures;
            this.connectionTimeouts = builder.connectionTimeouts;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public long getTotalConnectionsCreated() { return totalConnectionsCreated; }
        public long getTotalConnectionsDestroyed() { return totalConnectionsDestroyed; }
        public long getConnectionAcquisitionFailures() { return connectionAcquisitionFailures; }
        public long getConnectionTimeouts() { return connectionTimeouts; }
        
        public static class Builder {
            private int activeConnections;
            private int idleConnections;
            private long totalConnectionsCreated;
            private long totalConnectionsDestroyed;
            private long connectionAcquisitionFailures;
            private long connectionTimeouts;
            
            public Builder activeConnections(int activeConnections) {
                this.activeConnections = activeConnections;
                return this;
            }
            
            public Builder idleConnections(int idleConnections) {
                this.idleConnections = idleConnections;
                return this;
            }
            
            public Builder totalConnectionsCreated(long totalConnectionsCreated) {
                this.totalConnectionsCreated = totalConnectionsCreated;
                return this;
            }
            
            public Builder totalConnectionsDestroyed(long totalConnectionsDestroyed) {
                this.totalConnectionsDestroyed = totalConnectionsDestroyed;
                return this;
            }
            
            public Builder connectionAcquisitionFailures(long connectionAcquisitionFailures) {
                this.connectionAcquisitionFailures = connectionAcquisitionFailures;
                return this;
            }
            
            public Builder connectionTimeouts(long connectionTimeouts) {
                this.connectionTimeouts = connectionTimeouts;
                return this;
            }
            
            public ConnectionPoolStats build() {
                return new ConnectionPoolStats(this);
            }
        }
    }
    
    /**
     * Connection pool optimization recommendations
     */
    public static class ConnectionPoolOptimizationRecommendations {
        private final boolean increaseMaxConnections;
        private final boolean decreaseMaxConnections;
        private final boolean increaseAcquireTimeout;
        private final int recommendedMaxConnections;
        private final Duration recommendedAcquireTimeout;
        private final String reason;
        
        private ConnectionPoolOptimizationRecommendations(Builder builder) {
            this.increaseMaxConnections = builder.increaseMaxConnections;
            this.decreaseMaxConnections = builder.decreaseMaxConnections;
            this.increaseAcquireTimeout = builder.increaseAcquireTimeout;
            this.recommendedMaxConnections = builder.recommendedMaxConnections;
            this.recommendedAcquireTimeout = builder.recommendedAcquireTimeout;
            this.reason = builder.reason;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public boolean hasRecommendations() {
            return increaseMaxConnections || decreaseMaxConnections || increaseAcquireTimeout;
        }
        
        // Getters
        public boolean isIncreaseMaxConnections() { return increaseMaxConnections; }
        public boolean isDecreaseMaxConnections() { return decreaseMaxConnections; }
        public boolean isIncreaseAcquireTimeout() { return increaseAcquireTimeout; }
        public int getRecommendedMaxConnections() { return recommendedMaxConnections; }
        public Duration getRecommendedAcquireTimeout() { return recommendedAcquireTimeout; }
        public String getReason() { return reason; }
        
        public static class Builder {
            private boolean increaseMaxConnections;
            private boolean decreaseMaxConnections;
            private boolean increaseAcquireTimeout;
            private int recommendedMaxConnections;
            private Duration recommendedAcquireTimeout;
            private String reason;
            
            public Builder increaseMaxConnections(boolean increaseMaxConnections) {
                this.increaseMaxConnections = increaseMaxConnections;
                return this;
            }
            
            public Builder decreaseMaxConnections(boolean decreaseMaxConnections) {
                this.decreaseMaxConnections = decreaseMaxConnections;
                return this;
            }
            
            public Builder increaseAcquireTimeout(boolean increaseAcquireTimeout) {
                this.increaseAcquireTimeout = increaseAcquireTimeout;
                return this;
            }
            
            public Builder recommendedMaxConnections(int recommendedMaxConnections) {
                this.recommendedMaxConnections = recommendedMaxConnections;
                return this;
            }
            
            public Builder recommendedAcquireTimeout(Duration recommendedAcquireTimeout) {
                this.recommendedAcquireTimeout = recommendedAcquireTimeout;
                return this;
            }
            
            public Builder reason(String reason) {
                this.reason = reason;
                return this;
            }
            
            public ConnectionPoolOptimizationRecommendations build() {
                return new ConnectionPoolOptimizationRecommendations(this);
            }
        }
    }
}