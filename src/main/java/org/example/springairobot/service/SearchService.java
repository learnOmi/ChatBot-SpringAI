package org.example.springairobot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.springairobot.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 搜索服务
 * 
 * 提供网络搜索功能，集成Brave Search API
 * 
 * 功能特点：
 * - 集成Brave Search API获取实时搜索结果
 * - 支持熔断、重试等容错机制
 * - API不可用时自动降级到模拟数据
 * 
 * 容错配置：
 * - 重试：最多2次，间隔2秒
 * - 熔断：失败率60%触发，等待60秒恢复
 */
@Service
public class SearchService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${agent.search.brave-api-url}")
    private String braveApiUrl;
    @Value("${agent.search.brave-api-key:}")
    private String braveApiKey;

    public SearchService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(AppConstants.SearchConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(AppConstants.SearchConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 网络搜索
     * 
     * 支持重试、熔断等容错机制
     * 
     * @param query 搜索关键词
     * @return 搜索结果字符串
     * @throws IllegalArgumentException 如果搜索关键词为空
     */
    @Retry(name = "search")
    @CircuitBreaker(name = "search")
    public String webSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.SearchConstants.ERROR_QUERY_EMPTY);
        }
        // API Key未配置时使用模拟数据
        if (braveApiKey == null || braveApiKey.isBlank()) {
            return fallbackWebSearch(query);
        }
        try {
            return webSearchAsync(query).get();
        } catch (Exception e) {
            return fallbackWebSearch(query);
        }
    }

    /**
     * 异步网络搜索
     * 
     * 支持超时控制
     * 
     * @param query 搜索关键词
     * @return 异步搜索结果
     */
    @TimeLimiter(name = "search")
    public CompletableFuture<String> webSearchAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = braveApiUrl + "?q=" + query + "&count=" + AppConstants.SearchConstants.SEARCH_RESULT_COUNT;
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
                    sb.append(String.format(AppConstants.SearchConstants.SEARCH_RESULT_HEADER, query));
                    for (int i = 0; i < Math.min(AppConstants.SearchConstants.DISPLAY_RESULT_COUNT, webResults.size()); i++) {
                        JsonNode result = webResults.get(i);
                        sb.append(String.format(AppConstants.SearchConstants.SEARCH_RESULT_ITEM,
                                i + 1, result.path("title").asText(),
                                result.path("description").asText(), result.path("url").asText()));
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                return fallbackWebSearch(query);
            }
        });
    }

    /**
     * 降级处理：返回模拟搜索数据
     * 
     * @param query 搜索关键词
     * @return 模拟搜索结果
     */
    private String fallbackWebSearch(String query) {
        return String.format(AppConstants.SearchConstants.MOCK_SEARCH_TEMPLATE, query);
    }
}
