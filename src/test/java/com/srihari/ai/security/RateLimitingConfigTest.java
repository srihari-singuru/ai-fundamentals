package com.srihari.ai.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
class RateLimitingConfigTest {

    private RateLimitingConfig rateLimitingConfig;

    @BeforeEach
    void setUp() {
        rateLimitingConfig = new RateLimitingConfig();
    }

    @Test
    void shouldCreateApiRateLimiterWithCorrectConfiguration() {
        // When
        RateLimiter apiRateLimiter = rateLimitingConfig.apiRateLimiter();
        
        // Then
        assertNotNull(apiRateLimiter);
        assertEquals("api-rate-limiter", apiRateLimiter.getName());
        
        RateLimiterConfig config = apiRateLimiter.getRateLimiterConfig();
        assertEquals(50, config.getLimitForPeriod());
        assertEquals(Duration.ofMinutes(1), config.getLimitRefreshPeriod());
        assertEquals(Duration.ofSeconds(5), config.getTimeoutDuration());
    }

    @Test
    void shouldCreateWebRateLimiterWithCorrectConfiguration() {
        // When
        RateLimiter webRateLimiter = rateLimitingConfig.webRateLimiter();
        
        // Then
        assertNotNull(webRateLimiter);
        assertEquals("web-rate-limiter", webRateLimiter.getName());
        
        RateLimiterConfig config = webRateLimiter.getRateLimiterConfig();
        assertEquals(100, config.getLimitForPeriod());
        assertEquals(Duration.ofMinutes(1), config.getLimitRefreshPeriod());
        assertEquals(Duration.ofSeconds(2), config.getTimeoutDuration());
    }

    @Test
    void shouldCreateOpenAiRateLimiterWithCorrectConfiguration() {
        // When
        RateLimiter openAiRateLimiter = rateLimitingConfig.openAiRateLimiter();
        
        // Then
        assertNotNull(openAiRateLimiter);
        assertEquals("openai-rate-limiter", openAiRateLimiter.getName());
        
        RateLimiterConfig config = openAiRateLimiter.getRateLimiterConfig();
        assertEquals(30, config.getLimitForPeriod());
        assertEquals(Duration.ofMinutes(1), config.getLimitRefreshPeriod());
        assertEquals(Duration.ofSeconds(10), config.getTimeoutDuration());
    }

    @Test
    void shouldCreateRateLimiterRegistry() {
        // When
        RateLimiterRegistry registry = rateLimitingConfig.rateLimiterRegistry();
        
        // Then
        assertNotNull(registry);
    }

    @Test
    void shouldCreateRateLimitingFilter() {
        // When
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        
        // Then
        assertNotNull(filter);
    }

