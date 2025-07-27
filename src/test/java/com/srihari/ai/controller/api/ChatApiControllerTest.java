package com.srihari.ai.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.srihari.ai.configuration.TestSecurityConfig;
import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.model.dto.ChatCompletionRequest;
import com.srihari.ai.service.chat.ApiChatService;

import reactor.core.publisher.Flux;

@WebFluxTest(ChatApiController.class)
@Import({TestSecurityConfig.class, ChatApiControllerTest.TestConfig.class})
@Execution(ExecutionMode.CONCURRENT)
class ChatApiControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ApiChatService apiChatService() {
            return mock(ApiChatService.class);
        }

        @Bean
        @Primary
        public CustomMetrics customMetrics() {
            return mock(CustomMetrics.class);
        }
        
        @Bean
        @Primary
        public com.srihari.ai.common.StructuredLogger structuredLogger() {
            return mock(com.srihari.ai.common.StructuredLogger.class);
        }
        
        @Bean
        @Primary
        public com.srihari.ai.service.validation.InputValidationService inputValidationService() {
            return mock(com.srihari.ai.service.validation.InputValidationService.class);
        }
        
        @Bean
        @Primary
        public com.srihari.ai.security.SensitiveDataMasker sensitiveDataMasker() {
            return mock(com.srihari.ai.security.SensitiveDataMasker.class);
        }
        
        @Bean
        @Primary
        public com.srihari.ai.event.ChatEventPublisher chatEventPublisher() {
            return mock(com.srihari.ai.event.ChatEventPublisher.class);
        }
        
        @Bean
        @Primary
        public com.srihari.ai.security.JwtAuthenticationManager jwtAuthenticationManager() {
            return mock(com.srihari.ai.security.JwtAuthenticationManager.class);
        }
        
        @Bean
        @Primary
        public com.srihari.ai.security.SecurityHeadersFilter securityHeadersFilter() {
            com.srihari.ai.security.SecurityHeadersFilter filter = mock(com.srihari.ai.security.SecurityHeadersFilter.class);
            when(filter.filter(any(), any())).thenAnswer(invocation -> {
                org.springframework.web.server.WebFilterChain chain = invocation.getArgument(1);
                return chain.filter(invocation.getArgument(0));
            });
            return filter;
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ApiChatService apiChatService;

    @Autowired
    private CustomMetrics customMetrics;
    
    @Autowired
    private com.srihari.ai.service.validation.InputValidationService inputValidationService;

    @Test
    void shouldReturnChatCompletion() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setMessage("Hello");
        request.setModel("gpt-4.1-nano");
        request.setTemperature(0.7);

        // Mock validation service
        com.srihari.ai.service.validation.InputValidationService.ValidationResult validationResult = 
            com.srihari.ai.service.validation.InputValidationService.ValidationResult.valid("Hello");
        when(inputValidationService.validateUserMessage(anyString())).thenReturn(validationResult);

        when(apiChatService.generateResponse(anyString()))
                .thenReturn(Flux.just("Hello", " there", "!"));

        // When & Then
        webTestClient.post()
                .uri("/v1/chat-completion")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class);
    }

    @Test
    void shouldValidateRequest() {
        // Given
        ChatCompletionRequest invalidRequest = new ChatCompletionRequest();
        invalidRequest.setMessage(""); // Invalid: empty message
        invalidRequest.setTemperature(3.0); // Invalid: temperature > 2.0

        // When & Then
        webTestClient.post()
                .uri("/v1/chat-completion")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }
}