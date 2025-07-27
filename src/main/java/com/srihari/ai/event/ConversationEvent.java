package com.srihari.ai.event;

import java.util.Map;

import lombok.Getter;

/**
 * Event representing conversation lifecycle changes.
 * Fired when conversations are started, ended, or modified.
 */
@Getter
public class ConversationEvent extends ChatEvent {
    
    private final String conversationId;
    private final String userId;
    private final String source; // "api", "web"
    private final int messageCount;
    private final long durationMs;
    
    public ConversationEvent(ChatEventType eventType, String correlationId, 
                           String conversationId, String userId, String source, 
                           int messageCount, long durationMs, Map<String, Object> metadata) {
        super(eventType, correlationId, metadata);
        this.conversationId = conversationId;
        this.userId = userId;
        this.source = source;
        this.messageCount = messageCount;
        this.durationMs = durationMs;
    }
    
    @Override
    public String getSource() {
        return source;
    }
    
    @Override
    public String getDescription() {
        return String.format("%s for conversation %s (user: %s, source: %s, messages: %d)", 
                           getEventType().getDescription(), conversationId, userId, source, messageCount);
    }
    
    /**
     * Creates a conversation started event.
     */
    public static ConversationEvent started(String correlationId, String conversationId, 
                                          String userId, String source, Map<String, Object> metadata) {
        return new ConversationEvent(ChatEventType.CONVERSATION_STARTED, correlationId, 
                                   conversationId, userId, source, 0, 0, metadata);
    }
    
    /**
     * Creates a conversation ended event.
     */
    public static ConversationEvent ended(String correlationId, String conversationId, 
                                        String userId, String source, int messageCount, 
                                        long durationMs, Map<String, Object> metadata) {
        return new ConversationEvent(ChatEventType.CONVERSATION_ENDED, correlationId, 
                                   conversationId, userId, source, messageCount, durationMs, metadata);
    }
}