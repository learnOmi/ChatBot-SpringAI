package org.example.springairobot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springairobot.PO.DTO.EntityExtraction;
import org.example.springairobot.PO.DTO.RagAnswer;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.rag.RagEvaluatorService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

/**
 * 聊天服务
 * 
 * 提供多种对话模式：
 * - 普通对话：基础的AI对话功能
 * - RAG对话：基于检索增强生成的对话，结合知识库回答问题
 * - 结构化输出：返回包含引用来源和置信度的结构化答案
 * - 实体抽取：从知识库中提取实体信息
 * 
 * 所有对话都支持会话记忆，自动保存对话历史
 */
@Service
public class ChatService {
    
    private final ChatClient chatClient;
    private final ChatClient ragChatClient;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final RagEvaluatorService evaluatorService;

    public ChatService(@Qualifier(AppConstants.AiConfigConstants.QUALIFIER_CHAT_CLIENT) ChatClient chatClient,
                       @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_RAG_CHAT_CLIENT) ChatClient ragChatClient,
                       ConversationService conversationService,
                       RagEvaluatorService evaluatorService,
                       ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.ragChatClient = ragChatClient;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.evaluatorService = evaluatorService;
    }

    // ==================== 普通对话 ====================

    /**
     * 同步普通对话
     * 
     * @param sessionId 会话ID，可为null（自动创建新会话）
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @return AI回复内容
     */
    public String chat(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        String assistantReply = chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(AppConstants.AdvisorConstants.CHAT_MEMORY_CONVERSATION_ID_KEY, effectiveSessionId))
                .call()
                .content();

        conversationService.savePair(effectiveSessionId, userId, userMessage, assistantReply, null);
        return assistantReply;
    }

    /**
     * 流式普通对话
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @return 流式AI回复
     */
    public Flux<String> chatStream(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        StringBuilder fullReplyBuilder = new StringBuilder();

        return chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(AppConstants.AdvisorConstants.CHAT_MEMORY_CONVERSATION_ID_KEY, effectiveSessionId))
                .stream()
                .content()
                .doOnNext(chunk -> fullReplyBuilder.append(chunk))
                .doOnComplete(() -> {
                    String fullReply = fullReplyBuilder.toString();
                    conversationService.savePair(effectiveSessionId, userId, userMessage, fullReply, null);
                });
    }

    // ==================== RAG 对话 ====================

    /**
     * 同步RAG对话
     * 
     * 评估和自修复由Advisor链自动处理：检索→生成→评估→自修复→兜底
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @return AI回复内容
     */
    public String ragChat(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        // Advisor链自动处理：检索→生成→评估→自修复→兜底
        String response = ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a
                        .param(AppConstants.AdvisorConstants.CHAT_MEMORY_CONVERSATION_ID_KEY, effectiveSessionId)
                        .param(AppConstants.AdvisorConstants.SESSION_ID_KEY, effectiveSessionId))
                .call()
                .content();

        conversationService.savePair(effectiveSessionId, userId, userMessage, response, null);
        return response;
    }

    /**
     * 流式RAG对话
     * 
     * CallAdvisor不适用于流式调用，仍需手动评估
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @return 流式AI回复
     */
    public Flux<String> ragChatStream(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        StringBuilder fullReplyBuilder = new StringBuilder();
        List<Document> retrievedDocs = new java.util.ArrayList<>();

        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a
                        .param(AppConstants.AdvisorConstants.CHAT_MEMORY_CONVERSATION_ID_KEY, effectiveSessionId)
                        .param(AppConstants.AdvisorConstants.SESSION_ID_KEY, effectiveSessionId))
                .stream()
                .chatResponse()
                .doOnNext(chatResponse -> {
                    String content = chatResponse.getResult().getOutput().getText();
                    if (content != null) {
                        fullReplyBuilder.append(content);
                    }
                    List<Document> docs = chatResponse.getMetadata()
                            .getOrDefault(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, Collections.emptyList());
                    if (!docs.isEmpty()) {
                        retrievedDocs.clear();
                        retrievedDocs.addAll(docs);
                    }
                })
                .map(chatResponse -> {
                    String content = chatResponse.getResult().getOutput().getText();
                    return content != null ? content : "";
                })
                .doOnComplete(() -> {
                    String fullReply = fullReplyBuilder.toString();
                    boolean acceptable = evaluatorService.evaluate(userMessage, retrievedDocs, fullReply);
                    String finalResponse = acceptable ? fullReply : evaluatorService.getDefaultUnknownAnswer();
                    conversationService.savePair(effectiveSessionId, userId, userMessage, finalResponse, null);
                });
    }

    /**
     * 结构化RAG回答
     * 
     * 返回答案 + 引用来源 + 置信度
     * 评估和自修复由Advisor链自动处理
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @return 结构化答案对象
     */
    public RagAnswer ragChatStructured(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        // 创建针对 RagAnswer 的输出转换器
        BeanOutputConverter<RagAnswer> converter = new BeanOutputConverter<>(RagAnswer.class);

        // 构建提示词，包含格式指令
        String prompt = String.format(AppConstants.ChatServiceConstants.RAG_PROMPT_TEMPLATE,
                userMessage, converter.getFormat());

        // Advisor链自动处理：检索→生成→评估→自修复→兜底
        String response = ragChatClient.prompt()
                .user(prompt)
                .advisors(a -> a
                        .param(AppConstants.AdvisorConstants.CHAT_MEMORY_CONVERSATION_ID_KEY, effectiveSessionId)
                        .param(AppConstants.AdvisorConstants.SESSION_ID_KEY, effectiveSessionId))
                .call()
                .content();

        // 转换为对象（兜底时响应为纯文本，JSON解析可能失败）
        RagAnswer ragAnswer;
        try {
            ragAnswer = converter.convert(response);
        } catch (Exception e) {
            ragAnswer = new RagAnswer();
            ragAnswer.setAnswer(evaluatorService.getDefaultUnknownAnswer());
            ragAnswer.setSources(Collections.emptyList());
            ragAnswer.setConfidence(0.0);
        }

        // 持久化对话历史
        conversationService.savePair(effectiveSessionId, userId, userMessage, ragAnswer.getAnswer(), null);

        return ragAnswer;
    }

    /**
     * 批量实体抽取
     * 
     * 从知识库中提取与用户查询相关的实体信息（人物、地点、事件等）
     * 评估和自修复由Advisor链自动处理
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param userQuery 用户查询
     * @return 实体列表
     */
    public List<EntityExtraction> extractEntities(String sessionId, String userId, String userQuery) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        // 创建针对 List<EntityExtraction> 的输出转换器
        BeanOutputConverter<List<EntityExtraction>> converter =
                new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});

        // 构建提示词
        String prompt = String.format(AppConstants.ChatServiceConstants.ENTITY_EXTRACTION_PROMPT_TEMPLATE,
                userQuery, converter.getFormat());

        // Advisor链自动处理：检索→生成→评估→自修复→兜底
        String response = ragChatClient.prompt()
                .user(prompt)
                .advisors(a -> a
                        .param(AppConstants.AdvisorConstants.CHAT_MEMORY_CONVERSATION_ID_KEY, effectiveSessionId)
                        .param(AppConstants.AdvisorConstants.SESSION_ID_KEY, effectiveSessionId))
                .call()
                .content();

        // 转换为 List<EntityExtraction>（兜底时响应为纯文本，JSON解析可能失败）
        List<EntityExtraction> entities;
        try {
            entities = converter.convert(response);
        } catch (Exception e) {
            entities = Collections.emptyList();
        }

        // 持久化
        try {
            conversationService.saveMessage(effectiveSessionId, userId, AppConstants.ChatMessages.MESSAGE_TYPE_USER, userQuery, null);
            String entitiesJson = objectMapper.writeValueAsString(entities);
            conversationService.saveMessage(effectiveSessionId, userId, AppConstants.ChatMessages.MESSAGE_TYPE_ASSISTANT, entitiesJson, null);
        } catch (Exception e) {
            System.err.println(AppConstants.ChatServiceConstants.ERROR_PERSIST_ENTITIES_FAILED + e.getMessage());
        }

        return entities;
    }
}
