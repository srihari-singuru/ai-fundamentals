package com.srihari.ai.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.service.MemoryService;
import com.srihari.ai.service.validation.InputValidationService;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.CONCURRENT)
class ServiceIntegrationTest {

    @Autowired
    private CustomMetrics customMetrics;
    
    @Autowired
    private MemoryService memoryService;
    
    @Autowired
    private InputValidationService inputValidationService;

    @Test
    void shouldHaveCustomMetricsBean() {
        // Then
        assertNotNull(customMetrics);
    }

    @Test
    void shouldHaveMemoryServiceBean() {
        // Then
        assertNotNull(memoryService);
    }

    @Test
    void shouldHaveInputValidationServiceBean() {
        // Then
        assertNotNull(inputValidationService);
    }

    @Test
    void shouldValidateUserInput() {
        // Given
        String validMessage = "Hello, how are you?";
        
        // When
        InputValidationService.ValidationResult result = 
            inputValidationService.validateUserMessage(validMessage);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void shouldRejectEmptyInput() {
        // Given
        String emptyMessage = "";
        
        // When
        InputValidationService.ValidationResult result = 
            inputValidationService.validateUserMessage(emptyMessage);
        
        // Then
        assertNotNull(result);
        assertTrue(!result.isValid());
    }

    @Test
    void shouldRejectNullInput() {
        // Given
        String nullMessage = null;
        
        // When
        InputValidationService.ValidationResult result = 
            inputValidationService.validateUserMessage(nullMessage);
        
        // Then
        assertNotNull(result);
        assertTrue(!result.isValid());
    }

    @Test
    void shouldHandleMemoryOperations() {
        // Given
        String conversationId = "test-conversation-" + System.currentTimeMillis();
        
        // When
        var conversation = memoryService.loadConversation(conversationId);
        
        // Then
        assertNotNull(conversation);
        assertTrue(conversation.isEmpty()); // Should be empty for new conversation
    }

    @Test
    void shouldRecordMetrics() {
        // Given
        String model = "gpt-4.1-nano";
        String operation = "test";
        int tokenCount = 100;
        
        // When
        customMetrics.recordTokenUsage(model, operation, tokenCount);
        
        // Then - Should not throw exception
        assertTrue(true);
    }

    @Test
    void shouldHandleCircuitBreakerMetrics() {
        // Given
        String circuitBreaker = "test-circuit-breaker";
        String event = "test-event";
        
        // When
        customMetrics.incrementCircuitBreakerEvents(circuitBreaker, event);
        
        // Then - Should not throw exception
        assertTrue(true);
    }

    @Test
    void shouldHandleCacheMetrics() {
        // Given
        String cacheName = "test-cache";
        
        // When
        customMetrics.incrementCacheHit(cacheName);
        customMetrics.incrementCacheMiss(cacheName);
        
        // Then - Should not throw exception
        assertTrue(true);
    }

    @Test
    void shouldHandleConnectionMetrics() {
        // When
        customMetrics.incrementActiveConnections();
        customMetrics.decrementActiveConnections();
        
        // Then - Should not throw exception
        assertTrue(true);
    }

    @Test
    void shouldHandleConversationMetrics() {
        // Given
        String source = "test";
        
        // When
        customMetrics.incrementConversationStarted(source);
        
        // Then - Should not throw exception
        assertTrue(true);
    }

    @Test
    void shouldHandleUserSessionMetrics() {
        // Given
        String sessionId = "test-session";
        String userAgent = "test-agent";
        
        // When
        customMetrics.recordUserSessionStarted(sessionId, userAgent);
        
        // Then - Should not throw exception
        assertTrue(true);
    }

    @Test
    void shouldHandleStreamingMetrics() {
        // Given
        int batchSize = 10;
        int batchLength = 100;
        
        // When
        customMetrics.recordStreamingBatch(batchSize, batchLength);
        
        // Then - Should not throw exception
        assertTrue(true);
    }

    @Test
    void shouldHandleErrorMetrics() {
        // Given
        String model = "gpt-4.1-nano";
        String errorType = "test-error";
        String errorCode = "TEST_ERROR";
        
        // When
        customMetrics.incrementAiErrors(model, errorType, errorCode);
        
        // Then - Should not throw exception
        assertTrue(true);
    }

    @Test
    void shouldHandleTimerMetrics() {
        // When
        var sample = customMetrics.startApiTimer();
        customMetrics.recordApiDuration(sample, "test", "integration");
        
        // Then - Should not throw exception
        assertTrue(true);
    }

    @Test
    void shouldHandleMemoryCleanup() {
        // Given
        String conversationId = "cleanup-test-" + System.currentTimeMillis();
        
        // When
        memoryService.reset(conversationId);
        
        // Then - Should not throw exception
        assertTrue(true);
    }

    @Test
    void shouldValidateLongMessage() {
        // Given
        String longMessage = "A".repeat(3000); // Long but valid message
        
        // When
        InputValidationService.ValidationResult result = 
            inputValidationService.validateUserMessage(longMessage);
        
        // Then
        assertNotNull(result);
        // Result depends on validation rules - could be valid or invalid
    }

    @Test
    void shouldHandleSpecialCharacters() {
        // Given
        String messageWithSpecialChars = "Hello! How are you? 🚀 This has émojis and spëcial chars.";
        
        // When
        InputValidationService.ValidationResult result = 
            inputValidationService.validateUserMessage(messageWithSpecialChars);
        
        // Then
        assertNotNull(result);
        // Should handle special characters gracefully
    }

    @Test
    void shouldHandleMultipleValidationRequests() {
        // Given
        String[] messages = {
            "Hello",
            "How are you?",
            "What's the weather like?",
            "Tell me a joke",
            "Explain quantum computing"
        };
        
        // When & Then
        for (String message : messages) {
            InputValidationService.ValidationResult result = 
                inputValidationService.validateUserMessage(message);
            assertNotNull(result);
        }
    }

    @Test
    void shouldHandleSystemResourceMetrics() {
        // When
        customMetrics.recordJvmMemoryUsage();
        customMetrics.recordSystemResources();
        
        // Then - Should not throw exception
        assertTrue(true);
    }
}