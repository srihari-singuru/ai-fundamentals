package com.srihari.ai.controller;

import io.netty.handler.timeout.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/v1")
@Slf4j
public class ChatCompletionController {
    private final ChatClient chatClient;

    public ChatCompletionController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping(value = "/ai-chat")     // produces = "text/event-stream" for SSE
    public Flux<String> getResponse(@RequestParam(name = "user-message") String userMessage) {
        return chatClient.prompt(new Prompt(new UserMessage(userMessage)))
                .stream()
                .content()
                .doOnNext(response -> logPromptAndResponse(userMessage, response))
                .retryWhen(customRetrySpec())
                .onErrorResume(ex -> Flux.just("Exception: " + extractErrorMessage(ex)));
    }

    private void logPromptAndResponse(String userMessage, String response) {
        log.info("=== AI Request ===");
        log.info("Prompt: " + userMessage);
        log.info("Response: " + response);
        log.info("==================");
    }

    private RetryBackoffSpec customRetrySpec() {
        return Retry.backoff(3, Duration.ofSeconds(2))
                .jitter(0.5)
                .filter(this::isRetriableException)
                .doBeforeRetry(retrySignal -> log.warn("Retrying due to: {}", retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("Retries exhausted after {} attempts", retrySignal.totalRetries());
                    return retrySignal.failure();
                });
    }

    private boolean isRetriableException(Throwable throwable) {
        if (throwable instanceof IOException || throwable instanceof TimeoutException) {
            return true;
        }

        if (throwable instanceof WebClientResponseException responseException) {
            int statusCode = responseException.getStatusCode().value();
            return statusCode == 429 || statusCode == 401    // Too Many Requests
                    || (statusCode >= 500 && statusCode < 600); // Retry on server errors
        }

        return false;
    }

    private String extractErrorMessage(Throwable ex) {
        log.error("Error occurred while processing chat completion request", ex);
        return ex.getMessage();
    }
}
