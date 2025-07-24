package com.srihari.ai.security;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
        
        // Allow all origins for development
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedOrigins(Arrays.asList("*"));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials
        configuration.setAllowCredentials(false); // Set to false when allowing all origins
        
        // Cache preflight response
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}