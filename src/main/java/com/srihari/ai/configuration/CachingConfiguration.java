package com.srihari.ai.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.stats.CacheStats;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for caching using Caffeine.
 * Provides conversation caching and response caching for performance optimization.
 */
@Configuration
@EnableCaching
@Slf4j
public class CachingConfiguration {

    /**
     * Configuration properties for cache settings.
     */
    @ConfigurationProperties(prefix = "app.cache")
    public static class CacheProperties {
        private Conversation conversation = new Conversation();
        private Response response = new Response();
        private Model model = new Model();

        public static class Conversation {
            private int maximumSize = 1000;
            private Duration expireAfterWrite = Duration.ofHours(2);
            private Duration expireAfterAccess = Duration.ofMinutes(30);
            private Duration refreshAfterWrite = Duration.ofMinutes(15);

            // Getters and setters
            public int getMaximumSize() { return maximumSize; }
            public void setMaximumSize(int maximumSize) { this.maximumSize = maximumSize; }
            public Duration getExpireAfterWrite() { return expireAfterWrite; }
            public void setExpireAfterWrite(Duration expireAfterWrite) { this.expireAfterWrite = expireAfterWrite; }
            public Duration getExpireAfterAccess() { return expireAfterAccess; }
            public void setExpireAfterAccess(Duration expireAfterAccess) { this.expireAfterAccess = expireAfterAccess; }
            public Duration getRefreshAfterWrite() { return refreshAfterWrite; }
            public void setRefreshAfterWrite(Duration refreshAfterWrite) { this.refreshAfterWrite = refreshAfterWrite; }
        }

        public static class Response {
            private int maximumSize = 500;
            private Duration expireAfterWrite = Duration.ofMinutes(30);
            private Duration expireAfterAccess = Duration.ofMinutes(10);

            // Getters and setters
            public int getMaximumSize() { return maximumSize; }
            public void setMaximumSize(int maximumSize) { this.maximumSize = maximumSize; }
            public Duration getExpireAfterWrite() { return expireAfterWrite; }
            public void setExpireAfterWrite(Duration expireAfterWrite) { this.expireAfterWrite = expireAfterWrite; }
            public Duration getExpireAfterAccess() { return expireAfterAccess; }
            public void setExpireAfterAccess(Duration expireAfterAccess) { this.expireAfterAccess = expireAfterAccess; }
        }

        public static class Model {
            private int maximumSize = 100;
            private Duration expireAfterWrite = Duration.ofHours(1);

            // Getters and setters
            public int getMaximumSize() { return maximumSize; }
            public void setMaximumSize(int maximumSize) { this.maximumSize = maximumSize; }
            public Duration getExpireAfterWrite() { return expireAfterWrite; }
            public void setExpireAfterWrite(Duration expireAfterWrite) { this.expireAfterWrite = expireAfterWrite; }
        }

        // Getters and setters
        public Conversation getConversation() { return conversation; }
        public void setConversation(Conversation conversation) { this.conversation = conversation; }
        public Response getResponse() { return response; }
        public void setResponse(Response response) { this.response = response; }
        public Model getModel() { return model; }
        public void setModel(Model model) { this.model = model; }
    }



    /**
     * Cache manager with multiple cache configurations.
     */
    @Bean
    public CacheManager cacheManager(CacheProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure conversation cache
        cacheManager.registerCustomCache("conversations", 
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(properties.getConversation().getMaximumSize())
                .expireAfterWrite(properties.getConversation().getExpireAfterWrite())
                .expireAfterAccess(properties.getConversation().getExpireAfterAccess())
                .recordStats()
                .removalListener((key, value, cause) -> {
                    log.debug("Conversation cache entry removed: key={}, cause={}", key, cause);
                })
                .build());

        // Configure response cache for repeated queries
        cacheManager.registerCustomCache("responses", 
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(properties.getResponse().getMaximumSize())
                .expireAfterWrite(properties.getResponse().getExpireAfterWrite())
                .expireAfterAccess(properties.getResponse().getExpireAfterAccess())
                .recordStats()
                .removalListener((key, value, cause) -> {
                    log.debug("Response cache entry removed: key={}, cause={}", key, cause);
                })
                .build());

        // Configure model metadata cache
        cacheManager.registerCustomCache("models", 
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(properties.getModel().getMaximumSize())
                .expireAfterWrite(properties.getModel().getExpireAfterWrite())
                .recordStats()
                .removalListener((key, value, cause) -> {
                    log.debug("Model cache entry removed: key={}, cause={}", key, cause);
                })
                .build());

        // Configure session cache for user sessions
        cacheManager.registerCustomCache("sessions", 
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterAccess(Duration.ofHours(4))
                .expireAfterWrite(Duration.ofHours(8))
                .recordStats()
                .removalListener((key, value, cause) -> {
                    log.debug("Session cache entry removed: key={}, cause={}", key, cause);
                })
                .build());

        return cacheManager;
    }

    /**
     * Cache statistics reporter for monitoring.
     */
    @Bean
    public CacheStatsReporter cacheStatsReporter(CacheManager cacheManager) {
        return new CacheStatsReporter(cacheManager);
    }

    /**
     * Component to report cache statistics for monitoring.
     */
    public static class CacheStatsReporter {
        private final CacheManager cacheManager;

        public CacheStatsReporter(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
        }

        public CacheStats getConversationCacheStats() {
            return getCacheStats("conversations");
        }

        public CacheStats getResponseCacheStats() {
            return getCacheStats("responses");
        }

        public CacheStats getModelCacheStats() {
            return getCacheStats("models");
        }

        public CacheStats getSessionCacheStats() {
            return getCacheStats("sessions");
        }

        private CacheStats getCacheStats(String cacheName) {
            if (cacheManager instanceof CaffeineCacheManager caffeineCacheManager) {
                var cache = caffeineCacheManager.getCache(cacheName);
                if (cache != null) {
                    var nativeCache = cache.getNativeCache();
                    if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
                        return caffeineCache.stats();
                    }
                }
            }
            return CacheStats.empty();
        }

        public void logCacheStatistics() {
            log.info("Cache Statistics:");
            log.info("Conversations - {}", getConversationCacheStats());
            log.info("Responses - {}", getResponseCacheStats());
            log.info("Models - {}", getModelCacheStats());
            log.info("Sessions - {}", getSessionCacheStats());
        }
    }
}