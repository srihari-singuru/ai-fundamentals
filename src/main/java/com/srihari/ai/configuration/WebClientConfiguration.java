package com.srihari.ai.configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.srihari.ai.service.connection.ConnectionPoolManager;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Configuration for WebClient with timeout and bulkhead patterns.
 * Provides resilient HTTP client configuration for external service calls.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebClientConfiguration {
    
    private final ConnectionPoolManager connectionPoolManager;

    /**
     * Configuration properties for timeout settings.
     */
    @ConfigurationProperties(prefix = "app.webclient")
    public static class WebClientProperties {
        private Timeout timeout = new Timeout();
        private ConnectionPool connectionPool = new ConnectionPool();
        private Bulkhead bulkhead = new Bulkhead();

        public static class Timeout {
            private Duration connect = Duration.ofSeconds(5);
            private Duration read = Duration.ofSeconds(30);
            private Duration write = Duration.ofSeconds(10);
            private Duration response = Duration.ofSeconds(60);

            // Getters and setters
            public Duration getConnect() { return connect; }
            public void setConnect(Duration connect) { this.connect = connect; }
            public Duration getRead() { return read; }
            public void setRead(Duration read) { this.read = read; }
            public Duration getWrite() { return write; }
            public void setWrite(Duration write) { this.write = write; }
            public Duration getResponse() { return response; }
            public void setResponse(Duration response) { this.response = response; }
        }

        public static class ConnectionPool {
            private int maxConnections = 100;
            private int maxIdleTime = 30;
            private int maxLifeTime = 60;
            private Duration pendingAcquireTimeout = Duration.ofSeconds(10);

            // Getters and setters
            public int getMaxConnections() { return maxConnections; }
            public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
            public int getMaxIdleTime() { return maxIdleTime; }
            public void setMaxIdleTime(int maxIdleTime) { this.maxIdleTime = maxIdleTime; }
            public int getMaxLifeTime() { return maxLifeTime; }
            public void setMaxLifeTime(int maxLifeTime) { this.maxLifeTime = maxLifeTime; }
            public Duration getPendingAcquireTimeout() { return pendingAcquireTimeout; }
            public void setPendingAcquireTimeout(Duration pendingAcquireTimeout) { this.pendingAcquireTimeout = pendingAcquireTimeout; }
        }

        public static class Bulkhead {
            private int maxConcurrentCalls = 25;
            private Duration maxWaitDuration = Duration.ofSeconds(5);

            // Getters and setters
            public int getMaxConcurrentCalls() { return maxConcurrentCalls; }
            public void setMaxConcurrentCalls(int maxConcurrentCalls) { this.maxConcurrentCalls = maxConcurrentCalls; }
            public Duration getMaxWaitDuration() { return maxWaitDuration; }
            public void setMaxWaitDuration(Duration maxWaitDuration) { this.maxWaitDuration = maxWaitDuration; }
        }

        // Getters and setters
        public Timeout getTimeout() { return timeout; }
        public void setTimeout(Timeout timeout) { this.timeout = timeout; }
        public ConnectionPool getConnectionPool() { return connectionPool; }
        public void setConnectionPool(ConnectionPool connectionPool) { this.connectionPool = connectionPool; }
        public Bulkhead getBulkhead() { return bulkhead; }
        public void setBulkhead(Bulkhead bulkhead) { this.bulkhead = bulkhead; }
    }

    @Bean
    public WebClientProperties webClientProperties() {
        return new WebClientProperties();
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        return BulkheadRegistry.ofDefaults();
    }

    /**
     * Connection provider with optimized settings for external API calls.
     */
    @Bean
    public ConnectionProvider connectionProvider(WebClientProperties properties) {
        return connectionPoolManager.createOptimizedConnectionProvider(
            "ai-service-pool",
            properties.getConnectionPool().getMaxConnections(),
            Duration.ofSeconds(properties.getConnectionPool().getMaxIdleTime()),
            Duration.ofSeconds(properties.getConnectionPool().getMaxLifeTime())
        );
    }

    /**
     * HttpClient with timeout configurations and connection pooling.
     */
    @Bean
    public HttpClient httpClient(ConnectionProvider connectionProvider, WebClientProperties properties) {
        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getTimeout().getConnect().toMillis())
                .responseTimeout(properties.getTimeout().getResponse())
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(properties.getTimeout().getRead().toSeconds(), TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(properties.getTimeout().getWrite().toSeconds(), TimeUnit.SECONDS));
                })
                .doOnRequest((request, connection) -> {
                    log.debug("Initiating HTTP request to: {} {}", request.method(), request.uri());
                })
                .doOnResponse((response, connection) -> {
                    log.debug("Received HTTP response: {} from {}", response.status(), response.uri());
                })
                .doOnConnected(connection -> {
                    connectionPoolManager.recordConnectionAcquired("ai-service-pool");
                })
                .doOnDisconnected(connection -> {
                    connectionPoolManager.recordConnectionReleased("ai-service-pool");
                })
                .doOnError((request, throwable) -> {
                    log.error("HTTP request failed for: {} {}, error: {}", 
                            request.method(), request.uri(), throwable.getMessage());
                    connectionPoolManager.recordConnectionAcquisitionFailure("ai-service-pool", throwable.getMessage());
                }, (response, throwable) -> {
                    log.error("HTTP response failed for: {}, error: {}", 
                            response.uri(), throwable.getMessage());
                });
    }

    /**
     * WebClient with timeout and resilience configurations.
     */
    @Bean
    public WebClient webClient(HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    // Increase buffer size for streaming responses
                    configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB
                })
                .build();
    }

    /**
     * Specialized WebClient for OpenAI API calls with streaming support.
     */
    @Bean("openAiWebClient")
    public WebClient openAiWebClient(HttpClient httpClient, WebClientProperties properties) {
        // Create specialized HttpClient for OpenAI with longer timeouts for streaming
        HttpClient openAiHttpClient = httpClient.responseTimeout(Duration.ofMinutes(5)) // Longer timeout for streaming
                .doOnConnected(conn -> {
                    // Longer read timeout for streaming responses
                    conn.addHandlerLast(new ReadTimeoutHandler(300, TimeUnit.SECONDS)); // 5 minutes
                    conn.addHandlerLast(new WriteTimeoutHandler(properties.getTimeout().getWrite().toSeconds(), TimeUnit.SECONDS));
                });

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(openAiHttpClient))
                .codecs(configurer -> {
                    // Larger buffer for AI responses
                    configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024); // 5MB
                })
                .build();
    }

    /**
     * WebClient for general external API calls with standard timeouts.
     */
    @Bean("externalApiWebClient")
    public WebClient externalApiWebClient(HttpClient httpClient) {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(512 * 1024); // 512KB
                })
                .build();
    }
}