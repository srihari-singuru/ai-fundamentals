package com.srihari.ai.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.srihari.ai.security.SensitiveDataMasker;

@Execution(ExecutionMode.CONCURRENT)
class StructuredLoggerTest {

    private final SensitiveDataMasker sensitiveDataMasker = new SensitiveDataMasker();
    private final StructuredLogger structuredLogger = new StructuredLogger(sensitiveDataMasker);

    @Test
    void shouldMaskSensitiveApiKey() {
        // Given
        String sensitiveText = "api-key: sk-1234567890abcdef1234567890abcdef1234567890abcdef";
        
        // When
        String masked = structuredLogger.maskSensitiveData(sensitiveText);
        
        // Then
        assertNotEquals(sensitiveText, masked);
        assertFalse(masked.contains("1234567890abcdef1234567890abcdef123456789"));
    }

    @Test
    void shouldMaskEmailAddresses() {
        // Given
        String textWithEmail = "Contact us at support@example.com for help";
        
        // When
        String masked = structuredLogger.maskSensitiveData(textWithEmail);
        
        // Then
        assertNotEquals(textWithEmail, masked);
        assertFalse(masked.contains("support@example.com"));
    }

    @Test
    void shouldMaskPhoneNumbers() {
        // Given
        String textWithPhone = "Call us at 555-123-4567";
        
        // When
        String masked = structuredLogger.maskSensitiveData(textWithPhone);
        
        // Then
        assertNotEquals(textWithPhone, masked);
        assertFalse(masked.contains("555-123-4567"));
    }

    @Test
    void shouldMaskSensitiveDataInContext() {
        // Given
        Map<String, Object> context = Map.of(
            "apiKey", "sk-1234567890abcdef",
            "email", "user@example.com",
            "normalField", "normalValue"
        );
        
        // When
        Map<String, Object> masked = structuredLogger.maskSensitiveData(context);
        
        // Then
        assertNotEquals(context.get("apiKey"), masked.get("apiKey"));
        assertNotEquals(context.get("email"), masked.get("email"));
        assertEquals(context.get("normalField"), masked.get("normalField"));
    }

    @Test
    void shouldHandleNullValues() {
        // When & Then
        assertNull(structuredLogger.maskSensitiveData((String) null));
        assertNull(structuredLogger.maskSensitiveData((Map<String, Object>) null));
    }

    @Test
    void shouldCreateLogEventBuilder() {
        // When
        StructuredLogger.LogEventBuilder builder = structuredLogger.event("INFO");
        
        // Then
        assertNotNull(builder);
        
        // Should be able to chain methods
        assertDoesNotThrow(() -> {
            builder.message("Test message")
                   .context("key", "value")
                   .operation("test_operation")
                   .duration(100L)
                   .log();
        });
    }
}