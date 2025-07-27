package com.srihari.ai.service.chat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.service.CachingService;
import com.srihari.ai.service.chat.strategy.ChatModelOptions;

import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
class ApiChatServiceTest {

    @Mock
    private ChatModelService chatModelService;
    
    @Mock
    private StructuredLogger structuredLogger;
    
    @Mock
    private CustomMetrics customMetrics;
    
    @Mock
    private CachingService cachingService;
    
    @Mock
    private com.srihari.ai.event.ChatEventPublisher eventPublisher;

    @InjectMocks
    private ApiChatService apiChatService;

    @Test
    void shouldGenerateResponseSuccessfully() {
        // Given
        String userMessage = "Hello, how are you?";
        Flux<String> expectedResponse = Flux.just("I'm", " doing", " well");

        // Mock caching service
        when(cachingService.generateMessageHash(any(String.class), any(String.class), any(ChatModelOptions.class))).thenReturn("test-hash");
        when(cachingService.getCachedResponse(any(String.class), any(String.class), any(String.class))).thenReturn(null); // Cache miss
        when(cachingService.shouldCacheResponse(any(String.class), any(String.class))).thenReturn(false); // Don't cache to avoid additional mocking
        
        when(chatModelService.generateStreamingResponse(any(), any(), any())).thenReturn(expectedResponse);

        // When
        Flux<String> result = apiChatService.generateResponse(userMessage);

        // Then
        assertNotNull(result);
        // Test that the flux produces the expected values
        reactor.test.StepVerifier.create(result)
            .expectNext("I'm", " doing", " well")
            .verifyComplete();
    }

    @Test
    void shouldCreateService() {
        // Basic test to ensure service can be created
        assertNotNull(apiChatService);
    }
}