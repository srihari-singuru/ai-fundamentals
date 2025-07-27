package com.srihari.ai.service.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for comprehensive input validation and sanitization.
 * Implements security-focused validation rules for user input and system messages.
 */
@Service
@Slf4j
public class InputValidationService {

    private static final int MAX_MESSAGE_LENGTH = 10000;
    private static final int MAX_SYSTEM_MESSAGE_LENGTH = 5000;
    private static final int MIN_MESSAGE_LENGTH = 1;
    
    // Patterns for detecting potentially malicious content
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern HTML_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)\\s");
    private static final Pattern XSS_PATTERN = Pattern.compile("(?i)(javascript:|vbscript:|onload|onerror|onclick)");
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile("[;&|`$(){}\\[\\]<>]");
    
    // Allowed characters pattern (alphanumeric, common punctuation, whitespace)
    private static final Pattern ALLOWED_CHARS_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}\\p{S}\\r\\n\\t]*$");

    /**
     * Validates and sanitizes user chat messages.
     * 
     * @param message the user message to validate
     * @return ValidationResult containing validation status and sanitized message
     */
    public ValidationResult validateUserMessage(String message) {
        List<String> errors = new ArrayList<>();
        
        // Basic null and empty checks
        if (!StringUtils.hasText(message)) {
            errors.add("Message cannot be empty");
            return ValidationResult.invalid(errors);
        }
        
        // Length validation
        if (message.length() < MIN_MESSAGE_LENGTH) {
            errors.add("Message is too short");
        }
        
        if (message.length() > MAX_MESSAGE_LENGTH) {
            errors.add("Message exceeds maximum length of " + MAX_MESSAGE_LENGTH + " characters");
        }
        
        // Security validation
        validateSecurityThreats(message, errors);
        
        if (!errors.isEmpty()) {
            return ValidationResult.invalid(errors);
        }
        
        // Sanitize the message
        String sanitizedMessage = sanitizeMessage(message);
        
        return ValidationResult.valid(sanitizedMessage);
    }

    /**
     * Validates and sanitizes system messages.
     * 
     * @param systemMessage the system message to validate
     * @return ValidationResult containing validation status and sanitized message
     */
    public ValidationResult validateSystemMessage(String systemMessage) {
        List<String> errors = new ArrayList<>();
        
        // System messages can be empty (optional)
        if (systemMessage == null) {
            return ValidationResult.valid("");
        }
        
        // Length validation
        if (systemMessage.length() > MAX_SYSTEM_MESSAGE_LENGTH) {
            errors.add("System message exceeds maximum length of " + MAX_SYSTEM_MESSAGE_LENGTH + " characters");
        }
        
        // Security validation (more lenient for system messages)
        if (SCRIPT_PATTERN.matcher(systemMessage).find()) {
            errors.add("System message contains potentially malicious script content");
        }
        
        if (!errors.isEmpty()) {
            return ValidationResult.invalid(errors);
        }
        
        // Sanitize the system message
        String sanitizedMessage = sanitizeSystemMessage(systemMessage);
        
        return ValidationResult.valid(sanitizedMessage);
    }

    /**
     * Validates conversation ID format.
     * 
     * @param conversationId the conversation ID to validate
     * @return true if valid, false otherwise
     */
    public boolean validateConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return false;
        }
        
        // UUID format validation
        Pattern uuidPattern = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        return uuidPattern.matcher(conversationId).matches();
    }

    /**
     * Sanitizes user input by removing potentially harmful content.
     * 
     * @param input the input to sanitize
     * @return sanitized input
     */
    public String sanitizeMessage(String input) {
        if (input == null) {
            return null;
        }
        
        String sanitized = input;
        
        // Remove script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove HTML tags (but preserve content)
        sanitized = HTML_PATTERN.matcher(sanitized).replaceAll("");
        
        // Normalize whitespace
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        
        // Remove null bytes and control characters (except newlines and tabs)
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        return sanitized;
    }

    /**
     * Sanitizes system messages with more lenient rules.
     * 
     * @param input the system message to sanitize
     * @return sanitized system message
     */
    public String sanitizeSystemMessage(String input) {
        if (input == null) {
            return null;
        }
        
        String sanitized = input;
        
        // Remove script tags only
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove null bytes and dangerous control characters
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        return sanitized.trim();
    }

    private void validateSecurityThreats(String message, List<String> errors) {
        // Check for script injection
        if (SCRIPT_PATTERN.matcher(message).find()) {
            errors.add("Message contains potentially malicious script content");
        }
        
        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(message).find()) {
            errors.add("Message contains potentially malicious SQL patterns");
        }
        
        // Check for XSS patterns
        if (XSS_PATTERN.matcher(message).find()) {
            errors.add("Message contains potentially malicious XSS patterns");
        }
        
        // Check for command injection patterns
        if (COMMAND_INJECTION_PATTERN.matcher(message).find()) {
            errors.add("Message contains potentially dangerous characters");
        }
        
        // Check for allowed characters
        if (!ALLOWED_CHARS_PATTERN.matcher(message).matches()) {
            errors.add("Message contains invalid characters");
        }
        
        // Check for excessive repetition (potential DoS)
        if (hasExcessiveRepetition(message)) {
            errors.add("Message contains excessive character repetition");
        }
    }

    private boolean hasExcessiveRepetition(String message) {
        // Check for more than 50 consecutive identical characters
        Pattern repetitionPattern = Pattern.compile("(.)\\1{49,}");
        return repetitionPattern.matcher(message).find();
    }

    /**
     * Result of input validation containing status and sanitized content.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final String sanitizedContent;

        private ValidationResult(boolean valid, List<String> errors, String sanitizedContent) {
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.sanitizedContent = sanitizedContent;
        }

        public static ValidationResult valid(String sanitizedContent) {
            return new ValidationResult(true, null, sanitizedContent);
        }

        public static ValidationResult invalid(List<String> errors) {
            return new ValidationResult(false, errors, null);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public String getSanitizedContent() {
            return sanitizedContent;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}