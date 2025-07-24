package com.srihari.ai.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import com.srihari.ai.model.dto.ErrorResponse;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation error on path {}: {}", exchange.getRequest().getPath(), errors);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR", 
            "Invalid input: " + errors.toString()
        );
        errorResponse.setPath(exchange.getRequest().getPath().toString());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, ServerWebExchange exchange) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Method argument validation error: {}", errors);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR", 
            "Invalid request parameters: " + errors.toString()
        );
        errorResponse.setPath(exchange.getRequest().getPath().toString());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleCircuitBreakerOpen(
            CallNotPermittedException ex, ServerWebExchange exchange) {
        
        log.error("Circuit breaker is open: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "SERVICE_UNAVAILABLE", 
            "AI service is temporarily unavailable. Please try again later."
        );
        errorResponse.setPath(exchange.getRequest().getPath().toString());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, ServerWebExchange exchange) {
        
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "BAD_REQUEST", 
            ex.getMessage()
        );
        errorResponse.setPath(exchange.getRequest().getPath().toString());
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        
        log.error("Unexpected error on path {}: {}", 
                exchange.getRequest().getPath(), ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR", 
            "An unexpected error occurred. Please try again later."
        );
        errorResponse.setPath(exchange.getRequest().getPath().toString());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}