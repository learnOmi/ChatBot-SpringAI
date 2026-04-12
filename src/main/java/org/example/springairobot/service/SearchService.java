package org.example.springairobot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Retry(name = "search")
    @CircuitBreaker(name = "search")
    public String webSearch(String query) {
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
