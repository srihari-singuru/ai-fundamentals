package com.srihari.ai.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Configuration for rate limiting using Resilience4j.
 * Provides different rate limits for API endpoints vs web endpoints.
 */
@Configuration
@Slf4j
public class RateLimitingConfig {

    /**
     * Configuration for API rate limiting - more restrictive
     */
    @Bean
    public RateLimiter apiRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(50) // 50 requests per period
                .limitRefreshPeriod(Duration.ofMinutes(1)) // 1 minute window
                .timeoutDuration(Duration.ofSeconds(5)) // Wait up to 5 seconds for permission
                .build();
        
        return RateLimiter.of("api-rate-limiter", config);
    }

    /**
     * Configuration for web UI rate limiting - more lenient
     */
    @Bean
    public RateLimiter webRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100) // 100 requests per period
                .limitRefreshPeriod(Duration.ofMinutes(1)) // 1 minute window
                .timeoutDuration(Duration.ofSeconds(2)) // Wait up to 2 seconds for permission
                .build();
        
        return RateLimiter.of("web-rate-limiter", config);
    }

    /**
     * Configuration for external service calls (OpenAI) - conservative
     */
    @Bean
    public RateLimiter openAiRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(30) // 30 requests per period
                .limitRefreshPeriod(Duration.ofMinutes(1)) // 1 minute window
                .timeoutDuration(Duration.ofSeconds(10)) // Wait up to 10 seconds for permission
                .build();
        
        return RateLimiter.of("openai-rate-limiter", config);
    }

    /**
     * Rate limiter registry for managing multiple rate limiters
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    /**
     * Web filter for applying rate limiting to incoming requests
     */
    @Bean
    public WebFilter rateLimitingFilter() {
        return new RateLimitingWebFilter();
    }

    /**
     * WebFilter implementation that applies rate limiting based on request path
     */
    private class RateLimitingWebFilter implements WebFilter {
        private final Map<String, RateLimiter> clientRateLimiters = new ConcurrentHashMap<>();

        @Override
        @NonNull
        public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
            String path = exchange.getRequest().getPath().value();
            String clientId = getClientId(exchange);
            
            // Get or create client-specific rate limiter
            RateLimiter rateLimiter = getClientRateLimiter(clientId, path);
            
            // Apply rate limiting with intelligent backoff
            return Mono.fromCallable(() -> rateLimiter.acquirePermission())
                    .flatMap(permitted -> {
                        if (permitted) {
                            log.debug("Rate limit check passed for client: {} on path: {}", clientId, path);
                            return chain.filter(exchange);
                        } else {
                            log.warn("Rate limit exceeded for client: {} on path: {}", clientId, path);
                            return handleRateLimitExceeded(exchange, rateLimiter);
                        }
                    })
                    .onErrorResume(throwable -> {
                        log.error("Error during rate limiting for client: {} on path: {}", clientId, path, throwable);
                        // In case of error, allow the request to proceed
                        return chain.filter(exchange);
                    });
        }

        private String getClientId(ServerWebExchange exchange) {
            // Use X-Forwarded-For header if available, otherwise use remote address
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                return forwardedFor.split(",")[0].trim();
            }
            
            return exchange.getRequest().getRemoteAddress() != null 
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
        }

        private RateLimiter getClientRateLimiter(String clientId, String path) {
            String key = clientId + ":" + (path.startsWith("/v1/") || path.startsWith("/api/") ? "api" : "web");
            return clientRateLimiters.computeIfAbsent(key, k -> determineRateLimiter(path));
        }

        private RateLimiter determineRateLimiter(String path) {
            if (path.startsWith("/v1/") || path.startsWith("/api/")) {
                return apiRateLimiter();
            } else {
                return webRateLimiter();
            }
        }

        private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, RateLimiter rateLimiter) {
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            
            // Intelligent backoff - calculate retry after based on rate limiter config
            long retryAfterSeconds = rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod().getSeconds();
            exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(retryAfterSeconds));
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", 
                String.valueOf(rateLimiter.getRateLimiterConfig().getLimitForPeriod()));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            
            String errorResponse = String.format("""
                {
                    "error": "Rate limit exceeded",
                    "message": "Too many requests. Please try again later.",
                    "retryAfter": %d,
                    "limit": %d
                }
                """, retryAfterSeconds, rateLimiter.getRateLimiterConfig().getLimitForPeriod());
            
            org.springframework.core.io.buffer.DataBuffer buffer = 
                    exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes());
            
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }
}