package com.srihari.ai.service.chat;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.model.view.ConversationModel;
import com.srihari.ai.service.MemoryService;
import com.srihari.ai.service.ViewMappingService;
import com.srihari.ai.service.integration.OpenAiChatClient;
import com.srihari.ai.service.validation.InputValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebChatService {

    private final MemoryService memoryService;
    private final OpenAiChatClient chatClient;
    private final ViewMappingService viewMapper;
    private final StructuredLogger structuredLogger;
    private final CustomMetrics customMetrics;
    private final InputValidationService inputValidationService;

    public Mono<String> loadChat(Model model, ServerWebExchange exchange) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        structuredLogger.info("Loading chat page", Map.of(
            "operation", "load_chat",
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        customMetrics.recordUserSessionStarted("web-chat", "web-browser");
        
        return Mono.fromCallable(() -> {
            List<Message> messages = memoryService.loadConversation("default");
            return viewMapper.buildModel(model, messages);
        });
    }

    public Mono<String> processUserMessage(ConversationModel input, Model model, ServerWebExchange exchange) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        structuredLogger.info("Processing user message", Map.of(
            "operation", "process_user_message",
            "messageLength", input.getUserMessage() != null ? input.getUserMessage().length() : 0,
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));

        customMetrics.incrementMessageCount("web", "user");

        // Handle reset
        if (input.isReset()) {
            memoryService.reset("default");
            return loadChat(model, exchange);
        }

        // Validate input
        if (!StringUtils.hasText(input.getUserMessage())) {
            return loadChat(model, exchange);
        }

        // Validate user message
        InputValidationService.ValidationResult validationResult = 
            inputValidationService.validateUserMessage(input.getUserMessage());
        
        if (!validationResult.isValid()) {
            structuredLogger.warn("Invalid user message", Map.of(
                "operation", "validation_failed",
                "errors", validationResult.getErrorMessage(),
                "correlationId", correlationId != null ? correlationId : "unknown"
            ));
            return loadChat(model, exchange);
        }

        return Mono.fromCallable(() -> {
            // Load existing conversation
            List<Message> existingMessages = memoryService.loadConversation("default");
            
            // Add system message if provided
            List<Message> allMessages = new java.util.ArrayList<>(existingMessages);
            if (StringUtils.hasText(input.getSystemMessage())) {
                allMessages.add(0, new org.springframework.ai.chat.messages.SystemMessage(input.getSystemMessage()));
            }
            
            // Add user message
            allMessages.add(new org.springframework.ai.chat.messages.UserMessage(validationResult.getSanitizedContent()));
            
            return allMessages;
        })
        .flatMap(messages -> chatClient.sendComplete(messages))
        .flatMap(response -> {
            return Mono.fromCallable(() -> {
                // Load updated conversation and add AI response
                List<Message> updatedMessages = memoryService.loadConversation("default");
                List<Message> allMessages = new java.util.ArrayList<>(updatedMessages);
                
                // Add system message if provided
                if (StringUtils.hasText(input.getSystemMessage())) {
                    allMessages.add(0, new org.springframework.ai.chat.messages.SystemMessage(input.getSystemMessage()));
                }
                
                // Add user message and AI response
                allMessages.add(new org.springframework.ai.chat.messages.UserMessage(validationResult.getSanitizedContent()));
                allMessages.add(new org.springframework.ai.chat.messages.AssistantMessage(response));
                
                // Save updated conversation
                memoryService.save("default", allMessages);
                
                return viewMapper.buildModel(model, allMessages);
            });
        })
        .doOnSuccess(result -> {
            customMetrics.incrementMessageCount("web", "assistant");
            structuredLogger.info("User message processed successfully", Map.of(
                "operation", "process_user_message_success",
                "correlationId", correlationId != null ? correlationId : "unknown"
            ));
        });
    }

    public Mono<String> fallback(Model model, String operation, Throwable error) {
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        structuredLogger.error("Web chat operation failed", Map.of(
            "operation", "web_chat_fallback",
            "originalOperation", operation,
            "errorType", error.getClass().getSimpleName(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ), error);

        customMetrics.incrementAiErrors("web-chat", "web_error", error.getClass().getSimpleName());
        
        return Mono.fromCallable(() -> viewMapper.buildModel(model, List.of()));
    }
}