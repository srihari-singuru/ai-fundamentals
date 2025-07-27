package com.srihari.ai.configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for feature toggles to enable gradual rollouts and A/B testing
 */
@Configuration
@Slf4j
public class FeatureToggleConfiguration {

    @Bean
    public FeatureToggleService featureToggleService(FeatureToggleProperties properties) {
        log.info("Initializing feature toggle service with {} features", properties.getFeatureFlags().size());
        return new FeatureToggleService(properties);
    }

    @Component
    @ConfigurationProperties(prefix = "app.feature-flags")
    @Data
    public static class FeatureToggleProperties {
        private boolean advancedLogging = true;
        private boolean caching = true;
        private boolean rateLimiting = true;
        private boolean circuitBreaker = true;
        private boolean metrics = true;
        private boolean tracing = false;
        private boolean notifications = true;
        private boolean securityHeaders = true;
        private boolean performanceOptimization = true;
        private boolean enhancedErrorHandling = true;
        private boolean streamingOptimization = true;
        private boolean memoryManagement = true;
        
        /**
         * Get all feature flags as a map for easier access
         */
        public Map<String, Boolean> getFeatureFlags() {
            Map<String, Boolean> flags = new ConcurrentHashMap<>();
            flags.put("advanced-logging", advancedLogging);
            flags.put("caching", caching);
            flags.put("rate-limiting", rateLimiting);
            flags.put("circuit-breaker", circuitBreaker);
            flags.put("metrics", metrics);
            flags.put("tracing", tracing);
            flags.put("notifications", notifications);
            flags.put("security-headers", securityHeaders);
            flags.put("performance-optimization", performanceOptimization);
            flags.put("enhanced-error-handling", enhancedErrorHandling);
            flags.put("streaming-optimization", streamingOptimization);
            flags.put("memory-management", memoryManagement);
            return flags;
        }
    }

    /**
     * Service for checking feature toggle states
     */
    public static class FeatureToggleService {
        private final FeatureToggleProperties properties;
        private final Map<String, Boolean> runtimeToggles = new ConcurrentHashMap<>();

        public FeatureToggleService(FeatureToggleProperties properties) {
            this.properties = properties;
            log.info("Feature toggles initialized: {}", properties.getFeatureFlags());
        }

        /**
         * Check if a feature is enabled
         */
        public boolean isEnabled(String featureName) {
            // Check runtime toggles first (for dynamic changes)
            Boolean runtimeValue = runtimeToggles.get(featureName);
            if (runtimeValue != null) {
                return runtimeValue;
            }

            // Fall back to configuration properties
            return properties.getFeatureFlags().getOrDefault(featureName, false);
        }

        /**
         * Enable a feature at runtime
         */
        public void enableFeature(String featureName) {
            log.info("Enabling feature: {}", featureName);
            runtimeToggles.put(featureName, true);
        }

        /**
         * Disable a feature at runtime
         */
        public void disableFeature(String featureName) {
            log.info("Disabling feature: {}", featureName);
            runtimeToggles.put(featureName, false);
        }

        /**
         * Reset a feature to its configuration default
         */
        public void resetFeature(String featureName) {
            log.info("Resetting feature to default: {}", featureName);
            runtimeToggles.remove(featureName);
        }

        /**
         * Get all current feature states
         */
        public Map<String, Boolean> getAllFeatureStates() {
            Map<String, Boolean> allStates = new ConcurrentHashMap<>(properties.getFeatureFlags());
            allStates.putAll(runtimeToggles);
            return allStates;
        }

        /**
         * Check if advanced logging is enabled
         */
        public boolean isAdvancedLoggingEnabled() {
            return isEnabled("advanced-logging");
        }

        /**
         * Check if caching is enabled
         */
        public boolean isCachingEnabled() {
            return isEnabled("caching");
        }

        /**
         * Check if rate limiting is enabled
         */
        public boolean isRateLimitingEnabled() {
            return isEnabled("rate-limiting");
        }

        /**
         * Check if circuit breaker is enabled
         */
        public boolean isCircuitBreakerEnabled() {
            return isEnabled("circuit-breaker");
        }

        /**
         * Check if metrics collection is enabled
         */
        public boolean isMetricsEnabled() {
            return isEnabled("metrics");
        }

        /**
         * Check if distributed tracing is enabled
         */
        public boolean isTracingEnabled() {
            return isEnabled("tracing");
        }

        /**
         * Check if notifications are enabled
         */
        public boolean isNotificationsEnabled() {
            return isEnabled("notifications");
        }

        /**
         * Check if security headers are enabled
         */
        public boolean isSecurityHeadersEnabled() {
            return isEnabled("security-headers");
        }

        /**
         * Check if performance optimization is enabled
         */
        public boolean isPerformanceOptimizationEnabled() {
            return isEnabled("performance-optimization");
        }

        /**
         * Check if enhanced error handling is enabled
         */
        public boolean isEnhancedErrorHandlingEnabled() {
            return isEnabled("enhanced-error-handling");
        }

        /**
         * Check if streaming optimization is enabled
         */
        public boolean isStreamingOptimizationEnabled() {
            return isEnabled("streaming-optimization");
        }

        /**
         * Check if memory management is enabled
         */
        public boolean isMemoryManagementEnabled() {
            return isEnabled("memory-management");
        }
    }
}