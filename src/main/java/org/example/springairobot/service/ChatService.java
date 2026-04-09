package org.example.springairobot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {
    private final ChatClient chatClient;
    private final ChatClient ragChatClient;   // RAG


    public ChatService(@Qualifier("chatClient") ChatClient chatClient,
                       @Qualifier("ragChatClient") ChatClient ragChatClient) {
        this.chatClient = chatClient;
        this.ragChatClient = ragChatClient;
    }

    // 同步对话（支持会话）
    public String chat(String sessionId, String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("sessionId", sessionId))
                .call()
                .content();
    }

    // 流式对话
    public Flux<String> chatStream(String sessionId, String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("sessionId", sessionId))
                .stream()
                .content();
    }

    // RAG 对话（同样使用 advisor，因为 QuestionAnswerAdvisor 已注册）
    public String ragChat(String sessionId, String userMessage) {
        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("sessionId", sessionId))
                .call()
                .content();
    }

    public Flux<String> ragChatStream(String sessionId, String userMessage) {
        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("sessionId", sessionId))
                .stream()
                .content();
    }
}