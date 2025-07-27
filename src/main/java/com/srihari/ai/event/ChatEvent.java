package com.srihari.ai.event;

import java.time.Instant;
import java.util.Map;

import lombok.Data;

/**
 * Base class for all chat-related events in the system.
 * Provides common event properties and metadata.
 */
@Data
public abstract class ChatEvent {
    
    private final String eventId;
    private final ChatEventType eventType;
    private final Instant timestamp;
    private final String correlationId;
    private final Map<String, Object> metadata;
    
    protected ChatEvent(ChatEventType eventType, String correlationId, Map<String, Object> metadata) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = Instant.now();
        this.correlationId = correlationId;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }
    
    /**
     * Gets the event source identifier.
     * 
     * @return the source identifier for this event
     */
    public abstract String getSource();
    
    /**
     * Gets a human-readable description of the event.
     * 
     * @return event description
     */
    public abstract String getDescription();
}