package com.srihari.ai.model.response;

import java.time.Instant;
import java.util.Map;



/**
 * Fluent API interface for building different types of responses.
 * Provides a consistent way to construct responses with metadata,
 * correlation IDs, and error information.
 */
public interface ResponseBuilder<T> {
    
    /**
     * Set the correlation ID for request tracking
     * 
     * @param correlationId The correlation ID
     * @return This builder for method chaining
     */
    ResponseBuilder<T> withCorrelationId(String correlationId);
    
    /**
     * Set the timestamp for the response
     * 
     * @param timestamp The response timestamp
     * @return This builder for method chaining
     */
    ResponseBuilder<T> withTimestamp(Instant timestamp);
    
    /**
     * Add metadata to the response
     * 
     * @param metadata Map of metadata key-value pairs
     * @return This builder for method chaining
     */
    ResponseBuilder<T> withMetadata(Map<String, Object> metadata);
    
    /**
     * Add a single metadata entry
     * 
     * @param key The metadata key
     * @param value The metadata value
     * @return This builder for method chaining
     */
    ResponseBuilder<T> withMetadata(String key, Object value);
    
    /**
     * Set the session ID
     * 
     * @param sessionId The session ID
     * @return This builder for method chaining
     */
    ResponseBuilder<T> withSessionId(String sessionId);
    
    /**
     * Set the response status
     * 
     * @param status The response status
     * @return This builder for method chaining
     */
    ResponseBuilder<T> withStatus(ApiResponseStatus status);
    
    /**
     * Add error information to the response
     * 
     * @param error The error information
     * @return This builder for method chaining
     */
    ResponseBuilder<T> withError(ErrorInfo error);
    
    /**
     * Add error information with code and message
     * 
     * @param errorCode The error code
     * @param errorMessage The error message
     * @return This builder for method chaining
     */
    ResponseBuilder<T> withError(String errorCode, String errorMessage);
    
    /**
     * Set the response data/content
     * 
     * @param data The response data
     * @return This builder for method chaining
     */
    ResponseBuilder<T> withData(T data);
    
    /**
     * Build the final response object
     * 
     * @return The constructed response
     */
    ApiResponse<T> build();
}