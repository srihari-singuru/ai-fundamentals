package com.srihari.ai.model.view;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationModel {
    private String systemMessage;
    private String userMessage;
    private boolean reset;
}