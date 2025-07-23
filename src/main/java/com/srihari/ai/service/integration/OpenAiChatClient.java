package com.srihari.ai.service.integration;

import com.srihari.ai.common.Constants;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class OpenAiChatClient {

    private final ChatClient chatClient;

    public OpenAiChatClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Send a single message and get streaming response (for API)
     */
    @CircuitBreaker(name = Constants.OPENAI_CIRCUIT_BREAKER, fallbackMethod = "fallbackStream")
    public Flux<String> sendStream(String message) {
        return chatClient.prompt(new Prompt(new UserMessage(message)))
                .stream()
                .content();
    }

    /**
     * Send conversation messages and get complete response (for Web UI)
     */
    @CircuitBreaker(name = Constants.OPENAI_CIRCUIT_BREAKER, fallbackMethod = "fallbackComplete")
    public Mono<String> sendComplete(List<Message> messages) {
        return chatClient.prompt(new Prompt(messages))
                .stream()
                .content()
                .collectList()
                .map(tokens -> String.join("", tokens));
    }

    // Fallback methods
    public Flux<String> fallbackStream(String message, Throwable ex) {
        log.error("OpenAI streaming failed for message: '{}', reason: {}", message, ex.getMessage(), ex);
        return Flux.just(Constants.AI_UNAVAILABLE_MESSAGE);
    }

    public Mono<String> fallbackComplete(List<Message> messages, Throwable ex) {
        log.error("OpenAI complete response failed. Returning fallback response.", ex);
        return Mono.just(Constants.AI_UNAVAILABLE_DETAILED);
    }
}