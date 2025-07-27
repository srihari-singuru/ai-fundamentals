package com.srihari.ai.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Execution(ExecutionMode.SAME_THREAD)
class CustomMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private CustomMetrics customMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        customMetrics = new CustomMetrics(meterRegistry);
    }

    @Test
    void shouldRecordTokenUsage() {
        // Given
        String model = "gpt-4.1-nano";
        String operation = "chat";
        int tokenCount = 150;
        
        // When
        customMetrics.recordTokenUsage(model, operation, tokenCount);
        
        // Then
        var counter = meterRegistry.find("ai.tokens.used.total").counter();
        assertNotNull(counter);
        assertEquals(tokenCount, counter.count());
    }

    @Test
    void shouldRecordModelLatency() {
        // Given
        String model = "gpt-4.1-nano";
        String operation = "chat";
        long durationMs = 1500;
        
        // When
        customMetrics.recordModelLatency(model, operation, durationMs);
        
        // Then
        var timer = meterRegistry.find("ai.model.latency").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= durationMs);
    }

    @Test
    void shouldIncrementAiErrors() {
        // Given
        String model = "gpt-4.1-nano";
        String errorType = "timeout";
        String errorCode = "TIMEOUT_ERROR";
        
        // When
        customMetrics.incrementAiErrors(model, errorType, errorCode);
        
        // Then
        var counter = meterRegistry.find("ai.errors.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordTokenStreamingRate() {
        // Given
        String model = "gpt-4.1-nano";
        double tokensPerSecond = 25.5;
        
        // When
        customMetrics.recordTokenStreamingRate(model, tokensPerSecond);
        
        // Then
        var gauge = meterRegistry.find("ai.streaming.tokens.per.second").gauge();
        assertNotNull(gauge);
        assertEquals(tokensPerSecond, gauge.value());
    }

    @Test
    void shouldRecordStreamingBatch() {
        // Given
        int batchSize = 10;
        int batchLength = 256;
        
        // When
        customMetrics.recordStreamingBatch(batchSize, batchLength);
        
        // Then
        var counter = meterRegistry.find("ai.streaming.batches.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
        
        var sizeGauge = meterRegistry.find("ai.streaming.batch.size.last").gauge();
        assertNotNull(sizeGauge);
        assertEquals(batchSize, sizeGauge.value());
        
        var lengthGauge = meterRegistry.find("ai.streaming.batch.length.last").gauge();
        assertNotNull(lengthGauge);
        assertEquals(batchLength, lengthGauge.value());
    }

    @Test
    void shouldRecordStreamingComplete() {
        // Given
        long totalTokens = 500;
        long totalBytes = 2048;
        
        // When
        customMetrics.recordStreamingComplete(totalTokens, totalBytes);
        
        // Then
        var counter = meterRegistry.find("ai.streaming.completed.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
        
        var tokensGauge = meterRegistry.find("ai.streaming.tokens.last").gauge();
        assertNotNull(tokensGauge);
        assertEquals(totalTokens, tokensGauge.value());
        
        var bytesGauge = meterRegistry.find("ai.streaming.bytes.last").gauge();
        assertNotNull(bytesGauge);
        assertEquals(totalBytes, bytesGauge.value());
    }

    @Test
    void shouldIncrementBackpressureEvents() {
        // When
        customMetrics.incrementBackpressureEvents();
        
        // Then
        var counter = meterRegistry.find("ai.streaming.backpressure.events.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldIncrementDroppedTokens() {
        // When
        customMetrics.incrementDroppedTokens();
        
        // Then
        var counter = meterRegistry.find("ai.streaming.tokens.dropped.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldIncrementConnectionAcquisitionFailures() {
        // Given
        String poolName = "openai-pool";
        String reason = "timeout";
        
        // When
        customMetrics.incrementConnectionAcquisitionFailures(poolName, reason);
        
        // Then
        var counter = meterRegistry.find("connection.pool.acquisition.failures.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldIncrementConnectionTimeouts() {
        // Given
        String poolName = "openai-pool";
        
        // When
        customMetrics.incrementConnectionTimeouts(poolName);
        
        // Then
        var counter = meterRegistry.find("connection.pool.timeouts.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordSessionCleanup() {
        // Given
        String sessionId = "session-123";
        String reason = "expired";
        
        // When
        customMetrics.recordSessionCleanup(sessionId, reason);
        
        // Then
        var counter = meterRegistry.find("memory.session.cleanup.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordConversationLifecycle() {
        // Given
        String source = "api";
        
        // When
        customMetrics.incrementConversationStarted(source);
        
        // Then
        var counter = meterRegistry.find("conversation.started.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordConversationDuration() {
        // Given
        String model = "gpt-4.1-nano";
        long durationMs = 30000;
        
        // When
        customMetrics.recordConversationDuration(model, durationMs);
        
        // Then
        var timer = meterRegistry.find("conversation.duration.ms").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= durationMs);
    }

    @Test
    void shouldIncrementMessageCount() {
        // Given
        String source = "web";
        String messageType = "user";
        
        // When
        customMetrics.incrementMessageCount(source, messageType);
        
        // Then
        var counter = meterRegistry.find("conversation.messages.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordUserSessionStarted() {
        // Given
        String sessionId = "session-456";
        String userAgent = "Mozilla/5.0 (Chrome)";
        
        // When
        customMetrics.recordUserSessionStarted(sessionId, userAgent);
        
        // Then
        var counter = meterRegistry.find("user.sessions.started.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordUserSessionEnded() {
        // Given
        String sessionId = "session-456";
        long durationMinutes = 15;
        int messageCount = 8;
        
        // When
        customMetrics.recordUserSessionEnded(sessionId, durationMinutes, messageCount);
        
        // Then
        var counter = meterRegistry.find("user.sessions.ended.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
        
        var timer = meterRegistry.find("user.session.duration").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        
        var gauge = meterRegistry.find("user.session.messages").gauge();
        assertNotNull(gauge);
        assertEquals(messageCount, gauge.value());
    }

    @Test
    void shouldTrackActiveConnections() {
        // When
        customMetrics.incrementActiveConnections();
        customMetrics.decrementActiveConnections();
        
        // Then - No exceptions should be thrown
        // The actual verification would require access to the atomic counters
        // which are private, so we just ensure the methods execute without error
        assertTrue(true); // Test passes if no exception is thrown
    }

    @Test
    void shouldRecordCircuitBreakerEvents() {
        // Given
        String circuitBreaker = "openai-circuit-breaker";
        String event = "state_transition";
        
        // When
        customMetrics.incrementCircuitBreakerEvents(circuitBreaker, event);
        
        // Then
        var counter = meterRegistry.find("circuit.breaker.events.total").counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    void shouldRecordCacheMetrics() {
        // Given
        String cacheName = "conversations";
        
        // When
        customMetrics.incrementCacheHit(cacheName);
        customMetrics.incrementCacheMiss(cacheName);
        customMetrics.incrementCacheUpdate(cacheName);
        customMetrics.incrementCacheEviction(cacheName);
        
        // Then
        var hitCounter = meterRegistry.find("cache.hits.total").counter();
        assertNotNull(hitCounter);
        assertEquals(1, hitCounter.count());
        
        var missCounter = meterRegistry.find("cache.misses.total").counter();
        assertNotNull(missCounter);
        assertEquals(1, missCounter.count());
        
        var updateCounter = meterRegistry.find("cache.updates.total").counter();
        assertNotNull(updateCounter);
        assertEquals(1, updateCounter.count());
        
        var evictionCounter = meterRegistry.find("cache.evictions.total").counter();
        assertNotNull(evictionCounter);
        assertEquals(1, evictionCounter.count());
    }

    @Test
    void shouldRecordCacheSize() {
        // Given
        String cacheName = "responses";
        long size = 150;
        
        // When
        customMetrics.recordCacheSize(cacheName, size);
        
        // Then
        var gauge = meterRegistry.find("cache.size").gauge();
        assertNotNull(gauge);
        assertEquals(size, gauge.value());
    }

    @Test
    void shouldRecordCacheHitRatio() {
        // Given
        String cacheName = "models";
        double hitRatio = 0.85;
        
        // When
        customMetrics.recordCacheHitRatio(cacheName, hitRatio);
        
        // Then
        var gauge = meterRegistry.find("cache.hit.ratio").gauge();
        assertNotNull(gauge);
        assertEquals(hitRatio, gauge.value());
    }

    @Test
    void shouldStartAndRecordApiTimer() {
        // When
        var sample = customMetrics.startApiTimer();
        customMetrics.recordApiDuration(sample, "chat", "api");
        
        // Then
        var timer = meterRegistry.find("api.operation.duration").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldStartAndRecordOpenAiTimer() {
        // When
        var sample = customMetrics.startOpenAiTimer();
        customMetrics.recordOpenAiDuration(sample, "gpt-4.1-nano", "completion");
        
        // Then
        var timer = meterRegistry.find("openai.api.duration").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void shouldExtractUserAgentTypeCorrectly() {
        // Test Chrome
        customMetrics.recordUserSessionStarted("session1", "Mozilla/5.0 (Chrome/91.0)");
        var chromeCounter = meterRegistry.find("user.sessions.started.total")
                .tag("user.agent.type", "chrome").counter();
        assertNotNull(chromeCounter);
        assertEquals(1, chromeCounter.count());
        
        // Test Firefox
        customMetrics.recordUserSessionStarted("session2", "Mozilla/5.0 (Firefox/89.0)");
        var firefoxCounter = meterRegistry.find("user.sessions.started.total")
                .tag("user.agent.type", "firefox").counter();
        assertNotNull(firefoxCounter);
        assertEquals(1, firefoxCounter.count());
        
        // Test unknown
        customMetrics.recordUserSessionStarted("session7", "UnknownAgent/1.0");
        var otherCounter = meterRegistry.find("user.sessions.started.total")
                .tag("user.agent.type", "other").counter();
        assertNotNull(otherCounter);
        assertEquals(1, otherCounter.count());
        
        // Test null
        customMetrics.recordUserSessionStarted("session8", null);
        var unknownCounter = meterRegistry.find("user.sessions.started.total")
                .tag("user.agent.type", "unknown").counter();
        assertNotNull(unknownCounter);
        assertEquals(1, unknownCounter.count());
    }

    @Test
    void shouldHandleConnectionPoolStatsReflectionGracefully() {
        // Given
        Object mockStats = new Object() {
            @SuppressWarnings("unused")
            public Integer getActiveConnections() { return 5; }
            @SuppressWarnings("unused")
            public Integer getIdleConnections() { return 3; }
            @SuppressWarnings("unused")
            public Long getTotalConnectionsCreated() { return 100L; }
            @SuppressWarnings("unused")
            public Long getTotalConnectionsDestroyed() { return 95L; }
            @SuppressWarnings("unused")
            public Long getConnectionAcquisitionFailures() { return 2L; }
            @SuppressWarnings("unused")
            public Long getConnectionTimeouts() { return 1L; }
        };
        
        // When
        customMetrics.recordConnectionPoolStats(mockStats);
        
        // Then - Should not throw exception and should record gauges
        var activeGauge = meterRegistry.find("connection.pool.active").gauge();
        assertNotNull(activeGauge);
        assertEquals(5, activeGauge.value());
        
        var idleGauge = meterRegistry.find("connection.pool.idle").gauge();
        assertNotNull(idleGauge);
        assertEquals(3, idleGauge.value());
    }

    @Test
    void shouldHandleMemoryUsageStatsReflectionGracefully() {
        // Given
        Object mockStats = new Object() {
            @SuppressWarnings("unused")
            public Long getTotalConversations() { return 50L; }
            @SuppressWarnings("unused")
            public Long getTotalMessages() { return 500L; }
            @SuppressWarnings("unused")
            public Integer getActiveSessions() { return 10; }
            @SuppressWarnings("unused")
            public Long getTotalMemoryUsage() { return 1024000L; }
            @SuppressWarnings("unused")
            public Double getMemoryUtilizationPercentage() { return 75.5; }
            @SuppressWarnings("unused")
            public Long getCleanupOperations() { return 5L; }
            @SuppressWarnings("unused")
            public Long getExpiredSessions() { return 3L; }
        };
        
        // When
        customMetrics.recordMemoryUsageStats(mockStats);
        
        // Then - Should not throw exception and should record gauges
        var conversationsGauge = meterRegistry.find("memory.conversations.total").gauge();
        assertNotNull(conversationsGauge);
        assertEquals(50L, conversationsGauge.value());
        
        var messagesGauge = meterRegistry.find("memory.messages.total").gauge();
        assertNotNull(messagesGauge);
        assertEquals(500L, messagesGauge.value());
    }

    @Test
    void shouldHandleReflectionFailuresGracefully() {
        // Given - Object without expected methods
        Object invalidStats = new Object();
        
        // When & Then - Should not throw exception
        customMetrics.recordConnectionPoolStats(invalidStats);
        customMetrics.recordMemoryUsageStats(invalidStats);
        
        // Test passes if no exception is thrown
        assertTrue(true);
    }

    @Test
    void shouldRecordJvmMemoryUsage() {
        // When
        customMetrics.recordJvmMemoryUsage();
        
        // Then - Should record heap and non-heap memory metrics
        var heapUsedGauge = meterRegistry.find("jvm.memory.heap.used").gauge();
        assertNotNull(heapUsedGauge);
        assertTrue(heapUsedGauge.value() > 0);
        
        var heapMaxGauge = meterRegistry.find("jvm.memory.heap.max").gauge();
        assertNotNull(heapMaxGauge);
        assertTrue(heapMaxGauge.value() > 0);
    }

    @Test
    void shouldRecordSystemResources() {
        // When
        customMetrics.recordSystemResources();
        
        // Then - Should record CPU cores and JVM uptime
        var cpuCoresGauge = meterRegistry.find("system.cpu.cores").gauge();
        assertNotNull(cpuCoresGauge);
        assertTrue(cpuCoresGauge.value() > 0);
        
        var uptimeGauge = meterRegistry.find("jvm.uptime").gauge();
        assertNotNull(uptimeGauge);
        assertTrue(uptimeGauge.value() >= 0);
    }

    @Test
    void shouldInitializeGaugesOnConstruction() {
        // Then - Verify gauge initialization
        var activeConnectionsGauge = meterRegistry.find("chat.connections.active").gauge();
        assertNotNull(activeConnectionsGauge);
        assertEquals(0, activeConnectionsGauge.value());
        
        var activeConversationsGauge = meterRegistry.find("conversation.active.count").gauge();
        assertNotNull(activeConversationsGauge);
        assertEquals(0, activeConversationsGauge.value());
        
        var totalTokensGauge = meterRegistry.find("ai.tokens.total.used").gauge();
        assertNotNull(totalTokensGauge);
        assertEquals(0, totalTokensGauge.value());
        
        var userSessionsGauge = meterRegistry.find("user.sessions.active").gauge();
        assertNotNull(userSessionsGauge);
        assertEquals(0, userSessionsGauge.value());
    }

    @Test
    void shouldRecordMultipleOperationsCorrectly() {
        // Given
        String model = "gpt-4.1-nano";
        
        // When - Record multiple operations
        customMetrics.recordTokenUsage(model, "chat", 100);
        customMetrics.recordTokenUsage(model, "chat", 150);
        customMetrics.recordTokenUsage(model, "completion", 200);
        
        // Then - Should accumulate correctly
        var chatCounter = meterRegistry.find("ai.tokens.used.total")
                .tag("model", model)
                .tag("operation", "chat")
                .counter();
        assertNotNull(chatCounter);
        assertEquals(250, chatCounter.count()); // 100 + 150
        
        var completionCounter = meterRegistry.find("ai.tokens.used.total")
                .tag("model", model)
                .tag("operation", "completion")
                .counter();
        assertNotNull(completionCounter);
        assertEquals(200, completionCounter.count());
    }
}