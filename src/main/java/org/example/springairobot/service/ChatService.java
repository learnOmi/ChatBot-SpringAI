package org.example.springairobot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {
    private final ChatClient chatClient;
    private final ChatClient ragChatClient;
    private final ConversationService conversationService;

    public ChatService(@Qualifier("chatClient") ChatClient chatClient,
                       @Qualifier("ragChatClient") ChatClient ragChatClient,
                       ConversationService conversationService) {
        this.chatClient = chatClient;
        this.ragChatClient = ragChatClient;
        this.conversationService = conversationService;
    }

    // 同步对话（记忆由 MessageChatMemoryAdvisor 自动管理）
    public String chat(String sessionId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, null);
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .call()
                .content();
    }

    // 流式对话
    public Flux<String> chatStream(String sessionId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, null);
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .stream()
                .content();
    }

    // RAG 同步对话
    public String ragChat(String sessionId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, null);
        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .call()
                .content();
    }

    // RAG 流式对话
    public Flux<String> ragChatStream(String sessionId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, null);
        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .stream()
                .content();
    }
}