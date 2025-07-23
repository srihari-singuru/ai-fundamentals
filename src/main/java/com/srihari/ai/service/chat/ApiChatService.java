package com.srihari.ai.service.chat;

import com.srihari.ai.service.integration.OpenAiChatClient;

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
public class ApiChatService {

    private final OpenAiChatClient chatClient;

    public Flux<String> generateResponse(String userMessage) {
        return chatClient.sendStream(userMessage)
                .doOnNext(token -> log.debug("Token received for user message: {}", userMessage))
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(2))
                                .jitter(0.4)
                                .filter(this::isRetryable)
                                .onRetryExhaustedThrow((spec, signal) -> signal.failure())
                );
    }

    private boolean isRetryable(Throwable ex) {
        return ex instanceof IOException || ex.getMessage().contains("429");
    }
}