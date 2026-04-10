package org.example.springairobot.Config;

import org.example.springairobot.RagOpt.Retriever.MultiQueryDocumentRetrieverAdapter;
import org.example.springairobot.RagOpt.Transformer.ContextualQueryTransformer;
import org.example.springairobot.service.ConversationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
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
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(QueryTransformer queryTransformer, DocumentRetriever multiQueryDocumentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(multiQueryDocumentRetriever)
                .queryTransformers(queryTransformer)
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
     * 创建并配置一个QueryTransformer类型的Bean
     * 这个Bean用于查询转换，将用户输入的查询转换为适合系统处理的格式
     *
     * @param builder ChatClient的构建器，用于构建聊天客户端
     * @param conversationService 对话服务，用于处理对话相关逻辑
     * @return 返回一个配置好的QueryTransformer实例，具体实现为ContextualQueryTransformer
     */
    @Bean
    public QueryTransformer queryTransformer(ChatClient.Builder builder, ConversationService conversationService) {
        return new ContextualQueryTransformer(builder, conversationService);
    }

    /**
     * 创建并配置一个多查询文档检索器，该检索器通过生成多个查询变体来提高检索效果
     *
     * @param chatClientBuilder 聊天客户端构建器，用于生成查询变体
     * @param vectorStore 向量存储，用于文档相似性检索
     * @return 配置好的多查询文档检索器实例
     */
    @Bean
    public DocumentRetriever multiQueryDocumentRetriever(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        // 1. 创建向量检索器
        VectorStoreDocumentRetriever vectorRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore) // 设置向量存储
                .similarityThreshold(0.5) // 设置相似度阈值为0.5
                .topK(5) // 为后续去重和重排留出空间，设置检索前5个最相似文档
                .build();

        // 2. 关键：基于原始 builder 创建一个独立的“副本” builder，避免污染全局配置
        ChatClient.Builder clonedBuilder = chatClientBuilder.clone();

        // 3. 创建多查询扩展器
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(clonedBuilder) // 设置聊天客户端构建器
                .numberOfQueries(3) // 生成 3 个查询变体，增加检索覆盖面
                .build();

        // 3. 使用自定义适配器组装多查询检索器
        return new MultiQueryDocumentRetrieverAdapter(queryExpander, vectorRetriever);
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