package com.srihari.ai.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Autowired
    private WebFilter rateLimitingFilter;
    
    @Autowired
    private SecurityHeadersFilter securityHeadersFilter;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .addFilterBefore(securityHeadersFilter, SecurityWebFiltersOrder.CORS)
                .addFilterBefore(rateLimitingFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHORIZATION)
                .authorizeExchange(exchanges -> exchanges
                        // Actuator endpoints
                        .pathMatchers("/actuator/**").permitAll()
                        
                        // REST API endpoints
                        .pathMatchers("/v1/**").permitAll()
                        .pathMatchers("/api/**").permitAll()
                        
                        // Web UI endpoints
                        .pathMatchers("/chat/**").permitAll()
                        .pathMatchers("/chat").permitAll()
                        
                        // Static resources
                        .pathMatchers("/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                        .pathMatchers("/webjars/**").permitAll()
                        .pathMatchers("/favicon.ico").permitAll()
                        
                        // Root and common paths
                        .pathMatchers("/", "/index.html", "/home").permitAll()
                        .pathMatchers("/error").permitAll()
                        
                        // Allow all other requests (development mode)
                        .anyExchange().permitAll()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Production-ready CORS configuration
        // Allow specific origins (configure via environment variables in production)
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "https://localhost:*",
            "https://*.yourdomain.com" // Replace with actual domain
        ));
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        
        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Correlation-ID"
        ));
        
        // Expose specific headers
        configuration.setExposedHeaders(Arrays.asList(
            "X-Correlation-ID",
            "X-Total-Count",
            "X-Rate-Limit-Remaining"
        ));
        
        // Allow credentials for authenticated requests
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}