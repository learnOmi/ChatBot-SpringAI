package org.example.springairobot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springairobot.PO.DTO.EntityExtraction;
import org.example.springairobot.PO.DTO.RagAnswer;
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

@Service
public class ChatService {
    private final ChatClient chatClient;
    private final ChatClient ragChatClient;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final RagEvaluatorService evaluatorService;


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

    // ==================== 普通对话 ====================
    public String chat(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        String assistantReply = chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .call()
                .content();

        conversationService.savePair(effectiveSessionId, userId, userMessage, assistantReply, null);
        return assistantReply;
    }

    public Flux<String> chatStream(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

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

    /**
     * 同步RAG对话
     * 评估和自修复由Advisor链自动处理，无需手动评估
     */
    public String ragChat(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        // Advisor链自动处理：检索→生成→评估→自修复→兜底
        String response = ragChatClient.prompt()
                .user(userMessage)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", effectiveSessionId)
                        .param("sessionId", effectiveSessionId))
                .call()
                .content();

        conversationService.savePair(effectiveSessionId, userId, userMessage, response, null);
        return response;
    }

    /**
     * 流式RAG对话
     * CallAdvisor不适用于流式调用，仍需手动评估
     */
    public Flux<String> ragChatStream(String sessionId, String userId, String userMessage) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        StringBuilder fullReplyBuilder = new StringBuilder();
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
     * 结构化RAG回答：返回答案 + 引用来源 + 置信度
     * 评估和自修复由Advisor链自动处理
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

        // 3. Advisor链自动处理：检索→生成→评估→自修复→兜底
        String response = ragChatClient.prompt()
                .user(prompt)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", effectiveSessionId)
                        .param("sessionId", effectiveSessionId))
                .call()
                .content();

        // 4. 转换为对象（兜底时响应为纯文本，JSON解析可能失败）
        RagAnswer ragAnswer;
        try {
            ragAnswer = converter.convert(response);
        } catch (Exception e) {
            ragAnswer = new RagAnswer();
            ragAnswer.setAnswer(evaluatorService.getDefaultUnknownAnswer());
            ragAnswer.setSources(Collections.emptyList());
            ragAnswer.setConfidence(0.0);
        }

        // 5. 持久化对话历史
        conversationService.savePair(effectiveSessionId, userId, userMessage, ragAnswer.getAnswer(), null);

        return ragAnswer;
    }

    /**
     * 批量实体抽取（结构化列表输出）
     * 评估和自修复由Advisor链自动处理
     */
    public List<EntityExtraction> extractEntities(String sessionId, String userId, String userQuery) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        // 1. 创建针对 List<EntityExtraction> 的输出转换器
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

        // 3. Advisor链自动处理：检索→生成→评估→自修复→兜底
        String response = ragChatClient.prompt()
                .user(prompt)
                .advisors(a -> a
                        .param("chat_memory_conversation_id", effectiveSessionId)
                        .param("sessionId", effectiveSessionId))
                .call()
                .content();

        // 4. 转换为 List<EntityExtraction>（兜底时响应为纯文本，JSON解析可能失败）
        List<EntityExtraction> entities;
        try {
            entities = converter.convert(response);
        } catch (Exception e) {
            entities = Collections.emptyList();
        }

        // 5. 持久化
        try {
            conversationService.saveMessage(effectiveSessionId, userId, "user", userQuery, null);
            String entitiesJson = objectMapper.writeValueAsString(entities);
            conversationService.saveMessage(effectiveSessionId, userId, "assistant", entitiesJson, null);
        } catch (Exception e) {
            System.err.println("持久化实体抽取结果失败: " + e.getMessage());
        }

        return entities;
    }
}
