package com.srihari.ai.model.view;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatMessageView {
    private String role;
    private String content;
}