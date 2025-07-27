package com.srihari.ai.model.response;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enhanced error information model with correlation tracking and context.
 * Provides structured error details for comprehensive error handling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorInfo {
    
    private String code;
    private String message;
    private String category;
    private String correlationId;
    private LocalDateTime timestamp;
    private String path;
    private Integer httpStatus;
    private Map<String, Object> context;
    private String traceId;
    private String spanId;
    
    /**
     * Create error info with minimal required fields
     */
    public static ErrorInfo of(String code, String message) {
        return ErrorInfo.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error info with correlation ID
     */
    public static ErrorInfo of(String code, String message, String correlationId) {
        return ErrorInfo.builder()
                .code(code)
                .message(message)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create simple error info with minimal fields
     */
    public static ErrorInfo simple(String code, String message) {
        return ErrorInfo.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error info from AiApplicationException
     */
    public static ErrorInfo fromException(com.srihari.ai.exception.AiApplicationException ex, String correlationId) {
        return ErrorInfo.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .category(ex.getErrorCategory())
                .correlationId(correlationId)
                .timestamp(ex.getTimestamp())
                .httpStatus(ex.getHttpStatusCode())
                .context(ex.getContext())
                .build();
    }
    
    /**
     * Add context information
     */
    public ErrorInfo withContext(String key, Object value) {
        if (this.context == null) {
            this.context = new java.util.HashMap<>();
        }
        this.context.put(key, value);
        return this;
    }
    
    /**
     * Add path information
     */
    public ErrorInfo withPath(String path) {
        this.path = path;
        return this;
    }
    
    /**
     * Add HTTP status
     */
    public ErrorInfo withHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
        return this;
    }
    
    /**
     * Add tracing information
     */
    public ErrorInfo withTracing(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
        return this;
    }
}