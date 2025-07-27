package com.srihari.ai.service.chat;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.service.chat.strategy.ChatModelOptions;
import com.srihari.ai.service.chat.strategy.ChatModelStrategy;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service that orchestrates the selection and execution of appropriate
 * ChatModelStrategy implementations based on the requested model.
 * Implements the Strategy pattern to support multiple AI providers.
 */
@Service
@RequiredArgsConstructor
public class ChatModelService {
    
    private final List<ChatModelStrategy> strategies;
    private final StructuredLogger structuredLogger;
    
    /**
     * Generate streaming response using the appropriate strategy
     * 
     * @param message The user message
     * @param modelName The requested model name
     * @param options Chat options
     * @return Flux of response tokens
     */
    public Flux<String> generateStreamingResponse(String message, String modelName, ChatModelOptions options) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        return findStrategy(modelName)
                .map(strategy -> {
                    structuredLogger.debug("Strategy selected for streaming response", Map.of(
                        "operation", "strategy_selection",
                        "selectedStrategy", strategy.getClass().getSimpleName(),
                        "requestedModel", modelName,
                        "actualModel", strategy.getModelName(),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                    
                    return strategy.generateStreamingResponse(message, options);
                })
                .orElseGet(() -> {
                    structuredLogger.warn("No strategy found for model, using fallback", Map.of(
                        "operation", "strategy_fallback",
                        "requestedModel", modelName,
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                    
                    return Flux.just("I'm sorry, but the requested AI model '" + modelName + "' is not available. Please try again with a supported model.");
                });
    }
    
    /**
     * Generate complete response using the appropriate strategy
     * 
     * @param messages The conversation messages
     * @param modelName The requested model name
     * @param options Chat options
     * @return Mono containing the complete response
     */
    public Mono<String> generateCompleteResponse(List<Message> messages, String modelName, ChatModelOptions options) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        return findStrategy(modelName)
                .map(strategy -> {
                    structuredLogger.debug("Strategy selected for complete response", Map.of(
                        "operation", "strategy_selection",
                        "selectedStrategy", strategy.getClass().getSimpleName(),
                        "requestedModel", modelName,
                        "actualModel", strategy.getModelName(),
                        "messageCount", messages.size(),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                    
                    return strategy.generateCompleteResponse(messages, options);
                })
                .orElseGet(() -> {
                    structuredLogger.warn("No strategy found for model, using fallback", Map.of(
                        "operation", "strategy_fallback",
                        "requestedModel", modelName,
                        "messageCount", messages.size(),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                    
                    return Mono.just("I'm sorry, but the requested AI model '" + modelName + "' is not available. Please try again with a supported model.");
                });
    }
    
    /**
     * Get all available model names from registered strategies
     * 
     * @return List of supported model names
     */
    public List<String> getAvailableModels() {
        return strategies.stream()
                .map(ChatModelStrategy::getModelName)
                .distinct()
                .sorted()
                .toList();
    }
    
    /**
     * Check if a model is supported by any strategy
     * 
     * @param modelName The model name to check
     * @return true if the model is supported
     */
    public boolean isModelSupported(String modelName) {
        return findStrategy(modelName).isPresent();
    }
    
    /**
     * Find the best strategy for the given model name
     * 
     * @param modelName The model name to find a strategy for
     * @return Optional containing the best strategy, or empty if none found
     */
    private Optional<ChatModelStrategy> findStrategy(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            // Use default strategy (highest priority)
            return strategies.stream()
                    .max(Comparator.comparing(ChatModelStrategy::getPriority));
        }
        
        return strategies.stream()
                .filter(strategy -> strategy.supports(modelName))
                .max(Comparator.comparing(ChatModelStrategy::getPriority));
    }
}