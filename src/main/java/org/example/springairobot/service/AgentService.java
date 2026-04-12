package org.example.springairobot.service;

import org.example.springairobot.PO.Tables.UserProfile;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentService {

    private final ChatClient agentChatClient;
    private final ConversationService conversationService;
    private final MemoryEnhancementService memoryService;


    public AgentService(@Qualifier("agentChatClient") ChatClient agentChatClient,
                        ConversationService conversationService,
                        MemoryEnhancementService memoryService) {
        this.agentChatClient = agentChatClient;
        this.conversationService = conversationService;
        this.memoryService = memoryService;
    }

    public String execute(String sessionId, String userInput) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, null);

        // 1. 获取用户画像
        UserProfile profile = memoryService.getUserProfile(effectiveSessionId);
        String profileContext = profile != null ?
                "用户偏好：单位-" + profile.getPreferredUnits() + "，语言-" + profile.getLanguage() : "";

        // 2. 检索相关长期记忆
        List<Document> memories = memoryService.retrieveRelevantMemories(effectiveSessionId, userInput, 3);
        String memoryContext = memories.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n[历史记忆] " + b);

        // 3. 构建增强提示词
        String enhancedInput = userInput;
        if (!profileContext.isEmpty() || !memoryContext.isEmpty()) {
            enhancedInput = String.format("""
                    用户上下文：
                    %s
                    %s
                    
                    用户问题：%s
                    """, profileContext, memoryContext, userInput);
        }

        // 4. 调用智能体
        String response = agentChatClient.prompt()
                .user(enhancedInput)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .call()
                .content();

        // 5. 保存当前消息到会话历史
        conversationService.savePair(effectiveSessionId, userInput, response, null);

        // 6. 异步更新长期记忆和画像
        memoryService.updateUserProfileAndMemory(effectiveSessionId);

        return response;

    }
}