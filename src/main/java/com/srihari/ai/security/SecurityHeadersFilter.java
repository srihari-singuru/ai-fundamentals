package com.srihari.ai.security;

import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Web filter that adds comprehensive security headers to all HTTP responses.
 * Implements OWASP recommended security headers for production deployments.
 */
@Component
@RequiredArgsConstructor
public class SecurityHeadersFilter implements WebFilter, Ordered {

    private final StructuredLogger structuredLogger;

    // Security header constants
    private static final String CONTENT_SECURITY_POLICY = 
        "default-src 'self'; " +
        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
        "style-src 'self' 'unsafe-inline'; " +
        "img-src 'self' data: https:; " +
        "font-src 'self'; " +
        "connect-src 'self'; " +
        "frame-ancestors 'none'; " +
        "base-uri 'self'; " +
        "form-action 'self'";

    private static final String STRICT_TRANSPORT_SECURITY = "max-age=31536000; includeSubDomains; preload";
    private static final String X_CONTENT_TYPE_OPTIONS = "nosniff";
    private static final String X_FRAME_OPTIONS = "DENY";
    private static final String X_XSS_PROTECTION = "1; mode=block";
    private static final String REFERRER_POLICY = "strict-origin-when-cross-origin";
    private static final String PERMISSIONS_POLICY = 
        "geolocation=(), microphone=(), camera=(), payment=(), usb=(), " +
        "gyroscope=(), magnetometer=(), ambient-light-sensor=()";

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        structuredLogger.debug("Applying security headers", Map.of(
            "operation", "security_headers_filter",
            "path", exchange.getRequest().getPath().value(),
            "method", exchange.getRequest().getMethod().name(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));

        // Add security headers before processing the request
        try {
            addSecurityHeaders(exchange);
        } catch (Exception e) {
            structuredLogger.error("Error adding security headers", Map.of(
                "operation", "security_headers_filter_error",
                "path", exchange.getRequest().getPath().value(),
                "method", exchange.getRequest().getMethod().name(),
                "correlationId", correlationId != null ? correlationId : "unknown",
                "errorType", e.getClass().getSimpleName()
            ), e);
        }

        return chain.filter(exchange)
            .doOnError(error -> {
                structuredLogger.error("Error in security headers filter chain", Map.of(
                    "operation", "security_headers_filter_chain_error",
                    "path", exchange.getRequest().getPath().value(),
                    "method", exchange.getRequest().getMethod().name(),
                    "correlationId", correlationId != null ? correlationId : "unknown",
                    "errorType", error.getClass().getSimpleName()
                ), error);
            });
    }

    private void addSecurityHeaders(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        
        try {
            // Content Security Policy
            safeAddHeader(headers, "Content-Security-Policy", CONTENT_SECURITY_POLICY);
            
            // HTTP Strict Transport Security (HSTS)
            if (isSensitiveEndpoint(exchange)) {
                safeAddHeader(headers, "Strict-Transport-Security", STRICT_TRANSPORT_SECURITY);
            }
            
            // Cache Control for sensitive endpoints
            if (isSensitiveEndpoint(exchange)) {
                safeAddHeader(headers, "Cache-Control", "no-cache, no-store, must-revalidate");
                safeAddHeader(headers, "Pragma", "no-cache");
                safeAddHeader(headers, "Expires", "0");
            }
            
            // X-Content-Type-Options
            safeAddHeader(headers, "X-Content-Type-Options", X_CONTENT_TYPE_OPTIONS);
            
            // X-Frame-Options
            safeAddHeader(headers, "X-Frame-Options", X_FRAME_OPTIONS);
            
            // X-XSS-Protection
            safeAddHeader(headers, "X-XSS-Protection", X_XSS_PROTECTION);
            
            // Referrer Policy
            safeAddHeader(headers, "Referrer-Policy", REFERRER_POLICY);
            
            // Permissions Policy (formerly Feature Policy)
            safeAddHeader(headers, "Permissions-Policy", PERMISSIONS_POLICY);
            
            // Remove server information (safely)
            safeRemoveHeader(headers, "Server");
        } catch (Exception e) {
            String correlationId = CorrelationIdHolder.getCorrelationId();
            structuredLogger.warn("Could not add all security headers", Map.of(
                "operation", "security_headers_add_error",
                "path", exchange.getRequest().getPath().value(),
                "correlationId", correlationId != null ? correlationId : "unknown",
                "errorType", e.getClass().getSimpleName()
            ));
        }
    }
    
    private void safeAddHeader(HttpHeaders headers, String name, String value) {
        try {
            if (!headers.containsKey(name)) {
                headers.add(name, value);
            }
        } catch (UnsupportedOperationException e) {
            // Headers are read-only, skip silently
        }
    }
    
    private void safeRemoveHeader(HttpHeaders headers, String name) {
        try {
            headers.remove(name);
        } catch (UnsupportedOperationException e) {
            // Headers are read-only, skip silently
        }
    }

    private boolean isSensitiveEndpoint(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        return path.startsWith("/v1/") || 
               path.startsWith("/api/") || 
               path.contains("chat") ||
               path.contains("admin");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}