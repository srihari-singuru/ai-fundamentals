package com.srihari.ai.event.listener;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.event.ChatEvent;
import com.srihari.ai.event.ChatEventListener;
import com.srihari.ai.event.ConversationEvent;
import com.srihari.ai.event.MessageEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event listener that handles notifications for important chat events.
 * Can be enabled/disabled via configuration and handles alerts for critical events.
 */
@Component
@ConditionalOnProperty(name = "app.events.notifications.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class NotificationChatEventListener implements ChatEventListener {
    
    private final StructuredLogger structuredLogger;
    
    @Override
    public void onConversationStarted(ConversationEvent event) {
        // Could send notifications for new conversations if needed
        log.debug("Conversation started notification: {}", event.getDescription());
    }
    
    @Override
    public void onConversationEnded(ConversationEvent event) {
        // Notify for long conversations or high message counts
        if (event.getDurationMs() > 30 * 60 * 1000) { // 30 minutes
            structuredLogger.info("Long conversation detected", Map.of(
                "operation", "long_conversation_notification",
                "conversationId", event.getConversationId(),
                "durationMs", event.getDurationMs(),
                "messageCount", event.getMessageCount(),
                "source", event.getSource()
            ));
        }
        
        if (event.getMessageCount() > 50) {
            structuredLogger.info("High message count conversation detected", Map.of(
                "operation", "high_message_count_notification",
                "conversationId", event.getConversationId(),
                "messageCount", event.getMessageCount(),
                "source", event.getSource()
            ));
        }
    }
    
    @Override
    public void onResponseGenerated(MessageEvent event) {
        // Notify for slow responses
        if (event.getProcessingTimeMs() > 10000) { // 10 seconds
            structuredLogger.warn("Slow response detected", Map.of(
                "operation", "slow_response_notification",
                "conversationId", event.getConversationId(),
                "messageId", event.getMessageId(),
                "processingTimeMs", event.getProcessingTimeMs(),
                "model", event.getModel() != null ? event.getModel() : "unknown"
            ));
        }
        
        // Notify for high token usage
        if (event.getTokenCount() > 2000) {
            structuredLogger.warn("High token usage detected", Map.of(
                "operation", "high_token_usage_notification",
                "conversationId", event.getConversationId(),
                "messageId", event.getMessageId(),
                "tokenCount", event.getTokenCount(),
                "model", event.getModel() != null ? event.getModel() : "unknown"
            ));
        }
    }
    
    @Override
    public void onErrorOccurred(ChatEvent event) {
        // Send notifications for critical errors
        switch (event.getEventType()) {
            case AI_SERVICE_ERROR -> {
                structuredLogger.error("AI service error notification", Map.of(
                    "operation", "ai_service_error_notification",
                    "eventType", event.getEventType().toString(),
                    "description", event.getDescription(),
                    "source", event.getSource(),
                    "metadata", event.getMetadata()
                ), null);
            }
            case RATE_LIMIT_EXCEEDED -> {
                structuredLogger.warn("Rate limit exceeded notification", Map.of(
                    "operation", "rate_limit_notification",
                    "eventType", event.getEventType().toString(),
                    "description", event.getDescription(),
                    "source", event.getSource()
                ));
            }
            case CIRCUIT_BREAKER_OPENED -> {
                structuredLogger.error("Circuit breaker opened notification", Map.of(
                    "operation", "circuit_breaker_notification",
                    "eventType", event.getEventType().toString(),
                    "description", event.getDescription(),
                    "source", event.getSource(),
                    "metadata", event.getMetadata()
                ), null);
            }
            case SUSPICIOUS_INPUT -> {
                structuredLogger.warn("Suspicious input detected notification", Map.of(
                    "operation", "suspicious_input_notification",
                    "eventType", event.getEventType().toString(),
                    "description", event.getDescription(),
                    "source", event.getSource()
                ));
            }
            case AUTHENTICATION_FAILED -> {
                structuredLogger.warn("Authentication failed notification", Map.of(
                    "operation", "auth_failed_notification",
                    "eventType", event.getEventType().toString(),
                    "description", event.getDescription(),
                    "source", event.getSource()
                ));
            }
            default -> {
                log.debug("General error notification: {}", event.getDescription());
            }
        }
    }
    
    @Override
    public int getPriority() {
        return 50; // Medium priority for notifications
    }
    
    @Override
    public boolean isEnabled() {
        return true; // Enabled when the conditional property is true
    }
}