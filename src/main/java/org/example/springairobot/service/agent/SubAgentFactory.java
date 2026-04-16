package org.example.springairobot.service.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.example.springairobot.Advisor.ChatMemory.NoOpChatMemory;
import org.example.springairobot.PO.Context.AgentContextHolder;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.ChatService;
import org.example.springairobot.service.SearchService;
import org.example.springairobot.service.WeatherService;
import org.example.springairobot.service.agent.Type.KnowledgeRequest;
import org.example.springairobot.service.agent.Type.WeatherRequest;
import org.example.springairobot.service.agent.Type.WebSearchRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.example.springairobot.constants.AgentType;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

@Component
public class SubAgentFactory {

    private final WeatherService weatherService;
    private final SearchService searchService;
    private final ChatService chatService;
    private final ChatClient.Builder chatClientBuilder;

    public SubAgentFactory(WeatherService weatherService,
                           SearchService searchService,
                           ChatService chatService,
                           ChatClient.Builder chatClientBuilder) {
        this.weatherService = weatherService;
        this.searchService = searchService;
        this.chatService = chatService;
        this.chatClientBuilder = chatClientBuilder;
    }

    private ChatClient createIsolatedChatClient() {
        ChatMemory noOpMemory = new NoOpChatMemory();
        MessageChatMemoryAdvisor noOpMemoryAdvisor = MessageChatMemoryAdvisor.builder(noOpMemory).build();
        return chatClientBuilder.clone()
                .defaultAdvisors(noOpMemoryAdvisor)
                .build();
    }

    public ReactAgent createWeatherAgent() {
        ToolCallback weatherTool = FunctionToolCallback
                .builder(AppConstants.SubAgentConstants.TOOL_GET_WEATHER, (WeatherRequest req) -> {
                    if (req.input() == null || req.input().trim().isEmpty()) {
                        return AppConstants.SubAgentConstants.ERROR_WEATHER_NO_CITY;
                    }
                    return weatherService.getWeather(req.input());
                })
                .description(AppConstants.SubAgentConstants.TOOL_DESC_WEATHER)
                .inputType(WeatherRequest.class)
                .build();

        return ReactAgent.builder()
                .name(AgentType.WEATHER.getValue())
                .chatClient(createIsolatedChatClient())
                .systemPrompt(AppConstants.SubAgentConstants.WEATHER_AGENT_PROMPT)
                .tools(weatherTool)
                .outputKey(AppConstants.SubAgentConstants.OUTPUT_KEY_WEATHER)
                .outputType(String.class)
                .build();
    }

    public ReactAgent createSearchAgent() {
        ToolCallback searchTool = FunctionToolCallback
                .builder(AppConstants.SubAgentConstants.TOOL_WEB_SEARCH, (WebSearchRequest req) -> {
                    if (req.input() == null || req.input().trim().isEmpty()) {
                        return AppConstants.SubAgentConstants.ERROR_SEARCH_NO_KEYWORD;
                    }
                    return searchService.webSearch(req.input());
                })
                .description(AppConstants.SubAgentConstants.TOOL_DESC_SEARCH)
                .inputType(WebSearchRequest.class)
                .build();

        return ReactAgent.builder()
                .name(AgentType.SEARCH.getValue())
                .chatClient(createIsolatedChatClient())
                .systemPrompt(AppConstants.SubAgentConstants.SEARCH_AGENT_PROMPT)
                .tools(searchTool)
                .outputKey(AppConstants.SubAgentConstants.OUTPUT_KEY_SEARCH)
                .outputType(String.class)
                .build();
    }

    public ReactAgent createKnowledgeAgent() {
        ToolCallback knowledgeTool = FunctionToolCallback
                .builder(AppConstants.SubAgentConstants.TOOL_QUERY_KNOWLEDGE, (KnowledgeRequest req) -> {
                    if (req.input() == null || req.input().trim().isEmpty()) {
                        return AppConstants.SubAgentConstants.ERROR_KNOWLEDGE_NO_QUERY;
                    }
                    String sessionId = AgentContextHolder.getSessionId();
                    String userId = AgentContextHolder.getUserId();
                    return chatService.ragChat(sessionId, userId, req.input());
                })
                .description(AppConstants.SubAgentConstants.TOOL_DESC_KNOWLEDGE)
                .inputType(KnowledgeRequest.class)
                .build();

        return ReactAgent.builder()
                .name(AgentType.KNOWLEDGE.getValue())
                .chatClient(createIsolatedChatClient())
                .systemPrompt(AppConstants.SubAgentConstants.KNOWLEDGE_AGENT_PROMPT)
                .tools(knowledgeTool)
                .outputKey(AppConstants.SubAgentConstants.OUTPUT_KEY_KNOWLEDGE)
                .outputType(String.class)
                .build();
    }
}
