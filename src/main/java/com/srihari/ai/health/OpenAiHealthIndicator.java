package com.srihari.ai.health;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;

import com.srihari.ai.metrics.CustomMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Enhanced OpenAI health indicator with API connectivity checks, response time monitoring,
 * and detailed status reporting for production monitoring.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiHealthIndicator implements ReactiveHealthIndicator {

    private final ChatClient.Builder chatClientBuilder;
    private final CustomMetrics customMetrics;
    
    // Track health check metrics
    private final AtomicLong lastSuccessfulCheck = new AtomicLong(0);
    private final AtomicLong lastFailedCheck = new AtomicLong(0);
    private final AtomicLong consecutiveFailures = new AtomicLong(0);
    private final AtomicReference<String> lastError = new AtomicReference<>("none");
    private final AtomicLong averageResponseTime = new AtomicLong(0);

    @Override
    public Mono<Health> health() {
        long startTime = System.currentTimeMillis();
        
        return checkOpenAiConnection()
                .map(response -> buildHealthStatus(response, startTime))
                .onErrorResume(ex -> buildErrorStatus(ex, startTime))
                .timeout(Duration.ofSeconds(15))
                .doOnError(ex -> log.warn("OpenAI health check failed", ex));
    }

    private Mono<String> checkOpenAiConnection() {
        ChatClient client = chatClientBuilder.build();
        
        // Use a simple health check message
        return client.prompt(new Prompt(new UserMessage("health")))
                .stream()
                .content()
                .take(1)
                .next()
                .map(response -> {
                    log.debug("OpenAI health check response received: {}", response.substring(0, Math.min(50, response.length())));
                    return response;
                })
                .switchIfEmpty(Mono.just("empty_response"));
    }

    private Health buildHealthStatus(String response, long startTime) {
        long responseTime = System.currentTimeMillis() - startTime;
        long now = System.currentTimeMillis();
        
        // Update success metrics
        lastSuccessfulCheck.set(now);
        consecutiveFailures.set(0);
        lastError.set("none");
        updateAverageResponseTime(responseTime);
        
        // Record metrics
        customMetrics.recordModelLatency("gpt-4.1-nano", "health_check", responseTime);
        
        return Health.up()
                .withDetail("status", "connected")
                .withDetail("service", "OpenAI GPT-4.1-nano")
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("averageResponseTime", averageResponseTime.get() + "ms")
                .withDetail("lastSuccessfulCheck", Instant.ofEpochMilli(lastSuccessfulCheck.get()).toString())
                .withDetail("consecutiveFailures", consecutiveFailures.get())
                .withDetail("responsePreview", response.substring(0, Math.min(100, response.length())))
                .withDetail("timestamp", Instant.ofEpochMilli(now).toString())
                .build();
    }

    private Mono<Health> buildErrorStatus(Throwable ex, long startTime) {
        long responseTime = System.currentTimeMillis() - startTime;
        long now = System.currentTimeMillis();
        
        // Update failure metrics
        lastFailedCheck.set(now);
        consecutiveFailures.incrementAndGet();
        lastError.set(ex.getMessage());
        
        // Record error metrics
        customMetrics.incrementAiErrors("gpt-4.1-nano", "health_check_error", ex.getClass().getSimpleName());
        
        Health.Builder healthBuilder = Health.down()
                .withDetail("status", "disconnected")
                .withDetail("service", "OpenAI GPT-4.1-nano")
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("averageResponseTime", averageResponseTime.get() + "ms")
                .withDetail("error", ex.getMessage())
                .withDetail("errorType", ex.getClass().getSimpleName())
                .withDetail("lastSuccessfulCheck", 
                    lastSuccessfulCheck.get() > 0 ? 
                        Instant.ofEpochMilli(lastSuccessfulCheck.get()).toString() : "never")
                .withDetail("consecutiveFailures", consecutiveFailures.get())
                .withDetail("timestamp", Instant.ofEpochMilli(now).toString());
        
        // Add circuit breaker information if available
        if (ex.getMessage() != null && ex.getMessage().contains("CircuitBreaker")) {
            healthBuilder.withDetail("circuitBreakerOpen", true);
        }
        
        return Mono.just(healthBuilder.build());
    }
    
    private void updateAverageResponseTime(long newResponseTime) {
        // Simple moving average calculation
        long currentAverage = averageResponseTime.get();
        if (currentAverage == 0) {
            averageResponseTime.set(newResponseTime);
        } else {
            // Weighted average: 80% old, 20% new
            long newAverage = (currentAverage * 4 + newResponseTime) / 5;
            averageResponseTime.set(newAverage);
        }
    }
}