package com.srihari.ai.common;

import java.util.UUID;

/**
 * Thread-local storage utility for correlation IDs.
 * Provides methods to set, get, and clear correlation IDs for request tracking.
 */
public class CorrelationIdHolder {
    
    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();
    
    /**
     * Sets the correlation ID for the current thread.
     * 
     * @param correlationId the correlation ID to set
     */
    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }
    
    /**
     * Gets the correlation ID for the current thread.
     * 
     * @return the correlation ID, or null if not set
     */
    public static String getCorrelationId() {
        return CORRELATION_ID.get();
    }
    
    /**
     * Generates and sets a new UUID-based correlation ID for the current thread.
     * 
     * @return the generated correlation ID
     */
    public static String generateAndSetCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        setCorrelationId(correlationId);
        return correlationId;
    }
    
    /**
     * Clears the correlation ID for the current thread.
     * Should be called at the end of request processing to prevent memory leaks.
     */
    public static void clear() {
        CORRELATION_ID.remove();
    }
    
    /**
     * Checks if a correlation ID is set for the current thread.
     * 
     * @return true if correlation ID is set, false otherwise
     */
    public static boolean hasCorrelationId() {
        return CORRELATION_ID.get() != null;
    }
}