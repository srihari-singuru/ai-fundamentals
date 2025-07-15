package com.srihari.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemoryService {
    private final ChatMemoryRepository repo;

    public List<Message> loadConversation(String id) {
        try {
            return repo.findByConversationId(id);
        } catch (Exception e) {
            log.error("Failed to load memory for {}", id, e);
            return List.of();
        }
    }

    public void save(String id, List<Message> messages) {
        repo.deleteByConversationId(id);
        repo.saveAll(id, messages);
    }

    public void reset(String id) {
        repo.deleteByConversationId(id);
    }
}