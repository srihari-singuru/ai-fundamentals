package com.srihari.ai.metrics;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMetrics {

    private final MeterRegistry meterRegistry;

    // Counters
    public void incrementApiRequests() {
        Counter.builder("chat.api.requests.total")
                .description("Total number of API chat requests")
                .register(meterRegistry)
                .increment();
    }

    public void incrementWebRequests() {
        Counter.builder("chat.web.requests.total")
                .description("Total number of web chat requests")
                .register(meterRegistry)
                .increment();
    }

    public void incrementOpenAiCalls() {
        Counter.builder("openai.api.calls.total")
                .description("Total number of OpenAI API calls")
                .register(meterRegistry)
                .increment();
    }

    public void incrementErrors(String errorType) {
        Counter.builder("chat.errors.total")
                .description("Total number of chat errors")
                .tag("error.type", errorType)
                .register(meterRegistry)
                .increment();
    }

    public void incrementCircuitBreakerEvents(String circuitBreaker, String event) {
        Counter.builder("circuit.breaker.events.total")
                .description("Circuit breaker events")
                .tag("circuit.breaker", circuitBreaker)
                .tag("event", event)
                .register(meterRegistry)
                .increment();
    }

    // Timers
    public Timer.Sample startApiTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordApiDuration(Timer.Sample sample) {
        sample.stop(Timer.builder("chat.api.duration")
                .description("API chat request duration")
                .register(meterRegistry));
    }

    public Timer.Sample startOpenAiTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordOpenAiDuration(Timer.Sample sample) {
        sample.stop(Timer.builder("openai.api.duration")
                .description("OpenAI API call duration")
                .register(meterRegistry));
    }

    // Gauges
    public void recordActiveConnections(int count) {
        meterRegistry.gauge("chat.connections.active", count);
    }

    public void recordMemoryUsage(String conversationId, int messageCount) {
        meterRegistry.gauge("chat.memory.messages", 
                io.micrometer.core.instrument.Tags.of("conversation.id", conversationId), 
                messageCount);
    }
}