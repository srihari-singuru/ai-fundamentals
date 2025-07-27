package com.srihari.ai.service.chat.strategy;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.service.integration.OpenAiChatClient;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * OpenAI-specific implementation of the ChatModelStrategy.
 * Handles GPT-4 and other OpenAI models with specific optimizations
 * and error handling patterns.
 */
@Component
@RequiredArgsConstructor
public class OpenAiChatModelStrategy implements ChatModelStrategy {
    
    private final OpenAiChatClient openAiChatClient;
    private final StructuredLogger structuredLogger;
    
    private static final String GPT_4_MODEL_PREFIX = "gpt-4";
    private static final String GPT_3_5_MODEL_PREFIX = "gpt-3.5";
    
    @Override
    public Flux<String> generateStreamingResponse(String message, ChatModelOptions options) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        structuredLogger.debug("OpenAI streaming response generation started", Map.of(
            "operation", "openai_streaming_generation",
            "model", getModelName(),
            "messageLength", message.length(),
            "temperature", options.getTemperature(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        return openAiChatClient.sendStream(message)
                .doOnNext(token -> {
                    structuredLogger.debug("OpenAI token received", Map.of(
                        "operation", "openai_token_received",
                        "model", getModelName(),
                        "tokenLength", token.length(),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                })
                .doOnComplete(() -> {
                    structuredLogger.debug("OpenAI streaming response completed", Map.of(
                        "operation", "openai_streaming_completed",
                        "model", getModelName(),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                })
                .doOnError(error -> {
                    structuredLogger.error("OpenAI streaming response failed", Map.of(
                        "operation", "openai_streaming_failed",
                        "model", getModelName(),
                        "errorType", error.getClass().getSimpleName(),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ), error);
                });
    }
    
    @Override
    public Mono<String> generateCompleteResponse(List<Message> messages, ChatModelOptions options) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        structuredLogger.debug("OpenAI complete response generation started", Map.of(
            "operation", "openai_complete_generation",
            "model", getModelName(),
            "messageCount", messages.size(),
            "temperature", options.getTemperature(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        return openAiChatClient.sendComplete(messages)
                .doOnSuccess(response -> {
                    structuredLogger.debug("OpenAI complete response generated", Map.of(
                        "operation", "openai_complete_generated",
                        "model", getModelName(),
                        "responseLength", response != null ? response.length() : 0,
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                })
                .doOnError(error -> {
                    structuredLogger.error("OpenAI complete response failed", Map.of(
                        "operation", "openai_complete_failed",
                        "model", getModelName(),
                        "errorType", error.getClass().getSimpleName(),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ), error);
                });
    }
    
    @Override
    public boolean supports(String modelName) {
        if (modelName == null) {
            return false;
        }
        
        String lowerModel = modelName.toLowerCase();
        return lowerModel.startsWith(GPT_4_MODEL_PREFIX) || 
               lowerModel.startsWith(GPT_3_5_MODEL_PREFIX);
    }
    
    @Override
    public String getModelName() {
        return "gpt-4.1-nano"; // Default OpenAI model
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority for OpenAI models
    }
}