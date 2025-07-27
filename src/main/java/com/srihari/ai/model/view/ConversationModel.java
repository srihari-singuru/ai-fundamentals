package com.srihari.ai.model.view;

import com.srihari.ai.service.validation.annotation.ValidChatMessage;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationModel {
    
    @Size(max = 1000, message = "System message exceeds maximum length of 1000 characters")
    private String systemMessage;
    
    @ValidChatMessage(maxLength = 4000, securityCheck = true, contentFilter = true)
    private String userMessage;
    
    private boolean reset;
}