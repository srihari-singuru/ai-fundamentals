package com.srihari.ai.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * JWT Authentication Filter for extracting and validating JWT tokens from requests.
 * Integrates with Spring Security's reactive authentication system.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtAuthenticationManager jwtAuthenticationManager;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Skip JWT authentication for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        String token = extractTokenFromRequest(exchange);
        
        if (token == null) {
            // No token provided, continue without authentication
            return chain.filter(exchange);
        }

        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(null, token);

        return jwtAuthenticationManager.authenticate(authToken)
            .cast(UsernamePasswordAuthenticationToken.class)
            .flatMap(authentication -> 
                chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
            )
            .switchIfEmpty(chain.filter(exchange))
            .doOnError(error -> log.error("JWT authentication error for path {}: {}", path, error.getMessage()));
    }

    private String extractTokenFromRequest(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (StringUtils.hasText(authHeader) && authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator/") ||
               path.startsWith("/chat") ||
               path.equals("/") ||
               path.equals("/index.html") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/static/") ||
               path.startsWith("/webjars/") ||
               path.equals("/error");
    }
}