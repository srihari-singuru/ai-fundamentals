package com.srihari.ai.service.chat;

import com.srihari.ai.common.Constants;
import com.srihari.ai.model.view.ConversationModel;
import com.srihari.ai.service.MemoryService;
import com.srihari.ai.service.ViewMappingService;
import com.srihari.ai.service.integration.OpenAiChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebChatService {

    private final MemoryService memoryService;
    private final OpenAiChatClient chatClient;
    private final ViewMappingService viewMapper;

    public Mono<String> loadChat(Model model, ServerWebExchange exchange) {
        return getSessionId(exchange).flatMap(conversationId -> {
            model.addAttribute(Constants.CONVERSATION_ID_ATTR, conversationId);
            return Mono.fromCallable(() -> memoryService.loadConversation(conversationId))
                    .map(messages -> viewMapper.buildModel(model, messages));
        });
    }

    public Mono<String> processUserMessage(ConversationModel input,
            Model model,
            ServerWebExchange exchange) {
        return getSessionId(exchange).flatMap(conversationId -> {
            model.addAttribute(Constants.CONVERSATION_ID_ATTR, conversationId);

            return Mono.fromCallable(() -> {
                if (input.isReset()) {
                    memoryService.reset(conversationId);
                }

                List<Message> history = new ArrayList<>(memoryService.loadConversation(conversationId));
                if (history.stream().noneMatch(m -> m instanceof SystemMessage)) {
                    history.add(new SystemMessage(input.getSystemMessage()));
                }
                history.add(new UserMessage(input.getUserMessage()));
                return history;
            })
                    .flatMap(messages -> chatClient.sendComplete(messages)
                            .map(reply -> {
                                messages.add(new AssistantMessage(reply));
                                memoryService.save(conversationId, messages);
                                return viewMapper.buildModel(model, messages);
                            }));
        });
    }

    public Mono<String> fallback(Model model, String context, Throwable e) {
        log.error("Fallback triggered in {}: {}", context, e.getMessage(), e);
        model.addAttribute(Constants.CHAT_VIEW_ATTR, List.of());
        model.addAttribute(Constants.INITIAL_SYSTEM_MESSAGE_ATTR, Constants.DEFAULT_SYSTEM_MESSAGE);
        return Mono.just(Constants.CHAT_VIEW);
    }

    private Mono<String> getSessionId(ServerWebExchange exchange) {
        return exchange.getSession()
                .map(session -> {
                    String id = (String) session.getAttributes().get(Constants.CONVERSATION_ID_ATTR);
                    if (id == null) {
                        id = UUID.randomUUID().toString();
                        session.getAttributes().put(Constants.CONVERSATION_ID_ATTR, id);
                    }
                    return id;
                });
    }
}