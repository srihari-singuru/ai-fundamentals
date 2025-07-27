package com.srihari.ai.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when AI service operations fail.
 * This includes OpenAI API failures, model errors, and service unavailability.
 */
public class AiServiceException extends AiApplicationException {
    
    public static final String ERROR_CATEGORY = "AI_SERVICE_ERROR";
    
    public AiServiceException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public AiServiceException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public AiServiceException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }
    
    public AiServiceException(String errorCode, String message, Throwable cause, Map<String, Object> context) {
        super(errorCode, message, cause, context);
    }
    
    @Override
    public int getHttpStatusCode() {
        return HttpStatus.BAD_GATEWAY.value();
    }
    
    @Override
    public String getErrorCategory() {
        return ERROR_CATEGORY;
    }
    
    // Factory methods for common AI service errors
    
    public static AiServiceException apiKeyInvalid() {
        return new AiServiceException("AI_API_KEY_INVALID", 
                "Invalid or missing API key for AI service");
    }
    
    public static AiServiceException modelNotAvailable(String modelName) {
        return new AiServiceException("AI_MODEL_NOT_AVAILABLE", 
                "AI model is not available: " + modelName)
                .addContext("modelName", modelName);
    }
    
    public static AiServiceException rateLimitExceeded() {
        return new AiServiceException("AI_RATE_LIMIT_EXCEEDED", 
                "AI service rate limit exceeded");
    }
    
    public static AiServiceException serviceTimeout() {
        return new AiServiceException("AI_SERVICE_TIMEOUT", 
                "AI service request timed out");
    }
    
    public static AiServiceException serviceUnavailable() {
        return new AiServiceException("AI_SERVICE_UNAVAILABLE", 
                "AI service is temporarily unavailable");
    }
    
    public static AiServiceException invalidResponse(String reason) {
        return new AiServiceException("AI_INVALID_RESPONSE", 
                "Invalid response from AI service: " + reason)
                .addContext("reason", reason);
    }
    
    public static AiServiceException tokenLimitExceeded(int tokenCount, int maxTokens) {
        return new AiServiceException("AI_TOKEN_LIMIT_EXCEEDED", 
                "Token limit exceeded: " + tokenCount + " > " + maxTokens)
                .addContext("tokenCount", tokenCount)
                .addContext("maxTokens", maxTokens);
    }
}