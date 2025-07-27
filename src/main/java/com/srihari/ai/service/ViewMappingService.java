package com.srihari.ai.service;

import java.util.Collections;
import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.srihari.ai.common.Constants;
import com.srihari.ai.model.view.ChatMessageView;
import com.srihari.ai.util.SafeEvaluator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewMappingService {

    public String buildModel(Model model, List<Message> messages) {
        Collections.reverse(messages);
        
        List<ChatMessageView> views = messages.stream()
                .map(m -> new ChatMessageView(
                        SafeEvaluator.eval(() -> m.getClass().getSimpleName(), "Unknown"),
                        SafeEvaluator.eval(m::getText, "[Unreadable]")
                ))
                .toList();
        
        model.addAttribute("chatView", views);
        model.addAttribute("conversationId", java.util.UUID.randomUUID().toString());
        model.addAttribute("initialSystemMessage", Constants.DEFAULT_SYSTEM_MESSAGE);
        
        return "chat";
    }
}