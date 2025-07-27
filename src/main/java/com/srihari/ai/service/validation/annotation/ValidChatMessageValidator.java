package com.srihari.ai.service.validation.annotation;

import org.springframework.beans.factory.annotation.Autowired;

import com.srihari.ai.service.validation.InputValidationService;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for the ValidChatMessage annotation.
 */
public class ValidChatMessageValidator implements ConstraintValidator<ValidChatMessage, String> {
    
    @Autowired
    private InputValidationService inputValidationService;
    
    private ValidChatMessage annotation;
    
    @Override
    public void initialize(ValidChatMessage constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Use the InputValidationService for validation
        InputValidationService.ValidationResult result = inputValidationService.validateUserMessage(value);
        
        if (!result.isValid()) {
            // Disable default constraint violation
            context.disableDefaultConstraintViolation();
            
            // Add custom violation message
            context.buildConstraintViolationWithTemplate(result.getErrorMessage())
                   .addConstraintViolation();
            
            return false;
        }
        
        return true;
    }
}