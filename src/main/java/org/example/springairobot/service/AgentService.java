package org.example.springairobot.service;

import org.example.springairobot.PO.Tables.UserProfile;
import org.example.springairobot.service.memory.MemoryEnhancementService;
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

    public String execute(String sessionId, String userId, String userInput) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        UserProfile profile = memoryService.getUserProfile(userId);
        String profileContext = profile != null ?
                "用户偏好：单位-" + profile.getPreferredUnits() + "，语言-" + profile.getLanguage() : "";

        List<Document> memories = memoryService.retrieveRelevantMemories(userId, userInput, 3);
        String memoryContext = memories.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n[历史记忆] " + b);

        String enhancedInput = userInput;
        if (!profileContext.isEmpty() || !memoryContext.isEmpty()) {
            enhancedInput = String.format("""
                    用户上下文：
                    %s
                    %s
                    
                    用户问题：%s
                    """, profileContext, memoryContext, userInput);
        }

        String response = agentChatClient.prompt()
                .user(enhancedInput)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .call()
                .content();

        conversationService.savePair(effectiveSessionId, userId, userInput, response, null);

        memoryService.updateUserProfileAndMemory(userId);

        return response;
    }
}