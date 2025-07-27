package com.srihari.ai.service.chat;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.event.ChatEventPublisher;
import com.srihari.ai.event.MessageEvent;
import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.service.CachingService;
import com.srihari.ai.service.chat.strategy.ChatModelOptions;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
public class ApiChatService {

    private final ChatModelService chatModelService;
    private final StructuredLogger structuredLogger;
    private final CustomMetrics customMetrics;
    private final CachingService cachingService;
    private final ChatEventPublisher eventPublisher;

    public Flux<String> generateResponse(String userMessage) {
        return generateResponse(userMessage, "gpt-4.1-nano", ChatModelOptions.forApi());
    }
    
    public Flux<String> generateResponse(String userMessage, String modelName, ChatModelOptions options) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        String messageId = UUID.randomUUID().toString();
        String conversationId = UUID.randomUUID().toString(); // In real app, this would come from session
        long startTime = System.currentTimeMillis();
        
        structuredLogger.debug("Starting AI response generation", Map.of(
            "operation", "ai_response_generation",
            "messageLength", userMessage.length(),
            "model", modelName,
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        // Publish message received event
        eventPublisher.publishEventAsync(MessageEvent.received(
            correlationId, conversationId, messageId, "anonymous", "api", 
            userMessage.length(), Map.of("model", modelName)
        ));
        
        // Record message metrics
        customMetrics.incrementMessageCount("api", "user");
        
        // Generate cache key for the message
        String messageHash = cachingService.generateMessageHash(userMessage, modelName, options);
        
        // Check cache first
        String cachedResponse = cachingService.getCachedResponse(messageHash, userMessage, modelName);
        if (cachedResponse != null) {
            structuredLogger.debug("Cache hit for message", Map.of(
                "operation", "cache_hit",
                "messageHash", messageHash,
                "model", modelName,
                "correlationId", correlationId != null ? correlationId : "unknown"
            ));
            customMetrics.incrementCacheHit("responses");
            
            // Publish cached response event
            eventPublisher.publishEventAsync(MessageEvent.responseGenerated(
                correlationId, conversationId, messageId, "anonymous", "api",
                modelName, userMessage.length(), cachedResponse.length(),
                0, cachedResponse.length() / 4, // Rough token estimate
                Map.of("cached", true)
            ));
            
            return Flux.just(cachedResponse);
        }
        
        // Cache miss - call AI service via strategy
        structuredLogger.debug("Cache miss - calling AI service via strategy", Map.of(
            "operation", "cache_miss",
            "messageHash", messageHash,
            "model", modelName,
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        return chatModelService.generateStreamingResponse(userMessage, modelName, options)
                .collectList()
                .flatMapMany(tokens -> {
                    String fullResponse = String.join("", tokens);
                    long processingTime = System.currentTimeMillis() - startTime;
                    
                    // Publish response generated event
                    eventPublisher.publishEventAsync(MessageEvent.responseGenerated(
                        correlationId, conversationId, messageId, "anonymous", "api",
                        modelName, userMessage.length(), fullResponse.length(),
                        processingTime, fullResponse.length() / 4, // Rough token estimate
                        Map.of("cached", false)
                    ));
                    
                    // Cache the response if it should be cached
                    if (cachingService.shouldCacheResponse(userMessage, fullResponse)) {
                        cachingService.cacheResponse(messageHash, fullResponse, userMessage, modelName);
                        structuredLogger.debug("Response cached", Map.of(
                            "operation", "response_cached",
                            "messageHash", messageHash,
                            "responseLength", fullResponse.length(),
                            "model", modelName,
                            "correlationId", correlationId != null ? correlationId : "unknown"
                        ));
                    }
                    
                    return Flux.fromIterable(tokens);
                })
                .doOnNext(token -> {
                    structuredLogger.debug("Token received from AI service", Map.of(
                        "operation", "token_received",
                        "tokenLength", token.length(),
                        "model", modelName,
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                })
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(2))
                                .jitter(0.4)
                                .filter(this::isRetryable)
                                .doBeforeRetry(retrySignal -> {
                                    structuredLogger.warn("Retrying AI service call", Map.of(
                                        "operation", "ai_service_retry",
                                        "attempt", retrySignal.totalRetries() + 1,
                                        "maxAttempts", 3,
                                        "errorType", retrySignal.failure().getClass().getSimpleName(),
                                        "model", modelName,
                                        "correlationId", correlationId != null ? correlationId : "unknown"
                                    ));
                                })
                                .onRetryExhaustedThrow((spec, signal) -> {
                                    structuredLogger.error("AI service retry exhausted", Map.of(
                                        "operation", "ai_service_retry_exhausted",
                                        "totalAttempts", signal.totalRetries(),
                                        "model", modelName,
                                        "correlationId", correlationId != null ? correlationId : "unknown"
                                    ), signal.failure());
                                    return signal.failure();
                                })
                );
    }

    private boolean isRetryable(Throwable ex) {
        boolean retryable = ex instanceof IOException || ex.getMessage().contains("429");
        
        if (retryable) {
            String correlationId = CorrelationIdHolder.getCorrelationId();
            structuredLogger.debug("Exception is retryable", Map.of(
                "operation", "retry_evaluation",
                "exceptionType", ex.getClass().getSimpleName(),
                "retryable", true,
                "correlationId", correlationId != null ? correlationId : "unknown"
            ));
        }
        
        return retryable;
    }
}