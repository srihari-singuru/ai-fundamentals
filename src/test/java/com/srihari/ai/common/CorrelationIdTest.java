package com.srihari.ai.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class CorrelationIdTest {

    @AfterEach
    void cleanup() {
        CorrelationIdHolder.clear();
    }

    @Test
    void shouldSetAndGetCorrelationId() {
        // Given
        String correlationId = "test-correlation-id";
        
        // When
        CorrelationIdHolder.setCorrelationId(correlationId);
        
        // Then
        assertEquals(correlationId, CorrelationIdHolder.getCorrelationId());
        assertTrue(CorrelationIdHolder.hasCorrelationId());
    }

    @Test
    void shouldGenerateAndSetCorrelationId() {
        // When
        String generatedId = CorrelationIdHolder.generateAndSetCorrelationId();
        
        // Then
        assertNotNull(generatedId);
        assertFalse(generatedId.isEmpty());
        assertEquals(generatedId, CorrelationIdHolder.getCorrelationId());
        assertTrue(CorrelationIdHolder.hasCorrelationId());
    }

    @Test
    void shouldClearCorrelationId() {
        // Given
        CorrelationIdHolder.setCorrelationId("test-id");
        assertTrue(CorrelationIdHolder.hasCorrelationId());
        
        // When
        CorrelationIdHolder.clear();
        
        // Then
        assertNull(CorrelationIdHolder.getCorrelationId());
        assertFalse(CorrelationIdHolder.hasCorrelationId());
    }

    @Test
    void shouldReturnNullWhenNoCorrelationIdSet() {
        // When & Then
        assertNull(CorrelationIdHolder.getCorrelationId());
        assertFalse(CorrelationIdHolder.hasCorrelationId());
    }
}