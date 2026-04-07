package org.example.springairobot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {
    private final ChatClient chatClient;
    private final ChatClient ragChatClient;
    private final VectorStore vectorStore;

    public ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.ragChatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    // 同步对话
    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    // 流式对话
    public Flux<String> chatStream(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();
    }

    /**
     * RAG 问答（基于知识库）
     */
    public String ragChat(String userMessage) {
        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .call()
                .content();
    }

    /**
     * RAG 流式问答
     */
    public Flux<String> ragChatStream(String userMessage) {
        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .stream()
                .content();
    }
}