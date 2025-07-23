package com.srihari.ai.controller.api;

import com.srihari.ai.model.dto.ChatCompletionRequest;
import com.srihari.ai.service.chat.ApiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class ChatApiController {

    private final ApiChatService chatService;

    @PostMapping(value = "/chat-completion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Flux<String> postChatCompletion(@RequestBody ChatCompletionRequest request) {
        return chatService.generateResponse(request.getMessage());
    }
}