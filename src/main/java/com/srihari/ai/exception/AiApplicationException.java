package com.srihari.ai.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * Base exception class for all AI application-specific exceptions.
 * Provides error codes, context metadata, and structured error information.
 */
@Getter
public abstract class AiApplicationException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, Object> context;
    private final LocalDateTime timestamp;
    
    protected AiApplicationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }
    
    protected AiApplicationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
        this.timestamp = LocalDateTime.now();
    }
    
    protected AiApplicationException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>(context);
        this.timestamp = LocalDateTime.now();
    }
    
    protected AiApplicationException(String errorCode, String message, Throwable cause, Map<String, Object> context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>(context);
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Add context information to the exception
     */
    @SuppressWarnings("unchecked")
    public <T extends AiApplicationException> T addContext(String key, Object value) {
        this.context.put(key, value);
        return (T) this;
    }
    
    /**
     * Add multiple context entries
     */
    @SuppressWarnings("unchecked")
    public <T extends AiApplicationException> T addContext(Map<String, Object> additionalContext) {
        this.context.putAll(additionalContext);
        return (T) this;
    }
    
    /**
     * Get a specific context value
     */
    public Object getContextValue(String key) {
        return context.get(key);
    }
    
    /**
     * Check if context contains a specific key
     */
    public boolean hasContext(String key) {
        return context.containsKey(key);
    }
    
    /**
     * Get a copy of the context map
     */
    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }
    
    /**
     * Get the HTTP status code that should be returned for this exception
     */
    public abstract int getHttpStatusCode();
    
    /**
     * Get a user-friendly error category
     */
    public abstract String getErrorCategory();
    
    @Override
    public String toString() {
        return String.format("%s{errorCode='%s', message='%s', context=%s, timestamp=%s}", 
                getClass().getSimpleName(), errorCode, getMessage(), context, timestamp);
    }
}