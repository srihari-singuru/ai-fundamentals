package com.srihari.ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/v1")
@Slf4j
public class ChatCompletionController {
    private final ChatClient chatClient;

    public ChatCompletionController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping(value = "/ai-chat")     // produces = "text/event-stream" for SSE
    public Flux<String> getResponse() {
        String promptMessage = "Explain the concept of AI in simple terms.";
        return chatClient.prompt(promptMessage)
                .stream()
                .content()
                .onErrorResume(ex -> Flux.just("Exception: " + extractErrorMessage(ex)));
    }

    private String extractErrorMessage(Throwable ex) {
        log.error("Error occurred while processing chat completion request", ex);
        return ex.getMessage();
    }
}
