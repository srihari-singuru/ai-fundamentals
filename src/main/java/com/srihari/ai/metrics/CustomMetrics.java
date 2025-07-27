package com.srihari.ai.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom metrics component for AI-specific operations, business metrics, and system monitoring.
 * Provides comprehensive metrics for token usage, model performance, conversation tracking,
 * user engagement, and system resource monitoring.
 */
@Component
@Slf4j
public class CustomMetrics {

    private final MeterRegistry meterRegistry;
    
    // Atomic counters for gauges
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicInteger activeConversations = new AtomicInteger(0);
    private final AtomicLong totalTokensUsed = new AtomicLong(0);
    private final AtomicInteger currentUserSessions = new AtomicInteger(0);

    // Initialize gauges in constructor
    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeGauges();
    }

    // ========== AI-SPECIFIC METRICS ==========

    /**
     * Record token usage for AI operations
     */
    public void recordTokenUsage(String model, String operation, int tokenCount) {
        Counter.builder("ai.tokens.used.total")
                .description("Total tokens used in AI operations")
                .tag("model", model)
                .tag("operation", operation)
                .register(meterRegistry)
                .increment(tokenCount);
        
        totalTokensUsed.addAndGet(tokenCount);
        log.debug("Recorded {} tokens for model {} operation {}", tokenCount, model, operation);
    }

    /**
     * Record AI model performance metrics
     */
    public void recordModelLatency(String model, String operation, long durationMs) {
        Timer.builder("ai.model.latency")
                .description("AI model response latency")
                .tag("model", model)
                .tag("operation", operation)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
    }

    /**
     * Record AI API error rates
     */
    public void incrementAiErrors(String model, String errorType, String errorCode) {
        Counter.builder("ai.errors.total")
                .description("Total AI operation errors")
                .tag("model", model)
                .tag("error.type", errorType)
                .tag("error.code", errorCode)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record streaming token rate
     */
    public void recordTokenStreamingRate(String model, double tokensPerSecond) {
        meterRegistry.gauge("ai.streaming.tokens.per.second", 
                Tags.of("model", model), tokensPerSecond);
    }

    /**
     * Record streaming batch processing metrics
     */
    public void recordStreamingBatch(int batchSize, int batchLength) {
        Counter.builder("ai.streaming.batches.total")
                .description("Total streaming batches processed")
                .register(meterRegistry)
                .increment();
        
        meterRegistry.gauge("ai.streaming.batch.size.last", batchSize);
        meterRegistry.gauge("ai.streaming.batch.length.last", batchLength);
    }

    /**
     * Record streaming completion metrics
     */
    public void recordStreamingComplete(long totalTokens, long totalBytes) {
        Counter.builder("ai.streaming.completed.total")
                .description("Total streaming operations completed")
                .register(meterRegistry)
                .increment();
        
        meterRegistry.gauge("ai.streaming.tokens.last", totalTokens);
        meterRegistry.gauge("ai.streaming.bytes.last", totalBytes);
    }

    /**
     * Record streaming memory usage
     */
    public void recordStreamingMemoryUsage(long memoryBytes) {
        meterRegistry.gauge("ai.streaming.memory.usage", memoryBytes);
    }

    /**
     * Increment backpressure events
     */
    public void incrementBackpressureEvents() {
        Counter.builder("ai.streaming.backpressure.events.total")
                .description("Total backpressure events during streaming")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Increment dropped tokens due to backpressure
     */
    public void incrementDroppedTokens() {
        Counter.builder("ai.streaming.tokens.dropped.total")
                .description("Total tokens dropped due to backpressure")
                .register(meterRegistry)
                .increment();
    }

    // ========== CONNECTION POOL METRICS ==========

    /**
     * Record connection acquisition failures
     */
    public void incrementConnectionAcquisitionFailures(String poolName, String reason) {
        Counter.builder("connection.pool.acquisition.failures.total")
                .description("Total connection acquisition failures")
                .tag("pool", poolName)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record connection timeouts
     */
    public void incrementConnectionTimeouts(String poolName) {
        Counter.builder("connection.pool.timeouts.total")
                .description("Total connection timeouts")
                .tag("pool", poolName)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record connection pool statistics
     */
    public void recordConnectionPoolStats(Object stats) {
        // Use reflection to access stats properties safely
        try {
            java.lang.reflect.Method getActiveConnections = stats.getClass().getMethod("getActiveConnections");
            java.lang.reflect.Method getIdleConnections = stats.getClass().getMethod("getIdleConnections");
            java.lang.reflect.Method getTotalConnectionsCreated = stats.getClass().getMethod("getTotalConnectionsCreated");
            java.lang.reflect.Method getTotalConnectionsDestroyed = stats.getClass().getMethod("getTotalConnectionsDestroyed");
            java.lang.reflect.Method getConnectionAcquisitionFailures = stats.getClass().getMethod("getConnectionAcquisitionFailures");
            java.lang.reflect.Method getConnectionTimeouts = stats.getClass().getMethod("getConnectionTimeouts");
            
            int activeConnections = (Integer) getActiveConnections.invoke(stats);
            int idleConnections = (Integer) getIdleConnections.invoke(stats);
            long totalCreated = (Long) getTotalConnectionsCreated.invoke(stats);
            long totalDestroyed = (Long) getTotalConnectionsDestroyed.invoke(stats);
            long acquisitionFailures = (Long) getConnectionAcquisitionFailures.invoke(stats);
            long timeouts = (Long) getConnectionTimeouts.invoke(stats);
            
            meterRegistry.gauge("connection.pool.active", activeConnections);
            meterRegistry.gauge("connection.pool.idle", idleConnections);
            meterRegistry.gauge("connection.pool.total.created", totalCreated);
            meterRegistry.gauge("connection.pool.total.destroyed", totalDestroyed);
            meterRegistry.gauge("connection.pool.acquisition.failures", acquisitionFailures);
            meterRegistry.gauge("connection.pool.timeouts", timeouts);
            
        } catch (Exception e) {
            log.warn("Failed to record connection pool stats: {}", e.getMessage());
        }
    }

    // ========== MEMORY MANAGEMENT METRICS ==========

    /**
     * Record session cleanup
     */
    public void recordSessionCleanup(String sessionId, String reason) {
        Counter.builder("memory.session.cleanup.total")
                .description("Total session cleanups")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record session started
     */
    public void recordSessionStarted(String sessionId, String source) {
        Counter.builder("memory.session.started.total")
                .description("Total sessions started")
                .tag("source", source)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record session ended
     */
    public void recordSessionEnded(String sessionId, String reason) {
        Counter.builder("memory.session.ended.total")
                .description("Total sessions ended")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record conversation trimmed
     */
    public void recordConversationTrimmed(String conversationId, int removedMessages) {
        Counter.builder("memory.conversation.trimmed.total")
                .description("Total conversations trimmed")
                .register(meterRegistry)
                .increment();
        
        Counter.builder("memory.messages.removed.total")
                .description("Total messages removed during trimming")
                .register(meterRegistry)
                .increment(removedMessages);
    }

    /**
     * Record memory usage statistics
     */
    public void recordMemoryUsageStats(Object stats) {
        try {
            java.lang.reflect.Method getTotalConversations = stats.getClass().getMethod("getTotalConversations");
            java.lang.reflect.Method getTotalMessages = stats.getClass().getMethod("getTotalMessages");
            java.lang.reflect.Method getActiveSessions = stats.getClass().getMethod("getActiveSessions");
            java.lang.reflect.Method getTotalMemoryUsage = stats.getClass().getMethod("getTotalMemoryUsage");
            java.lang.reflect.Method getMemoryUtilizationPercentage = stats.getClass().getMethod("getMemoryUtilizationPercentage");
            java.lang.reflect.Method getCleanupOperations = stats.getClass().getMethod("getCleanupOperations");
            java.lang.reflect.Method getExpiredSessions = stats.getClass().getMethod("getExpiredSessions");
            
            long totalConversations = (Long) getTotalConversations.invoke(stats);
            long totalMessages = (Long) getTotalMessages.invoke(stats);
            int activeSessions = (Integer) getActiveSessions.invoke(stats);
            long totalMemoryUsage = (Long) getTotalMemoryUsage.invoke(stats);
            double memoryUtilization = (Double) getMemoryUtilizationPercentage.invoke(stats);
            long cleanupOperations = (Long) getCleanupOperations.invoke(stats);
            long expiredSessions = (Long) getExpiredSessions.invoke(stats);
            
            meterRegistry.gauge("memory.conversations.total", totalConversations);
            meterRegistry.gauge("memory.messages.total", totalMessages);
            meterRegistry.gauge("memory.sessions.active", activeSessions);
            meterRegistry.gauge("memory.usage.bytes", totalMemoryUsage);
            meterRegistry.gauge("memory.utilization.percentage", memoryUtilization);
            meterRegistry.gauge("memory.cleanup.operations.total", cleanupOperations);
            meterRegistry.gauge("memory.sessions.expired.total", expiredSessions);
            
        } catch (Exception e) {
            log.warn("Failed to record memory usage stats: {}", e.getMessage());
        }
    }

    // ========== BUSINESS METRICS ==========

    /**
     * Track conversation lifecycle
     */
    public void incrementConversationStarted(String source) {
        Counter.builder("conversation.started.total")
                .description("Total conversations started")
                .tag("source", source) // api, web
                .register(meterRegistry)
                .increment();
        
        activeConversations.incrementAndGet();
    }
    
    /**
     * Track conversation count by status
     */
    public void incrementConversationCount(String status) {
        Counter.builder("conversation.count.total")
                .description("Total conversation count by status")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
        
        if ("started".equals(status)) {
            activeConversations.incrementAndGet();
        } else if ("ended".equals(status)) {
            activeConversations.decrementAndGet();
        }
    }
    
    /**
     * Record conversation duration
     */
    public void recordConversationDuration(String model, long durationMs) {
        Timer.builder("conversation.duration.ms")
                .description("Conversation duration in milliseconds")
                .tag("model", model)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
    }

    public void incrementConversationEnded(String source, long durationMinutes) {
        Counter.builder("conversation.ended.total")
                .description("Total conversations ended")
                .tag("source", source)
                .register(meterRegistry)
                .increment();
        
        Timer.builder("conversation.duration")
                .description("Conversation duration")
                .tag("source", source)
                .register(meterRegistry)
                .record(java.time.Duration.ofMinutes(durationMinutes));
        
        activeConversations.decrementAndGet();
    }

    /**
     * Track message metrics
     */
    public void incrementMessageCount(String source, String messageType) {
        Counter.builder("conversation.messages.total")
                .description("Total messages in conversations")
                .tag("source", source)
                .tag("message.type", messageType) // user, assistant
                .register(meterRegistry)
                .increment();
    }

    /**
     * Track user engagement metrics
     */
    public void recordUserSessionStarted(String sessionId, String userAgent) {
        Counter.builder("user.sessions.started.total")
                .description("Total user sessions started")
                .tag("user.agent.type", extractUserAgentType(userAgent))
                .register(meterRegistry)
                .increment();
        
        currentUserSessions.incrementAndGet();
    }

    public void recordUserSessionEnded(String sessionId, long durationMinutes, int messageCount) {
        Counter.builder("user.sessions.ended.total")
                .description("Total user sessions ended")
                .register(meterRegistry)
                .increment();
        
        Timer.builder("user.session.duration")
                .description("User session duration")
                .register(meterRegistry)
                .record(java.time.Duration.ofMinutes(durationMinutes));
        
        meterRegistry.gauge("user.session.messages", messageCount);
        
        currentUserSessions.decrementAndGet();
    }

    // ========== SYSTEM RESOURCE METRICS ==========

    /**
     * Record JVM memory metrics
     */
    public void recordJvmMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        // Heap memory
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        
        meterRegistry.gauge("jvm.memory.heap.used", heapUsed);
        meterRegistry.gauge("jvm.memory.heap.max", heapMax);
        
        // Non-heap memory
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
        
        meterRegistry.gauge("jvm.memory.nonheap.used", nonHeapUsed);
        
        if (nonHeapMax > 0) {
            meterRegistry.gauge("jvm.memory.nonheap.max", nonHeapMax);
        }
    }

    /**
     * Record system resource metrics
     */
    public void recordSystemResources() {
        Runtime runtime = Runtime.getRuntime();
        
        // CPU cores
        int availableProcessors = runtime.availableProcessors();
        meterRegistry.gauge("system.cpu.cores", availableProcessors);
        
        // JVM uptime
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeBean.getUptime();
        meterRegistry.gauge("jvm.uptime", uptime);
    }

    // ========== CONNECTION AND CIRCUIT BREAKER METRICS ==========

    /**
     * Track active connections
     */
    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

    /**
     * Record circuit breaker events
     */
    public void incrementCircuitBreakerEvents(String circuitBreaker, String event) {
        Counter.builder("circuit.breaker.events.total")
                .description("Circuit breaker events")
                .tag("circuit.breaker", circuitBreaker)
                .tag("event", event)
                .register(meterRegistry)
                .increment();
    }

    // ========== CACHE METRICS ==========

    /**
     * Record cache hit
     */
    public void incrementCacheHit(String cacheName) {
        Counter.builder("cache.hits.total")
                .description("Total cache hits")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record cache miss
     */
    public void incrementCacheMiss(String cacheName) {
        Counter.builder("cache.misses.total")
                .description("Total cache misses")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record cache update
     */
    public void incrementCacheUpdate(String cacheName) {
        Counter.builder("cache.updates.total")
                .description("Total cache updates")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record cache eviction
     */
    public void incrementCacheEviction(String cacheName) {
        Counter.builder("cache.evictions.total")
                .description("Total cache evictions")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record cache size
     */
    public void recordCacheSize(String cacheName, long size) {
        meterRegistry.gauge("cache.size", Tags.of("cache", cacheName), size);
    }

    /**
     * Record cache hit ratio
     */
    public void recordCacheHitRatio(String cacheName, double hitRatio) {
        meterRegistry.gauge("cache.hit.ratio", Tags.of("cache", cacheName), hitRatio);
    }

    // ========== TIMER UTILITIES ==========

    /**
     * Start timing for API operations
     */
    public Timer.Sample startApiTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Record API operation duration
     */
    public void recordApiDuration(Timer.Sample sample, String operation, String source) {
        sample.stop(Timer.builder("api.operation.duration")
                .description("API operation duration")
                .tag("operation", operation)
                .tag("source", source)
                .register(meterRegistry));
    }

    /**
     * Start timing for OpenAI operations
     */
    public Timer.Sample startOpenAiTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Record OpenAI operation duration
     */
    public void recordOpenAiDuration(Timer.Sample sample, String model, String operation) {
        sample.stop(Timer.builder("openai.api.duration")
                .description("OpenAI API call duration")
                .tag("model", model)
                .tag("operation", operation)
                .register(meterRegistry));
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void initializeGauges() {
        // Active connections gauge
        Gauge.builder("chat.connections.active", activeConnections, AtomicInteger::get)
                .description("Currently active chat connections")
                .register(meterRegistry);
        
        // Active conversations gauge
        Gauge.builder("conversation.active.count", activeConversations, AtomicInteger::get)
                .description("Currently active conversations")
                .register(meterRegistry);
        
        // Total tokens used gauge
        Gauge.builder("ai.tokens.total.used", totalTokensUsed, AtomicLong::get)
                .description("Total tokens used since startup")
                .register(meterRegistry);
        
        // Current user sessions gauge
        Gauge.builder("user.sessions.active", currentUserSessions, AtomicInteger::get)
                .description("Currently active user sessions")
                .register(meterRegistry);
        
        log.info("Custom metrics gauges initialized");
    }

    private String extractUserAgentType(String userAgent) {
        if (userAgent == null) return "unknown";
        
        String lowerAgent = userAgent.toLowerCase();
        if (lowerAgent.contains("chrome")) return "chrome";
        if (lowerAgent.contains("firefox")) return "firefox";
        if (lowerAgent.contains("safari")) return "safari";
        if (lowerAgent.contains("edge")) return "edge";
        if (lowerAgent.contains("postman")) return "postman";
        if (lowerAgent.contains("curl")) return "curl";
        
        return "other";
    }
}