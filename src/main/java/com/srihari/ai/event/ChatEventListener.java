package com.srihari.ai.event;

/**
 * Interface for listening to chat events in the system.
 * Implementations can handle specific event types and perform actions
 * such as logging, metrics collection, notifications, etc.
 */
public interface ChatEventListener {
    
    /**
     * Called when a conversation is started.
     * 
     * @param event the conversation started event
     */
    default void onConversationStarted(ConversationEvent event) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a conversation is ended.
     * 
     * @param event the conversation ended event
     */
    default void onConversationEnded(ConversationEvent event) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a user message is received.
     * 
     * @param event the message received event
     */
    default void onMessageReceived(MessageEvent event) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a user message has been processed.
     * 
     * @param event the message processed event
     */
    default void onMessageProcessed(MessageEvent event) {
        // Default implementation does nothing
    }
    
    /**
     * Called when an AI response is generated.
     * 
     * @param event the response generated event
     */
    default void onResponseGenerated(MessageEvent event) {
        // Default implementation does nothing
    }
    
    /**
     * Called when an AI response is streamed.
     * 
     * @param event the response streamed event
     */
    default void onResponseStreamed(MessageEvent event) {
        // Default implementation does nothing
    }
    
    /**
     * Called when an error occurs during chat processing.
     * 
     * @param event the error event
     */
    default void onErrorOccurred(ChatEvent event) {
        // Default implementation does nothing
    }
    
    /**
     * Called for any chat event. This is a catch-all method that receives
     * all events regardless of type. Useful for generic event processing.
     * 
     * @param event the chat event
     */
    default void onChatEvent(ChatEvent event) {
        // Default implementation does nothing
    }
    
    /**
     * Returns the priority of this listener. Higher priority listeners
     * are called first. Default priority is 0.
     * 
     * @return the listener priority
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Returns whether this listener is enabled. Disabled listeners
     * will not receive events. Default is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }
}