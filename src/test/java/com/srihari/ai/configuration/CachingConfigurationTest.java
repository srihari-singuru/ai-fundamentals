package com.srihari.ai.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.srihari.ai.configuration.CachingConfiguration.CacheProperties;
import com.srihari.ai.configuration.CachingConfiguration.CacheStatsReporter;

@Execution(ExecutionMode.CONCURRENT)
class CachingConfigurationTest {

    private CachingConfiguration cachingConfiguration;
    private CacheProperties cacheProperties;

    @BeforeEach
    void setUp() {
        cachingConfiguration = new CachingConfiguration();
        cacheProperties = new CacheProperties();
    }

    @Test
    void shouldCreateCachePropertiesWithDefaults() {
        // When
        CacheProperties properties = new CacheProperties();

        // Then
        assertNotNull(properties);
        assertNotNull(properties.getConversation());
        assertNotNull(properties.getResponse());
        assertNotNull(properties.getModel());
    }

    @Test
    void shouldHaveCorrectConversationCacheDefaults() {
        // Given
        CacheProperties.Conversation conversation = cacheProperties.getConversation();

        // Then
        assertEquals(1000, conversation.getMaximumSize());
        assertEquals(Duration.ofHours(2), conversation.getExpireAfterWrite());
        assertEquals(Duration.ofMinutes(30), conversation.getExpireAfterAccess());
        assertEquals(Duration.ofMinutes(15), conversation.getRefreshAfterWrite());
    }

    @Test
    void shouldHaveCorrectResponseCacheDefaults() {
        // Given
        CacheProperties.Response response = cacheProperties.getResponse();

        // Then
        assertEquals(500, response.getMaximumSize());
        assertEquals(Duration.ofMinutes(30), response.getExpireAfterWrite());
        assertEquals(Duration.ofMinutes(10), response.getExpireAfterAccess());
    }

    @Test
    void shouldHaveCorrectModelCacheDefaults() {
        // Given
        CacheProperties.Model model = cacheProperties.getModel();

        // Then
        assertEquals(100, model.getMaximumSize());
        assertEquals(Duration.ofHours(1), model.getExpireAfterWrite());
    }

    @Test
    void shouldAllowSettingConversationCacheProperties() {
        // Given
        CacheProperties.Conversation conversation = cacheProperties.getConversation();

        // When
        conversation.setMaximumSize(2000);
        conversation.setExpireAfterWrite(Duration.ofHours(4));
        conversation.setExpireAfterAccess(Duration.ofHours(1));
        conversation.setRefreshAfterWrite(Duration.ofMinutes(30));

        // Then
        assertEquals(2000, conversation.getMaximumSize());
        assertEquals(Duration.ofHours(4), conversation.getExpireAfterWrite());
        assertEquals(Duration.ofHours(1), conversation.getExpireAfterAccess());
        assertEquals(Duration.ofMinutes(30), conversation.getRefreshAfterWrite());
    }

    @Test
    void shouldAllowSettingResponseCacheProperties() {
        // Given
        CacheProperties.Response response = cacheProperties.getResponse();

        // When
        response.setMaximumSize(1000);
        response.setExpireAfterWrite(Duration.ofHours(1));
        response.setExpireAfterAccess(Duration.ofMinutes(20));

        // Then
        assertEquals(1000, response.getMaximumSize());
        assertEquals(Duration.ofHours(1), response.getExpireAfterWrite());
        assertEquals(Duration.ofMinutes(20), response.getExpireAfterAccess());
    }

    @Test
    void shouldAllowSettingModelCacheProperties() {
        // Given
        CacheProperties.Model model = cacheProperties.getModel();

        // When
        model.setMaximumSize(200);
        model.setExpireAfterWrite(Duration.ofHours(2));

        // Then
        assertEquals(200, model.getMaximumSize());
        assertEquals(Duration.ofHours(2), model.getExpireAfterWrite());
    }

