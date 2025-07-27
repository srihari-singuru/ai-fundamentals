package com.srihari.ai.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI configuration for AI Fundamentals application.
 * Provides comprehensive API documentation with Swagger UI integration.
 */
@Configuration
public class OpenApiConfiguration {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.application.name:ai-fundamentals}")
    private String applicationName;

    /**
     * Configures OpenAPI specification for the AI Fundamentals application.
     * 
     * @return OpenAPI configuration with comprehensive metadata
     */
    @Bean
    public OpenAPI aiApplicationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Fundamentals API")
                        .description("""
                                Production-ready AI-powered chat application built with Spring Boot 3, Spring AI, and OpenAI GPT-4.
                                
                                ## Features
                                - Real-time streaming chat completions
                                - Conversation memory and persistence
                                - Comprehensive resilience patterns
                                - Production-ready monitoring and observability
                                
                                ## Authentication
                                API endpoints may require JWT authentication in production environments.
                                
                                ## Rate Limiting
                                API requests are subject to rate limiting to ensure fair usage and system stability.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI Fundamentals Team")
                                .email("support@ai-fundamentals.com")
                                .url("https://github.com/srihari-singuru/ai-fundamentals"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development server"),
                        new Server()
                                .url("https://api.ai-fundamentals.com")
                                .description("Production server")
                ));
    }
}