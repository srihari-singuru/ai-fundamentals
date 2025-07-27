package com.srihari.ai.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

    private SensitiveDataMasker sensitiveDataMasker;

    @BeforeEach
    void setUp() {
        sensitiveDataMasker = new SensitiveDataMasker();
    }

    @Test
    void shouldMaskApiKey() {
        // Given
        String apiKey = "sk-1234567890abcdef1234567890abcdef1234567890abcdef";
        
        // When
        String masked = sensitiveDataMasker.maskApiKey(apiKey);
        
        // Then
        assertThat(masked).startsWith("sk-123");
        assertThat(masked).endsWith("cdef");
        assertThat(masked).contains("****");
        assertThat(masked).doesNotContain("567890abcdef1234567890abcdef123456789");
    }

    @Test
    void shouldMaskShortApiKey() {
        // Given
        String apiKey = "short";
        
        // When
        String masked = sensitiveDataMasker.maskApiKey(apiKey);
        
        // Then
        assertThat(masked).isEqualTo("****");
    }

    @Test
    void shouldMaskUserDataWithEmail() {
        // Given
        String userData = "User email is john.doe@example.com and phone is 555-123-4567";
        
        // When
        String masked = sensitiveDataMasker.maskUserData(userData);
        
        // Then
        assertThat(masked).doesNotContain("john.doe@example.com");
        assertThat(masked).doesNotContain("555-123-4567");
        assertThat(masked).contains("j****e@example.com");
        assertThat(masked).contains("****");
    }

    @Test
    void shouldMaskApiKeyInUserData() {
        // Given
        String userData = "API key: sk-1234567890abcdef1234567890abcdef1234567890abcdef";
        
        // When
        String masked = sensitiveDataMasker.maskUserData(userData);
        
        // Then
        assertThat(masked).doesNotContain("sk-1234567890abcdef1234567890abcdef1234567890abcdef");
        assertThat(masked).contains("****");
    }

    @Test
    void shouldMaskSsnInUserData() {
        // Given
        String userData = "SSN: 123-45-6789";
        
        // When
        String masked = sensitiveDataMasker.maskUserData(userData);
        
        // Then
        assertThat(masked).doesNotContain("123-45-6789");
        assertThat(masked).contains("***-**-****");
    }

    @Test
    void shouldMaskCreditCardInUserData() {
        // Given
        String userData = "Credit card: 1234 5678 9012 3456";
        
        // When
        String masked = sensitiveDataMasker.maskUserData(userData);
        
        // Then
        assertThat(masked).doesNotContain("1234 5678 9012 3456");
        assertThat(masked).contains("****-****-****-****");
    }

    @Test
    void shouldMaskLogDataWithSensitiveKeys() {
        // Given
        Map<String, Object> logData = new HashMap<>();
        logData.put("username", "john_doe");
        logData.put("api_key", "sk-1234567890abcdef1234567890abcdef1234567890abcdef");
        logData.put("password", "secretpassword");
        logData.put("email", "john.doe@example.com");
        logData.put("normal_field", "normal_value");
        
        // When
        Map<String, Object> masked = sensitiveDataMasker.maskLogData(logData);
        
        // Then
        assertThat(masked.get("username")).isEqualTo("john_doe");
        assertThat(masked.get("normal_field")).isEqualTo("normal_value");
        assertThat(masked.get("api_key")).asString().contains("****");
        assertThat(masked.get("password")).asString().contains("****");
        assertThat(masked.get("email")).asString().contains("j****e@example.com");
    }

    @Test
    void shouldMaskNestedLogData() {
        // Given
        Map<String, Object> nestedData = new HashMap<>();
        nestedData.put("secret", "topsecret");
        
        Map<String, Object> logData = new HashMap<>();
        logData.put("user", "john");
        logData.put("credentials", nestedData);
        
        // When
        Map<String, Object> masked = sensitiveDataMasker.maskLogData(logData);
        
        // Then
        assertThat(masked.get("user")).isEqualTo("john");
        Object credentialsObj = masked.get("credentials");
        assertThat(credentialsObj).isNotNull();
        
        // Since "secret" is a sensitive key, it should be masked as a string
        if (credentialsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> maskedNested = (Map<String, Object>) credentialsObj;
            assertThat(maskedNested.get("secret").toString()).contains("****");
        } else {
            // If it's treated as a sensitive key, the whole object might be masked
            assertThat(credentialsObj.toString()).contains("****");
        }
    }

    @Test
    void shouldHandleNullValues() {
        // Given & When & Then
        assertThat(sensitiveDataMasker.maskApiKey(null)).isEqualTo("****");
        assertThat(sensitiveDataMasker.maskUserData(null)).isNull();
        assertThat(sensitiveDataMasker.maskLogData(null)).isNull();
        assertThat(sensitiveDataMasker.maskExceptionData(null)).isNull();
    }

    @Test
    void shouldMaskExceptionData() {
        // Given
        Exception exception = new RuntimeException("Error with API key sk-1234567890abcdef1234567890abcdef1234567890abcdef");
        
        // When
        String masked = sensitiveDataMasker.maskExceptionData(exception);
        
        // Then
        assertThat(masked).doesNotContain("sk-1234567890abcdef1234567890abcdef1234567890abcdef");
        assertThat(masked).contains("****");
    }

    @Test
    void shouldMaskRequestData() {
        // Given
        String requestData = "Request contains email john.doe@example.com and token abc123def456";
        
        // When
        String masked = sensitiveDataMasker.maskRequestData(requestData);
        
        // Then
        assertThat(masked).doesNotContain("john.doe@example.com");
        assertThat(masked).contains("j****e@example.com");
    }

    @Test
    void shouldPreserveNonSensitiveData() {
        // Given
        String userData = "This is normal text with numbers 12345 and words";
        
        // When
        String masked = sensitiveDataMasker.maskUserData(userData);
        
        // Then
        assertThat(masked).isEqualTo(userData);
    }
}