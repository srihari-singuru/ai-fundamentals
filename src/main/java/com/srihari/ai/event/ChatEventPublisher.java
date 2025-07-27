package com.srihari.ai.event;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for publishing chat events to registered listeners.
 * Supports both synchronous and asynchronous event processing.
 */
@Service
@Slf4j
public class ChatEventPublisher {
    
    private final List<ChatEventListener> listeners;
    private final StructuredLogger structuredLogger;
    private final Executor taskExecutor;
    
    public ChatEventPublisher(List<ChatEventListener> listeners, 
                             StructuredLogger structuredLogger,
                             @Qualifier("eventTaskExecutor") Executor taskExecutor) {
        this.listeners = listeners;
        this.structuredLogger = structuredLogger;
        this.taskExecutor = taskExecutor;
    }
    
    /**
     * Publishes an event to all registered listeners synchronously.
     * 
     * @param event the event to publish
     */
    public void publishEvent(ChatEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null event");
            return;
        }
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        structuredLogger.debug("Publishing chat event", Map.of(
            "eventType", event.getEventType().toString(),
            "eventId", event.getEventId(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        // Sort listeners by priority (highest first)
        List<ChatEventListener> sortedListeners = listeners.stream()
                .filter(ChatEventListener::isEnabled)
                .sorted(Comparator.comparingInt(ChatEventListener::getPriority).reversed())
                .toList();
        
        for (ChatEventListener listener : sortedListeners) {
            try {
                // Call the generic event handler first
                listener.onChatEvent(event);
                
                // Then call specific event handlers based on event type
                dispatchToSpecificHandler(listener, event);
                
            } catch (Exception e) {
                structuredLogger.error("Error in event listener", Map.of(
                    "listenerClass", listener.getClass().getSimpleName(),
                    "eventType", event.getEventType().toString(),
                    "eventId", event.getEventId(),
                    "correlationId", correlationId != null ? correlationId : "unknown"
                ), e);
            }
        }
    }
    
    /**
     * Publishes an event to all registered listeners asynchronously.
     * 
     * @param event the event to publish
     * @return CompletableFuture that completes when all listeners have processed the event
     */
    @Async
    public CompletableFuture<Void> publishEventAsync(ChatEvent event) {
        return CompletableFuture.runAsync(() -> publishEvent(event), taskExecutor);
    }
    
    /**
     * Publishes multiple events in batch synchronously.
     * 
     * @param events the events to publish
     */
    public void publishEvents(List<ChatEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        for (ChatEvent event : events) {
            publishEvent(event);
        }
    }
    
    /**
     * Publishes multiple events in batch asynchronously.
     * 
     * @param events the events to publish
     * @return CompletableFuture that completes when all events have been processed
     */
    @Async
    public CompletableFuture<Void> publishEventsAsync(List<ChatEvent> events) {
        return CompletableFuture.runAsync(() -> publishEvents(events), taskExecutor);
    }
    
    /**
     * Gets the count of registered and enabled listeners.
     * 
     * @return the number of active listeners
     */
    public int getActiveListenerCount() {
        return (int) listeners.stream()
                .filter(ChatEventListener::isEnabled)
                .count();
    }
    
    /**
     * Gets information about registered listeners.
     * 
     * @return list of listener class names and their status
     */
    public List<String> getListenerInfo() {
        return listeners.stream()
                .map(listener -> String.format("%s (priority: %d, enabled: %s)", 
                    listener.getClass().getSimpleName(),
                    listener.getPriority(),
                    listener.isEnabled()))
                .toList();
    }
    
    private void dispatchToSpecificHandler(ChatEventListener listener, ChatEvent event) {
        switch (event.getEventType()) {
            case CONVERSATION_STARTED -> {
                if (event instanceof ConversationEvent conversationEvent) {
                    listener.onConversationStarted(conversationEvent);
                }
            }
            case CONVERSATION_ENDED -> {
                if (event instanceof ConversationEvent conversationEvent) {
                    listener.onConversationEnded(conversationEvent);
                }
            }
            case MESSAGE_RECEIVED -> {
                if (event instanceof MessageEvent messageEvent) {
                    listener.onMessageReceived(messageEvent);
                }
            }
            case MESSAGE_PROCESSED -> {
                if (event instanceof MessageEvent messageEvent) {
                    listener.onMessageProcessed(messageEvent);
                }
            }
            case RESPONSE_GENERATED -> {
                if (event instanceof MessageEvent messageEvent) {
                    listener.onResponseGenerated(messageEvent);
                }
            }
            case RESPONSE_STREAMED -> {
                if (event instanceof MessageEvent messageEvent) {
                    listener.onResponseStreamed(messageEvent);
                }
            }
            case ERROR_OCCURRED, VALIDATION_FAILED, AI_SERVICE_ERROR, 
                 RATE_LIMIT_EXCEEDED, CIRCUIT_BREAKER_OPENED, CIRCUIT_BREAKER_CLOSED,
                 SLOW_RESPONSE, HIGH_TOKEN_USAGE, SUSPICIOUS_INPUT, AUTHENTICATION_FAILED -> {
                listener.onErrorOccurred(event);
            }
        }
    }
}