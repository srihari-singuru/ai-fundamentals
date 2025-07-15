package com.srihari.ai.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class PromptService {

    private final ChatClient client;

    public PromptService(ChatClient.Builder chatClientBuilder) {
        this.client = chatClientBuilder.build();
    }

    @CircuitBreaker(name = "openai", fallbackMethod = "fallback")
    public Mono<String> getAssistantReply(List<Message> messages) {
        return client.prompt(new Prompt(messages))
                .stream()
                .content()
                .collectList()
                .map(tokens -> String.join("", tokens));
    }

    private Mono<String> fallback(List<Message> messages, Throwable ex) {
        log.error("OpenAI prompt failed. Returning fallback response.", ex);
        return Mono.just("[OpenAI service is temporarily unavailable. Please try again later.]");
    }
}