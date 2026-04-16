package org.example.springairobot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.springairobot.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 天气服务
 * 
 * 提供天气查询功能，支持实时天气和天气预报
 * 
 * 功能特点：
 * - 集成和风天气API获取实时天气数据
 * - 支持熔断、重试、限流等容错机制
 * - API不可用时自动降级到模拟数据
 * 
 * 容错配置：
 * - 重试：最多3次，间隔1秒
 * - 熔断：失败率50%触发，等待30秒恢复
 * - 限流：每秒最多10次请求
 */
@Service
public class WeatherService {

    /** 模拟天气数据，用于API不可用时的降级 */
    private static final Map<String, String> MOCK_WEATHER = Map.of(
            "北京", AppConstants.WeatherConstants.MOCK_WEATHER_BEIJING,
            "上海", AppConstants.WeatherConstants.MOCK_WEATHER_SHANGHAI,
            "广州", AppConstants.WeatherConstants.MOCK_WEATHER_GUANGZHOU
    );

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${agent.weather.qweather.api-key:}")
    private String qweatherApiKey;
    @Value("${agent.weather.qweather.api-url}")
    private String qweatherApiUrl;
    @Value("${agent.weather.qweather.geo-api-url}")
    private String qweatherGeoApiUrl;

    public WeatherService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(AppConstants.WeatherConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(AppConstants.WeatherConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 获取实时天气
     * 
     * 支持重试、熔断、限流等容错机制
     * 
     * @param city 城市名称
     * @return 天气信息字符串
     * @throws IllegalArgumentException 如果城市名称为空
     */
    @Retry(name = "weather")
    @CircuitBreaker(name = "weather")
    @RateLimiter(name = "weather")
    public String getWeather(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.WeatherConstants.ERROR_CITY_EMPTY);
        }
        // API Key未配置时使用模拟数据
        if (qweatherApiKey == null || qweatherApiKey.isBlank()) {
            return fallbackWeather(city);
        }
        try {
            return getWeatherAsync(city).get();
        } catch (Exception e) {
            return fallbackWeather(city);
        }
    }

    /**
     * 异步获取天气
     * 
     * 支持超时控制
     * 
     * @param city 城市名称
     * @return 异步天气结果
     */
    @TimeLimiter(name = "weather")
    public CompletableFuture<String> getWeatherAsync(String city) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 先获取城市ID
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
                    if (!AppConstants.WeatherConstants.API_SUCCESS_CODE.equals(code)) {
                        return fallbackWeather(city);
                    }
                    JsonNode locationArray = geoJson.path("location");
                    if (!locationArray.isArray() || locationArray.isEmpty()) {
                        return fallbackWeather(city);
                    }
                    JsonNode firstLocation = locationArray.get(0);
                    String locationId = firstLocation.path("id").asText();
                    String displayName = firstLocation.path("name").asText();

                    // 2. 根据城市ID获取天气
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
                        if (!AppConstants.WeatherConstants.API_SUCCESS_CODE.equals(weatherCode)) {
                            return fallbackWeather(city);
                        }
                        JsonNode now = weatherJson.path("now");
                        String temp = now.path("temp").asText();
                        String text = now.path("text").asText();
                        String windDir = now.path("windDir").asText();
                        String windScale = now.path("windScale").asText();
                        String humidity = now.path("humidity").asText();

                        return String.format(AppConstants.WeatherConstants.WEATHER_FORMAT,
                                displayName, text, temp, humidity, windDir, windScale);
                    }
                }
            } catch (Exception e) {
                return fallbackWeather(city);
            }
        });
    }

    /**
     * 获取天气预报
     * 
     * @param city 城市名称
     * @return 天气预报字符串
     */
    public String getForecast(String city) {
        return String.format(AppConstants.WeatherConstants.FORECAST_TEMPLATE, city);
    }

    /**
     * 降级处理：返回模拟天气数据
     * 
     * @param city 城市名称
     * @return 模拟天气数据
     */
    private String fallbackWeather(String city) {
        return MOCK_WEATHER.getOrDefault(city, 
            String.format(AppConstants.WeatherConstants.MOCK_WEATHER_TEMPLATE, city));
    }
}
