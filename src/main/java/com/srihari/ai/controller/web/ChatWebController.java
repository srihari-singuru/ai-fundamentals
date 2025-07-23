package com.srihari.ai.controller.web;

import com.srihari.ai.model.view.ConversationModel;
import com.srihari.ai.service.chat.WebChatService;
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
public class ChatWebController {

    private final WebChatService webChatService;

    @GetMapping("/chat")
    public Mono<String> getChat(Model model, ServerWebExchange exchange) {
        return webChatService.loadChat(model, exchange)
                .onErrorResume(e -> webChatService.fallback(model, "GET /chat", e));
    }

    @PostMapping("/chat")
    public Mono<String> postChat(@ModelAttribute ConversationModel input,
            Model model,
            ServerWebExchange exchange) {
        return webChatService.processUserMessage(input, model, exchange)
                .onErrorResume(e -> webChatService.fallback(model, "POST /chat", e));
    }
}