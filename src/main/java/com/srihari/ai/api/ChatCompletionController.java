package com.srihari.ai.api;

import com.srihari.ai.service.ChatCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class ChatCompletionController {

    private final ChatCompletionService chatService;

    @GetMapping(value = "/chat-completion")     // produces = MediaType.TEXT_EVENT_STREAM_VALUE - for SSE
    public Flux<String> getChatCompletion(@RequestParam("user-message") String userMessage) {
        return chatService.generateResponse(userMessage);
    }

    @PostMapping(value = "/chat-completion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Flux<String> postChatCompletion(@RequestBody String userMessage) {
        return chatService.generateResponse(userMessage);
    }
}