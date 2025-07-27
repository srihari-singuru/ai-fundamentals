package com.srihari.ai.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when security-related errors occur.
 * This includes authentication failures, authorization errors, and security policy violations.
 */
public class SecurityException extends AiApplicationException {
    
    public static final String ERROR_CATEGORY = "SECURITY_ERROR";
    
    public SecurityException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public SecurityException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public SecurityException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }
    
    public SecurityException(String errorCode, String message, Throwable cause, Map<String, Object> context) {
        super(errorCode, message, cause, context);
    }
    
    @Override
    public int getHttpStatusCode() {
        String errorCode = getErrorCode();
        if ("AUTHENTICATION_FAILED".equals(errorCode) || "INVALID_TOKEN".equals(errorCode)) {
            return HttpStatus.UNAUTHORIZED.value();
        } else if ("ACCESS_DENIED".equals(errorCode) || "INSUFFICIENT_PERMISSIONS".equals(errorCode)) {
            return HttpStatus.FORBIDDEN.value();
        }
        return HttpStatus.BAD_REQUEST.value();
    }
    
    @Override
    public String getErrorCategory() {
        return ERROR_CATEGORY;
    }
    
    // Factory methods for common security errors
    
    public static SecurityException authenticationFailed(String reason) {
        return new SecurityException("AUTHENTICATION_FAILED", 
                "Authentication failed: " + reason)
                .addContext("reason", reason);
    }
    
    public static SecurityException invalidToken(String tokenType) {
        return new SecurityException("INVALID_TOKEN", 
                "Invalid or expired " + tokenType + " token")
                .addContext("tokenType", tokenType);
    }
    
    public static SecurityException accessDenied(String resource, String action) {
        return new SecurityException("ACCESS_DENIED", 
                "Access denied for action '" + action + "' on resource '" + resource + "'")
                .addContext("resource", resource)
                .addContext("action", action);
    }
    
    public static SecurityException insufficientPermissions(String requiredRole, String userRole) {
        return new SecurityException("INSUFFICIENT_PERMISSIONS", 
                "Insufficient permissions. Required: " + requiredRole + ", User: " + userRole)
                .addContext("requiredRole", requiredRole)
                .addContext("userRole", userRole);
    }
    
    public static SecurityException suspiciousActivity(String activityType, String details) {
        return new SecurityException("SUSPICIOUS_ACTIVITY", 
                "Suspicious activity detected: " + activityType)
                .addContext("activityType", activityType)
                .addContext("details", details);
    }
    
    public static SecurityException inputSanitizationFailed(String input, String reason) {
        return new SecurityException("INPUT_SANITIZATION_FAILED", 
                "Input sanitization failed: " + reason)
                .addContext("reason", reason)
                .addContext("inputLength", input != null ? input.length() : 0);
    }
    
    public static SecurityException corsViolation(String origin, String allowedOrigins) {
        return new SecurityException("CORS_VIOLATION", 
                "CORS policy violation from origin: " + origin)
                .addContext("origin", origin)
                .addContext("allowedOrigins", allowedOrigins);
    }
}