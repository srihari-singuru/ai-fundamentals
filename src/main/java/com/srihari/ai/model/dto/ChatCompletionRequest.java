package com.srihari.ai.model.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Pattern;

import com.srihari.ai.service.validation.annotation.ValidChatMessage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(
    name = "ChatCompletionRequest",
    description = "Request payload for AI chat completion with validation and security features"
)
public class ChatCompletionRequest {
    
    @Schema(
        description = "The user message to send to the AI model",
        example = "Hello, how can you help me with Spring Boot development?",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 1,
        maxLength = 4000
    )
    @NotBlank(message = "Message cannot be empty")
    @ValidChatMessage(maxLength = 4000, securityCheck = true, contentFilter = true)
    private String message;
    
    @Schema(
        description = "The AI model to use for completion. Must be a valid GPT model identifier.",
        example = "gpt-4.1-nano",
        defaultValue = "gpt-4.1-nano",
        allowableValues = {"gpt-4.1-nano", "gpt-4", "gpt-3.5-turbo"}
    )
    @Pattern(regexp = "gpt-.*", message = "Invalid model format. Must start with 'gpt-'")
    private String model = "gpt-4.1-nano";
    
    @Schema(
        description = "Controls randomness in the AI response. Lower values make responses more focused and deterministic.",
        example = "0.7",
        defaultValue = "0.7",
        minimum = "0.0",
        maximum = "2.0"
    )
    @DecimalMin(value = "0.0", message = "Temperature must be between 0.0 and 2.0")
    @DecimalMax(value = "2.0", message = "Temperature must be between 0.0 and 2.0")
    private Double temperature = 0.7;
}