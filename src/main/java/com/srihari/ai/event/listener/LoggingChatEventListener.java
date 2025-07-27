package com.srihari.ai.event.listener;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.event.ChatEvent;
import com.srihari.ai.event.ChatEventListener;
import com.srihari.ai.event.ConversationEvent;
import com.srihari.ai.event.MessageEvent;

import lombok.RequiredArgsConstructor;

/**
 * Event listener that logs all chat events with structured logging.
 * Provides detailed logging for conversation and message events.
 */
@Component
@RequiredArgsConstructor
public class LoggingChatEventListener implements ChatEventListener {
    
    private final StructuredLogger structuredLogger;
    
    @Override
    public void onConversationStarted(ConversationEvent event) {
        structuredLogger.info("Conversation started", Map.of(
            "operation", "conversation_started",
            "conversationId", event.getConversationId(),
            "userId", event.getUserId() != null ? event.getUserId() : "anonymous",
            "source", event.getSource(),
            "correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "unknown",
            "eventId", event.getEventId(),
            "timestamp", event.getTimestamp().toString()
        ));
    }
    
    @Override
    public void onConversationEnded(ConversationEvent event) {
        structuredLogger.info("Conversation ended", Map.of(
            "operation", "conversation_ended",
            "conversationId", event.getConversationId(),
            "userId", event.getUserId() != null ? event.getUserId() : "anonymous",
            "source", event.getSource(),
            "messageCount", event.getMessageCount(),
            "durationMs", event.getDurationMs(),
            "correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "unknown",
            "eventId", event.getEventId(),
            "timestamp", event.getTimestamp().toString()
        ));
    }
    
    @Override
    public void onMessageReceived(MessageEvent event) {
        structuredLogger.info("Message received", Map.of(
            "operation", "message_received",
            "conversationId", event.getConversationId(),
            "messageId", event.getMessageId(),
            "userId", event.getUserId() != null ? event.getUserId() : "anonymous",
            "source", event.getSource(),
            "messageLength", event.getMessageLength(),
            "correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "unknown",
            "eventId", event.getEventId(),
            "timestamp", event.getTimestamp().toString()
        ));
    }
    
    @Override
    public void onMessageProcessed(MessageEvent event) {
        structuredLogger.info("Message processed", Map.of(
            "operation", "message_processed",
            "conversationId", event.getConversationId(),
            "messageId", event.getMessageId(),
            "userId", event.getUserId() != null ? event.getUserId() : "anonymous",
            "source", event.getSource(),
            "messageLength", event.getMessageLength(),
            "processingTimeMs", event.getProcessingTimeMs(),
            "correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "unknown",
            "eventId", event.getEventId(),
            "timestamp", event.getTimestamp().toString()
        ));
    }
    
    @Override
    public void onResponseGenerated(MessageEvent event) {
        Map<String, Object> context = new HashMap<>();
        context.put("operation", "response_generated");
        context.put("conversationId", event.getConversationId());
        context.put("messageId", event.getMessageId());
        context.put("userId", event.getUserId() != null ? event.getUserId() : "anonymous");
        context.put("source", event.getSource());
        context.put("model", event.getModel() != null ? event.getModel() : "unknown");
        context.put("messageLength", event.getMessageLength());
        context.put("responseLength", event.getResponseLength());
        context.put("processingTimeMs", event.getProcessingTimeMs());
        context.put("tokenCount", event.getTokenCount());
        context.put("correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "unknown");
        context.put("eventId", event.getEventId());
        context.put("timestamp", event.getTimestamp().toString());
        
        structuredLogger.info("Response generated", context);
    }
    
    @Override
    public void onResponseStreamed(MessageEvent event) {
        Map<String, Object> context = new HashMap<>();
        context.put("operation", "response_streamed");
        context.put("conversationId", event.getConversationId());
        context.put("messageId", event.getMessageId());
        context.put("userId", event.getUserId() != null ? event.getUserId() : "anonymous");
        context.put("source", event.getSource());
        context.put("model", event.getModel() != null ? event.getModel() : "unknown");
        context.put("responseLength", event.getResponseLength());
        context.put("tokenCount", event.getTokenCount());
        context.put("correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "unknown");
        context.put("eventId", event.getEventId());
        context.put("timestamp", event.getTimestamp().toString());
        
        structuredLogger.debug("Response streamed", context);
    }
    
    @Override
    public void onErrorOccurred(ChatEvent event) {
        structuredLogger.error("Chat event error occurred", Map.of(
            "operation", "chat_event_error",
            "eventType", event.getEventType().toString(),
            "eventDescription", event.getDescription(),
            "source", event.getSource(),
            "correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "unknown",
            "eventId", event.getEventId(),
            "timestamp", event.getTimestamp().toString(),
            "metadata", event.getMetadata()
        ), null);
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority for logging
    }
    
    @Override
    public boolean isEnabled() {
        return true; // Always enabled
    }
}