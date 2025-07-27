package com.srihari.ai.security;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Component responsible for masking sensitive data in logs and error responses.
 * Implements data protection patterns to prevent sensitive information leakage.
 */
@Component
@Slf4j
public class SensitiveDataMasker {

    private static final String MASK_PATTERN = "****";
    private static final String API_KEY_PATTERN = "sk-[a-zA-Z0-9]{48}";
    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
    private static final String PHONE_PATTERN = "\\b\\d{3}-\\d{3}-\\d{4}\\b|\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b";
    private static final String SSN_PATTERN = "\\b\\d{3}-\\d{2}-\\d{4}\\b";
    private static final String CREDIT_CARD_PATTERN = "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b";
    
    private static final Pattern API_KEY_REGEX = Pattern.compile(API_KEY_PATTERN);
    private static final Pattern EMAIL_REGEX = Pattern.compile(EMAIL_PATTERN);
    private static final Pattern PHONE_REGEX = Pattern.compile(PHONE_PATTERN);
    private static final Pattern SSN_REGEX = Pattern.compile(SSN_PATTERN);
    private static final Pattern CREDIT_CARD_REGEX = Pattern.compile(CREDIT_CARD_PATTERN);

    /**
     * Masks API keys in the provided text.
     * 
     * @param apiKey the API key to mask
     * @return masked API key showing only first 3 and last 4 characters
     */
    public String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return MASK_PATTERN;
        }
        
        if (apiKey.startsWith("sk-") && apiKey.length() > 10) {
            return apiKey.substring(0, 6) + MASK_PATTERN + apiKey.substring(apiKey.length() - 4);
        }
        
        return apiKey.substring(0, 3) + MASK_PATTERN + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Masks sensitive user data including emails, phone numbers, SSNs, and credit cards.
     * 
     * @param userData the user data to mask
     * @return masked user data with sensitive information replaced
     */
    public String maskUserData(String userData) {
        if (userData == null) {
            return null;
        }

        String maskedData = userData;
        
        // Mask API keys
        maskedData = API_KEY_REGEX.matcher(maskedData).replaceAll(MASK_PATTERN);
        
        // Mask email addresses
        maskedData = EMAIL_REGEX.matcher(maskedData).replaceAll(matchResult -> maskEmail(matchResult.group()));
        
        // Mask phone numbers
        maskedData = PHONE_REGEX.matcher(maskedData).replaceAll(MASK_PATTERN);
        
        // Mask SSNs
        maskedData = SSN_REGEX.matcher(maskedData).replaceAll("***-**-****");
        
        // Mask credit card numbers
        maskedData = CREDIT_CARD_REGEX.matcher(maskedData).replaceAll("****-****-****-****");
        
        return maskedData;
    }

    /**
     * Masks sensitive data in log data maps.
     * 
     * @param logData the log data map to mask
     * @return new map with sensitive data masked
     */
    public Map<String, Object> maskLogData(Map<String, Object> logData) {
        if (logData == null) {
            return null;
        }

        Map<String, Object> maskedData = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : logData.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();
            
            if (value == null) {
                maskedData.put(entry.getKey(), null);
                continue;
            }
            
            // Mask sensitive keys
            if (isSensitiveKey(key)) {
                maskedData.put(entry.getKey(), maskSensitiveValue(value.toString()));
            } else if (value instanceof String) {
                maskedData.put(entry.getKey(), maskUserData((String) value));
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                maskedData.put(entry.getKey(), maskLogData(nestedMap));
            } else {
                maskedData.put(entry.getKey(), value);
            }
        }
        
        return maskedData;
    }

    /**
     * Masks sensitive data in exception messages and stack traces.
     * 
     * @param exception the exception to mask
     * @return masked exception message
     */
    public String maskExceptionData(Throwable exception) {
        if (exception == null) {
            return null;
        }
        
        String message = exception.getMessage();
        if (message != null) {
            message = maskUserData(message);
        }
        
        return message;
    }

    /**
     * Masks request/response data for logging purposes.
     * 
     * @param requestData the request data to mask
     * @return masked request data
     */
    public String maskRequestData(String requestData) {
        if (requestData == null) {
            return null;
        }
        
        return maskUserData(requestData);
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return MASK_PATTERN + email.substring(atIndex);
        }
        
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return MASK_PATTERN + domainPart;
        }
        
        return localPart.charAt(0) + MASK_PATTERN + localPart.charAt(localPart.length() - 1) + domainPart;
    }

    private boolean isSensitiveKey(String key) {
        return key.contains("password") || 
               key.contains("secret") || 
               key.contains("token") || 
               key.contains("key") || 
               key.contains("auth") || 
               key.contains("credential") ||
               key.contains("ssn") ||
               key.contains("social") ||
               key.contains("credit") ||
               key.contains("card");
    }

    private String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 4) {
            return MASK_PATTERN;
        }
        
        if (value.length() <= 8) {
            return value.substring(0, 2) + MASK_PATTERN;
        }
        
        return value.substring(0, 2) + MASK_PATTERN + value.substring(value.length() - 2);
    }
}