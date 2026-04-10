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
    // ==================== 普通对话 ====================
    public String chat(String sessionId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, null);

        String assistantReply = chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .call()
                .content();

        // 持久化完整历史
        conversationService.savePair(effectiveSessionId, userMessage, assistantReply, null);
        return assistantReply;
    }

    public Flux<String> chatStream(String sessionId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, null);

        // 用于累积流式响应的 StringBuilder
        StringBuilder fullReplyBuilder = new StringBuilder();

        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .stream()
                .content()
                .doOnNext(chunk -> fullReplyBuilder.append(chunk))
                .doOnComplete(() -> {
                    String fullReply = fullReplyBuilder.toString();
                    conversationService.savePair(effectiveSessionId, userMessage, fullReply, null);
                });
    }

    // ==================== RAG 对话 ====================
    public String ragChat(String sessionId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, null);

        String assistantReply = ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", effectiveSessionId)
                        .param("sessionId", effectiveSessionId))
                .call()
                .content();

        conversationService.savePair(effectiveSessionId, userMessage, assistantReply, null);
        return assistantReply;
    }

    public Flux<String> ragChatStream(String sessionId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, null);

        StringBuilder fullReplyBuilder = new StringBuilder();

        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", effectiveSessionId)
                        .param("sessionId", effectiveSessionId))
                .stream()
                .content()
                .doOnNext(chunk -> fullReplyBuilder.append(chunk))
                .doOnComplete(() -> {
                    String fullReply = fullReplyBuilder.toString();
                    conversationService.savePair(effectiveSessionId, userMessage, fullReply, null);
                });
    }
}