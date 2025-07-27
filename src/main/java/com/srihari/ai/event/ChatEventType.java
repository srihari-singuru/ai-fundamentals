package com.srihari.ai.event;

/**
 * Enumeration of all possible chat event types in the system.
 * Used for event classification and routing.
 */
public enum ChatEventType {
    
    // Conversation lifecycle events
    CONVERSATION_STARTED("Conversation started"),
    CONVERSATION_ENDED("Conversation ended"),
    
    // Message events
    MESSAGE_RECEIVED("User message received"),
    MESSAGE_PROCESSED("User message processed"),
    RESPONSE_GENERATED("AI response generated"),
    RESPONSE_STREAMED("AI response streamed"),
    
    // Error events
    ERROR_OCCURRED("Error occurred"),
    VALIDATION_FAILED("Input validation failed"),
    AI_SERVICE_ERROR("AI service error"),
    
    // System events
    RATE_LIMIT_EXCEEDED("Rate limit exceeded"),
    CIRCUIT_BREAKER_OPENED("Circuit breaker opened"),
    CIRCUIT_BREAKER_CLOSED("Circuit breaker closed"),
    
    // Performance events
    SLOW_RESPONSE("Slow response detected"),
    HIGH_TOKEN_USAGE("High token usage detected"),
    
    // Security events
    SUSPICIOUS_INPUT("Suspicious input detected"),
    AUTHENTICATION_FAILED("Authentication failed");
    
    private final String description;
    
    ChatEventType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}