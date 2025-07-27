package com.srihari.ai.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when configuration-related errors occur.
 * This includes missing configuration, invalid configuration values, and startup failures.
 */
public class ConfigurationException extends AiApplicationException {
    
    public static final String ERROR_CATEGORY = "CONFIGURATION_ERROR";
    
    public ConfigurationException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public ConfigurationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public ConfigurationException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }
    
    public ConfigurationException(String errorCode, String message, Throwable cause, Map<String, Object> context) {
        super(errorCode, message, cause, context);
    }
    
    @Override
    public int getHttpStatusCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
    
    @Override
    public String getErrorCategory() {
        return ERROR_CATEGORY;
    }
    
    // Factory methods for common configuration errors
    
    public static ConfigurationException missingProperty(String propertyName) {
        return new ConfigurationException("MISSING_PROPERTY", 
                "Required configuration property is missing: " + propertyName)
                .addContext("propertyName", propertyName);
    }
    
    public static ConfigurationException invalidPropertyValue(String propertyName, String value, String expectedFormat) {
        return new ConfigurationException("INVALID_PROPERTY_VALUE", 
                "Invalid value for configuration property '" + propertyName + "': " + value)
                .addContext("propertyName", propertyName)
                .addContext("value", value)
                .addContext("expectedFormat", expectedFormat);
    }
    
    public static ConfigurationException beanCreationFailed(String beanName, String reason) {
        return new ConfigurationException("BEAN_CREATION_FAILED", 
                "Failed to create bean '" + beanName + "': " + reason)
                .addContext("beanName", beanName)
                .addContext("reason", reason);
    }
    
    public static ConfigurationException profileActivationFailed(String profile, String reason) {
        return new ConfigurationException("PROFILE_ACTIVATION_FAILED", 
                "Failed to activate profile '" + profile + "': " + reason)
                .addContext("profile", profile)
                .addContext("reason", reason);
    }
    
    public static ConfigurationException resourceNotFound(String resourcePath) {
        return new ConfigurationException("RESOURCE_NOT_FOUND", 
                "Configuration resource not found: " + resourcePath)
                .addContext("resourcePath", resourcePath);
    }
    
    public static ConfigurationException circuitBreakerConfigInvalid(String circuitName, String issue) {
        return new ConfigurationException("CIRCUIT_BREAKER_CONFIG_INVALID", 
                "Invalid circuit breaker configuration for '" + circuitName + "': " + issue)
                .addContext("circuitName", circuitName)
                .addContext("issue", issue);
    }
    
    public static ConfigurationException cacheConfigInvalid(String cacheName, String issue) {
        return new ConfigurationException("CACHE_CONFIG_INVALID", 
                "Invalid cache configuration for '" + cacheName + "': " + issue)
                .addContext("cacheName", cacheName)
                .addContext("issue", issue);
    }
}