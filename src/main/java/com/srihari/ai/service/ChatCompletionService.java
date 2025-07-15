package com.srihari.ai.service;

import com.srihari.ai.client.ChatClientWrapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatCompletionService {

    private static final String CIRCUIT_BREAKER_NAME = "chatCompletionCB";

    private final ChatClientWrapper chatClient;

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackResponse")
    public Flux<String> generateResponse(String userMessage) {
        return chatClient.send(userMessage)
                .doOnNext(token -> log.info("User: {}, Token: {}", userMessage, token))
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(2))
                                .jitter(0.4)
                                .filter(this::isRetryable)
                                .onRetryExhaustedThrow((spec, signal) -> signal.failure())
                );
    }

    public Flux<String> fallbackResponse(String userMessage, Throwable ex) {
        log.error("Fallback triggered for message: '{}', reason: {}", userMessage, ex.toString(), ex);
        return Flux.just("Sorry! AI is temporarily unavailable. Please try again.");
    }

    private boolean isRetryable(Throwable ex) {
        return ex instanceof IOException || ex.getMessage().contains("429");
    }
}
