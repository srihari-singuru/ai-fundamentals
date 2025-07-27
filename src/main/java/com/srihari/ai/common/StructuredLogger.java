package com.srihari.ai.common;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.srihari.ai.security.SensitiveDataMasker;

import lombok.RequiredArgsConstructor;

/**
 * Structured logger utility that provides consistent logging methods with sensitive data masking.
 * Integrates with correlation IDs and provides structured log event builders.
 */
@Component
@RequiredArgsConstructor
public class StructuredLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
    private final SensitiveDataMasker sensitiveDataMasker;
    
    /**
     * Logs an info message with structured context.
     * 
     * @param message the log message
     * @param context additional context data
     */
    public void info(String message, Map<String, Object> context) {
        logWithContext("INFO", message, context, null);
    }
    
    /**
     * Logs an info message with simple key-value pairs.
     * 
     * @param message the log message
     * @param key the context key
     * @param value the context value
     */
    public void info(String message, String key, Object value) {
        Map<String, Object> context = Map.of(key, value);
        info(message, context);
    }
    
    /**
     * Logs a debug message with structured context.
     * 
     * @param message the log message
     * @param context additional context data
     */
    public void debug(String message, Map<String, Object> context) {
        if (logger.isDebugEnabled()) {
            logWithContext("DEBUG", message, context, null);
        }
    }
    
    /**
     * Logs a debug message with simple key-value pairs.
     * 
     * @param message the log message
     * @param key the context key
     * @param value the context value
     */
    public void debug(String message, String key, Object value) {
        if (logger.isDebugEnabled()) {
            Map<String, Object> context = Map.of(key, value);
            debug(message, context);
        }
    }
    
    /**
     * Logs a warning message with structured context.
     * 
     * @param message the log message
     * @param context additional context data
     */
    public void warn(String message, Map<String, Object> context) {
        logWithContext("WARN", message, context, null);
    }
    
    /**
     * Logs a warning message with simple key-value pairs.
     * 
     * @param message the log message
     * @param key the context key
     * @param value the context value
     */
    public void warn(String message, String key, Object value) {
        Map<String, Object> context = Map.of(key, value);
        warn(message, context);
    }
    
    /**
     * Logs an error message with structured context and exception.
     * 
     * @param message the log message
     * @param context additional context data
     * @param throwable the exception
     */
    public void error(String message, Map<String, Object> context, Throwable throwable) {
        logWithContext("ERROR", message, context, throwable);
    }
    
    /**
     * Logs an error message with exception.
     * 
     * @param message the log message
     * @param throwable the exception
     */
    public void error(String message, Throwable throwable) {
        error(message, Map.of(), throwable);
    }
    
    /**
     * Logs an error message with simple key-value pairs and exception.
     * 
     * @param message the log message
     * @param key the context key
     * @param value the context value
     * @param throwable the exception
     */
    public void error(String message, String key, Object value, Throwable throwable) {
        Map<String, Object> context = Map.of(key, value);
        error(message, context, throwable);
    }
    
    /**
     * Creates a log event builder for complex logging scenarios.
     * 
     * @param level the log level
     * @return a new LogEventBuilder instance
     */
    public LogEventBuilder event(String level) {
        return new LogEventBuilder(level);
    }
    
    /**
     * Masks sensitive data in the provided text.
     * 
     * @param text the text to mask
     * @return the masked text
     */
    public String maskSensitiveData(String text) {
        return sensitiveDataMasker.maskUserData(text);
    }
    
    /**
     * Masks sensitive data in a map of context data.
     * 
     * @param context the context map to mask
     * @return a new map with masked sensitive data
     */
    public Map<String, Object> maskSensitiveData(Map<String, Object> context) {
        return sensitiveDataMasker.maskLogData(context);
    }
    
    private void logWithContext(String level, String message, Map<String, Object> context, Throwable throwable) {
        // Mask sensitive data in message and context
        String maskedMessage = sensitiveDataMasker.maskUserData(message);
        Map<String, Object> maskedContext = maskSensitiveData(context);
        
        // Mask exception data if present (for future use in structured logging)
        if (throwable != null) {
            sensitiveDataMasker.maskExceptionData(throwable);
        }
        
        // Add masked context to MDC
        if (maskedContext != null && !maskedContext.isEmpty()) {
            maskedContext.forEach((key, value) -> MDC.put(key, String.valueOf(value)));
        }
        
        try {
            switch (level) {
                case "DEBUG" -> {
                    if (throwable != null) {
                        logger.debug(maskedMessage, throwable);
                    } else {
                        logger.debug(maskedMessage);
                    }
                }
                case "INFO" -> {
                    if (throwable != null) {
                        logger.info(maskedMessage, throwable);
                    } else {
                        logger.info(maskedMessage);
                    }
                }
                case "WARN" -> {
                    if (throwable != null) {
                        logger.warn(maskedMessage, throwable);
                    } else {
                        logger.warn(maskedMessage);
                    }
                }
                case "ERROR" -> {
                    if (throwable != null) {
                        logger.error(maskedMessage, throwable);
                    } else {
                        logger.error(maskedMessage);
                    }
                }
                default -> logger.info(maskedMessage);
            }
        } finally {
            // Clean up context from MDC
            if (maskedContext != null && !maskedContext.isEmpty()) {
                maskedContext.keySet().forEach(MDC::remove);
            }
        }
    }
    
    /**
     * Builder class for creating complex log events with multiple context attributes.
     */
    public class LogEventBuilder {
        private final String level;
        private String message;
        private final Map<String, Object> context = new HashMap<>();
        private Throwable throwable;
        
        private LogEventBuilder(String level) {
            this.level = level;
        }
        
        public LogEventBuilder message(String message) {
            this.message = message;
            return this;
        }
        
        public LogEventBuilder context(String key, Object value) {
            this.context.put(key, value);
            return this;
        }
        
        public LogEventBuilder context(Map<String, Object> contextMap) {
            this.context.putAll(contextMap);
            return this;
        }
        
        public LogEventBuilder exception(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }
        
        public LogEventBuilder operation(String operation) {
            return context("operation", operation);
        }
        
        public LogEventBuilder duration(long durationMs) {
            return context("duration_ms", durationMs);
        }
        
        public LogEventBuilder userId(String userId) {
            return context("user_id", userId);
        }
        
        public LogEventBuilder requestId(String requestId) {
            return context("request_id", requestId);
        }
        
        public void log() {
            if (message == null) {
                throw new IllegalStateException("Message is required for logging");
            }
            logWithContext(level, message, context, throwable);
        }
    }
}