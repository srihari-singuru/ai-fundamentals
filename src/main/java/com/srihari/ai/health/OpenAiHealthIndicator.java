package com.srihari.ai.health;

import java.time.Duration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiHealthIndicator implements ReactiveHealthIndicator {

    private final ChatClient.Builder chatClientBuilder;

    @Override
    public Mono<Health> health() {
        return checkOpenAiConnection()
                .map(this::buildHealthStatus)
                .onErrorResume(this::buildErrorStatus)
                .timeout(Duration.ofSeconds(10))
                .doOnError(ex -> log.warn("OpenAI health check failed", ex));
    }

    private Mono<String> checkOpenAiConnection() {
        ChatClient client = chatClientBuilder.build();
        return client.prompt(new Prompt(new UserMessage("ping")))
                .stream()
                .content()
                .take(1)
                .next()
                .map(response -> "connected");
    }

    private Health buildHealthStatus(String status) {
        return Health.up()
                .withDetail("status", status)
                .withDetail("service", "OpenAI")
                .withDetail("timestamp", System.currentTimeMillis())
                .build();
    }

    private Mono<Health> buildErrorStatus(Throwable ex) {
        return Mono.just(Health.down()
                .withDetail("status", "disconnected")
                .withDetail("service", "OpenAI")
                .withDetail("error", ex.getMessage())
                .withDetail("timestamp", System.currentTimeMillis())
                .build());
    }
}