package com.srihari.ai.exception;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when rate limiting is triggered.
 * This includes API rate limits, user rate limits, and system protection limits.
 */
public class RateLimitException extends AiApplicationException {
    
    public static final String ERROR_CATEGORY = "RATE_LIMIT_ERROR";
    
    public RateLimitException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public RateLimitException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public RateLimitException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }
    
    public RateLimitException(String errorCode, String message, Throwable cause, Map<String, Object> context) {
        super(errorCode, message, cause, context);
    }
    
    @Override
    public int getHttpStatusCode() {
        return HttpStatus.TOO_MANY_REQUESTS.value();
    }
    
    @Override
    public String getErrorCategory() {
        return ERROR_CATEGORY;
    }
    
    // Factory methods for common rate limit errors
    
    public static RateLimitException apiRateLimitExceeded(int requestCount, int limit, Duration resetTime) {
        return new RateLimitException("API_RATE_LIMIT_EXCEEDED", 
                "API rate limit exceeded: " + requestCount + " requests in window (limit: " + limit + ")")
                .addContext("requestCount", requestCount)
                .addContext("limit", limit)
                .addContext("resetTime", resetTime.toString())
                .addContext("retryAfter", LocalDateTime.now().plus(resetTime));
    }
    
    public static RateLimitException userRateLimitExceeded(String userId, int requestCount, int limit) {
        return new RateLimitException("USER_RATE_LIMIT_EXCEEDED", 
                "User rate limit exceeded: " + requestCount + " requests (limit: " + limit + ")")
                .addContext("userId", userId)
                .addContext("requestCount", requestCount)
                .addContext("limit", limit);
    }
    
    public static RateLimitException conversationRateLimitExceeded(String conversationId, int messageCount, int limit) {
        return new RateLimitException("CONVERSATION_RATE_LIMIT_EXCEEDED", 
                "Conversation rate limit exceeded: " + messageCount + " messages (limit: " + limit + ")")
                .addContext("conversationId", conversationId)
                .addContext("messageCount", messageCount)
                .addContext("limit", limit);
    }
    
    public static RateLimitException systemProtectionTriggered(String reason) {
        return new RateLimitException("SYSTEM_PROTECTION_TRIGGERED", 
                "System protection rate limit triggered: " + reason)
                .addContext("reason", reason);
    }
    
    public static RateLimitException concurrentRequestLimitExceeded(int currentRequests, int maxConcurrent) {
        return new RateLimitException("CONCURRENT_REQUEST_LIMIT_EXCEEDED", 
                "Concurrent request limit exceeded: " + currentRequests + " active (limit: " + maxConcurrent + ")")
                .addContext("currentRequests", currentRequests)
                .addContext("maxConcurrent", maxConcurrent);
    }
    
    /**
     * Get the retry-after duration if available
     */
    public Duration getRetryAfter() {
        Object retryAfter = getContextValue("retryAfter");
        if (retryAfter instanceof LocalDateTime) {
            return Duration.between(LocalDateTime.now(), (LocalDateTime) retryAfter);
        }
        return Duration.ofMinutes(1); // Default retry after 1 minute
    }
}