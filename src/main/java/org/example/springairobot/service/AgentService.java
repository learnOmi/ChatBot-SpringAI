package org.example.springairobot.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final ChatClient agentChatClient;
    private final ConversationService conversationService;

    public AgentService(@Qualifier("agentChatClient") ChatClient agentChatClient,
                        ConversationService conversationService) {
        this.agentChatClient = agentChatClient;
        this.conversationService = conversationService;
    }

    public String execute(String sessionId, String userInput) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, null);
        return agentChatClient.prompt()
                .user(userInput)
                .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                .call()
                .content();
    }
}