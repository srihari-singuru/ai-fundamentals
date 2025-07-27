package com.srihari.ai.performance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
class MemoryLeakDetectionTest {

    @Autowired
    private CustomMetrics customMetrics;
    
    @Autowired
    private MemoryService memoryService;
    
    @Autowired
    private InputValidationService inputValidationService;

    @Test
    void shouldNotLeakMemoryDuringHighVolumeOperations() throws InterruptedException {
        // Given
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection and measure initial memory
        System.gc();
        Thread.sleep(1000);
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        int numberOfOperations = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfOperations);
        AtomicInteger completedOperations = new AtomicInteger(0);
        
        // When - Perform high volume operations
        for (int i = 0; i < numberOfOperations; i++) {
            final int operationId = i;
            
            new Thread(() -> {
                try {
                    // Simulate various operations that could cause memory leaks
                    String conversationId = "test-conversation-" + operationId;
                    String message = "Test message " + operationId;
                    
                    // Memory operations
                    memoryService.loadConversation(conversationId);
                    memoryService.reset(conversationId);
                    
                    // Validation operations
                    inputValidationService.validateUserMessage(message);
                    
                    // Metrics operations
                    customMetrics.recordTokenUsage("gpt-4.1-nano", "test", 100);
                    customMetrics.incrementConversationStarted("test");
                    customMetrics.recordUserSessionStarted("session-" + operationId, "test-agent");
                    
                    completedOperations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // Wait for all operations to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertTrue(completed, "All operations should complete within timeout");
        
        // Force garbage collection and measure final memory
        System.gc();
        Thread.sleep(2000); // Allow GC to complete
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        long memoryIncrease = finalMemory - initialMemory;
        long memoryIncreasePerOperation = memoryIncrease / numberOfOperations;
        
        // Then - Verify no significant memory leak
        System.out.println("Memory Leak Detection Results:");
        System.out.println("Operations completed: " + completedOperations.get());
        System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        System.out.println("Memory per operation: " + memoryIncreasePerOperation + " bytes");
        
        // Assert no significant memory leak (less than 50MB increase for 1000 operations)
        assertTrue(memoryIncrease < 50 * 1024 * 1024, 
            "Memory increase should be less than 50MB, but was " + (memoryIncrease / 1024 / 1024) + "MB");
        
        // Assert reasonable memory usage per operation (less than 10KB per operation)
        assertTrue(memoryIncreasePerOperation < 10 * 1024, 
            "Memory per operation should be less than 10KB, but was " + memoryIncreasePerOperation + " bytes");
    }

    @Test
    void shouldHandleConcurrentMemoryOperations() throws InterruptedException {
        // Given
        int numberOfThreads = 50;
        int operationsPerThread = 20;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger totalOperations = new AtomicInteger(0);
        
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        Thread.sleep(500);
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // When - Run concurrent memory operations
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            
            new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String conversationId = "thread-" + threadId + "-conv-" + j;
                        
                        // Perform memory operations
                        memoryService.loadConversation(conversationId);
                        memoryService.reset(conversationId);
                        
                        // Record metrics
                        customMetrics.recordTokenUsage("gpt-4.1-nano", "concurrent-test", 50);
                        
                        totalOperations.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // Wait for completion
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "All concurrent operations should complete");
        
        // Measure memory after operations
        System.gc();
        Thread.sleep(1000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // Then - Verify concurrent operations don't cause excessive memory usage
        System.out.println("Concurrent Memory Test Results:");
        System.out.println("Total operations: " + totalOperations.get());
        System.out.println("Expected operations: " + (numberOfThreads * operationsPerThread));
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        assertTrue(totalOperations.get() == numberOfThreads * operationsPerThread, 
            "All operations should complete successfully");
        
        // Memory increase should be reasonable for concurrent operations
        assertTrue(memoryIncrease < 100 * 1024 * 1024, 
            "Memory increase should be less than 100MB for concurrent operations");
    }

    @Test
    void shouldCleanupResourcesProperly() throws InterruptedException {
        // Given
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        Thread.sleep(500);
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // When - Create and cleanup resources
        for (int i = 0; i < 100; i++) {
            String conversationId = "cleanup-test-" + i;
            
            // Create resources
            memoryService.loadConversation(conversationId);
            
            // Use resources
            inputValidationService.validateUserMessage("Test message " + i);
            customMetrics.recordTokenUsage("gpt-4.1-nano", "cleanup-test", 100);
            
            // Cleanup resources
            memoryService.reset(conversationId);
        }
        
        // Force cleanup
        System.gc();
        Thread.sleep(1000);
        long afterCleanupMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryDifference = afterCleanupMemory - initialMemory;
        
        // Then - Verify proper cleanup
        System.out.println("Resource Cleanup Test Results:");
        System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("After cleanup memory: " + (afterCleanupMemory / 1024 / 1024) + " MB");
        System.out.println("Memory difference: " + (memoryDifference / 1024 / 1024) + " MB");
        
        // Memory should return close to initial levels after cleanup
        assertTrue(Math.abs(memoryDifference) < 20 * 1024 * 1024, 
            "Memory should return close to initial levels after cleanup");
    }

    @Test
    void shouldHandleLongRunningOperations() throws InterruptedException {
        // Given
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        Thread.sleep(500);
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        int duration = 10; // seconds
        long endTime = System.currentTimeMillis() + (duration * 1000);
        AtomicInteger operationCount = new AtomicInteger(0);
        
        // When - Run operations for extended period
        while (System.currentTimeMillis() < endTime) {
            int opId = operationCount.incrementAndGet();
            String conversationId = "long-running-" + opId;
            
            // Perform operations
            memoryService.loadConversation(conversationId);
            inputValidationService.validateUserMessage("Long running test " + opId);
            customMetrics.recordTokenUsage("gpt-4.1-nano", "long-running", 75);
            
            // Cleanup periodically
            if (opId % 10 == 0) {
                memoryService.reset(conversationId);
            }
            
            // Small delay to simulate real usage
            Thread.sleep(10);
        }
        
        // Final cleanup and memory measurement
        System.gc();
        Thread.sleep(1000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // Then - Verify long-running operations don't cause memory issues
        System.out.println("Long Running Operations Test Results:");
        System.out.println("Operations performed: " + operationCount.get());
        System.out.println("Duration: " + duration + " seconds");
        System.out.println("Operations per second: " + (operationCount.get() / duration));
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        assertTrue(operationCount.get() > 0, "Should perform operations during test period");
        
        // Memory increase should be bounded for long-running operations
        assertTrue(memoryIncrease < 30 * 1024 * 1024, 
            "Memory increase should be less than 30MB for long-running operations");
    }

    @Test
    void shouldHandleMemoryPressureGracefully() throws InterruptedException {
        // Given
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create memory pressure by using large objects
        int numberOfLargeOperations = 50;
        CountDownLatch latch = new CountDownLatch(numberOfLargeOperations);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        
        // When - Perform operations under memory pressure
        for (int i = 0; i < numberOfLargeOperations; i++) {
            final int operationId = i;
            
            new Thread(() -> {
                try {
                    // Create large message to simulate memory pressure
                    String largeMessage = "Large message " + "A".repeat(1000) + " " + operationId;
                    String conversationId = "pressure-test-" + operationId;
                    
                    // Perform operations under pressure
                    memoryService.loadConversation(conversationId);
                    inputValidationService.validateUserMessage(largeMessage);
                    customMetrics.recordTokenUsage("gpt-4.1-nano", "pressure-test", 200);
                    
                    // Cleanup
                    memoryService.reset(conversationId);
                    
                    successfulOperations.incrementAndGet();
                } catch (OutOfMemoryError e) {
                    // Expected under extreme memory pressure
                    System.out.println("OutOfMemoryError caught (expected under pressure): " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // Wait for completion
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertTrue(completed, "All operations should complete or fail gracefully");
        
        // Force cleanup
        System.gc();
        Thread.sleep(2000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Then - Verify graceful handling of memory pressure
        System.out.println("Memory Pressure Test Results:");
        System.out.println("Max memory: " + (maxMemory / 1024 / 1024) + " MB");
        System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Successful operations: " + successfulOperations.get() + "/" + numberOfLargeOperations);
        
        // At least some operations should succeed even under pressure
        assertTrue(successfulOperations.get() > numberOfLargeOperations * 0.5, 
            "At least 50% of operations should succeed under memory pressure");
        
        // System should recover after pressure is relieved
        assertTrue(finalMemory < maxMemory * 0.9, 
            "Memory should be released after operations complete");
    }
}