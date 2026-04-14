package org.example.springairobot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springairobot.PO.DTO.EntityExtraction;
import org.example.springairobot.PO.DTO.RagAnswer;
import org.example.springairobot.service.rag.RagEvaluatorService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

@Service
public class ChatService {
    private final ChatClient chatClient;
    private final ChatClient ragChatClient;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final RagEvaluatorService evaluatorService;   // 注入评估服务


    public ChatService(@Qualifier("chatClient") ChatClient chatClient,
                       @Qualifier("ragChatClient") ChatClient ragChatClient,
                       ConversationService conversationService,
                       RagEvaluatorService evaluatorService,
                       ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.ragChatClient = ragChatClient;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.evaluatorService = evaluatorService;
    }

    // 同步对话（记忆由 MessageChatMemoryAdvisor 自动管理）
    // ==================== 普通对话 ====================
    public String chat(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        String assistantReply = chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .call()
                .content();

        // 持久化完整历史
        conversationService.savePair(effectiveSessionId, userId, userMessage, assistantReply, null);
        return assistantReply;
    }

    public Flux<String> chatStream(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

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
                    conversationService.savePair(effectiveSessionId, userId, userMessage, fullReply, null);
                });
    }

    // ==================== RAG 对话 ====================
    public String ragChat(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        // 获取完整的 ChatResponse
        ChatResponse chatResponse = ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", effectiveSessionId)
                        .param("sessionId", effectiveSessionId))
                .call()
                .chatResponse();

        String response = chatResponse.getResult().getOutput().getText();

        // 从元数据中提取检索文档
        List<Document> retrievedDocs = chatResponse.getMetadata()
                .getOrDefault(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, Collections.emptyList());

        // 评估答案质量
        boolean acceptable = evaluatorService.evaluate(userMessage, retrievedDocs, response);
        String finalResponse = acceptable ? response : evaluatorService.getDefaultUnknownAnswer();

        conversationService.savePair(effectiveSessionId, userId, userMessage, finalResponse, null);
        return finalResponse;
    }

    public Flux<String> ragChatStream(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        // 用于累积流式响应的 StringBuilder
        StringBuilder fullReplyBuilder = new StringBuilder();
        // 用于收集检索文档
        List<Document> retrievedDocs = new java.util.ArrayList<>();

        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", effectiveSessionId)
                        .param("sessionId", effectiveSessionId))
                .stream()
                .chatResponse()
                .doOnNext(chatResponse -> {
                    String content = chatResponse.getResult().getOutput().getText();
                    if (content != null) {
                        fullReplyBuilder.append(content);
                    }
                    // 从元数据中提取检索文档
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
                    // 评估答案质量
                    boolean acceptable = evaluatorService.evaluate(userMessage, retrievedDocs, fullReply);
                    String finalResponse = acceptable ? fullReply : evaluatorService.getDefaultUnknownAnswer();
                    conversationService.savePair(effectiveSessionId, userId, userMessage, finalResponse, null);
                });
    }

    /**
     * 结构化 RAG 回答：返回答案 + 引用来源 + 置信度
     */
    public RagAnswer ragChatStructured(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        // 1. 创建针对 RagAnswer 的输出转换器
        BeanOutputConverter<RagAnswer> converter = new BeanOutputConverter<>(RagAnswer.class);

        // 2. 构建提示词，包含格式指令
        String prompt = """
                你是一个基于知识库的问答助手。请根据检索到的文档内容回答用户问题。
                要求：
                - 如果知识库中包含相关信息，请给出准确回答，并附上引用来源（文档中的关键句子或段落标题）。
                - 如果知识库中没有相关信息，请礼貌回答"不知道"，sources 为空，confidence 为 0。
                - 回答必须严格遵循下面的 JSON 格式。

                用户问题：%s

                %s
                """.formatted(userMessage, converter.getFormat());

        // 3. 调用 RAG ChatClient（它会自动执行检索增强）
        ChatResponse chatResponse = ragChatClient.prompt()
                .user(prompt)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", effectiveSessionId)
                        .param("sessionId", effectiveSessionId))
                .call()
                .chatResponse();

        String response = chatResponse.getResult().getOutput().getText();
        // 从元数据中提取检索文档
        List<Document> retrievedDocs = chatResponse.getMetadata()
                .getOrDefault(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, Collections.emptyList());

        // 评估答案质量
        boolean acceptable = evaluatorService.evaluate(userMessage, retrievedDocs, response);

        // 4. 转换为对象
        RagAnswer ragAnswer;
        if (acceptable) {
            ragAnswer = converter.convert(response);
        } else {
            ragAnswer = new RagAnswer();
            ragAnswer.setAnswer(evaluatorService.getDefaultUnknownAnswer());
            ragAnswer.setSources(Collections.emptyList());
            ragAnswer.setConfidence(0.0);
        }

        // 5. 持久化对话历史（保存原始问题和结构化答案的文本表示）
        conversationService.savePair(effectiveSessionId, userId, userMessage, ragAnswer.getAnswer(), null);

        return ragAnswer;
    }

    /**
     * 批量实体抽取（结构化列表输出）
     */
    public List<EntityExtraction> extractEntities(String sessionId, String userId, String userQuery) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        // 1. 创建针对 List<EntityExtraction> 的输出转换器
        //    关键：使用 ParameterizedTypeReference 传递泛型信息
        BeanOutputConverter<List<EntityExtraction>> converter =
                new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});

        // 2. 构建提示词
        String prompt = """
                你是一个信息抽取助手。请根据知识库中的内容，提取与用户查询相关的实体信息。
                要求：
                - 返回一个 JSON 数组，每个元素包含 name (实体名称)、type (实体类型，如人物/地点/事件)、description (简短描述)。
                - 如果知识库中没有相关信息，返回空数组 []。
                - 严格遵循以下 JSON 格式。

                用户查询：%s

                %s
                """.formatted(userQuery, converter.getFormat());

        // 3. 调用 RAG ChatClient
        ChatResponse chatResponse = ragChatClient.prompt()
                .user(prompt)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", effectiveSessionId)
                        .param("sessionId", effectiveSessionId))
                .call()
                .chatResponse();

        String response = chatResponse.getResult().getOutput().getText();
        // 从元数据中提取检索文档
        List<Document> retrievedDocs = chatResponse.getMetadata()
                .getOrDefault(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, Collections.emptyList());

        // 评估答案质量
        boolean acceptable = evaluatorService.evaluate(userQuery, retrievedDocs, response);

        // 4. 转换为 List<EntityExtraction>
        List<EntityExtraction> entities;
        if (acceptable) {
            entities = converter.convert(response);
        } else {
            entities = Collections.emptyList();
        }

        // 5. 持久化：保存用户查询和抽取结果（JSON 格式）
        try {
            // 保存用户原始查询
            conversationService.saveMessage(effectiveSessionId, userId, "user", userQuery, null);
            // 将抽取结果序列化为 JSON 字符串，作为助手回复保存
            String entitiesJson = objectMapper.writeValueAsString(entities);
            conversationService.saveMessage(effectiveSessionId, userId, "assistant", entitiesJson, null);
        } catch (Exception e) {
            // 日志记录，但不影响主流程返回
            System.err.println("持久化实体抽取结果失败: " + e.getMessage());
        }

        return entities;
    }
}