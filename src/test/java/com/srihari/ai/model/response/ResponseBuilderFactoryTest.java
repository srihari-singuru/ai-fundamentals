package com.srihari.ai.model.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponseBuilderFactoryTest {
    
    private ResponseBuilderFactory factory;
    
    @BeforeEach
    void setUp() {
        factory = new ResponseBuilderFactory();
    }
    
    @Test
    void shouldCreateSuccessResponse() {
        String data = "Hello, World!";
        
        ApiResponse<String> response = factory.successResponse(data);
        
        assertNotNull(response);
        assertEquals(ApiResponseStatus.SUCCESS, response.getStatus());
        assertEquals(data, response.getData());
        assertTrue(response.isSuccess());
        assertFalse(response.hasErrors());
    }
    
    @Test
    void shouldCreateErrorResponse() {
        String errorCode = "INVALID_INPUT";
        String errorMessage = "The input is invalid";
        
        ApiResponse<String> response = factory.errorResponse(errorCode, errorMessage);
        
        assertNotNull(response);
        assertEquals(ApiResponseStatus.ERROR, response.getStatus());
        assertFalse(response.isSuccess());
        assertTrue(response.hasErrors());
        
        ErrorInfo error = response.getFirstError();
        assertNotNull(error);
        assertEquals(errorCode, error.getCode());
        assertEquals(errorMessage, error.getMessage());
    }
    
    @Test
    void shouldCreateErrorResponseFromException() {
        Exception exception = new RuntimeException("Something went wrong");
        
        ApiResponse<String> response = factory.errorResponse(exception);
        
        assertNotNull(response);
        assertEquals(ApiResponseStatus.ERROR, response.getStatus());
        assertFalse(response.isSuccess());
        assertTrue(response.hasErrors());
        
        ErrorInfo error = response.getFirstError();
        assertNotNull(error);
        assertEquals("INTERNAL_ERROR", error.getCode());
        assertEquals("Something went wrong", error.getMessage());
    }
    
    @Test
    void shouldCreateSuccessBuilder() {
        ResponseBuilder<String> builder = factory.success();
        
        assertNotNull(builder);
        assertTrue(builder instanceof SuccessResponseBuilder);
    }
    
    @Test
    void shouldCreateErrorBuilder() {
        ResponseBuilder<String> builder = factory.error();
        
        assertNotNull(builder);
        assertTrue(builder instanceof ErrorResponseBuilder);
    }
    
    @Test
    void shouldCreateStreamingBuilder() {
        StreamingResponseBuilder<String> builder = factory.streaming();
        
        assertNotNull(builder);
        assertTrue(builder instanceof StreamingResponseBuilder);
    }
}