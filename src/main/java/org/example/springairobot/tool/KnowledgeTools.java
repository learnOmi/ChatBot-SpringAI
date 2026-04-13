package org.example.springairobot.tool;

import org.example.springairobot.PO.DTO.EntityExtraction;
import org.example.springairobot.service.ChatService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeTools {

    private final ChatService chatService;

    public KnowledgeTools(ChatService chatService) {
        this.chatService = chatService;
    }

    @Tool(description = """
        从本地知识库中检索信息。
        当用户询问以下内容时必须调用此工具：
        - 小说《秦锋》的剧情、人物、事件等。
        - 例如："秦锋的故事里有哪些人物？"、"秦锋的对手是谁？"。
        绝对禁止编造知识库内容。
        """)
    public String queryKnowledgeBase(
            @ToolParam(description = "会话ID") String sessionId,
            @ToolParam(description = "用户Id") String userId,
            @ToolParam(description = "要查询的问题") String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("查询问题不能为空");
        }
        return chatService.ragChat(sessionId, userId, query);
    }

    @Tool(description = "从知识库中批量抽取实体信息，如人物、地点、事件等")
    public List<EntityExtraction> extractEntities(
            @ToolParam(description = "会话ID") String sessionId,
            @ToolParam(description = "用户Id") String userId,
            @ToolParam(description = "查询描述") String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("查询描述不能为空");
        }
        return chatService.extractEntities(sessionId, userId, query);
    }
}
