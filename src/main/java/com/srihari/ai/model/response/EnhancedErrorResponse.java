package com.srihari.ai.model.response;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enhanced error response with correlation tracking, multiple errors support,
 * and comprehensive error context for production debugging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedErrorResponse {
    
    private String correlationId;
    private LocalDateTime timestamp;
    private String path;
    private String method;
    private Integer status;
    private String error;
    private String message;
    private List<ErrorInfo> errors;
    private Map<String, Object> metadata;
    private String traceId;
    private String spanId;
    private String instance;
    
    /**
     * Create error response with single error
     */
    public static EnhancedErrorResponse of(ErrorInfo errorInfo, String path, String method) {
        return EnhancedErrorResponse.builder()
                .correlationId(errorInfo.getCorrelationId())
                .timestamp(LocalDateTime.now())
                .path(path)
                .method(method)
                .status(errorInfo.getHttpStatus())
                .error(errorInfo.getCategory())
                .message(errorInfo.getMessage())
                .errors(List.of(errorInfo))
                .build();
    }
    
    /**
     * Create error response with multiple errors
     */
    public static EnhancedErrorResponse of(List<ErrorInfo> errors, String path, String method, String correlationId) {
        ErrorInfo primaryError = errors.isEmpty() ? null : errors.get(0);
        
        return EnhancedErrorResponse.builder()
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .path(path)
                .method(method)
                .status(primaryError != null ? primaryError.getHttpStatus() : 500)
                .error(primaryError != null ? primaryError.getCategory() : "INTERNAL_ERROR")
                .message(primaryError != null ? primaryError.getMessage() : "An error occurred")
                .errors(new ArrayList<>(errors))
                .build();
    }
    
    /**
     * Create error response from exception
     */
    public static EnhancedErrorResponse fromException(Exception ex, String correlationId, String path, String method) {
        if (ex instanceof com.srihari.ai.exception.AiApplicationException) {
            com.srihari.ai.exception.AiApplicationException aiEx = (com.srihari.ai.exception.AiApplicationException) ex;
            ErrorInfo errorInfo = ErrorInfo.fromException(aiEx, correlationId).withPath(path);
            
            return EnhancedErrorResponse.builder()
                    .correlationId(correlationId)
                    .timestamp(LocalDateTime.now())
                    .path(path)
                    .method(method)
                    .status(aiEx.getHttpStatusCode())
                    .error(aiEx.getErrorCategory())
                    .message(aiEx.getMessage())
                    .errors(List.of(errorInfo))
                    .build();
        } else {
            ErrorInfo errorInfo = ErrorInfo.builder()
                    .code("INTERNAL_SERVER_ERROR")
                    .message("An unexpected error occurred")
                    .category("SYSTEM_ERROR")
                    .correlationId(correlationId)
                    .timestamp(LocalDateTime.now())
                    .path(path)
                    .httpStatus(500)
                    .build();
            
            return EnhancedErrorResponse.builder()
                    .correlationId(correlationId)
                    .timestamp(LocalDateTime.now())
                    .path(path)
                    .method(method)
                    .status(500)
                    .error("SYSTEM_ERROR")
                    .message("An unexpected error occurred")
                    .errors(List.of(errorInfo))
                    .build();
        }
    }
    
    /**
     * Add metadata
     */
    public EnhancedErrorResponse withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * Add tracing information
     */
    public EnhancedErrorResponse withTracing(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
        return this;
    }
    
    /**
     * Add instance information
     */
    public EnhancedErrorResponse withInstance(String instance) {
        this.instance = instance;
        return this;
    }
    
    /**
     * Add additional error
     */
    public EnhancedErrorResponse addError(ErrorInfo errorInfo) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(errorInfo);
        return this;
    }
    
    /**
     * Get primary error code
     */
    public String getPrimaryErrorCode() {
        return errors != null && !errors.isEmpty() ? errors.get(0).getCode() : null;
    }
    
    /**
     * Check if response contains specific error code
     */
    public boolean hasErrorCode(String errorCode) {
        return errors != null && errors.stream()
                .anyMatch(error -> errorCode.equals(error.getCode()));
    }
    
    /**
     * Get error count
     */
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }
}