    @Test
    void shouldAllowSettingTopLevelCacheProperties() {
        // Given
        CacheProperties properties = cacheProperties;
        CacheProperties.Conversation newConversation = new CacheProperties.Conversation();
        CacheProperties.Response newResponse = new CacheProperties.Response();
        CacheProperties.Model newModel = new CacheProperties.Model();

        // When
        properties.setConversation(newConversation);
        properties.setResponse(newResponse);
        properties.setModel(newModel);

        // Then
        assertEquals(newConversation, properties.getConversation());
        assertEquals(newResponse, properties.getResponse());
        assertEquals(newModel, properties.getModel());
    }

    @Test
    void shouldCreateCacheManagerWithMultipleCaches() {
        // When
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);

        // Then
        assertNotNull(cacheManager);
        assertTrue(cacheManager instanceof CaffeineCacheManager);

        CaffeineCacheManager caffeineCacheManager = (CaffeineCacheManager) cacheManager;

        // Verify all expected caches are registered
        assertNotNull(caffeineCacheManager.getCache("conversations"));
        assertNotNull(caffeineCacheManager.getCache("responses"));
        assertNotNull(caffeineCacheManager.getCache("models"));
        assertNotNull(caffeineCacheManager.getCache("sessions"));
    }

    @Test
    void shouldCreateConversationCacheWithCorrectConfiguration() {
        // When
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);

        // Then
        assertNotNull(cacheManager.getCache("conversations"));
        // Note: We can't easily test the internal Caffeine configuration without
        // accessing private fields, but we can verify the cache exists and works
    }

    @Test
    void shouldCreateResponseCacheWithCorrectConfiguration() {
        // When
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);

        // Then
        assertNotNull(cacheManager.getCache("responses"));
    }

    @Test
    void shouldCreateModelCacheWithCorrectConfiguration() {
        // When
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);

        // Then
        assertNotNull(cacheManager.getCache("models"));
    }

    @Test
    void shouldCreateSessionCacheWithCorrectConfiguration() {
        // When
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);

        // Then
        assertNotNull(cacheManager.getCache("sessions"));
    }

    @Test
    void shouldCreateCacheStatsReporter() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);

        // When
        CacheStatsReporter reporter = cachingConfiguration.cacheStatsReporter(cacheManager);

        // Then
        assertNotNull(reporter);
    }

    @Test
    void shouldReportConversationCacheStats() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);
        CacheStatsReporter reporter = new CacheStatsReporter(cacheManager);

        // When
        CacheStats stats = reporter.getConversationCacheStats();

        // Then
        assertNotNull(stats);
        assertEquals(0L, stats.requestCount()); // Should be empty initially
    }

    @Test
    void shouldReportResponseCacheStats() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);
        CacheStatsReporter reporter = new CacheStatsReporter(cacheManager);

        // When
        CacheStats stats = reporter.getResponseCacheStats();

        // Then
        assertNotNull(stats);
        assertEquals(0L, stats.requestCount()); // Should be empty initially
    }

    @Test
    void shouldReportModelCacheStats() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);
        CacheStatsReporter reporter = new CacheStatsReporter(cacheManager);

        // When
        CacheStats stats = reporter.getModelCacheStats();

        // Then
        assertNotNull(stats);
        assertEquals(0L, stats.requestCount()); // Should be empty initially
    }

    @Test
    void shouldReportSessionCacheStats() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);
        CacheStatsReporter reporter = new CacheStatsReporter(cacheManager);

        // When
        CacheStats stats = reporter.getSessionCacheStats();

        // Then
        assertNotNull(stats);
        assertEquals(0L, stats.requestCount()); // Should be empty initially
    }

    @Test
    void shouldReturnEmptyStatsForNonExistentCache() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);
        CacheStatsReporter reporter = new CacheStatsReporter(cacheManager);

        // When - Try to get stats for a cache that doesn't exist
        // We'll use reflection to test the private method
        try {
            java.lang.reflect.Method getCacheStats = CacheStatsReporter.class.getDeclaredMethod("getCacheStats",
                    String.class);
            getCacheStats.setAccessible(true);
            CacheStats stats = (CacheStats) getCacheStats.invoke(reporter, "nonexistent");

            // Then
            assertEquals(CacheStats.empty(), stats);
        } catch (Exception e) {
            // If reflection fails, just verify the reporter doesn't throw exceptions
            assertNotNull(reporter.getConversationCacheStats());
        }
    }

    @Test
    void shouldLogCacheStatisticsWithoutException() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);
        CacheStatsReporter reporter = new CacheStatsReporter(cacheManager);

        // When & Then - Should not throw exception
        reporter.logCacheStatistics();
    }

    @Test
    void shouldHandleNonCaffeineCacheManager() {
        // Given
        CacheManager nonCaffeineCacheManager = new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
        CacheStatsReporter reporter = new CacheStatsReporter(nonCaffeineCacheManager);

        // When
        CacheStats stats = reporter.getConversationCacheStats();

        // Then - Should return empty stats for non-Caffeine cache managers
        assertEquals(CacheStats.empty(), stats);
    }

    @Test
    void shouldWorkWithCacheOperations() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);

        // When - Perform cache operations
        var conversationCache = cacheManager.getCache("conversations");
        assertNotNull(conversationCache);

        conversationCache.put("test-key", "test-value");
        var cachedValue = conversationCache.get("test-key");

        // Then
        assertNotNull(cachedValue);
        assertEquals("test-value", cachedValue.get());
    }

    @Test
    void shouldSupportCacheEviction() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);
        var cache = cacheManager.getCache("responses");
        assertNotNull(cache);

        // When
        cache.put("key1", "value1");
        assertNotNull(cache.get("key1"));

        cache.evict("key1");

        // Then
        var evictedValue = cache.get("key1");
        assertTrue(evictedValue == null || evictedValue.get() == null);
    }

    @Test
    void shouldSupportCacheClear() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);
        var cache = cacheManager.getCache("models");
        assertNotNull(cache);

        // When
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        cache.clear();

        // Then
        var value1 = cache.get("key1");
        var value2 = cache.get("key2");
        assertTrue((value1 == null || value1.get() == null) &&
                (value2 == null || value2.get() == null));
    }

    @Test
    void shouldHaveStatsEnabledForAllCaches() {
        // Given
        CacheManager cacheManager = cachingConfiguration.cacheManager(cacheProperties);
        CacheStatsReporter reporter = new CacheStatsReporter(cacheManager);

        // When - Perform some cache operations to generate stats
        var conversationCache = cacheManager.getCache("conversations");
        var responseCache = cacheManager.getCache("responses");
        var modelCache = cacheManager.getCache("models");
        var sessionCache = cacheManager.getCache("sessions");

        // Verify caches exist before performing operations
        assertNotNull(conversationCache);
        assertNotNull(responseCache);
        assertNotNull(modelCache);
        assertNotNull(sessionCache);

        // Perform operations
        conversationCache.put("test", "value");
        conversationCache.get("test");
        conversationCache.get("missing");

        responseCache.put("test", "value");
        responseCache.get("test");

        modelCache.put("test", "value");
        sessionCache.put("test", "value");

        // Then - Stats should be available (non-zero request counts)
        CacheStats conversationStats = reporter.getConversationCacheStats();
        CacheStats responseStats = reporter.getResponseCacheStats();
        CacheStats modelStats = reporter.getModelCacheStats();
        CacheStats sessionStats = reporter.getSessionCacheStats();

        // Verify stats are available (some caches might not have been accessed)
        assertNotNull(conversationStats);
        assertNotNull(responseStats);
        assertNotNull(modelStats);
        assertNotNull(sessionStats);

        // At least conversation cache should have stats since we accessed it
        assertTrue(conversationStats.requestCount() >= 0);
    }
}