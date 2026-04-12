package org.example.springairobot.FunctionCalling.Tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Request;
import org.example.springairobot.PO.DTO.EntityExtraction;
import org.example.springairobot.service.ChatService;
import org.example.springairobot.service.VisionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class AgentTools {

    private final VisionService visionService;
    private final ChatService chatService;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${agent.search.brave-api-url}")
    private String braveApiUrl;
    @Value("${agent.search.brave-api-key:}")
    private String braveApiKey;
    @Value("${agent.weather.qweather.api-key:}")
    private String qweatherApiKey;
    @Value("${agent.weather.qweather.api-url}")
    private String qweatherApiUrl;
    @Value("${agent.weather.qweather.geo-api-url}")
    private String qweatherGeoApiUrl;

    public AgentTools(VisionService visionService, ChatService chatService) {
        this.visionService = visionService;
        this.chatService = chatService;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // ========== 复用现有服务 ==========
    @Tool(description = "分析一张图片的内容，根据用户的问题返回相关信息")
    public String analyzeImage(
            @ToolParam(description = "会话ID") String sessionId,
            @ToolParam(description = "关于图片的问题") String question,
            @ToolParam(description = "图片文件") MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }
        return visionService.analyzeMedia(sessionId, question, image);
    }

    @Tool(description = """
        从本地知识库中检索信息。
        当用户询问以下内容时必须调用此工具：
        - 小说《秦锋》的剧情、人物、事件等。
        - 例如：“秦锋的故事里有哪些人物？”、“秦锋的对手是谁？”。
        绝对禁止编造知识库内容。
        """)
    public String queryKnowledgeBase(
            @ToolParam(description = "会话ID") String sessionId,
            @ToolParam(description = "要查询的问题") String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("查询问题不能为空");
        }
        return chatService.ragChat(sessionId, query);
    }

    @Tool(description = "从知识库中批量抽取实体信息，如人物、地点、事件等")
    public List<EntityExtraction> extractEntities(
            @ToolParam(description = "会话ID") String sessionId,
            @ToolParam(description = "查询描述") String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("查询描述不能为空");
        }
        return chatService.extractEntities(sessionId, query);
    }

    // ========== 天气查询（带容错保护） ==========
    @Tool(description = """
        查询指定城市的实时天气信息。
        当用户询问以下内容时必须调用此工具：
        - 某城市的当前天气、温度、湿度、风力、降雨等。
        - 例如：“北京今天天气怎么样？”、“上海会下雨吗？”。
        绝对禁止编造天气数据。
        """)
    @Retry(name = "weather")
    @CircuitBreaker(name = "weather")
    @RateLimiter(name = "weather")
    public String getWeather(@ToolParam(description = "需要查询天气的城市名称，例如：北京、上海") String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("城市名称不能为空");
        }
        if (qweatherApiKey == null || qweatherApiKey.isBlank()) {
            return fallbackWeather(city);
        }
        // 使用 TimeLimiter 需要返回 CompletableFuture，因此内部调用异步方法
        try {
            return getWeatherAsync(city).get();
        } catch (Exception e) {
            return fallbackWeather(city);
        }
    }

    @TimeLimiter(name = "weather")
    public CompletableFuture<String> getWeatherAsync(String city) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 城市搜索
                String geoUrl = qweatherGeoApiUrl + "?location=" + city;
                Request geoRequest = new Request.Builder()
                        .url(geoUrl)
                        .header("X-QW-Api-Key", qweatherApiKey)
                        .header("Accept", "application/json")
                        .build();
                try (Response geoResponse = httpClient.newCall(geoRequest).execute()) {
                    if (!geoResponse.isSuccessful()) {
                        return fallbackWeather(city);
                    }
                    String geoBody = geoResponse.body().string();
                    JsonNode geoJson = objectMapper.readTree(geoBody);
                    String code = geoJson.path("code").asText();
                    if (!"200".equals(code)) {
                        return fallbackWeather(city);
                    }
                    JsonNode locationArray = geoJson.path("location");
                    if (!locationArray.isArray() || locationArray.isEmpty()) {
                        return fallbackWeather(city);
                    }
                    JsonNode firstLocation = locationArray.get(0);
                    String locationId = firstLocation.path("id").asText();
                    String displayName = firstLocation.path("name").asText();

                    // 2. 实时天气
                    String weatherUrl = qweatherApiUrl + "?location=" + locationId;
                    Request weatherRequest = new Request.Builder()
                            .url(weatherUrl)
                            .header("X-QW-Api-Key", qweatherApiKey)
                            .header("Accept", "application/json")
                            .build();
                    try (Response weatherResponse = httpClient.newCall(weatherRequest).execute()) {
                        if (!weatherResponse.isSuccessful()) {
                            return fallbackWeather(city);
                        }
                        String weatherBody = weatherResponse.body().string();
                        JsonNode weatherJson = objectMapper.readTree(weatherBody);
                        String weatherCode = weatherJson.path("code").asText();
                        if (!"200".equals(weatherCode)) {
                            return fallbackWeather(city);
                        }
                        JsonNode now = weatherJson.path("now");
                        String temp = now.path("temp").asText();
                        String text = now.path("text").asText();
                        String windDir = now.path("windDir").asText();
                        String windScale = now.path("windScale").asText();
                        String humidity = now.path("humidity").asText();

                        return String.format("%s当前天气：%s，温度%s°C，湿度%s%%，%s%s级",
                                displayName, text, temp, humidity, windDir, windScale);
                    }
                }
            } catch (Exception e) {
                return fallbackWeather(city);
            }
        });
    }

    // 降级方法：与 getWeather 参数、返回值完全一致
    private String fallbackWeather(String city) {
        return String.format("%s当前天气：晴，温度22°C，湿度40%%（模拟数据，实时服务暂时不可用）", city);
    }

    // ========== 网络搜索（带容错保护） ==========
    @Tool(description = """
        在互联网上搜索实时信息。
        当用户询问以下内容时必须调用此工具：
        - 最新新闻、百科知识、市场行情、产品价格、人物背景等无法从本地知识库获取的信息。
        - 例如：“2026年人工智能趋势”、“秦锋的对手有哪些”。
        绝对禁止编造搜索结果。
        """)
    @Retry(name = "search")
    @CircuitBreaker(name = "search")
    public String webSearch(@ToolParam(description = "搜索关键词") String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        if (braveApiKey == null || braveApiKey.isBlank()) {
            return fallbackWebSearch(query);
        }
        try {
            return webSearchAsync(query).get();
        } catch (Exception e) {
            return fallbackWebSearch(query);
        }
    }

    @TimeLimiter(name = "search")
    public CompletableFuture<String> webSearchAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = braveApiUrl + "?q=" + query + "&count=5";
                Request request = new Request.Builder()
                        .url(url)
                        .header("X-Subscription-Token", braveApiKey)
                        .header("Accept", "application/json")
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        return fallbackWebSearch(query);
                    }
                    String body = response.body().string();
                    JsonNode root = objectMapper.readTree(body);
                    JsonNode webResults = root.path("web").path("results");
                    if (!webResults.isArray() || webResults.isEmpty()) {
                        return fallbackWebSearch(query);
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("搜索 \"").append(query).append("\" 的结果：\n");
                    for (int i = 0; i < Math.min(3, webResults.size()); i++) {
                        JsonNode result = webResults.get(i);
                        sb.append(i + 1).append(". ").append(result.path("title").asText()).append("\n");
                        sb.append("   ").append(result.path("description").asText()).append("\n");
                        sb.append("   URL: ").append(result.path("url").asText()).append("\n");
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                return fallbackWebSearch(query);
            }
        });
    }

    private String fallbackWebSearch(String query) {
        return String.format("关于'%s'的搜索结果：1. 相关介绍... 2. 最新动态...（模拟数据，搜索服务暂时不可用）", query);
    }
}