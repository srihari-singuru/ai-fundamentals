package com.srihari.ai.service.streaming;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.metrics.CustomMetrics;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Component responsible for optimizing streaming responses for memory efficiency
 * and performance. Implements backpressure handling, buffer management, and
 * memory-efficient token streaming.
 */
@Component
@RequiredArgsConstructor
public class StreamingOptimizer {
    
    private final StructuredLogger structuredLogger;
    private final CustomMetrics customMetrics;
    
    // Configuration constants
    private static final int DEFAULT_BUFFER_SIZE = 64;
    private static final int MAX_BUFFER_SIZE = 512;
    private static final Duration BUFFER_TIMEOUT = Duration.ofMillis(100);
    private static final int BACKPRESSURE_THRESHOLD = 1000;
    private static final Duration BACKPRESSURE_DELAY = Duration.ofMillis(10);
    
    /**
     * Optimizes a token stream for memory efficiency and performance
     * 
     * @param tokenStream The original token stream
     * @param bufferSize Buffer size for batching tokens
     * @return Optimized token stream
     */
    public Flux<String> optimizeTokenStream(Flux<String> tokenStream, int bufferSize) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        AtomicLong tokenCount = new AtomicLong(0);
        AtomicLong totalBytes = new AtomicLong(0);
        
        int effectiveBufferSize = Math.min(Math.max(bufferSize, 1), MAX_BUFFER_SIZE);
        
