package com.srihari.ai.exception;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.model.response.EnhancedErrorResponse;
import com.srihari.ai.model.response.ErrorInfo;
import com.srihari.ai.security.SensitiveDataMasker;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final SensitiveDataMasker sensitiveDataMasker;
    private final CustomMetrics customMetrics;
    private final StructuredLogger structuredLogger;
    
    @Value("${spring.application.name:ai-fundamentals}")
    private String applicationName;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    public GlobalExceptionHandler(SensitiveDataMasker sensitiveDataMasker, 
                                CustomMetrics customMetrics, 
                                StructuredLogger structuredLogger) {
        this.sensitiveDataMasker = sensitiveDataMasker;
        this.customMetrics = customMetrics;
        this.structuredLogger = structuredLogger;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<EnhancedErrorResponse> handleValidationErrors(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        Map<String, String> fieldErrors = new HashMap<>();
        List<ErrorInfo> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    Object rejectedValue = ((FieldError) error).getRejectedValue();
                    fieldErrors.put(fieldName, errorMessage);
                    
                    return ErrorInfo.builder()
                            .code("FIELD_VALIDATION_ERROR")
                            .message(errorMessage)
                            .category("VALIDATION_ERROR")
                            .correlationId(correlationId)
                            .timestamp(LocalDateTime.now())
                            .path(path)
                            .httpStatus(HttpStatus.BAD_REQUEST.value())
                            .context(Map.of(
                                    "field", fieldName, 
                                    "rejectedValue", sensitiveDataMasker.maskUserData(String.valueOf(rejectedValue))
                            ))
                            .build();
                })
                .collect(Collectors.toList());
        
        // Record metrics
        customMetrics.incrementAiErrors("validation", "field_validation", "FIELD_VALIDATION_ERROR");
        
        // Structured logging
        structuredLogger.warn("Field validation errors occurred", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCount", errors.size(),
                "fieldErrors", fieldErrors
        ));
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.of(errors, path, method, correlationId)
                .withMetadata("fieldErrors", fieldErrors)
                .withMetadata("errorCount", errors.size())
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<EnhancedErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        Map<String, String> fieldErrors = new HashMap<>();
        List<ErrorInfo> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    Object rejectedValue = ((FieldError) error).getRejectedValue();
                    fieldErrors.put(fieldName, errorMessage);
                    
                    return ErrorInfo.builder()
                            .code("METHOD_ARGUMENT_VALIDATION_ERROR")
                            .message(errorMessage)
                            .category("VALIDATION_ERROR")
                            .correlationId(correlationId)
                            .timestamp(LocalDateTime.now())
                            .path(path)
                            .httpStatus(HttpStatus.BAD_REQUEST.value())
                            .context(Map.of(
                                    "field", fieldName,
                                    "rejectedValue", sensitiveDataMasker.maskUserData(String.valueOf(rejectedValue))
                            ))
                            .build();
                })
                .collect(Collectors.toList());
        
        // Record metrics
        customMetrics.incrementAiErrors("validation", "method_argument", "METHOD_ARGUMENT_VALIDATION_ERROR");
        
        // Structured logging
        structuredLogger.warn("Method argument validation errors occurred", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCount", errors.size(),
                "fieldErrors", fieldErrors
        ));
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.of(errors, path, method, correlationId)
                .withMetadata("fieldErrors", fieldErrors)
                .withMetadata("errorCount", errors.size())
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<EnhancedErrorResponse> handleCircuitBreakerOpen(
            CallNotPermittedException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        Duration retryAfter = Duration.ofMinutes(5);
        
        ErrorInfo errorInfo = ErrorInfo.builder()
                .code("CIRCUIT_BREAKER_OPEN")
                .message("AI service is temporarily unavailable due to circuit breaker protection")
                .category("CIRCUIT_BREAKER_ERROR")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .path(path)
                .httpStatus(HttpStatus.SERVICE_UNAVAILABLE.value())
                .context(Map.of(
                        "circuitBreakerName", ex.getCausingCircuitBreakerName(),
                        "retryAfter", retryAfter.toString(),
                        "estimatedRecoveryTime", LocalDateTime.now().plus(retryAfter).toString()
                ))
                .build();
        
        // Record metrics
        customMetrics.incrementCircuitBreakerEvents(ex.getCausingCircuitBreakerName(), "open");
        customMetrics.incrementAiErrors("circuit_breaker", "open", "CIRCUIT_BREAKER_OPEN");
        
        // Structured logging
        structuredLogger.error("Circuit breaker is open", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "circuitBreakerName", ex.getCausingCircuitBreakerName(),
                "retryAfter", retryAfter.toString(),
                "errorCode", "CIRCUIT_BREAKER_OPEN"
        ), ex);
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.of(errorInfo, path, method)
                .withMetadata("circuitBreakerName", ex.getCausingCircuitBreakerName())
                .withMetadata("retryAfter", "5 minutes")
                .withMetadata("estimatedRecoveryTime", LocalDateTime.now().plus(retryAfter).toString())
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", String.valueOf(retryAfter.getSeconds()))
                .body(errorResponse);
    }

    // ========== CUSTOM AI APPLICATION EXCEPTIONS ==========
    
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<EnhancedErrorResponse> handleAiServiceException(
            AiServiceException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        // Record metrics
        customMetrics.incrementAiErrors("ai_service", ex.getErrorCode().toLowerCase(), ex.getErrorCode());
        
        // Structured logging with masked context
        Map<String, Object> maskedContext = maskSensitiveContext(ex.getContext());
        structuredLogger.error("AI service error occurred", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCode", ex.getErrorCode(),
                "errorCategory", ex.getErrorCategory(),
                "context", maskedContext,
                "timestamp", ex.getTimestamp().toString()
        ), ex);
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.fromException(ex, correlationId, path, method)
                .withMetadata("errorContext", maskedContext)
                .withMetadata("errorTimestamp", ex.getTimestamp().toString())
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.status(ex.getHttpStatusCode()).body(errorResponse);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<EnhancedErrorResponse> handleValidationException(
            ValidationException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        // Record metrics
        customMetrics.incrementAiErrors("validation", ex.getErrorCode().toLowerCase(), ex.getErrorCode());
        
        // Structured logging with masked context
        Map<String, Object> maskedContext = maskSensitiveContext(ex.getContext());
        structuredLogger.warn("Validation error occurred", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCode", ex.getErrorCode(),
                "errorCategory", ex.getErrorCategory(),
                "context", maskedContext,
                "timestamp", ex.getTimestamp().toString()
        ));
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.fromException(ex, correlationId, path, method)
                .withMetadata("validationContext", maskedContext)
                .withMetadata("errorTimestamp", ex.getTimestamp().toString())
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.status(ex.getHttpStatusCode()).body(errorResponse);
    }
    
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<EnhancedErrorResponse> handleRateLimitException(
            RateLimitException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        Duration retryAfter = ex.getRetryAfter();
        
        // Record metrics
        customMetrics.incrementAiErrors("rate_limit", ex.getErrorCode().toLowerCase(), ex.getErrorCode());
        
        // Structured logging with masked context
        Map<String, Object> maskedContext = maskSensitiveContext(ex.getContext());
        structuredLogger.warn("Rate limit exceeded", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCode", ex.getErrorCode(),
                "retryAfter", retryAfter.toString(),
                "context", maskedContext,
                "timestamp", ex.getTimestamp().toString()
        ));
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.fromException(ex, correlationId, path, method)
                .withMetadata("retryAfter", retryAfter.toString())
                .withMetadata("retryAfterSeconds", retryAfter.getSeconds())
                .withMetadata("rateLimitContext", maskedContext)
                .withMetadata("errorTimestamp", ex.getTimestamp().toString())
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.status(ex.getHttpStatusCode())
                .header("Retry-After", String.valueOf(retryAfter.getSeconds()))
                .header("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + retryAfter.toMillis()))
                .body(errorResponse);
    }
    
    @ExceptionHandler(CircuitBreakerException.class)
    public ResponseEntity<EnhancedErrorResponse> handleCircuitBreakerException(
            CircuitBreakerException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        // Record metrics
        String circuitName = (String) ex.getContextValue("circuitName");
        if (circuitName != null) {
            customMetrics.incrementCircuitBreakerEvents(circuitName, ex.getErrorCode().toLowerCase());
        }
        customMetrics.incrementAiErrors("circuit_breaker", ex.getErrorCode().toLowerCase(), ex.getErrorCode());
        
        // Structured logging with masked context
        Map<String, Object> maskedContext = maskSensitiveContext(ex.getContext());
        structuredLogger.error("Circuit breaker error occurred", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCode", ex.getErrorCode(),
                "circuitName", circuitName != null ? circuitName : "unknown",
                "estimatedRecoveryTime", ex.getEstimatedRecoveryTime().toString(),
                "context", maskedContext,
                "timestamp", ex.getTimestamp().toString()
        ), ex);
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.fromException(ex, correlationId, path, method)
                .withMetadata("estimatedRecoveryTime", ex.getEstimatedRecoveryTime().toString())
                .withMetadata("circuitName", circuitName)
                .withMetadata("circuitBreakerContext", maskedContext)
                .withMetadata("errorTimestamp", ex.getTimestamp().toString())
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        Duration retryAfterDuration = Duration.between(LocalDateTime.now(), ex.getEstimatedRecoveryTime());
        return ResponseEntity.status(ex.getHttpStatusCode())
                .header("Retry-After", String.valueOf(Math.max(1, retryAfterDuration.getSeconds())))
                .body(errorResponse);
    }
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<EnhancedErrorResponse> handleSecurityException(
            SecurityException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        // Record metrics
        customMetrics.incrementAiErrors("security", ex.getErrorCode().toLowerCase(), ex.getErrorCode());
        
        // Structured logging with heavily masked context for security
        Map<String, Object> maskedContext = maskSensitiveContext(ex.getContext());
        structuredLogger.warn("Security error occurred", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCode", ex.getErrorCode(),
                "errorCategory", ex.getErrorCategory(),
                "context", maskedContext,
                "timestamp", ex.getTimestamp().toString(),
                "securityAlert", true
        ));
        
        // Don't expose internal security details in response
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.fromException(ex, correlationId, path, method)
                .withMetadata("securityContext", Map.of("message", "Security validation failed"))
                .withMetadata("errorTimestamp", ex.getTimestamp().toString())
                .withMetadata("supportReference", correlationId)
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.status(ex.getHttpStatusCode()).body(errorResponse);
    }
    
    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<EnhancedErrorResponse> handleConfigurationException(
            ConfigurationException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        // Record metrics
        customMetrics.incrementAiErrors("configuration", ex.getErrorCode().toLowerCase(), ex.getErrorCode());
        
        // Structured logging with masked context
        Map<String, Object> maskedContext = maskSensitiveContext(ex.getContext());
        structuredLogger.error("Configuration error occurred", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCode", ex.getErrorCode(),
                "errorCategory", ex.getErrorCategory(),
                "context", maskedContext,
                "timestamp", ex.getTimestamp().toString(),
                "severity", "HIGH"
        ), ex);
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.fromException(ex, correlationId, path, method)
                .withMetadata("configurationContext", maskedContext)
                .withMetadata("errorTimestamp", ex.getTimestamp().toString())
                .withMetadata("severity", "HIGH")
                .withMetadata("supportReference", correlationId)
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.status(ex.getHttpStatusCode()).body(errorResponse);
    }
    
    // ========== STANDARD EXCEPTIONS ==========
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<EnhancedErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        String maskedMessage = sensitiveDataMasker.maskExceptionData(ex);
        String userFriendlyMessage = sensitiveDataMasker.maskUserData(ex.getMessage());
        
        ErrorInfo errorInfo = ErrorInfo.builder()
                .code("ILLEGAL_ARGUMENT")
                .message(userFriendlyMessage)
                .category("VALIDATION_ERROR")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .path(path)
                .httpStatus(HttpStatus.BAD_REQUEST.value())
                .context(Map.of("argumentType", "invalid_parameter"))
                .build();
        
        // Record metrics
        customMetrics.incrementAiErrors("validation", "illegal_argument", "ILLEGAL_ARGUMENT");
        
        // Structured logging
        structuredLogger.warn("Illegal argument provided", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCode", "ILLEGAL_ARGUMENT",
                "maskedMessage", maskedMessage
        ));
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.of(errorInfo, path, method)
                .withMetadata("argumentValidation", "failed")
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<EnhancedErrorResponse> handleResourceNotFound(
            NoResourceFoundException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        ErrorInfo errorInfo = ErrorInfo.builder()
                .code("RESOURCE_NOT_FOUND")
                .message("Resource not found: " + path)
                .category("NOT_FOUND_ERROR")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .path(path)
                .httpStatus(HttpStatus.NOT_FOUND.value())
                .context(Map.of("requestedResource", path))
                .build();
        
        // Don't log favicon requests as errors - they're expected browser behavior
        if (path.equals("/favicon.ico")) {
            structuredLogger.debug("Favicon request - returning 404", Map.of(
                    "correlationId", correlationId,
                    "path", path,
                    "method", method
            ));
        } else {
            // Record metrics for non-favicon 404s
            customMetrics.incrementAiErrors("resource", "not_found", "RESOURCE_NOT_FOUND");
            
            structuredLogger.warn("Resource not found", Map.of(
                    "correlationId", correlationId,
                    "path", path,
                    "method", method,
                    "errorCode", "RESOURCE_NOT_FOUND"
            ));
        }
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.of(errorInfo, path, method)
                .withMetadata("requestedResource", path)
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<EnhancedErrorResponse> handleTimeoutException(
            TimeoutException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        ErrorInfo errorInfo = ErrorInfo.builder()
                .code("REQUEST_TIMEOUT")
                .message("Request timed out. Please try again.")
                .category("TIMEOUT_ERROR")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .path(path)
                .httpStatus(HttpStatus.REQUEST_TIMEOUT.value())
                .context(Map.of("timeoutReason", ex.getMessage()))
                .build();
        
        // Record metrics
        customMetrics.incrementAiErrors("timeout", "request_timeout", "REQUEST_TIMEOUT");
        
        // Structured logging
        structuredLogger.error("Request timeout occurred", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCode", "REQUEST_TIMEOUT",
                "timeoutMessage", ex.getMessage()
        ), ex);
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.of(errorInfo, path, method)
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);
    }
    
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<EnhancedErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        ErrorInfo errorInfo = ErrorInfo.builder()
                .code("HTTP_STATUS_ERROR")
                .message(ex.getReason() != null ? ex.getReason() : "HTTP error occurred")
                .category("HTTP_ERROR")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .path(path)
                .httpStatus(ex.getStatusCode().value())
                .context(Map.of("httpStatus", ex.getStatusCode().value()))
                .build();
        
        // Record metrics
        customMetrics.incrementAiErrors("http", "status_error", "HTTP_STATUS_ERROR");
        
        // Structured logging
        structuredLogger.warn("HTTP status exception occurred", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "httpStatus", ex.getStatusCode().value(),
                "reason", ex.getReason()
        ));
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.of(errorInfo, path, method)
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo());
        
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<EnhancedErrorResponse> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        
        String correlationId = getOrGenerateCorrelationId();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        
        String maskedMessage = sensitiveDataMasker.maskExceptionData(ex);
        
        ErrorInfo errorInfo = ErrorInfo.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .category("SYSTEM_ERROR")
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .path(path)
                .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .context(Map.of("exceptionType", ex.getClass().getSimpleName()))
                .build();
        
        // Record metrics
        customMetrics.incrementAiErrors("system", "internal_error", "INTERNAL_SERVER_ERROR");
        
        // Structured logging with full context
        structuredLogger.error("Unexpected system error occurred", Map.of(
                "correlationId", correlationId,
                "path", path,
                "method", method,
                "errorCode", "INTERNAL_SERVER_ERROR",
                "exceptionType", ex.getClass().getSimpleName(),
                "maskedMessage", maskedMessage
        ), ex);
        
        EnhancedErrorResponse errorResponse = EnhancedErrorResponse.of(errorInfo, path, method)
                .withTracing(getTraceId(), getSpanId())
                .withInstance(getInstanceInfo())
                .withMetadata("supportContact", "Please contact support with this correlation ID");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Get or generate correlation ID for the current request
     */
    private String getOrGenerateCorrelationId() {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        if (correlationId == null) {
            correlationId = CorrelationIdHolder.generateAndSetCorrelationId();
        }
        return correlationId;
    }
    
    /**
     * Get current trace ID from distributed tracing
     */
    private String getTraceId() {
        // For now, return null - can be enhanced with actual tracing implementation
        return null;
    }
    
    /**
     * Get current span ID from distributed tracing
     */
    private String getSpanId() {
        // For now, return null - can be enhanced with actual tracing implementation
        return null;
    }
    
    /**
     * Get instance information for debugging
     */
    private String getInstanceInfo() {
        return applicationName + ":" + serverPort;
    }
    
    /**
     * Mask sensitive context information for security
     */
    private Map<String, Object> maskSensitiveContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return context;
        }
        
        Map<String, Object> maskedContext = new HashMap<>(context);
        
        // Mask common sensitive fields
        maskedContext.replaceAll((key, value) -> {
            String lowerKey = key.toLowerCase();
            if (lowerKey.contains("password") || lowerKey.contains("token") || 
                lowerKey.contains("key") || lowerKey.contains("secret") ||
                lowerKey.contains("credential")) {
                return sensitiveDataMasker.maskApiKey(String.valueOf(value));
            }
            return value;
        });
        
        return maskedContext;
    }
}