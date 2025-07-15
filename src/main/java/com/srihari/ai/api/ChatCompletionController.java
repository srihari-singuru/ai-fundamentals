package com.srihari.ai.api;

import com.srihari.ai.service.ChatCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class ChatCompletionController {

    private final ChatCompletionService chatService;

    @GetMapping(value = "/chat-completion")     // produces = "text/event-stream") - for SSE
    public Flux<String> getChatCompletion(@RequestParam("user-message") String userMessage) {
        return chatService.generateResponse(userMessage);
    }
}