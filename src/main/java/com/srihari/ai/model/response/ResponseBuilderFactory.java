package com.srihari.ai.model.response;

import org.springframework.stereotype.Component;

import com.srihari.ai.common.CorrelationIdHolder;

/**
 * Factory for creating different types of response builders.
 * Implements the Factory pattern to provide consistent response construction.
 */
@Component
public class ResponseBuilderFactory {
    
    /**
     * Response type enumeration
     */
    public enum ResponseType {
        SUCCESS,
        ERROR,
        STREAMING
    }
    
    /**
     * Create a response builder of the specified type
     * 
     * @param type The type of response builder to create
     * @param <T> The response data type
     * @return A new response builder instance
     */
    @SuppressWarnings("unchecked")
    public <T> ResponseBuilder<T> createBuilder(ResponseType type) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        return switch (type) {
            case SUCCESS -> (ResponseBuilder<T>) new SuccessResponseBuilder<>()
                    .withCorrelationId(correlationId);
            case ERROR -> (ResponseBuilder<T>) new ErrorResponseBuilder<>()
                    .withCorrelationId(correlationId);
            case STREAMING -> (ResponseBuilder<T>) new StreamingResponseBuilder<>()
                    .withCorrelationId(correlationId);
        };
    }
    
    /**
     * Create a success response builder
     * 
     * @param <T> The response data type
     * @return A new success response builder
     */
    public <T> ResponseBuilder<T> success() {
        return createBuilder(ResponseType.SUCCESS);
    }
    
    /**
     * Create an error response builder
     * 
     * @param <T> The response data type
     * @return A new error response builder
     */
    public <T> ResponseBuilder<T> error() {
        return createBuilder(ResponseType.ERROR);
    }
    
    /**
     * Create a streaming response builder
     * 
     * @param <T> The response data type
     * @return A new streaming response builder
     */
    public <T> StreamingResponseBuilder<T> streaming() {
        return (StreamingResponseBuilder<T>) createBuilder(ResponseType.STREAMING);
    }
    
    /**
     * Create a success response with data
     * 
     * @param data The response data
     * @param <T> The response data type
     * @return A complete success response
     */
    public <T> ApiResponse<T> successResponse(T data) {
        return this.<T>success()
                .withData(data)
                .build();
    }
    
    /**
     * Create an error response with error information
     * 
     * @param errorCode The error code
     * @param errorMessage The error message
     * @param <T> The response data type
     * @return A complete error response
     */
    public <T> ApiResponse<T> errorResponse(String errorCode, String errorMessage) {
        return this.<T>error()
                .withError(errorCode, errorMessage)
                .build();
    }
    
    /**
     * Create an error response with status and error information
     * 
     * @param status The response status
     * @param errorCode The error code
     * @param errorMessage The error message
     * @param <T> The response data type
     * @return A complete error response
     */
    public <T> ApiResponse<T> errorResponse(ApiResponseStatus status, String errorCode, String errorMessage) {
        return this.<T>error()
                .withStatus(status)
                .withError(errorCode, errorMessage)
                .build();
    }
    
    /**
     * Create an error response from an exception
     * 
     * @param exception The exception to convert
     * @param <T> The response data type
     * @return A complete error response
     */
    public <T> ApiResponse<T> errorResponse(Exception exception) {
        return this.<T>error()
                .withError("INTERNAL_ERROR", exception.getMessage())
                .withMetadata("exceptionType", exception.getClass().getSimpleName())
                .build();
    }
}