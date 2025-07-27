package com.srihari.ai.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.service.MemoryService;
import com.srihari.ai.service.validation.InputValidationService;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.CONCURRENT)
class PerformanceBenchmarkTest {

    @Autowired
    private CustomMetrics customMetrics;
    
    @Autowired
    private MemoryService memoryService;
    
    @Autowired
    private InputValidationService inputValidationService;

    @Test
    void shouldMeetValidationPerformanceRequirements() throws InterruptedException {
        // Given
        int numberOfValidations = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfValidations);
        AtomicInteger successfulValidations = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);
        
        String[] testMessages = {
            "Hello, how are you?",
            "What's the weather like today?",
            "Can you help me with a programming question?",
            "Tell me about artificial intelligence",
            "How do I improve my coding skills?"
        };
        
        // When - Perform validation benchmark
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfValidations; i++) {
            final int index = i;
            
            new Thread(() -> {
                try {
                    long operationStart = System.nanoTime();
                    String message = testMessages[index % testMessages.length] + " " + index;
                    
                    InputValidationService.ValidationResult result = 
                        inputValidationService.validateUserMessage(message);
                    
                    long operationTime = System.nanoTime() - operationStart;
                    totalTime.addAndGet(operationTime);
                    
                    if (result != null) {
                        successfulValidations.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // Wait for completion
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long totalTestTime = endTime - startTime;
        
        // Then - Verify performance requirements
        assertTrue(completed, "All validations should complete within timeout");
        
        double averageTimeMs = (totalTime.get() / numberOfValidations) / 1_000_000.0;
        double throughput = successfulValidations.get() / (totalTestTime / 1000.0);
        
        System.out.println("Validation Performance Results:");
        System.out.println("Total validations: " + numberOfValidations);
        System.out.println("Successful validations: " + successfulValidations.get());
        System.out.println("Total time: " + totalTestTime + "ms");
        System.out.println("Average validation time: " + String.format("%.2f", averageTimeMs) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " validations/second");
        
        // Performance assertions
        assertTrue(averageTimeMs < 10, "Average validation time should be less than 10ms");
        assertTrue(throughput > 100, "Should handle at least 100 validations per second");
        assertTrue(successfulValidations.get() > numberOfValidations * 0.95, 
            "At least 95% of validations should succeed");
    }

    @Test
    void shouldMeetMemoryOperationPerformanceRequirements() throws InterruptedException {
        // Given
        int numberOfOperations = 500;
        CountDownLatch latch = new CountDownLatch(numberOfOperations);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);
        
        // When - Perform memory operation benchmark
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfOperations; i++) {
            final int index = i;
            
            new Thread(() -> {
                try {
                    long operationStart = System.nanoTime();
                    String conversationId = "benchmark-" + index;
                    
                    // Perform memory operations
                    var conversation = memoryService.loadConversation(conversationId);
                    memoryService.reset(conversationId);
                    
                    long operationTime = System.nanoTime() - operationStart;
                    totalTime.addAndGet(operationTime);
                    
                    if (conversation != null) {
                        successfulOperations.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // Wait for completion
        boolean completed = latch.await(20, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long totalTestTime = endTime - startTime;
        
        // Then - Verify performance requirements
        assertTrue(completed, "All memory operations should complete within timeout");
        
        double averageTimeMs = (totalTime.get() / numberOfOperations) / 1_000_000.0;
        double throughput = successfulOperations.get() / (totalTestTime / 1000.0);
        
        System.out.println("Memory Operation Performance Results:");
        System.out.println("Total operations: " + numberOfOperations);
        System.out.println("Successful operations: " + successfulOperations.get());
        System.out.println("Total time: " + totalTestTime + "ms");
        System.out.println("Average operation time: " + String.format("%.2f", averageTimeMs) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " operations/second");
        
        // Performance assertions
        assertTrue(averageTimeMs < 5, "Average memory operation time should be less than 5ms");
        assertTrue(throughput > 50, "Should handle at least 50 memory operations per second");
        assertTrue(successfulOperations.get() == numberOfOperations, 
            "All memory operations should succeed");
    }

    @Test
    void shouldMeetMetricsRecordingPerformanceRequirements() throws InterruptedException {
        // Given
        int numberOfMetrics = 2000;
        CountDownLatch latch = new CountDownLatch(numberOfMetrics);
        AtomicInteger successfulMetrics = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);
        
        // When - Perform metrics recording benchmark
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfMetrics; i++) {
            final int index = i;
            
            new Thread(() -> {
                try {
                    long operationStart = System.nanoTime();
                    
                    // Record various metrics
                    customMetrics.recordTokenUsage("gpt-4.1-nano", "benchmark", 100);
                    customMetrics.incrementConversationStarted("benchmark");
                    customMetrics.recordUserSessionStarted("session-" + index, "benchmark-agent");
                    
                    long operationTime = System.nanoTime() - operationStart;
                    totalTime.addAndGet(operationTime);
                    
                    successfulMetrics.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // Wait for completion
        boolean completed = latch.await(15, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long totalTestTime = endTime - startTime;
        
        // Then - Verify performance requirements
        assertTrue(completed, "All metrics recording should complete within timeout");
        
        double averageTimeMs = (totalTime.get() / numberOfMetrics) / 1_000_000.0;
        double throughput = successfulMetrics.get() / (totalTestTime / 1000.0);
        
        System.out.println("Metrics Recording Performance Results:");
        System.out.println("Total metrics: " + numberOfMetrics);
        System.out.println("Successful metrics: " + successfulMetrics.get());
        System.out.println("Total time: " + totalTestTime + "ms");
        System.out.println("Average recording time: " + String.format("%.2f", averageTimeMs) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " metrics/second");
        
        // Performance assertions
        assertTrue(averageTimeMs < 2, "Average metrics recording time should be less than 2ms");
        assertTrue(throughput > 200, "Should handle at least 200 metrics per second");
        assertTrue(successfulMetrics.get() == numberOfMetrics, 
            "All metrics recording should succeed");
    }

    @Test
    void shouldHandleConcurrentLoadEfficiently() throws InterruptedException {
        // Given
        int numberOfThreads = 20;
        int operationsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);
        
        // When - Perform concurrent load test
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            
            new Thread(() -> {
                try {
                    long threadStart = System.nanoTime();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        String conversationId = "concurrent-" + threadId + "-" + j;
                        String message = "Concurrent test message " + threadId + "-" + j;
                        
                        // Perform mixed operations
                        memoryService.loadConversation(conversationId);
                        inputValidationService.validateUserMessage(message);
                        customMetrics.recordTokenUsage("gpt-4.1-nano", "concurrent", 75);
                        memoryService.reset(conversationId);
                        
                        totalOperations.incrementAndGet();
                    }
                    
                    long threadTime = System.nanoTime() - threadStart;
                    totalTime.addAndGet(threadTime);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // Wait for completion
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long totalTestTime = endTime - startTime;
        
        // Then - Verify concurrent performance
        assertTrue(completed, "All concurrent operations should complete within timeout");
        
        int expectedOperations = numberOfThreads * operationsPerThread;
        double averageTimeMs = (totalTime.get() / numberOfThreads) / 1_000_000.0;
        double throughput = totalOperations.get() / (totalTestTime / 1000.0);
        
        System.out.println("Concurrent Load Performance Results:");
        System.out.println("Threads: " + numberOfThreads);
        System.out.println("Operations per thread: " + operationsPerThread);
        System.out.println("Total operations: " + totalOperations.get());
        System.out.println("Expected operations: " + expectedOperations);
        System.out.println("Total time: " + totalTestTime + "ms");
        System.out.println("Average thread time: " + String.format("%.2f", averageTimeMs) + "ms");
        System.out.println("Overall throughput: " + String.format("%.2f", throughput) + " operations/second");
        
        // Performance assertions
        assertTrue(totalOperations.get() == expectedOperations, 
            "All concurrent operations should complete successfully");
        assertTrue(throughput > 100, "Should handle at least 100 concurrent operations per second");
        assertTrue(averageTimeMs < 5000, "Average thread completion time should be less than 5 seconds");
    }

    @Test
    void shouldMaintainPerformanceUnderSustainedLoad() throws InterruptedException {
        // Given
        int durationSeconds = 15;
        AtomicInteger operationCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicInteger errors = new AtomicInteger(0);
        
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
        
        // When - Run sustained load
        while (System.currentTimeMillis() < endTime) {
            new Thread(() -> {
                try {
                    long operationStart = System.nanoTime();
                    int opId = operationCount.incrementAndGet();
                    
                    String conversationId = "sustained-" + opId;
                    String message = "Sustained load test " + opId;
                    
                    // Perform operations
                    memoryService.loadConversation(conversationId);
                    inputValidationService.validateUserMessage(message);
                    customMetrics.recordTokenUsage("gpt-4.1-nano", "sustained", 50);
                    
                    // Cleanup periodically
                    if (opId % 5 == 0) {
                        memoryService.reset(conversationId);
                    }
                    
                    long responseTime = System.nanoTime() - operationStart;
                    totalResponseTime.addAndGet(responseTime);
                    
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }).start();
            
            // Small delay to control load
            Thread.sleep(5);
        }
        
        // Allow remaining operations to complete
        Thread.sleep(2000);
        
        // Then - Verify sustained performance
        double averageResponseTimeMs = (totalResponseTime.get() / operationCount.get()) / 1_000_000.0;
        double operationsPerSecond = operationCount.get() / (double) durationSeconds;
        double errorRate = (errors.get() / (double) operationCount.get()) * 100;
        
        System.out.println("Sustained Load Performance Results:");
        System.out.println("Duration: " + durationSeconds + " seconds");
        System.out.println("Total operations: " + operationCount.get());
        System.out.println("Operations per second: " + String.format("%.2f", operationsPerSecond));
        System.out.println("Average response time: " + String.format("%.2f", averageResponseTimeMs) + "ms");
        System.out.println("Error count: " + errors.get());
        System.out.println("Error rate: " + String.format("%.2f", errorRate) + "%");
        
        // Performance assertions
        assertTrue(operationCount.get() > 0, "Should perform operations during sustained load");
        assertTrue(operationsPerSecond > 10, "Should maintain at least 10 operations per second");
        assertTrue(averageResponseTimeMs < 100, "Average response time should remain under 100ms");
        assertTrue(errorRate < 5, "Error rate should be less than 5%");
    }
}