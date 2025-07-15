package com.srihari.ai.controller;

import com.srihari.ai.model.ConversationModel;
import com.srihari.ai.service.ChatFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatFlowService chatFlowService;

    @GetMapping("/chat")
    public Mono<String> getChat(Model model, ServerWebExchange exchange) {
        return chatFlowService.loadChat(model, exchange)
                .onErrorResume(e -> chatFlowService.fallback(model, "GET /chat", e));
    }

    @PostMapping("/chat")
    public Mono<String> postChat(@ModelAttribute ConversationModel input,
                                 Model model,
                                 ServerWebExchange exchange) {
        return chatFlowService.processUserMessage(input, model, exchange)
                .onErrorResume(e -> chatFlowService.fallback(model, "POST /chat", e));
    }
}