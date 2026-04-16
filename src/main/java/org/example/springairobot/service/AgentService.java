package org.example.springairobot.service;

import org.example.springairobot.PO.Tables.UserProfile;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.memory.MemoryEnhancementService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent服务
 * 
 * 智能体服务，支持工具调用和多专家协作
 * 
 * 功能特点：
 * - 自动调用天气、搜索、知识库等工具
 * - 结合用户画像和历史记忆增强对话
 * - 支持多专家协调工作
 */
@Service
public class AgentService {

    private final ChatClient agentChatClient;
    private final ConversationService conversationService;
    private final MemoryEnhancementService memoryService;

    public AgentService(@Qualifier(AppConstants.AiConfigConstants.QUALIFIER_AGENT_CHAT_CLIENT) ChatClient agentChatClient,
                        ConversationService conversationService,
                        MemoryEnhancementService memoryService) {
        this.agentChatClient = agentChatClient;
        this.conversationService = conversationService;
        this.memoryService = memoryService;
    }

    /**
     * 执行Agent对话
     * 
     * 自动结合用户画像和历史记忆，增强对话效果
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param userInput 用户输入
     * @return AI回复
     */
    public String execute(String sessionId, String userId, String userInput) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        // 获取用户画像
        UserProfile profile = memoryService.getUserProfile(userId);
        String profileContext = profile != null ?
                String.format(AppConstants.AgentServiceConstants.USER_PROFILE_CONTEXT_TEMPLATE,
                        profile.getPreferredUnits(), profile.getLanguage()) : "";

        // 检索相关记忆
        List<Document> memories = memoryService.retrieveRelevantMemories(userId, userInput,
                AppConstants.AgentServiceConstants.DEFAULT_MEMORY_RETRIEVAL_COUNT);
        String memoryContext = memories.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + AppConstants.AgentServiceConstants.MEMORY_CONTEXT_PREFIX + b);

        // 构建增强输入
        String enhancedInput = userInput;
        if (!profileContext.isEmpty() || !memoryContext.isEmpty()) {
            enhancedInput = String.format(AppConstants.AgentServiceConstants.USER_CONTEXT_TEMPLATE,
                    profileContext, memoryContext, userInput);
        }

        // 调用Agent
        String response = agentChatClient.prompt()
                .user(enhancedInput)
                .advisors(a -> a.param(AppConstants.AdvisorConstants.CHAT_MEMORY_CONVERSATION_ID_KEY, effectiveSessionId))
                .call()
                .content();

        // 保存对话
        conversationService.savePair(effectiveSessionId, userId, userInput, response, null);

        // 异步更新用户画像和记忆
        memoryService.updateUserProfileAndMemory(userId);

        return response;
    }
}
