package com.srihari.ai.service.validation.annotation;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for the ValidIdentifier annotation.
 */
public class ValidIdentifierValidator implements ConstraintValidator<ValidIdentifier, String> {
    
    private ValidIdentifier annotation;
    private Pattern pattern;
    
    @Override
    public void initialize(ValidIdentifier constraintAnnotation) {
        this.annotation = constraintAnnotation;
        this.pattern = Pattern.compile(annotation.pattern());
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Check length constraints
        if (value.length() < annotation.minLength() || value.length() > annotation.maxLength()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Identifier length must be between %d and %d characters", 
                    annotation.minLength(), annotation.maxLength()))
                   .addConstraintViolation();
            return false;
        }
        
        // Check pattern
        if (!pattern.matcher(value).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Identifier contains invalid characters. Only alphanumeric characters, hyphens, and underscores are allowed")
                   .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}