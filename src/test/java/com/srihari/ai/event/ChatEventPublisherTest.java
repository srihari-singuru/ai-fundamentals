package com.srihari.ai.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.srihari.ai.common.StructuredLogger;

@ExtendWith(MockitoExtension.class)
class ChatEventPublisherTest {

    @Mock
    private StructuredLogger structuredLogger;
    
    @Mock
    private Executor taskExecutor;
    
    private ChatEventPublisher eventPublisher;
    private TestChatEventListener testListener;

    @BeforeEach
    void setUp() {
        testListener = new TestChatEventListener();
        eventPublisher = new ChatEventPublisher(
            List.of(testListener), 
            structuredLogger, 
            taskExecutor
        );
    }

    @Test
    void shouldPublishConversationStartedEvent() {
        // Given
        ConversationEvent event = ConversationEvent.started(
            "test-correlation-id", 
            "test-conversation-id", 
            "test-user", 
            "api", 
            Map.of("model", "gpt-4")
        );

        // When
        eventPublisher.publishEvent(event);

        // Then
        assertEquals(1, testListener.conversationStartedCount);
        assertEquals(1, testListener.chatEventCount);
        assertNotNull(testListener.lastEvent);
        assertEquals(ChatEventType.CONVERSATION_STARTED, testListener.lastEvent.getEventType());
    }

    @Test
    void shouldPublishMessageReceivedEvent() {
        // Given
        MessageEvent event = MessageEvent.received(
            "test-correlation-id", 
            "test-conversation-id", 
            "test-message-id", 
            "test-user", 
            "api", 
            100, 
            Map.of("model", "gpt-4")
        );

        // When
        eventPublisher.publishEvent(event);

        // Then
        assertEquals(1, testListener.messageReceivedCount);
        assertEquals(1, testListener.chatEventCount);
        assertNotNull(testListener.lastEvent);
        assertEquals(ChatEventType.MESSAGE_RECEIVED, testListener.lastEvent.getEventType());
    }

    @Test
    void shouldGetActiveListenerCount() {
        // When
        int count = eventPublisher.getActiveListenerCount();

        // Then
        assertEquals(1, count);
    }

    @Test
    void shouldGetListenerInfo() {
        // When
        List<String> info = eventPublisher.getListenerInfo();

        // Then
        assertEquals(1, info.size());
        assertEquals("TestChatEventListener (priority: 0, enabled: true)", info.get(0));
    }

    @Test
    void shouldPublishEventAsync() {
        // Given
        ConversationEvent event = ConversationEvent.started(
            "test-correlation-id", 
            "test-conversation-id", 
            "test-user", 
            "api", 
            Map.of("model", "gpt-4")
        );

        // When
        eventPublisher.publishEventAsync(event);

        // Then
        verify(taskExecutor, times(1)).execute(any(Runnable.class));
    }

    private static class TestChatEventListener implements ChatEventListener {
        int conversationStartedCount = 0;
        int messageReceivedCount = 0;
        int chatEventCount = 0;
        ChatEvent lastEvent;

        @Override
        public void onConversationStarted(ConversationEvent event) {
            conversationStartedCount++;
            lastEvent = event;
        }

        @Override
        public void onMessageReceived(MessageEvent event) {
            messageReceivedCount++;
            lastEvent = event;
        }

        @Override
        public void onChatEvent(ChatEvent event) {
            chatEventCount++;
            lastEvent = event;
        }
    }
}