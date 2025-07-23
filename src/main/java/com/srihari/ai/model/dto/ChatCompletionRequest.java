package com.srihari.ai.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatCompletionRequest {
    private String message;
    private String model;
    private Double temperature;
}