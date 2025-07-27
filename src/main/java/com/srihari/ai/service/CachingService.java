package com.srihari.ai.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.messages.Message;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.service.chat.strategy.ChatModelOptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing conversation and response caching.
 * Provides intelligent caching strategies for AI responses and conversation memory.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CachingService {

    private final CustomMetrics customMetrics;
    private final Map<String, Long> cacheAccessTimes = new ConcurrentHashMap<>();

    /**
     * Cache conversation messages for memory optimization.
     */
    @Cacheable(value = "conversations", key = "#conversationId")
    public List<Message> getCachedConversation(String conversationId) {
        log.debug("Cache miss for conversation: {}", conversationId);
        customMetrics.incrementCacheMiss("conversations");
        return null; // Will be populated by the calling service
    }

    /**
     * Update cached conversation with new messages.
     */
    @CachePut(value = "conversations", key = "#conversationId")
    public List<Message> updateCachedConversation(String conversationId, List<Message> messages) {
        log.debug("Updating cached conversation: {} with {} messages", conversationId, messages.size());
        customMetrics.incrementCacheUpdate("conversations");
        cacheAccessTimes.put("conversation:" + conversationId, System.currentTimeMillis());
        return messages;
    }

    /**
     * Cache AI responses for repeated queries.
     */
    @Cacheable(value = "responses", key = "#messageHash")
    public String getCachedResponse(String messageHash, String message, String model) {
        log.debug("Cache miss for response hash: {}", messageHash);
        customMetrics.incrementCacheMiss("responses");
        return null; // Will be populated by the calling service
    }

    /**
     * Store AI response in cache.
     */
    @CachePut(value = "responses", key = "#messageHash")
    public String cacheResponse(String messageHash, String response, String message, String model) {
        log.debug("Caching response for hash: {} (model: {})", messageHash, model);
        customMetrics.incrementCacheUpdate("responses");
        cacheAccessTimes.put("response:" + messageHash, System.currentTimeMillis());
        return response;
    }

    /**
     * Cache model metadata and configuration.
     */
    @Cacheable(value = "models", key = "#modelName")
    public Map<String, Object> getCachedModelInfo(String modelName) {
        log.debug("Cache miss for model info: {}", modelName);
        customMetrics.incrementCacheMiss("models");
        return null;
    }

    /**
     * Update cached model information.
     */
    @CachePut(value = "models", key = "#modelName")
    public Map<String, Object> updateCachedModelInfo(String modelName, Map<String, Object> modelInfo) {
        log.debug("Updating cached model info: {}", modelName);
        customMetrics.incrementCacheUpdate("models");
        return modelInfo;
    }

    /**
     * Cache user session data.
     */
    @Cacheable(value = "sessions", key = "#sessionId")
    public Map<String, Object> getCachedSession(String sessionId) {
        log.debug("Cache miss for session: {}", sessionId);
        customMetrics.incrementCacheMiss("sessions");
        return null;
    }

    /**
     * Update cached session data.
     */
    @CachePut(value = "sessions", key = "#sessionId")
    public Map<String, Object> updateCachedSession(String sessionId, Map<String, Object> sessionData) {
        log.debug("Updating cached session: {}", sessionId);
        customMetrics.incrementCacheUpdate("sessions");
        cacheAccessTimes.put("session:" + sessionId, System.currentTimeMillis());
        return sessionData;
    }

    /**
     * Evict conversation from cache.
     */
    @CacheEvict(value = "conversations", key = "#conversationId")
    public void evictConversation(String conversationId) {
        log.debug("Evicting conversation from cache: {}", conversationId);
        customMetrics.incrementCacheEviction("conversations");
        cacheAccessTimes.remove("conversation:" + conversationId);
    }

    /**
     * Evict response from cache.
     */
    @CacheEvict(value = "responses", key = "#messageHash")
    public void evictResponse(String messageHash) {
        log.debug("Evicting response from cache: {}", messageHash);
        customMetrics.incrementCacheEviction("responses");
        cacheAccessTimes.remove("response:" + messageHash);
    }

    /**
     * Evict session from cache.
     */
    @CacheEvict(value = "sessions", key = "#sessionId")
    public void evictSession(String sessionId) {
        log.debug("Evicting session from cache: {}", sessionId);
        customMetrics.incrementCacheEviction("sessions");
        cacheAccessTimes.remove("session:" + sessionId);
    }

    /**
     * Clear all caches.
     */
    @CacheEvict(value = {"conversations", "responses", "models", "sessions"}, allEntries = true)
    public void clearAllCaches() {
        log.info("Clearing all caches");
        customMetrics.incrementCacheEviction("all");
        cacheAccessTimes.clear();
    }

    /**
     * Generate a hash for message content to use as cache key.
     */
    public String generateMessageHash(String message, String model, Map<String, Object> options) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = message + ":" + model + ":" + (options != null ? options.toString() : "");
            byte[] hash = digest.digest(combined.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16); // Use first 16 characters
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating message hash", e);
            return String.valueOf(message.hashCode());
        }
    }
    
    /**
     * Generate a hash for message content with ChatModelOptions to use as cache key.
     */
    public String generateMessageHash(String message, String model, ChatModelOptions options) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String optionsString = options != null ? 
                String.format("temp:%.2f,maxTokens:%s,topP:%s", 
                    options.getTemperature(), 
                    options.getMaxTokens(), 
                    options.getTopP()) : "";
            String combined = message + ":" + model + ":" + optionsString;
            byte[] hash = digest.digest(combined.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16); // Use first 16 characters
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating message hash", e);
            return String.valueOf(message.hashCode());
        }
    }

    /**
     * Check if a response should be cached based on message characteristics.
     */
    public boolean shouldCacheResponse(String message, String response) {
        // Don't cache very short messages or responses
        if (message.length() < 10 || response.length() < 20) {
            return false;
        }
        
        // Don't cache responses that contain time-sensitive information
        String lowerResponse = response.toLowerCase();
        if (lowerResponse.contains("today") || lowerResponse.contains("now") || 
            lowerResponse.contains("current") || lowerResponse.contains("latest")) {
            return false;
        }
        
        // Don't cache error responses
        if (lowerResponse.contains("error") || lowerResponse.contains("sorry") || 
            lowerResponse.contains("unavailable")) {
            return false;
        }
        
        return true;
    }

    /**
     * Get cache access time for monitoring.
     */
    public Long getCacheAccessTime(String cacheType, String key) {
        return cacheAccessTimes.get(cacheType + ":" + key);
    }

    /**
     * Clean up old cache access times.
     */
    public void cleanupOldAccessTimes() {
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours ago
        cacheAccessTimes.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
        log.debug("Cleaned up {} old cache access time entries", cacheAccessTimes.size());
    }
}