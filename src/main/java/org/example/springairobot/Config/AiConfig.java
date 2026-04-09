package org.example.springairobot.Config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * RAG 顾问（使用 RetrievalAugmentationAdvisor）
     */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore) {
        VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5)
                .topK(3)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .build();
    }

    /**
     * 消息记忆顾问
     * ChatMemory Bean 由 Spring Boot 自动配置提供（JdbcChatMemoryRepository + MessageWindowChatMemory）
     */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    /**
     * 基础 ChatClient（包含记忆）
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 MessageChatMemoryAdvisor memoryAdvisor) {
        return builder
                .defaultAdvisors(memoryAdvisor)
                .build();
    }

    /**
     * RAG 增强 ChatClient
     */
    @Bean
    public ChatClient ragChatClient(ChatClient.Builder builder,
                                    MessageChatMemoryAdvisor memoryAdvisor,
                                    RetrievalAugmentationAdvisor ragAdvisor) {
        return builder
                .defaultAdvisors(memoryAdvisor, ragAdvisor)
                .build();
    }
}