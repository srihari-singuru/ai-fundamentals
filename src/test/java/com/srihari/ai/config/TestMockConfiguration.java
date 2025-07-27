package com.srihari.ai.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.srihari.ai.service.chat.ApiChatService;

import reactor.core.publisher.Flux;

/**
 * Test configuration that provides mocked services for integration tests
 * to avoid external API calls.
 */
@TestConfiguration
@Profile("integration-test")
public class TestMockConfiguration {

    @Bean
    @Primary
    public ApiChatService mockApiChatService() {
        ApiChatService mockService = mock(ApiChatService.class);
        
        // Mock the generateResponse method to return a simple response
        when(mockService.generateResponse(anyString()))
            .thenReturn(Flux.just("Mocked", " AI", " response"));
            
        return mockService;
    }
}