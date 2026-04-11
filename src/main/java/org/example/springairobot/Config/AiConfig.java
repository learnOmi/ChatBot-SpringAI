package org.example.springairobot.Config;

import org.example.springairobot.RagOpt.ReRanker.OllamaReRankerDocumentPostProcessor;
import org.example.springairobot.RagOpt.Retriever.BM25DocumentRetriever;
import org.example.springairobot.RagOpt.Retriever.MultiQueryDocumentRetrieverAdapter;
import org.example.springairobot.RagOpt.Transformer.ContextualQueryTransformer;
import org.example.springairobot.service.ConversationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Configuration
public class AiConfig {

    /**
     * RAG 顾问（使用 RetrievalAugmentationAdvisor）
     */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(QueryTransformer queryTransformer, DocumentRetriever hybridRetriever, DocumentPostProcessor ollamaReRanker) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(hybridRetriever)
                .queryTransformers(queryTransformer)
                .documentPostProcessors(ollamaReRanker)
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
     * 创建并配置一个BM25DocumentRetriever Bean，用于文档检索
     * BM25是一种常用的信息检索算法，用于计算文档与查询的相关性得分
     *
     * @param jdbcTemplate Spring的JdbcTemplate对象，用于数据库操作
     * @return 配置好的BM25DocumentRetriever实例，参数5可能表示BM25算法中的参数k1
     */
    @Bean
    public BM25DocumentRetriever bm25Retriever(JdbcTemplate jdbcTemplate) {
    // 使用JdbcTemplate和参数5创建BM25DocumentRetriever实例
        return new BM25DocumentRetriever(jdbcTemplate, 5);
    }

    /**
     * 创建并配置一个VectorStoreDocumentRetriever Bean
     * 该Bean用于根据向量相似度检索文档
     *
     * @param vectorStore 向量存储对象，用于存储和检索文档向量
     * @return 配置好的VectorStoreDocumentRetriever实例
     */
    @Bean
    public VectorStoreDocumentRetriever vectorRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
            // 设置向量存储对象
                .vectorStore(vectorStore)
            // 设置相似度阈值为0.5，只有相似度大于此值的文档才会被检索到
                .similarityThreshold(0.5)
            // 设置检索结果返回前5个最相似的文档
                .topK(5)
            // 构建并返回VectorStoreDocumentRetriever实例
                .build();
    }

    /**
     * 创建并配置一个MultiQueryExpander Bean
     * MultiQueryExpander用于扩展查询，可能会将一个查询分解为多个子查询进行处理
     *
     * @param chatClientBuilder Spring AI的ChatClient构建器，用于构建聊天客户端
     * @return 配置好的MultiQueryExpander实例
     */
    @Bean
    public MultiQueryExpander multiQueryExpander(ChatClient.Builder chatClientBuilder) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder.clone()) // 避免污染全局Builder，使用克隆的Builder实例
                .numberOfQueries(3) // 设置查询数量为3，即每个查询会被扩展为3个子查询
                .build();
    }

    /**
     * 创建一个混合文档检索器，结合了向量检索和BM25检索的优点
     *
     * @param expander 多查询扩展器，用于生成多个查询变体
     * @param vectorRetriever 基于向量存储的文档检索器
     * @param bm25Retriever 基于BM25算法的文档检索器
     * @return 配置好的混合文档检索器实例
     */
    @Bean
    public DocumentRetriever hybridRetriever(MultiQueryExpander expander,
                                             VectorStoreDocumentRetriever vectorRetriever,
                                             BM25DocumentRetriever bm25Retriever) {
        // 将两个检索器放入列表，传给适配器
        List<DocumentRetriever> retrievers = List.of(vectorRetriever, bm25Retriever);
        return new MultiQueryDocumentRetrieverAdapter(expander, retrievers);
    }

    // 利用DocumentPostProcessor实现的自定义轻量级ReRanker
    @Bean
    public DocumentPostProcessor ollamaReRanker(ChatClient.Builder chatClientBuilder) {
        ChatClient.Builder clonedBuilder = chatClientBuilder.clone();
        return new OllamaReRankerDocumentPostProcessor(clonedBuilder, 3);
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

    /**
     * 视觉模型专用 ChatClient
     */
    @Bean
    @Qualifier("visionChatClient")
    public ChatClient visionChatClient(ChatClient.Builder builder,
                                       MessageChatMemoryAdvisor memoryAdvisor) {
        return builder
                .defaultOptions(ChatOptions.builder()
                        .model("llava")      // 指定视觉模型
                        .temperature(0.7)
                        .build())
                .defaultSystem("你是一个乐于助人的助手。请始终使用简体中文回答用户的问题，不要使用英文。")
                .defaultAdvisors(memoryAdvisor)  // 可共享记忆顾问
                .build();
    }
}