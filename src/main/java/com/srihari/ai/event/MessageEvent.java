package com.srihari.ai.event;

import java.util.Map;

import lombok.Getter;

/**
 * Event representing message processing activities.
 * Fired when messages are received, processed, or responses are generated.
 */
@Getter
public class MessageEvent extends ChatEvent {
    
    private final String conversationId;
    private final String messageId;
    private final String userId;
    private final String source; // "api", "web"
    private final String model;
    private final int messageLength;
    private final int responseLength;
    private final long processingTimeMs;
    private final int tokenCount;
    
    public MessageEvent(ChatEventType eventType, String correlationId, 
                       String conversationId, String messageId, String userId, String source,
                       String model, int messageLength, int responseLength, 
                       long processingTimeMs, int tokenCount, Map<String, Object> metadata) {
        super(eventType, correlationId, metadata);
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.userId = userId;
        this.source = source;
        this.model = model;
        this.messageLength = messageLength;
        this.responseLength = responseLength;
        this.processingTimeMs = processingTimeMs;
        this.tokenCount = tokenCount;
    }
    
    @Override
    public String getSource() {
        return source;
    }
    
    @Override
    public String getDescription() {
        return String.format("%s for message %s (conversation: %s, user: %s, model: %s, tokens: %d)", 
                           getEventType().getDescription(), messageId, conversationId, userId, model, tokenCount);
    }
    
    /**
     * Creates a message received event.
     */
    public static MessageEvent received(String correlationId, String conversationId, 
                                      String messageId, String userId, String source,
                                      int messageLength, Map<String, Object> metadata) {
        return new MessageEvent(ChatEventType.MESSAGE_RECEIVED, correlationId, 
                              conversationId, messageId, userId, source, null, 
                              messageLength, 0, 0, 0, metadata);
    }
    
    /**
     * Creates a message processed event.
     */
    public static MessageEvent processed(String correlationId, String conversationId, 
                                       String messageId, String userId, String source,
                                       int messageLength, long processingTimeMs, Map<String, Object> metadata) {
        return new MessageEvent(ChatEventType.MESSAGE_PROCESSED, correlationId, 
                              conversationId, messageId, userId, source, null, 
                              messageLength, 0, processingTimeMs, 0, metadata);
    }
    
    /**
     * Creates a response generated event.
     */
    public static MessageEvent responseGenerated(String correlationId, String conversationId, 
                                               String messageId, String userId, String source,
                                               String model, int messageLength, int responseLength,
                                               long processingTimeMs, int tokenCount, Map<String, Object> metadata) {
        return new MessageEvent(ChatEventType.RESPONSE_GENERATED, correlationId, 
                              conversationId, messageId, userId, source, model, 
                              messageLength, responseLength, processingTimeMs, tokenCount, metadata);
    }
    
    /**
     * Creates a response streamed event.
     */
    public static MessageEvent responseStreamed(String correlationId, String conversationId, 
                                              String messageId, String userId, String source,
                                              String model, int responseLength, int tokenCount, 
                                              Map<String, Object> metadata) {
        return new MessageEvent(ChatEventType.RESPONSE_STREAMED, correlationId, 
                              conversationId, messageId, userId, source, model, 
                              0, responseLength, 0, tokenCount, metadata);
    }
}