package com.srihari.ai.event.listener;

import org.springframework.stereotype.Component;

import com.srihari.ai.event.ChatEvent;
import com.srihari.ai.event.ChatEventListener;
import com.srihari.ai.event.ConversationEvent;
import com.srihari.ai.event.MessageEvent;
import com.srihari.ai.metrics.CustomMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event listener that records metrics for all chat events.
 * Integrates with the CustomMetrics component to track performance and usage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsChatEventListener implements ChatEventListener {
    
    private final CustomMetrics customMetrics;
    
    @Override
    public void onConversationStarted(ConversationEvent event) {
        customMetrics.incrementConversationStarted(event.getSource());
        
        // Record user session started if we have user info
        if (event.getUserId() != null) {
            customMetrics.recordUserSessionStarted(event.getUserId(), "chat-session");
        }
        
        log.debug("Recorded conversation started metrics for source: {}", event.getSource());
    }
    
    @Override
    public void onConversationEnded(ConversationEvent event) {
        long durationMinutes = event.getDurationMs() / (1000 * 60);
        customMetrics.incrementConversationEnded(event.getSource(), durationMinutes);
        
        // Record user session ended if we have user info
        if (event.getUserId() != null) {
            customMetrics.recordUserSessionEnded(event.getUserId(), durationMinutes, event.getMessageCount());
        }
        
        log.debug("Recorded conversation ended metrics for source: {} (duration: {}min, messages: {})", 
                 event.getSource(), durationMinutes, event.getMessageCount());
    }
    
    @Override
    public void onMessageReceived(MessageEvent event) {
        customMetrics.incrementMessageCount(event.getSource(), "user");
        
        log.debug("Recorded message received metrics for source: {}", event.getSource());
    }
    
    @Override
    public void onMessageProcessed(MessageEvent event) {
        // Record processing time if available
        if (event.getProcessingTimeMs() > 0) {
            customMetrics.recordConversationDuration(
                event.getModel() != null ? event.getModel() : "unknown", 
                event.getProcessingTimeMs()
            );
        }
        
        log.debug("Recorded message processed metrics (processing time: {}ms)", event.getProcessingTimeMs());
    }
    
    @Override
    public void onResponseGenerated(MessageEvent event) {
        customMetrics.incrementMessageCount(event.getSource(), "assistant");
        
        // Record model latency
        if (event.getModel() != null && event.getProcessingTimeMs() > 0) {
            customMetrics.recordModelLatency(event.getModel(), "chat_completion", event.getProcessingTimeMs());
        }
        
        // Record token usage
        if (event.getTokenCount() > 0 && event.getModel() != null) {
            customMetrics.recordTokenUsage(event.getModel(), "chat_completion", event.getTokenCount());
        }
        
        log.debug("Recorded response generated metrics (model: {}, tokens: {}, processing time: {}ms)", 
                 event.getModel(), event.getTokenCount(), event.getProcessingTimeMs());
    }
    
    @Override
    public void onResponseStreamed(MessageEvent event) {
        // Record streaming token rate if we have the data
        if (event.getTokenCount() > 0 && event.getModel() != null) {
            customMetrics.recordTokenUsage(event.getModel(), "streaming", event.getTokenCount());
            
            // Calculate tokens per second if we have timing data
            if (event.getProcessingTimeMs() > 0) {
                double tokensPerSecond = (double) event.getTokenCount() / (event.getProcessingTimeMs() / 1000.0);
                customMetrics.recordTokenStreamingRate(event.getModel(), tokensPerSecond);
            }
        }
        
        log.debug("Recorded response streamed metrics (model: {}, tokens: {})", 
                 event.getModel(), event.getTokenCount());
    }
    
    @Override
    public void onErrorOccurred(ChatEvent event) {
        // Determine error type and model from event metadata
        String errorType = event.getEventType().toString().toLowerCase();
        String model = "unknown";
        
        // Try to extract model from metadata
        if (event.getMetadata().containsKey("model")) {
            model = event.getMetadata().get("model").toString();
        } else if (event instanceof MessageEvent messageEvent && messageEvent.getModel() != null) {
            model = messageEvent.getModel();
        }
        
        customMetrics.incrementAiErrors(model, errorType, event.getEventType().toString());
        
        log.debug("Recorded error metrics (type: {}, model: {})", errorType, model);
    }
    
    @Override
    public void onChatEvent(ChatEvent event) {
        // Record general event metrics
        switch (event.getEventType()) {
            case RATE_LIMIT_EXCEEDED -> {
                customMetrics.incrementCircuitBreakerEvents("rate_limiter", "limit_exceeded");
            }
            case CIRCUIT_BREAKER_OPENED -> {
                String circuitBreaker = event.getMetadata().getOrDefault("circuitBreaker", "unknown").toString();
                customMetrics.incrementCircuitBreakerEvents(circuitBreaker, "opened");
            }
            case CIRCUIT_BREAKER_CLOSED -> {
                String circuitBreaker = event.getMetadata().getOrDefault("circuitBreaker", "unknown").toString();
                customMetrics.incrementCircuitBreakerEvents(circuitBreaker, "closed");
            }
            case SLOW_RESPONSE -> {
                // Could record slow response metrics here
                log.debug("Slow response detected: {}", event.getDescription());
            }
            case HIGH_TOKEN_USAGE -> {
                // Could record high token usage metrics here
                log.debug("High token usage detected: {}", event.getDescription());
            }
            default -> {
                // No specific metrics for other event types
            }
        }
    }
    
    @Override
    public int getPriority() {
        return 90; // High priority for metrics, but after logging
    }
    
    @Override
    public boolean isEnabled() {
        return true; // Always enabled
    }
}