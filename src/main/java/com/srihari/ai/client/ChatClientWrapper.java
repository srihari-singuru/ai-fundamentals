package com.srihari.ai.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ChatClientWrapper {

    private final ChatClient chatClient;

    public ChatClientWrapper (ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public Flux<String> send(String message) {
        return chatClient.prompt(new Prompt(new UserMessage(message)))
                .stream()
                .content();
    }
}
