package com.srihari.ai.service.chat.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChatModelServiceTest {
    
    @Test
    void shouldCreateChatModelOptions() {
        // Test that ChatModelOptions can be created
        ChatModelOptions options = ChatModelOptions.forApi();
        
        assertNotNull(options);
        assertEquals(0.7, options.getTemperature());
        assertTrue(options.getStreaming());
    }
    
    @Test
    void shouldCreateDefaultOptions() {
        ChatModelOptions options = ChatModelOptions.defaultOptions();
        
        assertNotNull(options);
        assertEquals(0.7, options.getTemperature());
        assertTrue(options.getStreaming());
    }
    
    @Test
    void shouldCreateWebOptions() {
        ChatModelOptions options = ChatModelOptions.forWeb();
        
        assertNotNull(options);
        assertEquals(0.7, options.getTemperature());
        assertFalse(options.getStreaming());
    }
}