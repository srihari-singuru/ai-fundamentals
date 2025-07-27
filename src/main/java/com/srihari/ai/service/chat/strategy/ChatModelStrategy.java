package com.srihari.ai.service.chat.strategy;

import java.util.List;

import org.springframework.ai.chat.messages.Message;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Strategy interface for different AI model implementations.
 * Allows the system to support multiple AI providers and models
 * with different capabilities and configurations.
 */
public interface ChatModelStrategy {
    
    /**
     * Generate streaming response for a single message (API use case)
     * 
     * @param message The user message to process
     * @param options Additional options for the chat request
     * @return Flux of response tokens
     */
    Flux<String> generateStreamingResponse(String message, ChatModelOptions options);
    
    /**
     * Generate complete response for conversation messages (Web UI use case)
     * 
     * @param messages List of conversation messages
     * @param options Additional options for the chat request
     * @return Mono containing the complete response
     */
    Mono<String> generateCompleteResponse(List<Message> messages, ChatModelOptions options);
    
    /**
     * Check if this strategy supports the given model name
     * 
     * @param modelName The model name to check
     * @return true if this strategy can handle the model
     */
    boolean supports(String modelName);
    
    /**
     * Get the model name this strategy handles
     * 
     * @return The model name
     */
    String getModelName();
    
    /**
     * Get the priority of this strategy (higher number = higher priority)
     * Used when multiple strategies support the same model
     * 
     * @return Priority value
     */
    default int getPriority() {
        return 0;
    }
}