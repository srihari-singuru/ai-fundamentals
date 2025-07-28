package com.srihari.ai.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.srihari.ai.common.CorrelationIdFilter;
import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.security.SensitiveDataMasker;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Configuration class for structured logging with JSON format support.
 * Configures different logging patterns based on active profiles and provides
 * logging-related beans for the application.
 */
@Configuration
@RequiredArgsConstructor
public class LoggingConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingConfiguration.class);
    private final Environment environment;
    private final LoggingProperties loggingProperties;
    

    
    /**
     * Correlation ID filter bean for request tracking.
     */
    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        logger.debug("Creating CorrelationIdFilter bean");
        return new CorrelationIdFilter();
    }
    
    /**
     * Structured logger bean with sensitive data masking.
     */
    @Bean
    public StructuredLogger structuredLogger(SensitiveDataMasker sensitiveDataMasker) {
        logger.debug("Creating StructuredLogger bean with sensitive data masking");
        return new StructuredLogger(sensitiveDataMasker);
    }
    
    /**
     * Logs the active logging configuration after application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        boolean isProduction = environment.matchesProfiles("prod", "production");
        String[] activeProfiles = environment.getActiveProfiles();
        LoggingProperties props = loggingProperties;
        
        if (activeProfiles.length == 0) {
            logger.info("Logging configuration initialized with default profile (structured text format)");
        } else {
            logger.info("Logging configuration initialized for profiles: {} (format: {})", 
                       String.join(", ", activeProfiles), 
                       isProduction ? "JSON" : "structured text");
        }
        
        logger.info("Correlation ID support enabled for request tracking");
        logger.info("Sensitive data masking enabled for security");
        logger.info("Structured logging level: {}", props.getLevel());
        logger.info("JSON logging enabled: {}", props.isJsonEnabled());
        logger.info("Request logging enabled: {}", props.isRequestLoggingEnabled());
        logger.info("Performance logging enabled: {}", props.isPerformanceLoggingEnabled());
        
        // Log MDC configuration
        logger.info("MDC keys configured: correlationId, userId, operation, source");
        
        // Log sensitive data masking patterns
        logger.info("Sensitive data masking patterns: API keys, email addresses, phone numbers, user data");
    }
    
    /**
     * Configuration properties for logging behavior.
     */
    @Component
    @ConfigurationProperties(prefix = "app.logging")
    @Data
    public static class LoggingProperties {
        
        /**
         * Default logging level for the application.
         */
        private String level = "INFO";
        
        /**
         * Whether JSON logging format is enabled.
         */
        private boolean jsonEnabled = false;
        
        /**
         * Whether request/response logging is enabled.
         */
        private boolean requestLoggingEnabled = true;
        
        /**
         * Whether performance logging is enabled.
         */
        private boolean performanceLoggingEnabled = true;
        
        /**
         * Whether to log sensitive data (should be false in production).
         */
        private boolean logSensitiveData = false;
        
        /**
         * Maximum length for logged messages before truncation.
         */
        private int maxMessageLength = 1000;
        
        /**
         * Whether to include stack traces in error logs.
         */
        private boolean includeStackTrace = true;
        
        /**
         * Whether to log method entry/exit for debugging.
         */
        private boolean traceMethodCalls = false;
        
        /**
         * Minimum duration in milliseconds to log slow operations.
         */
        private long slowOperationThresholdMs = 1000;
    }
}