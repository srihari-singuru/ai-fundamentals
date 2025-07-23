package com.srihari.ai.service;

import com.srihari.ai.common.Constants;
import com.srihari.ai.model.view.ChatMessageView;
import com.srihari.ai.util.SafeEvaluator;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

@Service
public class ViewMappingService {
    public String buildModel(Model model, List<Message> messages) {
        Collections.reverse(messages);
        List<ChatMessageView> views = messages.stream()
                .map(m -> new ChatMessageView(
                        SafeEvaluator.eval(() -> m.getClass().getSimpleName(), "Unknown"),
                        SafeEvaluator.eval(m::getText, "[Unreadable]")
                ))
                .toList();

        String initialSystemMessage = messages.stream()
                .filter(m -> m instanceof SystemMessage)
                .map(m -> ((SystemMessage) m).getText())
                .findFirst()
                .orElse(Constants.DEFAULT_SYSTEM_MESSAGE);

        model.addAttribute(Constants.CHAT_VIEW_ATTR, views);
        model.addAttribute(Constants.INITIAL_SYSTEM_MESSAGE_ATTR, initialSystemMessage);
        return Constants.CHAT_VIEW;
    }
}
