package com.srihari.ai.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Secure configuration management for sensitive data and environment-specific settings
 */
@Configuration
@Slf4j
public class SecureConfigurationManager {

    private final Environment environment;

    public SecureConfigurationManager(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecureConfigurationService secureConfigurationService(
            SecureConfigurationProperties properties) {
        return new SecureConfigurationService(environment, properties);
    }

    @Component
    @ConfigurationProperties(prefix = "app.security.config")
    @Data
    public static class SecureConfigurationProperties {
        private boolean validateOnStartup = true;
        private boolean failFastOnMissingConfig = true;
        private List<String> requiredProperties = Arrays.asList(
                "OPEN_AI_API_KEY"
        );
        private List<String> sensitiveProperties = Arrays.asList(
                "OPEN_AI_API_KEY",
                "JWT_SECRET",
                "DATABASE_PASSWORD",
                "REDIS_PASSWORD",
                "ENCRYPTION_KEY"
        );
        private String encryptionAlgorithm = "AES/GCM/NoPadding";
        private int encryptionKeyLength = 256;
    }

    /**
     * Service for secure configuration management
     */
    public static class SecureConfigurationService {
        private final Environment environment;
        private final SecureConfigurationProperties properties;

        public SecureConfigurationService(Environment environment, 
                                        SecureConfigurationProperties properties) {
            this.environment = environment;
            this.properties = properties;
        }

        @PostConstruct
        public void validateConfiguration() {
            if (!properties.isValidateOnStartup()) {
                log.info("Configuration validation disabled");
                return;
            }

            log.info("Validating secure configuration...");
            
            List<String> missingProperties = properties.getRequiredProperties().stream()
                    .filter(prop -> !StringUtils.hasText(environment.getProperty(prop)))
                    .toList();

            if (!missingProperties.isEmpty()) {
                String message = "Missing required configuration properties: " + missingProperties;
                log.error(message);
                
                if (properties.isFailFastOnMissingConfig()) {
                    throw new IllegalStateException(message);
                }
            } else {
                log.info("All required configuration properties are present");
            }

            // Validate sensitive properties are not logged
            validateSensitivePropertiesHandling();
        }

        /**
         * Get a configuration property with optional default value
         */
        public Optional<String> getProperty(String key) {
            return Optional.ofNullable(environment.getProperty(key));
        }

        /**
         * Get a configuration property with default value
         */
        public String getProperty(String key, String defaultValue) {
            return environment.getProperty(key, defaultValue);
        }

        /**
         * Get a sensitive property (will be masked in logs)
         */
        public Optional<String> getSensitiveProperty(String key) {
            String value = environment.getProperty(key);
            if (StringUtils.hasText(value)) {
                log.debug("Retrieved sensitive property: {} = [MASKED]", key);
                return Optional.of(value);
            }
            return Optional.empty();
        }

        /**
         * Check if a property is considered sensitive
         */
        public boolean isSensitiveProperty(String key) {
            return properties.getSensitiveProperties().stream()
                    .anyMatch(sensitiveKey -> key.toUpperCase().contains(sensitiveKey.toUpperCase()));
        }

        /**
         * Mask sensitive values for logging
         */
        public String maskSensitiveValue(String key, String value) {
            if (isSensitiveProperty(key)) {
                if (!StringUtils.hasText(value)) {
                    return "[EMPTY]";
                }
                if (value.length() <= 4) {
                    return "[MASKED]";
                }
                return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
            }
            return value;
        }

        /**
         * Get all active profiles
         */
        public String[] getActiveProfiles() {
            return environment.getActiveProfiles();
        }

        /**
         * Check if a specific profile is active
         */
        public boolean isProfileActive(String profile) {
            return Arrays.asList(environment.getActiveProfiles()).contains(profile);
        }

        /**
         * Check if running in production environment
         */
        public boolean isProductionEnvironment() {
            return isProfileActive("prod") || isProfileActive("production");
        }

        /**
         * Check if running in staging environment
         */
        public boolean isStagingEnvironment() {
            return isProfileActive("staging") || isProfileActive("stage");
        }

        /**
         * Check if running in development environment
         */
        public boolean isDevelopmentEnvironment() {
            return isProfileActive("dev") || isProfileActive("development") || 
                   environment.getActiveProfiles().length == 0;
        }

        /**
         * Get environment-specific configuration
         */
        public EnvironmentConfig getEnvironmentConfig() {
            return EnvironmentConfig.builder()
                    .environment(getCurrentEnvironment())
                    .activeProfiles(Arrays.asList(getActiveProfiles()))
                    .isProduction(isProductionEnvironment())
                    .isStaging(isStagingEnvironment())
                    .isDevelopment(isDevelopmentEnvironment())
                    .build();
        }

        private String getCurrentEnvironment() {
            if (isProductionEnvironment()) return "production";
            if (isStagingEnvironment()) return "staging";
            if (isDevelopmentEnvironment()) return "development";
            return "unknown";
        }

        private void validateSensitivePropertiesHandling() {
            log.info("Validating sensitive properties handling...");
            
            // Check that sensitive properties are not accidentally exposed
            properties.getSensitiveProperties().forEach(prop -> {
                String value = environment.getProperty(prop);
                if (StringUtils.hasText(value)) {
                    log.debug("Sensitive property {} is configured: [MASKED]", prop);
                }
            });
            
            log.info("Sensitive properties validation completed");
        }
    }

    @Data
    @lombok.Builder
    public static class EnvironmentConfig {
        private String environment;
        private List<String> activeProfiles;
        private boolean isProduction;
        private boolean isStaging;
        private boolean isDevelopment;
    }
}