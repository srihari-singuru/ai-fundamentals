package com.srihari.ai.service.chat.strategy;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration options for chat model strategies.
 * Provides a flexible way to pass model-specific parameters.
 */
@Data
@Builder
public class ChatModelOptions {
    
    /**
     * Temperature for response generation (0.0 to 2.0)
     * Higher values make output more random
     */
    @Builder.Default
    private Double temperature = 0.7;
    
    /**
     * Maximum number of tokens to generate
     */
    private Integer maxTokens;
    
    /**
     * Top-p sampling parameter (0.0 to 1.0)
     */
    private Double topP;
    
    /**
     * Frequency penalty (-2.0 to 2.0)
     */
    private Double frequencyPenalty;
    
    /**
     * Presence penalty (-2.0 to 2.0)
     */
    private Double presencePenalty;
    
    /**
     * Whether to enable streaming response
     */
    @Builder.Default
    private Boolean streaming = true;
    
    /**
     * Custom system prompt to use
     */
    private String systemPrompt;
    
    /**
     * Conversation ID for context tracking
     */
    private String conversationId;
    
    /**
     * Create default options
     */
    public static ChatModelOptions defaultOptions() {
        return ChatModelOptions.builder().build();
    }
    
    /**
     * Create options for API usage (streaming enabled)
     */
    public static ChatModelOptions forApi() {
        return ChatModelOptions.builder()
                .streaming(true)
                .temperature(0.7)
                .build();
    }
    
    /**
     * Create options for Web UI usage (complete response)
     */
    public static ChatModelOptions forWeb() {
        return ChatModelOptions.builder()
                .streaming(false)
                .temperature(0.7)
                .build();
    }
}