        structuredLogger.debug("Starting token stream optimization", Map.of(
            "operation", "stream_optimization_start",
            "bufferSize", effectiveBufferSize,
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        return tokenStream
                .doOnNext(token -> {
                    long count = tokenCount.incrementAndGet();
                    long bytes = totalBytes.addAndGet(token.getBytes().length);
                    
                    // Log memory usage periodically
                    if (count % 100 == 0) {
                        structuredLogger.debug("Stream progress", Map.of(
                            "operation", "stream_progress",
                            "tokenCount", count,
                            "totalBytes", bytes,
                            "avgTokenSize", bytes / count,
                            "correlationId", correlationId != null ? correlationId : "unknown"
                        ));
                    }
                })
                .transform(this::applyBackpressureHandling)
                .bufferTimeout(effectiveBufferSize, BUFFER_TIMEOUT)
                .flatMap(tokenBatch -> {
                    // Process batch efficiently
                    return Mono.fromCallable(() -> String.join("", tokenBatch))
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnNext(batch -> {
                                customMetrics.recordStreamingBatch(tokenBatch.size(), batch.length());
                                
                                structuredLogger.debug("Token batch processed", Map.of(
                                    "operation", "batch_processed",
                                    "batchSize", tokenBatch.size(),
                                    "batchLength", batch.length(),
                                    "correlationId", correlationId != null ? correlationId : "unknown"
                                ));
                            });
                })
                .doOnComplete(() -> {
                    long finalCount = tokenCount.get();
                    long finalBytes = totalBytes.get();
                    
                    customMetrics.recordStreamingComplete(finalCount, finalBytes);
                    
                    structuredLogger.info("Token stream optimization completed", Map.of(
                        "operation", "stream_optimization_complete",
                        "totalTokens", finalCount,
                        "totalBytes", finalBytes,
                        "avgTokenSize", finalCount > 0 ? finalBytes / finalCount : 0,
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                })
                .doOnError(error -> {
                    structuredLogger.error("Token stream optimization failed", Map.of(
                        "operation", "stream_optimization_error",
                        "errorType", error.getClass().getSimpleName(),
                        "processedTokens", tokenCount.get(),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ), error);
                });
    }
    
    /**
     * Optimizes a token stream with default buffer size
     * 
     * @param tokenStream The original token stream
     * @return Optimized token stream
     */
    public Flux<String> optimizeTokenStream(Flux<String> tokenStream) {
        return optimizeTokenStream(tokenStream, DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Applies backpressure handling to prevent memory overflow
     * 
     * @param tokenStream The token stream to apply backpressure to
     * @return Stream with backpressure handling
     */
    public Flux<String> applyBackpressureHandling(Flux<String> tokenStream) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        AtomicLong pendingTokens = new AtomicLong(0);
        
        return tokenStream
                .doOnNext(token -> {
                    long pending = pendingTokens.incrementAndGet();
                    
                    // Monitor backpressure
                    if (pending > BACKPRESSURE_THRESHOLD) {
                        structuredLogger.warn("High backpressure detected", Map.of(
                            "operation", "backpressure_warning",
                            "pendingTokens", pending,
                            "threshold", BACKPRESSURE_THRESHOLD,
                            "correlationId", correlationId != null ? correlationId : "unknown"
                        ));
                        
                        customMetrics.incrementBackpressureEvents();
                    }
                })
                .doOnRequest(requested -> {
                    long pending = pendingTokens.addAndGet(-requested);
                    
                    structuredLogger.debug("Backpressure request", Map.of(
                        "operation", "backpressure_request",
                        "requested", requested,
                        "pendingAfter", Math.max(0, pending),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                })
                .delayElements(BACKPRESSURE_DELAY, Schedulers.parallel())
                .onBackpressureBuffer(BACKPRESSURE_THRESHOLD, 
                    dropped -> {
                        structuredLogger.error("Token dropped due to backpressure", Map.of(
                            "operation", "token_dropped",
                            "droppedToken", dropped.toString(),
                            "correlationId", correlationId != null ? correlationId : "unknown"
                        ), null);
                        
                        customMetrics.incrementDroppedTokens();
                    });
    }
    
    /**
     * Creates an optimized buffer for large responses
     * 
     * @param expectedSize Expected response size in characters
     * @return Optimized buffer size
     */
    public int calculateOptimalBufferSize(int expectedSize) {
        if (expectedSize <= 0) {
            return DEFAULT_BUFFER_SIZE;
        }
        
        // Calculate buffer size based on expected response size
        int calculatedSize = Math.max(DEFAULT_BUFFER_SIZE, expectedSize / 100);
        int optimalSize = Math.min(calculatedSize, MAX_BUFFER_SIZE);
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        structuredLogger.debug("Calculated optimal buffer size", Map.of(
            "operation", "buffer_size_calculation",
            "expectedSize", expectedSize,
            "calculatedSize", calculatedSize,
            "optimalSize", optimalSize,
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        return optimalSize;
    }
    
    /**
     * Monitors memory usage during streaming
     * 
     * @param tokenStream The token stream to monitor
     * @return Stream with memory monitoring
     */
    public Flux<String> withMemoryMonitoring(Flux<String> tokenStream) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        AtomicLong memoryUsage = new AtomicLong(0);
        
        return tokenStream
                .doOnNext(token -> {
                    long currentMemory = memoryUsage.addAndGet(token.getBytes().length);
                    
                    // Log memory usage every 1MB
                    if (currentMemory % (1024 * 1024) < token.getBytes().length) {
                        structuredLogger.debug("Memory usage checkpoint", Map.of(
                            "operation", "memory_checkpoint",
                            "memoryUsageBytes", currentMemory,
                            "memoryUsageMB", currentMemory / (1024 * 1024),
                            "correlationId", correlationId != null ? correlationId : "unknown"
                        ));
                        
                        customMetrics.recordStreamingMemoryUsage(currentMemory);
                    }
                })
                .doOnComplete(() -> {
                    long finalMemory = memoryUsage.get();
                    structuredLogger.info("Final memory usage", Map.of(
                        "operation", "final_memory_usage",
                        "totalMemoryBytes", finalMemory,
                        "totalMemoryMB", finalMemory / (1024 * 1024),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                });
    }
    
    /**
     * Applies all optimizations to a token stream
     * 
     * @param tokenStream The original token stream
     * @param expectedSize Expected response size for buffer optimization
     * @return Fully optimized token stream
     */
    public Flux<String> applyAllOptimizations(Flux<String> tokenStream, int expectedSize) {
        int optimalBufferSize = calculateOptimalBufferSize(expectedSize);
        
        return tokenStream
                .transform(this::withMemoryMonitoring)
                .transform(stream -> optimizeTokenStream(stream, optimalBufferSize));
    }
    
    /**
     * Applies all optimizations with default settings
     * 
     * @param tokenStream The original token stream
     * @return Fully optimized token stream
     */
    public Flux<String> applyAllOptimizations(Flux<String> tokenStream) {
        return applyAllOptimizations(tokenStream, 0);
    }
}