package com.srihari.ai.controller.api;

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

import com.srihari.ai.config.TestSecurityConfig;
import com.srihari.ai.metrics.ChatMetrics;
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
        public ChatMetrics chatMetrics() {
            return mock(ChatMetrics.class);
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ApiChatService apiChatService;

    @Autowired
    private ChatMetrics chatMetrics;

    @Test
    void shouldReturnChatCompletion() {
        // Given
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setMessage("Hello");
        request.setModel("gpt-4.1-nano");
        request.setTemperature(0.7);

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