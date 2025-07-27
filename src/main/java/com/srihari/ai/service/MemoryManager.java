package com.srihari.ai.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.metrics.CustomMetrics;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Enhanced memory management component for conversation cleanup, session expiration,
 * and memory usage monitoring. Provides automatic cleanup and optimization of
 * conversation memory to prevent memory leaks and optimize performance.
 */
@Component
@RequiredArgsConstructor
public class MemoryManager implements HealthIndicator {
    
    private final ChatMemoryRepository chatMemoryRepository;
    private final StructuredLogger structuredLogger;
    private final CustomMetrics customMetrics;
    
    // Session tracking
    private final Map<String, ConversationSession> activeSessions = new ConcurrentHashMap<>();
    
    // Memory usage tracking
    private final AtomicLong totalConversations = new AtomicLong(0);
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong totalMemoryUsage = new AtomicLong(0);
    private final AtomicLong cleanupOperations = new AtomicLong(0);
    private final AtomicLong expiredSessions = new AtomicLong(0);
    
    // Configuration
    private static final Duration DEFAULT_SESSION_TIMEOUT = Duration.ofHours(2);
    private static final Duration INACTIVE_SESSION_TIMEOUT = Duration.ofMinutes(30);
    private static final int MAX_MESSAGES_PER_CONVERSATION = 100;
    private static final long MAX_MEMORY_USAGE_BYTES = 50 * 1024 * 1024; // 50MB
    
