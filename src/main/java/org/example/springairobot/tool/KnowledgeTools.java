package org.example.springairobot.tool;

import org.example.springairobot.PO.DTO.EntityExtraction;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.ChatService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库工具类
 * 
 * 提供知识库查询相关的工具函数，供AI Agent自动调用
 * 
 * 功能特点：
 * - 从本地知识库检索信息
 * - 批量抽取实体信息
 * - 基于RAG技术增强回答质量
 */
@Component
public class KnowledgeTools {

    private final ChatService chatService;

    public KnowledgeTools(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 查询知识库
     * 
     * 从本地知识库中检索与查询相关的信息
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param query 查询问题
     * @return 知识库检索结果
     * @throws IllegalArgumentException 如果查询为空
     */
    @Tool(description = AppConstants.KnowledgeConstants.TOOL_DESC_QUERY_KNOWLEDGE)
    public String queryKnowledgeBase(
            @ToolParam(description = AppConstants.KnowledgeConstants.TOOL_PARAM_SESSION_ID) String sessionId,
            @ToolParam(description = AppConstants.KnowledgeConstants.TOOL_PARAM_USER_ID) String userId,
            @ToolParam(description = AppConstants.KnowledgeConstants.TOOL_PARAM_QUERY) String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.KnowledgeConstants.ERROR_QUERY_EMPTY);
        }
        return chatService.ragChat(sessionId, userId, query);
    }

    /**
     * 提取实体信息
     * 
     * 从知识库中批量抽取实体信息（人物、地点、事件等）
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param query 查询描述
     * @return 实体列表
     * @throws IllegalArgumentException 如果查询描述为空
     */
    @Tool(description = AppConstants.KnowledgeConstants.TOOL_DESC_EXTRACT_ENTITIES)
    public List<EntityExtraction> extractEntities(
            @ToolParam(description = AppConstants.KnowledgeConstants.TOOL_PARAM_SESSION_ID) String sessionId,
            @ToolParam(description = AppConstants.KnowledgeConstants.TOOL_PARAM_USER_ID) String userId,
            @ToolParam(description = AppConstants.KnowledgeConstants.TOOL_PARAM_QUERY_DESC) String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.KnowledgeConstants.ERROR_QUERY_DESC_EMPTY);
        }
        return chatService.extractEntities(sessionId, userId, query);
    }
}
