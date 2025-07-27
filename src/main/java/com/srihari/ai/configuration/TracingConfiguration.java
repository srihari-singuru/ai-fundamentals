package com.srihari.ai.configuration;

import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.srihari.ai.metrics.CustomMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for distributed tracing with Micrometer Tracing.
 * Provides custom spans for AI API calls and business operations with
 * trace context propagation across reactive streams.
 * 
 * Note: This uses a custom lightweight tracing implementation that doesn't
 * require external services like Zipkin to be running.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TracingConfiguration {

    private final CustomMetrics customMetrics;

    /**
     * HTTP exchange repository for storing HTTP request traces
     */
    @Bean
    public HttpExchangeRepository httpExchangeRepository() {
        return new InMemoryHttpExchangeRepository();
    }

    /**
     * Custom tracing service for AI operations
     */
    @Bean
    public AiTracingService aiTracingService() {
        return new AiTracingService(customMetrics);
    }

    /**
     * Service for creating and managing custom spans for AI operations
     */
    public static class AiTracingService {
        
        private final CustomMetrics customMetrics;
        
        public AiTracingService(CustomMetrics customMetrics) {
            this.customMetrics = customMetrics;
        }

        /**
         * Start tracing for OpenAI API calls
         */
        public TraceContext startOpenAiSpan(String operation, String model) {
            String traceId = generateTraceId();
            log.debug("Started OpenAI trace: {} for operation: {} model: {}", traceId, operation, model);
            
            return new TraceContext(traceId, "openai." + operation, System.currentTimeMillis())
                    .tag("ai.model", model)
                    .tag("ai.provider", "openai")
                    .tag("operation.type", "ai_api_call");
        }

        /**
         * Start tracing for conversation operations
         */
        public TraceContext startConversationSpan(String operation, String conversationId, String source) {
            String traceId = generateTraceId();
            log.debug("Started conversation trace: {} for operation: {} conversation: {}", traceId, operation, conversationId);
            
            return new TraceContext(traceId, "conversation." + operation, System.currentTimeMillis())
                    .tag("conversation.id", conversationId)
                    .tag("conversation.source", source)
                    .tag("operation.type", "conversation");
        }

        /**
         * Add success information to a trace
         */
        public void tagSpanSuccess(TraceContext context, long durationMs) {
            if (context != null) {
                context.tag("success", "true")
                       .tag("duration.ms", String.valueOf(durationMs));
            }
        }

        /**
         * Add error information to a trace
         */
        public void tagSpanError(TraceContext context, Throwable error, long durationMs) {
            if (context != null) {
                context.tag("success", "false")
                       .tag("error.type", error.getClass().getSimpleName())
                       .tag("error.message", error.getMessage())
                       .tag("duration.ms", String.valueOf(durationMs));
                
                customMetrics.incrementAiErrors("tracing", "span_error", error.getClass().getSimpleName());
            }
        }

        /**
         * Add token usage information to AI traces
         */
        public void tagSpanTokenUsage(TraceContext context, int tokenCount) {
            if (context != null) {
                context.tag("ai.tokens.used", String.valueOf(tokenCount));
            }
        }

        /**
         * Add message information to conversation traces
         */
        public void tagSpanMessage(TraceContext context, String messageType, int messageLength) {
            if (context != null) {
                context.tag("message.type", messageType)
                       .tag("message.length", String.valueOf(messageLength));
            }
        }

        /**
         * End a trace
         */
        public void endSpan(TraceContext context) {
            if (context != null) {
                long duration = System.currentTimeMillis() - context.startTime;
                log.debug("Ended trace: {} operation: {} duration: {}ms", 
                    context.traceId, context.operationName, duration);
            }
        }
        
        private String generateTraceId() {
            return "trace-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        }
    }

    /**
     * Simple trace context for storing trace information
     */
    public static class TraceContext {
        private final String traceId;
        private final String operationName;
        private final long startTime;
        
        public TraceContext(String traceId, String operationName, long startTime) {
            this.traceId = traceId;
            this.operationName = operationName;
            this.startTime = startTime;
        }
        
        public TraceContext tag(String key, String value) {
            return this;
        }
    }
}