package com.srihari.ai.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration properties for application-wide settings.
 * Handles various app.* properties that don't have dedicated configuration classes.
 */
@Component
@ConfigurationProperties(prefix = "app")
@Data
public class ApplicationProperties {

    private Events events = new Events();
    private RateLimiting rateLimiting = new RateLimiting();
    private Security security = new Security();
    private FeatureFlags featureFlags = new FeatureFlags();

    @Data
    public static class Events {
        private Notifications notifications = new Notifications();

        @Data
        public static class Notifications {
            private boolean enabled = true;
        }
    }

    @Data
    public static class RateLimiting {
        private Api api = new Api();
        private Web web = new Web();
        private OpenAi openAi = new OpenAi();

        @Data
        public static class Api {
            private int limitForPeriod = 100;
            private Duration limitRefreshPeriod = Duration.ofSeconds(60);
            private Duration timeoutDuration = Duration.ofSeconds(5);
        }

        @Data
        public static class Web {
            private int limitForPeriod = 200;
            private Duration limitRefreshPeriod = Duration.ofSeconds(60);
            private Duration timeoutDuration = Duration.ofSeconds(2);
        }

        @Data
        public static class OpenAi {
            private int limitForPeriod = 50;
            private Duration limitRefreshPeriod = Duration.ofSeconds(60);
            private Duration timeoutDuration = Duration.ofSeconds(10);
        }
    }



    @Data
    public static class Security {
        private Cors cors = new Cors();
        private RateLimit rateLimit = new RateLimit();
        private Headers headers = new Headers();

        @Data
        public static class Cors {
            private String allowedOrigins = "";
            private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";
            private String allowedHeaders = "*";
            private boolean allowCredentials = false;
            private int maxAge = 3600;
        }

        @Data
        public static class RateLimit {
            private boolean enabled = true;
            private int perIpLimit = 1000;
            private Duration perIpWindow = Duration.ofSeconds(3600);
        }

        @Data
        public static class Headers {
            private String contentSecurityPolicy = "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'";
            private String strictTransportSecurity = "max-age=31536000; includeSubDomains";
            private String xFrameOptions = "DENY";
            private String xContentTypeOptions = "nosniff";
            private String referrerPolicy = "strict-origin-when-cross-origin";
        }
    }

    @Data
    public static class FeatureFlags {
        private boolean advancedLogging = true;
        private boolean caching = true;
        private boolean rateLimiting = true;
        private boolean circuitBreaker = true;
        private boolean metrics = true;
        private boolean tracing = false;
        private boolean notifications = true;
        private boolean securityHeaders = true;
    }
}