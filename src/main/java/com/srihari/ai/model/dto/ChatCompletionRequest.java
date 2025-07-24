package com.srihari.ai.model.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ChatCompletionRequest {
    
    @NotBlank(message = "Message cannot be empty")
    @Size(max = 4000, message = "Message exceeds maximum length of 4000 characters")
    private String message;
    
    @Pattern(regexp = "gpt-.*", message = "Invalid model format. Must start with 'gpt-'")
    private String model = "gpt-4.1-nano";
    
    @DecimalMin(value = "0.0", message = "Temperature must be between 0.0 and 2.0")
    @DecimalMax(value = "2.0", message = "Temperature must be between 0.0 and 2.0")
    private Double temperature = 0.7;
}