    /**
     * Optimizes conversation memory by removing old messages and cleaning up expired sessions
     */
    public Mono<Void> optimizeConversationMemory() {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        structuredLogger.info("Starting conversation memory optimization", Map.of(
            "operation", "memory_optimization_start",
            "activeSessions", activeSessions.size(),
            "totalConversations", totalConversations.get(),
            "totalMessages", totalMessages.get(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        return Mono.fromRunnable(() -> {
            // Clean up expired sessions
            cleanupExpiredSessions();
            
            // Optimize active conversations
            optimizeActiveConversations();
            
            // Update memory usage statistics
            updateMemoryUsageStats();
            
            cleanupOperations.incrementAndGet();
            
            structuredLogger.info("Conversation memory optimization completed", Map.of(
                "operation", "memory_optimization_complete",
                "cleanupOperations", cleanupOperations.get(),
                "activeSessions", activeSessions.size(),
                "totalMemoryUsage", totalMemoryUsage.get(),
                "correlationId", correlationId != null ? correlationId : "unknown"
            ));
        });
    }
    
    /**
     * Cleans up expired sessions automatically
     */
    public Mono<Void> cleanupExpiredSessions() {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        return Mono.fromRunnable(() -> {
            Instant now = Instant.now();
            List<String> expiredSessionIds = activeSessions.entrySet().stream()
                    .filter(entry -> isSessionExpired(entry.getValue(), now))
                    .map(Map.Entry::getKey)
                    .toList();
            
            for (String sessionId : expiredSessionIds) {
                ConversationSession session = activeSessions.remove(sessionId);
                if (session != null) {
                    // Clean up conversation data
                    try {
                        chatMemoryRepository.deleteByConversationId(sessionId);
                        expiredSessions.incrementAndGet();
                        
                        structuredLogger.debug("Expired session cleaned up", Map.of(
                            "operation", "session_cleanup",
                            "sessionId", sessionId,
                            "sessionAge", Duration.between(session.getCreatedAt(), now).toString(),
                            "lastActivity", Duration.between(session.getLastActivity(), now).toString(),
                            "correlationId", correlationId != null ? correlationId : "unknown"
                        ));
                        
                        customMetrics.recordSessionCleanup(sessionId, "expired");
                        
                    } catch (Exception e) {
                        structuredLogger.error("Failed to cleanup expired session", Map.of(
                            "operation", "session_cleanup_failed",
                            "sessionId", sessionId,
                            "errorType", e.getClass().getSimpleName(),
                            "correlationId", correlationId != null ? correlationId : "unknown"
                        ), e);
                    }
                }
            }
            
            if (!expiredSessionIds.isEmpty()) {
                structuredLogger.info("Expired sessions cleaned up", Map.of(
                    "operation", "expired_sessions_cleanup",
                    "cleanedSessions", expiredSessionIds.size(),
                    "totalExpiredSessions", expiredSessions.get(),
                    "correlationId", correlationId != null ? correlationId : "unknown"
                ));
            }
        });
    }
    
    /**
     * Creates memory usage monitoring and alerting
     */
    public MemoryUsageStats getMemoryStats() {
        long currentMemoryUsage = calculateCurrentMemoryUsage();
        
        return MemoryUsageStats.builder()
                .totalConversations(totalConversations.get())
                .totalMessages(totalMessages.get())
                .activeSessions(activeSessions.size())
                .totalMemoryUsage(currentMemoryUsage)
                .cleanupOperations(cleanupOperations.get())
                .expiredSessions(expiredSessions.get())
                .memoryUtilizationPercentage(calculateMemoryUtilization(currentMemoryUsage))
                .build();
    }
    
    /**
     * Registers a new conversation session
     */
    public void registerSession(String conversationId, String userId, String source) {
        ConversationSession session = ConversationSession.builder()
                .conversationId(conversationId)
                .userId(userId)
                .source(source)
                .createdAt(Instant.now())
                .lastActivity(Instant.now())
                .messageCount(0)
                .build();
        
        activeSessions.put(conversationId, session);
        totalConversations.incrementAndGet();
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        structuredLogger.debug("Session registered", Map.of(
            "operation", "session_registered",
            "conversationId", conversationId,
            "userId", userId,
            "source", source,
            "activeSessions", activeSessions.size(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        customMetrics.recordSessionStarted(conversationId, source);
    }
    
    /**
     * Updates session activity
     */
    public void updateSessionActivity(String conversationId) {
        ConversationSession session = activeSessions.get(conversationId);
        if (session != null) {
            session.setLastActivity(Instant.now());
            session.setMessageCount(session.getMessageCount() + 1);
            totalMessages.incrementAndGet();
            
            String correlationId = CorrelationIdHolder.getCorrelationId();
            structuredLogger.debug("Session activity updated", Map.of(
                "operation", "session_activity_updated",
                "conversationId", conversationId,
                "messageCount", session.getMessageCount(),
                "correlationId", correlationId != null ? correlationId : "unknown"
            ));
        }
    }
    
    /**
     * Removes a session manually
     */
    public void removeSession(String conversationId, String reason) {
        ConversationSession session = activeSessions.remove(conversationId);
        if (session != null) {
            String correlationId = CorrelationIdHolder.getCorrelationId();
            try {
                chatMemoryRepository.deleteByConversationId(conversationId);
                
                structuredLogger.info("Session removed", Map.of(
                    "operation", "session_removed",
                    "conversationId", conversationId,
                    "reason", reason,
                    "sessionDuration", Duration.between(session.getCreatedAt(), Instant.now()).toString(),
                    "messageCount", session.getMessageCount(),
                    "correlationId", correlationId != null ? correlationId : "unknown"
                ));
                
                customMetrics.recordSessionEnded(conversationId, reason);
                
            } catch (Exception e) {
                structuredLogger.error("Failed to remove session", Map.of(
                    "operation", "session_removal_failed",
                    "conversationId", conversationId,
                    "reason", reason,
                    "errorType", e.getClass().getSimpleName(),
                    "correlationId", correlationId != null ? correlationId : "unknown"
                ), e);
            }
        }
    }
    
    /**
     * Scheduled cleanup task that runs every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void scheduledCleanup() {
        optimizeConversationMemory().subscribe();
    }
    
    /**
     * Scheduled memory monitoring task that runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void scheduledMemoryMonitoring() {
        MemoryUsageStats stats = getMemoryStats();
        
        // Record metrics
        customMetrics.recordMemoryUsageStats(stats);
        
        // Check for memory pressure
        if (stats.getMemoryUtilizationPercentage() > 80) {
            String correlationId = CorrelationIdHolder.getCorrelationId();
            structuredLogger.warn("High memory utilization detected", Map.of(
                "operation", "memory_pressure_warning",
                "memoryUtilization", String.format("%.2f%%", stats.getMemoryUtilizationPercentage()),
                "totalMemoryUsage", stats.getTotalMemoryUsage(),
                "activeSessions", stats.getActiveSessions(),
                "correlationId", correlationId != null ? correlationId : "unknown"
            ));
            
            // Trigger aggressive cleanup
            optimizeConversationMemory().subscribe();
        }
    }
    
    @Override
    public Health health() {
        MemoryUsageStats stats = getMemoryStats();
        
        Health.Builder healthBuilder = Health.up()
                .withDetail("totalConversations", stats.getTotalConversations())
                .withDetail("totalMessages", stats.getTotalMessages())
                .withDetail("activeSessions", stats.getActiveSessions())
                .withDetail("totalMemoryUsage", stats.getTotalMemoryUsage())
                .withDetail("memoryUtilizationPercentage", stats.getMemoryUtilizationPercentage())
                .withDetail("cleanupOperations", stats.getCleanupOperations())
                .withDetail("expiredSessions", stats.getExpiredSessions());
        
        // Check for health issues
        if (stats.getMemoryUtilizationPercentage() > 90) {
            healthBuilder.down().withDetail("issue", "Critical memory utilization");
        } else if (stats.getMemoryUtilizationPercentage() > 80) {
            healthBuilder.down().withDetail("issue", "High memory utilization");
        }
        
        if (stats.getActiveSessions() > 1000) {
            healthBuilder.down().withDetail("issue", "High number of active sessions");
        }
        
        return healthBuilder.build();
    }
    
    private void optimizeActiveConversations() {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        for (Map.Entry<String, ConversationSession> entry : activeSessions.entrySet()) {
            String conversationId = entry.getKey();
            ConversationSession session = entry.getValue();
            
            try {
                List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);
                
                // Trim conversation if it has too many messages
                if (messages.size() > MAX_MESSAGES_PER_CONVERSATION) {
                    int messagesToRemove = messages.size() - MAX_MESSAGES_PER_CONVERSATION;
                    List<Message> trimmedMessages = messages.subList(messagesToRemove, messages.size());
                    
                    chatMemoryRepository.deleteByConversationId(conversationId);
                    chatMemoryRepository.saveAll(conversationId, trimmedMessages);
                    
                    structuredLogger.debug("Conversation trimmed", Map.of(
                        "operation", "conversation_trimmed",
                        "conversationId", conversationId,
                        "originalMessageCount", messages.size(),
                        "trimmedMessageCount", trimmedMessages.size(),
                        "removedMessages", messagesToRemove,
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                    
                    customMetrics.recordConversationTrimmed(conversationId, messagesToRemove);
                }
                
            } catch (Exception e) {
                structuredLogger.error("Failed to optimize conversation", Map.of(
                    "operation", "conversation_optimization_failed",
                    "conversationId", conversationId,
                    "errorType", e.getClass().getSimpleName(),
                    "correlationId", correlationId != null ? correlationId : "unknown"
                ), e);
            }
        }
    }
    
    private boolean isSessionExpired(ConversationSession session, Instant now) {
        Duration sessionAge = Duration.between(session.getCreatedAt(), now);
        Duration inactivityDuration = Duration.between(session.getLastActivity(), now);
        
        return sessionAge.compareTo(DEFAULT_SESSION_TIMEOUT) > 0 || 
               inactivityDuration.compareTo(INACTIVE_SESSION_TIMEOUT) > 0;
    }
    
    private long calculateCurrentMemoryUsage() {
        long memoryUsage = 0;
        
        for (ConversationSession session : activeSessions.values()) {
            try {
                List<Message> messages = chatMemoryRepository.findByConversationId(session.getConversationId());
                for (Message message : messages) {
                    memoryUsage += estimateMessageSize(message);
                }
            } catch (Exception e) {
                // Continue with other sessions if one fails
            }
        }
        
        totalMemoryUsage.set(memoryUsage);
        return memoryUsage;
    }
    
    private long estimateMessageSize(Message message) {
        // Rough estimation of message size in bytes
        String content = message.toString();
        return content != null ? content.getBytes().length + 100 : 100; // +100 for metadata
    }
    
    private double calculateMemoryUtilization(long currentUsage) {
        return (double) currentUsage / MAX_MEMORY_USAGE_BYTES * 100.0;
    }
    
    private void updateMemoryUsageStats() {
        MemoryUsageStats stats = getMemoryStats();
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        structuredLogger.debug("Memory usage statistics updated", Map.of(
            "operation", "memory_stats_update",
            "totalConversations", stats.getTotalConversations(),
            "totalMessages", stats.getTotalMessages(),
            "activeSessions", stats.getActiveSessions(),
            "memoryUtilization", String.format("%.2f%%", stats.getMemoryUtilizationPercentage()),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
    }
    
    /**
     * Conversation session data class
     */
    public static class ConversationSession {
        private final String conversationId;
        private final String userId;
        private final String source;
        private final Instant createdAt;
        private Instant lastActivity;
        private int messageCount;
        
        private ConversationSession(Builder builder) {
            this.conversationId = builder.conversationId;
            this.userId = builder.userId;
            this.source = builder.source;
            this.createdAt = builder.createdAt;
            this.lastActivity = builder.lastActivity;
            this.messageCount = builder.messageCount;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters and setters
        public String getConversationId() { return conversationId; }
        public String getUserId() { return userId; }
        public String getSource() { return source; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastActivity() { return lastActivity; }
        public void setLastActivity(Instant lastActivity) { this.lastActivity = lastActivity; }
        public int getMessageCount() { return messageCount; }
        public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
        
        public static class Builder {
            private String conversationId;
            private String userId;
            private String source;
            private Instant createdAt;
            private Instant lastActivity;
            private int messageCount;
            
            public Builder conversationId(String conversationId) {
                this.conversationId = conversationId;
                return this;
            }
            
            public Builder userId(String userId) {
                this.userId = userId;
                return this;
            }
            
            public Builder source(String source) {
                this.source = source;
                return this;
            }
            
            public Builder createdAt(Instant createdAt) {
                this.createdAt = createdAt;
                return this;
            }
            
            public Builder lastActivity(Instant lastActivity) {
                this.lastActivity = lastActivity;
                return this;
            }
            
            public Builder messageCount(int messageCount) {
                this.messageCount = messageCount;
                return this;
            }
            
            public ConversationSession build() {
                return new ConversationSession(this);
            }
        }
    }
    
    /**
     * Memory usage statistics data class
     */
    public static class MemoryUsageStats {
        private final long totalConversations;
        private final long totalMessages;
        private final int activeSessions;
        private final long totalMemoryUsage;
        private final long cleanupOperations;
        private final long expiredSessions;
        private final double memoryUtilizationPercentage;
        
        private MemoryUsageStats(Builder builder) {
            this.totalConversations = builder.totalConversations;
            this.totalMessages = builder.totalMessages;
            this.activeSessions = builder.activeSessions;
            this.totalMemoryUsage = builder.totalMemoryUsage;
            this.cleanupOperations = builder.cleanupOperations;
            this.expiredSessions = builder.expiredSessions;
            this.memoryUtilizationPercentage = builder.memoryUtilizationPercentage;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public long getTotalConversations() { return totalConversations; }
        public long getTotalMessages() { return totalMessages; }
        public int getActiveSessions() { return activeSessions; }
        public long getTotalMemoryUsage() { return totalMemoryUsage; }
        public long getCleanupOperations() { return cleanupOperations; }
        public long getExpiredSessions() { return expiredSessions; }
        public double getMemoryUtilizationPercentage() { return memoryUtilizationPercentage; }
        
        public static class Builder {
            private long totalConversations;
            private long totalMessages;
            private int activeSessions;
            private long totalMemoryUsage;
            private long cleanupOperations;
            private long expiredSessions;
            private double memoryUtilizationPercentage;
            
            public Builder totalConversations(long totalConversations) {
                this.totalConversations = totalConversations;
                return this;
            }
            
            public Builder totalMessages(long totalMessages) {
                this.totalMessages = totalMessages;
                return this;
            }
            
            public Builder activeSessions(int activeSessions) {
                this.activeSessions = activeSessions;
                return this;
            }
            
            public Builder totalMemoryUsage(long totalMemoryUsage) {
                this.totalMemoryUsage = totalMemoryUsage;
                return this;
            }
            
            public Builder cleanupOperations(long cleanupOperations) {
                this.cleanupOperations = cleanupOperations;
                return this;
            }
            
            public Builder expiredSessions(long expiredSessions) {
                this.expiredSessions = expiredSessions;
                return this;
            }
            
            public Builder memoryUtilizationPercentage(double memoryUtilizationPercentage) {
                this.memoryUtilizationPercentage = memoryUtilizationPercentage;
                return this;
            }
            
            public MemoryUsageStats build() {
                return new MemoryUsageStats(this);
            }
        }
    }
}