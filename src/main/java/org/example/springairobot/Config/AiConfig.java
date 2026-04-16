package org.example.springairobot.Config;

import org.example.springairobot.Advisor.ChatMemory.NoOpChatMemory;
import org.example.springairobot.Advisor.EvaluationAdvisor;
import org.example.springairobot.Advisor.SelfHealingAdvisor;
import org.example.springairobot.Advisor.SelfHealingRecursiveAdvisor;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.rag.reranker.OllamaReRankerDocumentPostProcessor;
import org.example.springairobot.service.rag.retriever.BM25DocumentRetriever;
import org.example.springairobot.service.rag.retriever.MultiQueryDocumentRetrieverAdapter;
import org.example.springairobot.service.rag.transformer.ContextualQueryTransformer;
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
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * AI配置类
 * 
 * 配置Spring AI的核心组件：
 * - ChatClient：各种对话客户端（普通对话、RAG对话、视觉对话等）
 * - Advisor：对话顾问（记忆、检索、评估、自修复等）
 * - Retriever：文档检索器（向量检索、BM25检索、混合检索等）
 * - QueryTransformer：查询转换器（上下文感知、多查询扩展等）
 * - DocumentPostProcessor：文档后处理器（重排序等）
 * 
 * 所有组件都使用常量配置，确保可维护性
 */
@Configuration
public class AiConfig {

