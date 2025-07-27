package com.srihari.ai.service.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Custom validation annotation for identifiers (conversation ID, session ID, etc.).
 * Validates that the identifier contains only allowed characters and is within length limits.
 */
@Documented
@Constraint(validatedBy = ValidIdentifierValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIdentifier {
    
    String message() default "Invalid identifier format";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Maximum allowed identifier length
     */
    int maxLength() default 100;
    
    /**
     * Minimum required identifier length
     */
    int minLength() default 1;
    
    /**
     * Pattern for allowed characters (default: alphanumeric, hyphens, underscores)
     */
    String pattern() default "^[a-zA-Z0-9_-]+$";
}