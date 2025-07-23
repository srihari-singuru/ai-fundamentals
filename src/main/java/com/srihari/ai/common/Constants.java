package com.srihari.ai.common;

public final class Constants {
    
    // View names
    public static final String CHAT_VIEW = "chat";
    
    // Default messages
    public static final String DEFAULT_SYSTEM_MESSAGE = "You are a helpful assistant.";
    public static final String AI_UNAVAILABLE_MESSAGE = "Sorry! AI is temporarily unavailable. Please try again.";
    public static final String AI_UNAVAILABLE_DETAILED = "[OpenAI service is temporarily unavailable. Please try again later.]";
    
    // Circuit breaker names
    public static final String OPENAI_CIRCUIT_BREAKER = "openai";
    
    // Session attributes
    public static final String CONVERSATION_ID_ATTR = "conversationId";
    
    // Model attributes
    public static final String CHAT_VIEW_ATTR = "chatView";
    public static final String INITIAL_SYSTEM_MESSAGE_ATTR = "initialSystemMessage";
    
    private Constants() {
        // Utility class
    }
}