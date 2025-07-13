package com.srihari.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatCompletionController {
    private final ChatClient chatClient;

    public ChatCompletionController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/demo")
    public Flux<String> getResponse() {
        String promptMessage = "Write a small bedtime story about a unicorn";
        return chatClient.prompt(promptMessage)
                .stream()
                .content();
    }
}
