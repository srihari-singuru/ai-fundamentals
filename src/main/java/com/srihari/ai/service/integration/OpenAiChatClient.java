package com.srihari.ai.service.integration;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import com.srihari.ai.common.Constants;
import com.srihari.ai.configuration.TracingConfiguration.AiTracingService;
import com.srihari.ai.configuration.TracingConfiguration.TraceContext;
import com.srihari.ai.metrics.CustomMetrics;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OpenAiChatClient {

    private final ChatClient chatClient;
    private final CustomMetrics customMetrics;
    private final AiTracingService tracingService;

    public OpenAiChatClient(ChatClient.Builder chatClientBuilder, CustomMetrics customMetrics, AiTracingService tracingService) {
        this.chatClient = chatClientBuilder.build();
        this.customMetrics = customMetrics;
        this.tracingService = tracingService;
    }

    /**
     * Send a single message and get streaming response (for API)
     */
    @CircuitBreaker(name = Constants.OPENAI_CIRCUIT_BREAKER, fallbackMethod = "fallbackStream")
    @RateLimiter(name = "openai", fallbackMethod = "fallbackStreamRateLimit")
    @Bulkhead(name = "ai-service", fallbackMethod = "fallbackStreamBulkhead")
    @TimeLimiter(name = "ai-service", fallbackMethod = "fallbackStreamTimeout")
    public Flux<String> sendStream(String message) {
        Timer.Sample sample = customMetrics.startOpenAiTimer();
        long startTime = System.currentTimeMillis();
        
        // Start tracing span
        TraceContext span = tracingService.startOpenAiSpan("stream", "gpt-4.1-nano");
        tracingService.tagSpanMessage(span, "user", message.length());
        
        return chatClient.prompt(new Prompt(new UserMessage(message)))
                .stream()
                .content()
                .doOnNext(token -> {
                    // Record token usage (approximate - each token is roughly 1 unit)
                    customMetrics.recordTokenUsage("gpt-4.1-nano", "stream", 1);
                    tracingService.tagSpanTokenUsage(span, 1);
                })
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    customMetrics.recordOpenAiDuration(sample, "gpt-4.1-nano", "stream");
                    customMetrics.recordModelLatency("gpt-4.1-nano", "stream", duration);
                    customMetrics.incrementMessageCount("api", "assistant");
                    
                    // Complete tracing span
                    tracingService.tagSpanSuccess(span, duration);
                    tracingService.endSpan(span);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    customMetrics.incrementAiErrors("gpt-4.1-nano", "openai_error", error.getClass().getSimpleName());
                    customMetrics.recordOpenAiDuration(sample, "gpt-4.1-nano", "stream");
                    
                    // Complete tracing span with error
                    tracingService.tagSpanError(span, error, duration);
                    tracingService.endSpan(span);
                });
    }

    /**
     * Send conversation messages and get complete response (for Web UI)
     */
    @CircuitBreaker(name = Constants.OPENAI_CIRCUIT_BREAKER, fallbackMethod = "fallbackComplete")
    @RateLimiter(name = "openai", fallbackMethod = "fallbackCompleteRateLimit")
    @Bulkhead(name = "ai-service", fallbackMethod = "fallbackCompleteBulkhead")
    @TimeLimiter(name = "ai-service", fallbackMethod = "fallbackCompleteTimeout")
    public Mono<String> sendComplete(List<Message> messages) {
        Timer.Sample sample = customMetrics.startOpenAiTimer();
        long startTime = System.currentTimeMillis();
        
        // Start tracing span
        TraceContext span = tracingService.startOpenAiSpan("complete", "gpt-4.1-nano");
        tracingService.tagSpanMessage(span, "conversation", messages.size());
        
        return chatClient.prompt(new Prompt(messages))
                .stream()
                .content()
                .collectList()
                .map(tokens -> {
                    String response = String.join("", tokens);
                    // Record token usage (approximate - response length / 4 for token count)
                    int estimatedTokens = Math.max(1, response.length() / 4);
                    customMetrics.recordTokenUsage("gpt-4.1-nano", "complete", estimatedTokens);
                    tracingService.tagSpanTokenUsage(span, estimatedTokens);
                    return response;
                })
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    customMetrics.recordOpenAiDuration(sample, "gpt-4.1-nano", "complete");
                    customMetrics.recordModelLatency("gpt-4.1-nano", "complete", duration);
                    customMetrics.incrementMessageCount("web", "assistant");
                    
                    // Complete tracing span
                    tracingService.tagSpanSuccess(span, duration);
                    tracingService.endSpan(span);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    customMetrics.incrementAiErrors("gpt-4.1-nano", "openai_error", error.getClass().getSimpleName());
                    customMetrics.recordOpenAiDuration(sample, "gpt-4.1-nano", "complete");
                    
                    // Complete tracing span with error
                    tracingService.tagSpanError(span, error, duration);
                    tracingService.endSpan(span);
                });
    }

    // Fallback methods for circuit breaker
    public Flux<String> fallbackStream(String message, Throwable ex) {
        log.error("OpenAI streaming failed for message: '{}', reason: {}", message, ex.getMessage(), ex);
        
        // Create fallback span
        TraceContext span = tracingService.startOpenAiSpan("stream_fallback", "gpt-4.1-nano");
        tracingService.tagSpanError(span, ex, 0);
        tracingService.endSpan(span);
        
        return Flux.just(Constants.AI_UNAVAILABLE_MESSAGE);
    }

    public Mono<String> fallbackComplete(List<Message> messages, Throwable ex) {
        log.error("OpenAI complete response failed. Returning fallback response.", ex);
        
        // Create fallback span
        TraceContext span = tracingService.startOpenAiSpan("complete_fallback", "gpt-4.1-nano");
        tracingService.tagSpanError(span, ex, 0);
        tracingService.endSpan(span);
        
        return Mono.just(Constants.AI_UNAVAILABLE_DETAILED);
    }

    // Fallback methods for rate limiting
    public Flux<String> fallbackStreamRateLimit(String message, Throwable ex) {
        log.warn("OpenAI rate limit exceeded for streaming message: '{}'", message);
        customMetrics.incrementAiErrors("gpt-4.1-nano", "rate_limit", "RateLimitExceededException");
        
        // Create rate limit span
        TraceContext span = tracingService.startOpenAiSpan("stream_rate_limit", "gpt-4.1-nano");
        tracingService.tagSpanError(span, ex, 0);
        tracingService.endSpan(span);
        
        return Flux.just("I'm currently experiencing high demand. Please try again in a moment.");
    }

    public Mono<String> fallbackCompleteRateLimit(List<Message> messages, Throwable ex) {
        log.warn("OpenAI rate limit exceeded for complete response");
        customMetrics.incrementAiErrors("gpt-4.1-nano", "rate_limit", "RateLimitExceededException");
        
        // Create rate limit span
        TraceContext span = tracingService.startOpenAiSpan("complete_rate_limit", "gpt-4.1-nano");
        tracingService.tagSpanError(span, ex, 0);
        tracingService.endSpan(span);
        
        return Mono.just("I'm currently experiencing high demand. Please try again in a moment.");
    }

    // Fallback methods for bulkhead
    public Flux<String> fallbackStreamBulkhead(String message, Throwable ex) {
        log.warn("AI service bulkhead limit exceeded for streaming message: '{}'", message);
        customMetrics.incrementAiErrors("gpt-4.1-nano", "bulkhead", "BulkheadFullException");
        
        // Create bulkhead span
        TraceContext span = tracingService.startOpenAiSpan("stream_bulkhead", "gpt-4.1-nano");
        tracingService.tagSpanError(span, ex, 0);
        tracingService.endSpan(span);
        
        return Flux.just("The AI service is currently at capacity. Please try again shortly.");
    }

    public Mono<String> fallbackCompleteBulkhead(List<Message> messages, Throwable ex) {
        log.warn("AI service bulkhead limit exceeded for complete response");
        customMetrics.incrementAiErrors("gpt-4.1-nano", "bulkhead", "BulkheadFullException");
        
        // Create bulkhead span
        TraceContext span = tracingService.startOpenAiSpan("complete_bulkhead", "gpt-4.1-nano");
        tracingService.tagSpanError(span, ex, 0);
        tracingService.endSpan(span);
        
        return Mono.just("The AI service is currently at capacity. Please try again shortly.");
    }

    // Fallback methods for timeout
    public Flux<String> fallbackStreamTimeout(String message, Throwable ex) {
        log.warn("AI service timeout for streaming message: '{}'", message);
        customMetrics.incrementAiErrors("gpt-4.1-nano", "timeout", "TimeoutException");
        
        // Create timeout span
        TraceContext span = tracingService.startOpenAiSpan("stream_timeout", "gpt-4.1-nano");
        tracingService.tagSpanError(span, ex, 0);
        tracingService.endSpan(span);
        
        return Flux.just("The request took too long to process. Please try again with a shorter message.");
    }

    public Mono<String> fallbackCompleteTimeout(List<Message> messages, Throwable ex) {
        log.warn("AI service timeout for complete response");
        customMetrics.incrementAiErrors("gpt-4.1-nano", "timeout", "TimeoutException");
        
        // Create timeout span
        TraceContext span = tracingService.startOpenAiSpan("complete_timeout", "gpt-4.1-nano");
        tracingService.tagSpanError(span, ex, 0);
        tracingService.endSpan(span);
        
        return Mono.just("The request took too long to process. Please try again with a shorter conversation.");
    }
}