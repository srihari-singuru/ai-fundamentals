package com.srihari.ai.health;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.service.MemoryService;

/**
 * Basic tests for health indicators to ensure they can be instantiated.
 */
@ExtendWith(MockitoExtension.class)
class HealthIndicatorBasicTest {

    @Mock
    private MemoryService memoryService;
    
    @Mock
    private CustomMetrics customMetrics;
    
    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Test
    void shouldCreateChatMemoryHealthIndicator() {
        // When
        ChatMemoryHealthIndicator healthIndicator = new ChatMemoryHealthIndicator(memoryService, customMetrics);
        
        // Then
        assertNotNull(healthIndicator);
    }

    @Test
    void shouldCreateOpenAiHealthIndicator() {
        // When
        OpenAiHealthIndicator healthIndicator = new OpenAiHealthIndicator(chatClientBuilder, customMetrics);
        
        // Then
        assertNotNull(healthIndicator);
    }

    @Test
    void shouldCreateSystemResourcesHealthIndicator() {
        // When
        SystemResourcesHealthIndicator healthIndicator = new SystemResourcesHealthIndicator(customMetrics);
        
        // Then
        assertNotNull(healthIndicator);
    }
}