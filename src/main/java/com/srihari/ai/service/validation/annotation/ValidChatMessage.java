package com.srihari.ai.service.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Custom validation annotation for chat messages.
 * Validates message content for security, length, and appropriateness.
 */
@Documented
@Constraint(validatedBy = ValidChatMessageValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidChatMessage {
    
    String message() default "Invalid chat message";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Maximum allowed message length
     */
    int maxLength() default 4000;
    
    /**
     * Minimum required message length
     */
    int minLength() default 1;
    
    /**
     * Whether to allow HTML content
     */
    boolean allowHtml() default false;
    
    /**
     * Whether to perform security validation
     */
    boolean securityCheck() default true;
    
    /**
     * Whether to check for inappropriate content
     */
    boolean contentFilter() default true;
}