    /**
     * RAG顾问
     * 
     * 配置检索增强生成（RAG）的核心组件：
     * - QueryTransformer：查询转换（上下文感知）
     * - DocumentRetriever：文档检索（混合检索）
     * - DocumentPostProcessor：文档后处理（重排序）
     * 
     * @param queryTransformer 查询转换器
     * @param hybridRetriever 混合检索器
     * @param ollamaReRanker 重排序器
     * @return RAG顾问实例
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
     * 对话记忆顾问
     * 
     * 提供对话历史记忆功能，支持多轮对话
     * 
     * @param chatMemory 对话记忆存储
     * @return 记忆顾问实例
     */
    @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_MESSAGE_CHAT_MEMORY_ADVISOR)
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    /**
     * 无记忆顾问
     * 
     * 用于不需要记忆的场景（如评估）
     * 
     * @return 无记忆顾问实例
     */
    @Bean
    @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_NO_MEMORY_ADVISOR)
    public MessageChatMemoryAdvisor noMemoryAdvisor() {
        ChatMemory noOpMemory = new NoOpChatMemory();
        return MessageChatMemoryAdvisor.builder(noOpMemory).build();
    }

    /**
     * 查询转换器
     * 
     * 将用户查询转换为更适合检索的格式，支持上下文感知
     * 
     * @param builder ChatClient构建器
     * @param conversationService 会话服务
     * @return 查询转换器实例
     */
    @Bean
    public QueryTransformer queryTransformer(ChatClient.Builder builder, ConversationService conversationService) {
        return new ContextualQueryTransformer(builder, conversationService);
    }

    /**
     * BM25检索器
     * 
     * 基于关键词的文本检索算法
     * 
     * @param jdbcTemplate JDBC模板
     * @return BM25检索器实例
     */
    @Bean
    public BM25DocumentRetriever bm25Retriever(JdbcTemplate jdbcTemplate) {
        return new BM25DocumentRetriever(jdbcTemplate, AppConstants.AiConfigConstants.BM25_K1);
    }

    /**
     * 向量检索器
     * 
     * 基于向量相似度的语义检索
     * 
     * @param vectorStore 向量存储
     * @return 向量检索器实例
     */
    @Bean
    public VectorStoreDocumentRetriever vectorRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(AppConstants.AiConfigConstants.VECTOR_SIMILARITY_THRESHOLD)
                .topK(AppConstants.AiConfigConstants.VECTOR_TOP_K)
                .filterExpression(new Filter.Expression(Filter.ExpressionType.EQ,
                        new Filter.Key(AppConstants.DatabaseConstants.METADATA_TYPE_KEY), 
                        new Filter.Value(AppConstants.DatabaseConstants.METADATA_TYPE_VALUE_KNOWLEDGE)))
                .build();
    }

    /**
     * 多查询扩展器
     * 
     * 将单个查询扩展为多个不同角度的查询，提高检索覆盖率
     * 
     * @param chatClientBuilder ChatClient构建器
     * @return 多查询扩展器实例
     */
    @Bean
    public MultiQueryExpander multiQueryExpander(ChatClient.Builder chatClientBuilder) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder.clone())
                .numberOfQueries(AppConstants.AiConfigConstants.MULTI_QUERY_NUMBER)
                .build();
    }

    /**
     * 混合检索器
     * 
     * 结合多查询扩展、向量检索和BM25检索的混合检索策略
     * 
     * @param expander 多查询扩展器
     * @param vectorRetriever 向量检索器
     * @param bm25Retriever BM25检索器
     * @return 混合检索器实例
     */
    @Bean
    public DocumentRetriever hybridRetriever(MultiQueryExpander expander,
                                             VectorStoreDocumentRetriever vectorRetriever,
                                             BM25DocumentRetriever bm25Retriever) {
        List<DocumentRetriever> retrievers = List.of(vectorRetriever, bm25Retriever);
        return new MultiQueryDocumentRetrieverAdapter(expander, retrievers);
    }

    /**
     * 文档重排序器
     * 
     * 使用LLM对检索到的文档进行相关性重排序
     * 
     * @param chatClientBuilder ChatClient构建器
     * @return 重排序器实例
     */
    @Bean
    public DocumentPostProcessor ollamaReRanker(ChatClient.Builder chatClientBuilder) {
        ChatClient.Builder clonedBuilder = chatClientBuilder.clone();
        return new OllamaReRankerDocumentPostProcessor(clonedBuilder, AppConstants.AiConfigConstants.RERANK_TOP_K);
    }

    /**
     * 普通对话客户端
     * 
     * 基础的AI对话客户端，支持对话记忆
     * 
     * @param builder ChatClient构建器
     * @param memoryAdvisor 记忆顾问
     * @return 对话客户端实例
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_MESSAGE_CHAT_MEMORY_ADVISOR) MessageChatMemoryAdvisor memoryAdvisor) {
        return builder
                .defaultAdvisors(memoryAdvisor)
                .build();
    }

    /**
     * RAG对话客户端
     * 
     * 支持检索增强生成的对话客户端，包含完整的Advisor链：
     * 1. MemoryAdvisor：对话记忆
     * 2. RecursiveAdvisor：自修复重试（最外层）
     * 3. RAGAdvisor：检索增强
     * 4. EvaluationAdvisor：答案评估
     * 5. SelfHealingAdvisor：自修复策略
     * 
     * @param builder ChatClient构建器
     * @param memoryAdvisor 记忆顾问
     * @param ragAdvisor RAG顾问
     * @param evaluationAdvisor 评估顾问
     * @param selfHealingAdvisor 自修复顾问
     * @param recursiveAdvisor 递归自修复顾问
     * @return RAG对话客户端实例
     */
    @Bean
    public ChatClient ragChatClient(ChatClient.Builder builder,
                                    @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_MESSAGE_CHAT_MEMORY_ADVISOR) MessageChatMemoryAdvisor memoryAdvisor,
                                    RetrievalAugmentationAdvisor ragAdvisor,
                                    EvaluationAdvisor evaluationAdvisor,
                                    SelfHealingAdvisor selfHealingAdvisor,
                                    SelfHealingRecursiveAdvisor recursiveAdvisor) {
        ChatClient.Builder retryBuilder = builder.clone()
                .defaultSystem(AppConstants.AiConfigConstants.RAG_SYSTEM_PROMPT)
                .defaultAdvisors(
                    memoryAdvisor,
                    ragAdvisor,
                    evaluationAdvisor,
                    selfHealingAdvisor
                );
        
        recursiveAdvisor.setRetryBuilder(retryBuilder);
        
        return builder.clone()
                .defaultSystem(AppConstants.AiConfigConstants.RAG_SYSTEM_PROMPT)
                .defaultAdvisors(
                    memoryAdvisor,
                    recursiveAdvisor,
                    ragAdvisor,
                    evaluationAdvisor,
                    selfHealingAdvisor
                )
                .build();
    }

    /**
     * 视觉对话客户端
     * 
     * 支持多模态（图片、视频、音频）分析的对话客户端
     * 
     * @param builder ChatClient构建器
     * @param memoryAdvisor 记忆顾问
     * @return 视觉对话客户端实例
     */
    @Bean
    @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_VISION_CHAT_CLIENT)
    public ChatClient visionChatClient(ChatClient.Builder builder,
                                       @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_MESSAGE_CHAT_MEMORY_ADVISOR) MessageChatMemoryAdvisor memoryAdvisor) {
        return builder
                .defaultOptions(ChatOptions.builder()
                        .model(AppConstants.AiConfigConstants.VISION_MODEL_NAME)
                        .temperature(AppConstants.AiConfigConstants.VISION_MODEL_TEMPERATURE)
                        .build())
                .defaultSystem(AppConstants.AiConfigConstants.VISION_SYSTEM_PROMPT)
                .defaultAdvisors(memoryAdvisor)
                .build();
    }

    /**
     * 用户画像提取客户端
     * 
     * 用于从对话中提取用户偏好和特征
     * 
     * @param builder ChatClient构建器
     * @return 画像提取客户端实例
     */
    @Bean
    @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_PROFILE_EXTRACTION_CHAT_CLIENT)
    public ChatClient profileExtractionChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * 评估对话客户端
     * 
     * 用于答案质量评估，不使用记忆
     * 
     * @param noMemoryAdvisor 无记忆顾问
     * @param builder ChatClient构建器
     * @return 评估客户端实例
     */
    @Bean
    @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_EVALUATION_CHAT_CLIENT)
    public ChatClient evaluationChatClient(@Qualifier(AppConstants.AiConfigConstants.QUALIFIER_NO_MEMORY_ADVISOR) MessageChatMemoryAdvisor noMemoryAdvisor, ChatClient.Builder builder) {
        return builder.clone().defaultAdvisors(noMemoryAdvisor).build();
    }
}
