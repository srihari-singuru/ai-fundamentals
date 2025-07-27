package com.srihari.ai.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when input validation fails.
 * This includes request validation, parameter validation, and business rule validation.
 */
public class ValidationException extends AiApplicationException {
    
    public static final String ERROR_CATEGORY = "VALIDATION_ERROR";
    
    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public ValidationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    public ValidationException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }
    
    public ValidationException(String errorCode, String message, Throwable cause, Map<String, Object> context) {
        super(errorCode, message, cause, context);
    }
    
    @Override
    public int getHttpStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
    
    @Override
    public String getErrorCategory() {
        return ERROR_CATEGORY;
    }
    
    // Factory methods for common validation errors
    
    public static ValidationException invalidInput(String fieldName, String value, String reason) {
        return new ValidationException("INVALID_INPUT", 
                "Invalid input for field '" + fieldName + "': " + reason)
                .addContext("fieldName", fieldName)
                .addContext("value", value)
                .addContext("reason", reason);
    }
    
    public static ValidationException missingRequiredField(String fieldName) {
        return new ValidationException("MISSING_REQUIRED_FIELD", 
                "Required field is missing: " + fieldName)
                .addContext("fieldName", fieldName);
    }
    
    public static ValidationException invalidFormat(String fieldName, String expectedFormat) {
        return new ValidationException("INVALID_FORMAT", 
                "Invalid format for field '" + fieldName + "'. Expected: " + expectedFormat)
                .addContext("fieldName", fieldName)
                .addContext("expectedFormat", expectedFormat);
    }
    
    public static ValidationException valueOutOfRange(String fieldName, Object value, Object min, Object max) {
        return new ValidationException("VALUE_OUT_OF_RANGE", 
                "Value for field '" + fieldName + "' is out of range. Value: " + value + ", Range: [" + min + ", " + max + "]")
                .addContext("fieldName", fieldName)
                .addContext("value", value)
                .addContext("minValue", min)
                .addContext("maxValue", max);
    }
    
    public static ValidationException invalidModel(String modelName) {
        return new ValidationException("INVALID_MODEL", 
                "Invalid or unsupported model: " + modelName)
                .addContext("modelName", modelName);
    }
    
    public static ValidationException messageTooLong(int length, int maxLength) {
        return new ValidationException("MESSAGE_TOO_LONG", 
                "Message length exceeds maximum allowed: " + length + " > " + maxLength)
                .addContext("messageLength", length)
                .addContext("maxLength", maxLength);
    }
    
    public static ValidationException invalidConversationId(String conversationId) {
        return new ValidationException("INVALID_CONVERSATION_ID", 
                "Invalid conversation ID format: " + conversationId)
                .addContext("conversationId", conversationId);
    }
}