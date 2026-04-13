package org.example.springairobot.service.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.example.springairobot.Advisor.ChatMemory.NoOpChatMemory;
import org.example.springairobot.PO.Context.AgentContextHolder;
import org.example.springairobot.service.ChatService;
import org.example.springairobot.service.SearchService;
import org.example.springairobot.service.WeatherService;
import org.example.springairobot.service.agent.Type.ImageAnalysisRequest;
import org.example.springairobot.service.agent.Type.KnowledgeRequest;
import org.example.springairobot.service.agent.Type.WeatherRequest;
import org.example.springairobot.service.agent.Type.WebSearchRequest;
import org.example.springairobot.service.vision.VisionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SubAgentFactory {

    private final WeatherService weatherService;
    private final SearchService searchService;
    private final ChatService chatService;
    private final VisionService visionService;
    private final ChatClient.Builder chatClientBuilder;

    public SubAgentFactory(WeatherService weatherService,
                           SearchService searchService,
                           ChatService chatService,
                           VisionService visionService,
                           ChatClient.Builder chatClientBuilder) {
        this.weatherService = weatherService;
        this.searchService = searchService;
        this.chatService = chatService;
        this.visionService = visionService;
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * 创建一个纯净的 ChatClient（无任何 Advisor，无记忆）
     */
    private ChatClient createIsolatedChatClient() {
        ChatMemory noOpMemory = new NoOpChatMemory();
        MessageChatMemoryAdvisor noOpMemoryAdvisor = MessageChatMemoryAdvisor.builder(noOpMemory).build();
        return chatClientBuilder.clone()
                .defaultAdvisors(noOpMemoryAdvisor)
                .build();  // 不添加任何 defaultAdvisors
    }

    public ReactAgent createWeatherAgent() {
        ToolCallback weatherTool = FunctionToolCallback
                .builder("get_weather", (WeatherRequest req) -> {
                    if (req.input() == null || req.input().trim().isEmpty()) {
                        return "无法查询天气：请提供城市名称。";
                    }
                    return weatherService.getWeather(req.input());
                })
                .description("查询指定城市的实时天气，参数：{\"input\": \"城市名\"}")
                .inputType(WeatherRequest.class)
                .build();

        return ReactAgent.builder()
                .name("weather_agent")
                .chatClient(createIsolatedChatClient())
                .systemPrompt("你是一个天气专家。调用 get_weather 工具，传入 { \"input\": \"城市名\" }。返回纯文本天气描述。")
                .tools(weatherTool)
                .outputKey("weather_result")
                .outputType(String.class)
                .build();
    }

    public ReactAgent createSearchAgent() {
        ToolCallback searchTool = FunctionToolCallback
                .builder("web_search", (WebSearchRequest req) -> {
                    if (req.input() == null || req.input().trim().isEmpty()) {
                        return "无法搜索：请提供关键词。";
                    }
                    return searchService.webSearch(req.input());
                })
                .description("搜索互联网信息，参数：{\"input\": \"关键词\"}")
                .inputType(WebSearchRequest.class)
                .build();

        return ReactAgent.builder()
                .name("search_agent")
                .chatClient(createIsolatedChatClient())
                .systemPrompt("你是一个搜索专家。调用 web_search 工具，传入 { \"input\": \"关键词\" }。返回纯文本摘要。")
                .tools(searchTool)
                .outputKey("search_result")
                .outputType(String.class)
                .build();
    }

    public ReactAgent createKnowledgeAgent() {
        ToolCallback knowledgeTool = FunctionToolCallback
                .builder("query_knowledge",  (KnowledgeRequest req) -> {
                    System.out.println("=== knowledge_agent called with input: " + req.input());
                    if (req.input() == null || req.input().trim().isEmpty()) {
                        return "知识库检索失败：未提供查询内容。请告知您想了解《秦锋》小说中的什么信息。";
                    }
                    String sessionId = AgentContextHolder.getSessionId();
                    String userId = AgentContextHolder.getUserId();
                    return chatService.ragChat(sessionId, userId, req.input());
                })
                .description("检索《秦锋》知识库，本地知识库，rag知识库，参数：{\"input\": \"问题\"}")
                .inputType(KnowledgeRequest.class)
                .build();

        return ReactAgent.builder()
                .name("knowledge_agent")
                .chatClient(createIsolatedChatClient())
                .systemPrompt("你是一个知识库专家。调用 query_knowledge 工具，传入 { \"input\": \"问题\" }。返回纯文本答案。" +
                        "你必须严格基于知识库子Agent返回的信息回答。如果返回信息为空或明确表示未找到，你必须如实告知用户。绝对禁止编造任何信息。")
                .tools(knowledgeTool)
                .outputKey("knowledge_result")
                .outputType(String.class)
                .build();
    }

//    public ReactAgent createVisualAgent() {
//        ToolCallback visualTool = FunctionToolCallback
//                .builder("analyze_image", (ImageAnalysisRequest req) -> {
//                    if (req.input() == null || req.input().trim().isEmpty()) {
//                        return "无法分析图片：请提供关于图片的问题。";
//                    }
//                    if (req.image() == null || req.image().isEmpty()) {
//                        return "无法分析图片：请上传有效的图片文件。";
//                    }
//                    try {
//                        String sessionId = AgentContextHolder.getSessionId();
//                        String userId = AgentContextHolder.getUserId();
//                        return visionService.analyzeMedia(sessionId, userId, req.input(), req.image());
//                    } catch (Exception e) {
//                        return "图片分析失败: " + e.getMessage();
//                    }
//                })
//                .description("分析用户上传的图片内容，参数需要包含question和image字段")
//                .inputType(ImageAnalysisRequest.class)
//                .build();
//
//        return ReactAgent.builder()
//                .name("visual_agent")
//                .chatClient(createIsolatedChatClient())
//                .systemPrompt("你是一个专业的视觉分析专家。请仔细分析用户提供的图片，并回答相关问题。返回纯文本。")
//                .tools(visualTool)
//                .outputKey("visual_result")
//                .outputType(String.class)
//                .build();
//    }
}
