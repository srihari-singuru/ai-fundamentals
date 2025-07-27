package com.srihari.ai.controller.api;

import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srihari.ai.common.CorrelationIdHolder;
import com.srihari.ai.common.StructuredLogger;
import com.srihari.ai.event.ChatEventPublisher;
import com.srihari.ai.event.ConversationEvent;
import com.srihari.ai.metrics.CustomMetrics;
import com.srihari.ai.model.dto.ChatCompletionRequest;
import com.srihari.ai.service.chat.ApiChatService;
import com.srihari.ai.service.validation.InputValidationService;

import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Chat API", description = "AI-powered chat completion endpoints with streaming responses")
public class ChatApiController {

    private final ApiChatService chatService;
    private final CustomMetrics customMetrics;
    private final StructuredLogger structuredLogger;
    private final InputValidationService inputValidationService;
    private final ChatEventPublisher eventPublisher;

    @Operation(
        summary = "Generate AI chat completion",
        description = """
            Generates an AI-powered chat completion using OpenAI's GPT models with real-time streaming response.
            
            ## Features
            - Real-time token streaming using Server-Sent Events
            - Conversation memory and context preservation
            - Input validation and sanitization
            - Comprehensive error handling and resilience patterns
            - Request correlation tracking for observability
            
            ## Rate Limiting
            This endpoint is subject to rate limiting. Please refer to response headers for current limits.
            
            ## Streaming Response
            The response is streamed as individual tokens, allowing for real-time display of the AI's response.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful chat completion with streaming response",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", description = "Streaming AI response tokens"),
                examples = @ExampleObject(
                    name = "Streaming Response",
                    description = "Individual tokens streamed in real-time",
                    value = "Hello! How can I assist you today?"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters or validation failure",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = "{\"error\": \"Invalid input: Message cannot be empty\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit exceeded",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Rate Limit Error",
                    value = "{\"error\": \"Rate limit exceeded. Please try again later.\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error or AI service unavailable",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Service Error",
                    value = "{\"error\": \"AI service temporarily unavailable\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Service unavailable due to circuit breaker",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Circuit Breaker",
                    value = "{\"error\": \"Service temporarily unavailable due to high error rate\"}"
                )
            )
        )
    })
    @PostMapping(value = "/chat-completion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Flux<String> postChatCompletion(
        @Parameter(
            description = "Chat completion request with message and optional parameters",
            required = true,
            schema = @Schema(implementation = ChatCompletionRequest.class)
        )
        @Valid @RequestBody ChatCompletionRequest request) {
        Timer.Sample sample = customMetrics.startApiTimer();
        customMetrics.incrementConversationStarted("api");
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        String conversationId = UUID.randomUUID().toString();
        
        // Publish conversation started event
        eventPublisher.publishEventAsync(ConversationEvent.started(
            correlationId, conversationId, "anonymous", "api", 
            Map.of("model", request.getModel())
        ));
        
        structuredLogger.info("Chat completion request received", Map.of(
            "operation", "chat_completion",
            "messageLength", request.getMessage().length(),
            "model", request.getModel(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        ));
        
        // Additional validation and sanitization
        InputValidationService.ValidationResult validationResult = inputValidationService.validateUserMessage(request.getMessage());
        if (!validationResult.isValid()) {
            structuredLogger.warn("Chat completion request validation failed", Map.of(
                "operation", "chat_completion_validation",
                "violations", validationResult.getErrorMessage(),
                "correlationId", correlationId != null ? correlationId : "unknown"
            ));
            return Flux.error(new IllegalArgumentException("Invalid input: " + validationResult.getErrorMessage()));
        }
        
        // Use sanitized input for processing
        String sanitizedMessage = validationResult.getSanitizedContent() != null ? 
            validationResult.getSanitizedContent() : request.getMessage();
        
        return chatService.generateResponse(sanitizedMessage)
                .doOnComplete(() -> {
                    customMetrics.recordApiDuration(sample, "chat_completion", "api");
                    customMetrics.incrementConversationEnded("api", 1); // Assume 1 minute for single request
                    
                    // Publish conversation ended event
                    eventPublisher.publishEventAsync(ConversationEvent.ended(
                        correlationId, conversationId, "anonymous", "api", 
                        1, 60000, // 1 message, ~1 minute duration
                        Map.of("model", request.getModel(), "status", "success")
                    ));
                    
                    structuredLogger.info("Chat completion request completed successfully", Map.of(
                        "operation", "chat_completion",
                        "status", "success",
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ));
                })
                .doOnError(error -> {
                    customMetrics.incrementAiErrors(request.getModel(), "api_error", "unknown");
                    
                    // Publish conversation ended event with error
                    eventPublisher.publishEventAsync(ConversationEvent.ended(
                        correlationId, conversationId, "anonymous", "api", 
                        1, System.currentTimeMillis() - System.currentTimeMillis(), // Duration calculation would be more accurate in real app
                        Map.of("model", request.getModel(), "status", "error", "errorType", error.getClass().getSimpleName())
                    ));
                    
                    structuredLogger.error("Chat completion request failed", Map.of(
                        "operation", "chat_completion",
                        "status", "error",
                        "errorType", error.getClass().getSimpleName(),
                        "correlationId", correlationId != null ? correlationId : "unknown"
                    ), error);
                });
    }
}