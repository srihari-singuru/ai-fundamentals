package com.srihari.ai.common;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * WebFlux filter that generates and propagates correlation IDs for request tracking.
 * Adds correlation ID to response headers and MDC for logging.
 */
@Component
public class CorrelationIdFilter implements WebFilter, Ordered {
    
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_KEY = "correlationId";
    
    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        // Get correlation ID from header or generate new one
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = CorrelationIdHolder.generateAndSetCorrelationId();
        } else {
            CorrelationIdHolder.setCorrelationId(correlationId);
        }
        
        // Add correlation ID to response header
        response.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        
        // Set MDC for logging
        MDC.put(CORRELATION_ID_KEY, correlationId);
        
        return chain.filter(exchange)
                .contextWrite(Context.of(CORRELATION_ID_KEY, correlationId))
                .doFinally(signalType -> {
                    // Clean up thread-local storage and MDC
                    CorrelationIdHolder.clear();
                    MDC.clear();
                });
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}