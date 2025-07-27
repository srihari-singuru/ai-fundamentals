package com.srihari.ai.health;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;

import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.service.MemoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Health indicator for chat memory system status, monitoring memory usage,
 * conversation count, and memory system performance.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMemoryHealthIndicator implements ReactiveHealthIndicator {

    private final MemoryService memoryService;
    private final CustomMetrics customMetrics;
    
    // Memory health thresholds
    private static final int MAX_CONVERSATIONS_WARNING = 1000;
    private static final int MAX_CONVERSATIONS_CRITICAL = 5000;
    private static final int MAX_MESSAGES_PER_CONVERSATION_WARNING = 100;
    private static final int MAX_MESSAGES_PER_CONVERSATION_CRITICAL = 500;
    
    // Track memory metrics
    private final AtomicLong totalConversations = new AtomicLong(0);
    private final AtomicLong totalMessages = new AtomicLong(0);

    @Override
    public Mono<Health> health() {
        return Mono.fromCallable(this::checkMemoryHealth)
                .onErrorResume(this::buildErrorStatus)
                .doOnError(ex -> log.warn("Chat memory health check failed", ex));
    }

    private Health checkMemoryHealth() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test memory operations
            String testConversationId = "health-check-" + System.currentTimeMillis();
            
            // Test basic memory operations
            List<Message> emptyConversation = memoryService.loadConversation(testConversationId);
            boolean canLoad = emptyConversation != null;
            
            // Test save operation (with empty list to avoid pollution)
            memoryService.save(testConversationId, emptyConversation);
            boolean canSave = true;
            
            // Test reset operation
            memoryService.reset(testConversationId);
            boolean canReset = true;
            
            // Calculate memory statistics
            MemoryStats stats = calculateMemoryStats();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Determine health status based on memory usage
            Health.Builder healthBuilder;
            String status;
            
            if (stats.totalConversations > MAX_CONVERSATIONS_CRITICAL || 
                stats.maxMessagesPerConversation > MAX_MESSAGES_PER_CONVERSATION_CRITICAL) {
                healthBuilder = Health.down();
                status = "critical";
            } else if (stats.totalConversations > MAX_CONVERSATIONS_WARNING || 
                       stats.maxMessagesPerConversation > MAX_MESSAGES_PER_CONVERSATION_WARNING) {
                healthBuilder = Health.up();
                status = "warning";
            } else {
                healthBuilder = Health.up();
                status = "healthy";
            }
            
            return healthBuilder
                    .withDetail("status", status)
                    .withDetail("service", "ChatMemory")
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("canLoad", canLoad)
                    .withDetail("canSave", canSave)
                    .withDetail("canReset", canReset)
                    .withDetail("totalConversations", stats.totalConversations)
                    .withDetail("totalMessages", stats.totalMessages)
                    .withDetail("averageMessagesPerConversation", stats.averageMessagesPerConversation)
                    .withDetail("maxMessagesPerConversation", stats.maxMessagesPerConversation)
                    .withDetail("memoryUsageBytes", stats.estimatedMemoryUsage)
                    .withDetail("warningThresholds", String.format("conversations: %d, messages: %d", 
                        MAX_CONVERSATIONS_WARNING, MAX_MESSAGES_PER_CONVERSATION_WARNING))
                    .withDetail("criticalThresholds", String.format("conversations: %d, messages: %d", 
                        MAX_CONVERSATIONS_CRITICAL, MAX_MESSAGES_PER_CONVERSATION_CRITICAL))
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
                    
        } catch (Exception ex) {
            throw new RuntimeException("Memory health check failed", ex);
        }
    }

    private MemoryStats calculateMemoryStats() {
        // Note: This is a simplified implementation since we don't have direct access
        // to the internal memory store. In a real implementation, you might want to
        // add methods to MemoryService to expose these statistics.
        
        MemoryStats stats = new MemoryStats();
        
        // For now, we'll use atomic counters that are updated by the metrics system
        stats.totalConversations = totalConversations.get();
        stats.totalMessages = totalMessages.get();
        
        if (stats.totalConversations > 0) {
            stats.averageMessagesPerConversation = stats.totalMessages / stats.totalConversations;
        } else {
            stats.averageMessagesPerConversation = 0;
        }
        
        // Estimate memory usage (rough calculation)
        // Assume average message size of 200 characters = ~400 bytes (UTF-16)
        stats.estimatedMemoryUsage = stats.totalMessages * 400;
        
        // For max messages per conversation, we'll use a reasonable estimate
        // In a real implementation, this would come from actual memory store analysis
        stats.maxMessagesPerConversation = Math.min(stats.averageMessagesPerConversation * 2, 50);
        
        return stats;
    }

    private Mono<Health> buildErrorStatus(Throwable ex) {
        customMetrics.incrementAiErrors("chat-memory", "health_check_error", ex.getClass().getSimpleName());
        
        return Mono.just(Health.down()
                .withDetail("status", "error")
                .withDetail("service", "ChatMemory")
                .withDetail("error", ex.getMessage())
                .withDetail("errorType", ex.getClass().getSimpleName())
                .withDetail("timestamp", Instant.now().toString())
                .build());
    }
    
    /**
     * Update conversation count (called by metrics system)
     */
    public void updateConversationCount(long count) {
        totalConversations.set(count);
    }
    
    /**
     * Update message count (called by metrics system)
     */
    public void updateMessageCount(long count) {
        totalMessages.set(count);
    }

    private static class MemoryStats {
        long totalConversations;
        long totalMessages;
        long averageMessagesPerConversation;
        long maxMessagesPerConversation;
        long estimatedMemoryUsage;
    }
}