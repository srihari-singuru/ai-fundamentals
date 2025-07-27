package com.srihari.ai.model.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Standard API response wrapper that provides consistent structure
 * for all API responses including success and error cases.
 */
@Data
@Builder
public class ApiResponse<T> {
    
    /**
     * Correlation ID for request tracking
     */
    private final String correlationId;
    
    /**
     * Response timestamp
     */
    private final Instant timestamp;
    
    /**
     * Session ID
     */
    private final String sessionId;
    
    /**
     * Response status
     */
    private final ApiResponseStatus status;
    
    /**
     * Response data/content
     */
    private final T data;
    
    /**
     * Response metadata
     */
    private final Map<String, Object> metadata;
    
    /**
     * Error information (if any)
     */
    private final List<ErrorInfo> errors;
    
    /**
     * Check if the response represents a success
     */
    public boolean isSuccess() {
        return status == ApiResponseStatus.SUCCESS;
    }
    
    /**
     * Check if the response has errors
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    /**
     * Get the first error (if any)
     */
    public ErrorInfo getFirstError() {
        return hasErrors() ? errors.get(0) : null;
    }
}