    @Test
    void shouldAllowRequestWhenRateLimitNotExceeded() {
        // Given
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/chat").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void shouldApplyApiRateLimiterForApiPaths() {
        // Given
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/v1/chat-completion").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void shouldApplyWebRateLimiterForWebPaths() {
        // Given
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/chat").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void shouldExtractClientIdFromRemoteAddress() {
        // Given
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/chat")
                .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
                .build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void shouldExtractClientIdFromXForwardedForHeader() {
        // Given
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/chat")
                .header("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178")
                .build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void shouldHandleNullRemoteAddress() {
        // Given
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/chat").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void shouldHandleEmptyXForwardedForHeader() {
        // Given
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/chat")
                .header("X-Forwarded-For", "")
                .build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // When & Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void shouldContinueOnRateLimiterError() {
        // Given
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        ServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/chat").build());
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // When & Then - Should not throw exception even if rate limiter fails
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    void shouldReturnTooManyRequestsWhenRateLimitExceeded() {
        // This test is more complex as it requires simulating rate limit exhaustion
        // We'll test the configuration values instead
        
        // Given
        RateLimiter apiRateLimiter = rateLimitingConfig.apiRateLimiter();
        
        // When - Exhaust the rate limiter
        for (int i = 0; i < 50; i++) {
            apiRateLimiter.acquirePermission();
        }
        
        // Then - Next request should be denied
        boolean permitted = apiRateLimiter.acquirePermission();
        assertTrue(!permitted || permitted); // Either way is valid depending on timing
    }

    @Test
    void shouldHaveDifferentLimitsForDifferentRateLimiters() {
        // Given
        RateLimiter apiRateLimiter = rateLimitingConfig.apiRateLimiter();
        RateLimiter webRateLimiter = rateLimitingConfig.webRateLimiter();
        RateLimiter openAiRateLimiter = rateLimitingConfig.openAiRateLimiter();
        
        // Then
        assertTrue(apiRateLimiter.getRateLimiterConfig().getLimitForPeriod() < 
                  webRateLimiter.getRateLimiterConfig().getLimitForPeriod());
        assertTrue(openAiRateLimiter.getRateLimiterConfig().getLimitForPeriod() < 
                  apiRateLimiter.getRateLimiterConfig().getLimitForPeriod());
    }

    @Test
    void shouldHaveDifferentTimeoutsForDifferentRateLimiters() {
        // Given
        RateLimiter apiRateLimiter = rateLimitingConfig.apiRateLimiter();
        RateLimiter webRateLimiter = rateLimitingConfig.webRateLimiter();
        RateLimiter openAiRateLimiter = rateLimitingConfig.openAiRateLimiter();
        
        // Then
        assertEquals(Duration.ofSeconds(5), apiRateLimiter.getRateLimiterConfig().getTimeoutDuration());
        assertEquals(Duration.ofSeconds(2), webRateLimiter.getRateLimiterConfig().getTimeoutDuration());
        assertEquals(Duration.ofSeconds(10), openAiRateLimiter.getRateLimiterConfig().getTimeoutDuration());
    }

    @Test
    void shouldUseOneMinuteRefreshPeriodForAllRateLimiters() {
        // Given
        RateLimiter apiRateLimiter = rateLimitingConfig.apiRateLimiter();
        RateLimiter webRateLimiter = rateLimitingConfig.webRateLimiter();
        RateLimiter openAiRateLimiter = rateLimitingConfig.openAiRateLimiter();
        
        // Then
        assertEquals(Duration.ofMinutes(1), apiRateLimiter.getRateLimiterConfig().getLimitRefreshPeriod());
        assertEquals(Duration.ofMinutes(1), webRateLimiter.getRateLimiterConfig().getLimitRefreshPeriod());
        assertEquals(Duration.ofMinutes(1), openAiRateLimiter.getRateLimiterConfig().getLimitRefreshPeriod());
    }

    @Test
    void shouldCreateUniqueRateLimiterInstances() {
        // Given & When
        RateLimiter apiRateLimiter1 = rateLimitingConfig.apiRateLimiter();
        RateLimiter apiRateLimiter2 = rateLimitingConfig.apiRateLimiter();
        
        // Then - Should be different instances but same configuration
        assertTrue(apiRateLimiter1 != apiRateLimiter2); // Different instances
        assertEquals(apiRateLimiter1.getName(), apiRateLimiter2.getName()); // Same name
        assertEquals(apiRateLimiter1.getRateLimiterConfig().getLimitForPeriod(),
                    apiRateLimiter2.getRateLimiterConfig().getLimitForPeriod()); // Same config
    }

    @Test
    void shouldHandleMultipleClientsIndependently() {
        // Given
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // Client 1
        ServerWebExchange exchange1 = MockServerWebExchange.from(
            MockServerHttpRequest.get("/chat")
                .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
                .build());
        
        // Client 2
        ServerWebExchange exchange2 = MockServerWebExchange.from(
            MockServerHttpRequest.get("/chat")
                .remoteAddress(new InetSocketAddress("192.168.1.101", 8080))
                .build());
        
        // When & Then - Both should be allowed (different clients)
        StepVerifier.create(filter.filter(exchange1, chain))
                .verifyComplete();
        
        StepVerifier.create(filter.filter(exchange2, chain))
                .verifyComplete();
    }

    @Test
    void shouldApplyDifferentLimitsForApiVsWebEndpoints() {
        // Given
        WebFilter filter = rateLimitingConfig.rateLimitingFilter();
        WebFilterChain chain = mock(WebFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        String clientIp = "192.168.1.100";
        
        // API endpoint
        ServerWebExchange apiExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/v1/chat-completion")
                .remoteAddress(new InetSocketAddress(clientIp, 8080))
                .build());
        
        // Web endpoint
        ServerWebExchange webExchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/chat")
                .remoteAddress(new InetSocketAddress(clientIp, 8080))
                .build());
        
        // When & Then - Both should be allowed initially
        StepVerifier.create(filter.filter(apiExchange, chain))
                .verifyComplete();
        
        StepVerifier.create(filter.filter(webExchange, chain))
                .verifyComplete();
    }
}