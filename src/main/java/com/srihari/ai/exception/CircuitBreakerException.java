package com.srihari.ai.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when circuit breaker patterns are triggered.
 * This includes circuit breaker open states, half-open failures, and bulkhead rejections.
 */
public class CircuitBreakerException extends AiApplicationException {
    
    public static final String ERROR_CATEGORY = "CIRCUIT_BREAKER_ERROR";
    
    public CircuitBreakerException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public CircuitBreakerException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public CircuitBreakerException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }
    
    public CircuitBreakerException(String errorCode, String message, Throwable cause, Map<String, Object> context) {
        super(errorCode, message, cause, context);
    }
    
    @Override
    public int getHttpStatusCode() {
        return HttpStatus.SERVICE_UNAVAILABLE.value();
    }
    
    @Override
    public String getErrorCategory() {
        return ERROR_CATEGORY;
    }
    
    // Factory methods for common circuit breaker errors
    
    public static CircuitBreakerException circuitOpen(String circuitName, int failureCount, double failureRate) {
        return new CircuitBreakerException("CIRCUIT_BREAKER_OPEN", 
                "Circuit breaker '" + circuitName + "' is open due to failures")
                .addContext("circuitName", circuitName)
                .addContext("failureCount", failureCount)
                .addContext("failureRate", failureRate)
                .addContext("state", "OPEN");
    }
    
    public static CircuitBreakerException halfOpenFailed(String circuitName, String reason) {
        return new CircuitBreakerException("CIRCUIT_BREAKER_HALF_OPEN_FAILED", 
                "Circuit breaker '" + circuitName + "' failed in half-open state: " + reason)
                .addContext("circuitName", circuitName)
                .addContext("reason", reason)
                .addContext("state", "HALF_OPEN");
    }
    
    public static CircuitBreakerException bulkheadRejection(String bulkheadName, int activeThreads, int maxThreads) {
        return new CircuitBreakerException("BULKHEAD_REJECTION", 
                "Bulkhead '" + bulkheadName + "' rejected request due to capacity")
                .addContext("bulkheadName", bulkheadName)
                .addContext("activeThreads", activeThreads)
                .addContext("maxThreads", maxThreads);
    }
    
    public static CircuitBreakerException timeoutExceeded(String operationName, long timeoutMs, long actualMs) {
        return new CircuitBreakerException("OPERATION_TIMEOUT", 
                "Operation '" + operationName + "' exceeded timeout: " + actualMs + "ms > " + timeoutMs + "ms")
                .addContext("operationName", operationName)
                .addContext("timeoutMs", timeoutMs)
                .addContext("actualMs", actualMs);
    }
    
    public static CircuitBreakerException retryExhausted(String operationName, int attempts, int maxAttempts) {
        return new CircuitBreakerException("RETRY_EXHAUSTED", 
                "Retry attempts exhausted for operation '" + operationName + "': " + attempts + "/" + maxAttempts)
                .addContext("operationName", operationName)
                .addContext("attempts", attempts)
                .addContext("maxAttempts", maxAttempts);
    }
    
    /**
     * Get the estimated time when the circuit breaker might close
     */
    public LocalDateTime getEstimatedRecoveryTime() {
        Object recoveryTime = getContextValue("estimatedRecoveryTime");
        if (recoveryTime instanceof LocalDateTime) {
            return (LocalDateTime) recoveryTime;
        }
        return LocalDateTime.now().plusMinutes(5); // Default 5 minutes
    }
}