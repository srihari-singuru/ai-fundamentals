package com.srihari.ai.controller.api;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.srihari.ai.metrics.ChatMetrics;
import com.srihari.ai.model.dto.ChatCompletionRequest;
import com.srihari.ai.service.chat.ApiChatService;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class ChatApiController {

    private final ApiChatService chatService;
    private final ChatMetrics chatMetrics;

    @PostMapping(value = "/chat-completion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Flux<String> postChatCompletion(@Valid @RequestBody ChatCompletionRequest request) {
        Timer.Sample sample = chatMetrics.startApiTimer();
        chatMetrics.incrementApiRequests();
        
        log.info("Chat completion request received - messageLength: {}, model: {}", 
                request.getMessage().length(), request.getModel());
        
        return chatService.generateResponse(request.getMessage())
                .doOnComplete(() -> {
                    chatMetrics.recordApiDuration(sample);
                    log.info("Chat completion request completed successfully");
                })
                .doOnError(error -> {
                    chatMetrics.incrementErrors("api_error");
                    log.error("Chat completion request failed: {}", error.getMessage());
                });
    }
}