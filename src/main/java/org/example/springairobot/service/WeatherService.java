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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class WeatherService {

    private static final Map<String, String> MOCK_WEATHER = Map.of(
            "北京", "晴，温度 22°C，湿度 40%",
            "上海", "多云，温度 25°C，湿度 60%",
            "广州", "雷阵雨，温度 28°C，湿度 80%"
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
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Retry(name = "weather")
    @CircuitBreaker(name = "weather")
    @RateLimiter(name = "weather")
    public String getWeather(String city) {
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("城市名称不能为空");
        }
        if (qweatherApiKey == null || qweatherApiKey.isBlank()) {
            return fallbackWeather(city);
        }
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

    public String getForecast(String city) {
        return city + "未来三天：周一晴，周二多云，周三小雨";
    }

    private String fallbackWeather(String city) {
        return MOCK_WEATHER.getOrDefault(city, 
            String.format("%s当前天气：晴，温度22°C，湿度40%%（模拟数据，实时服务暂时不可用）", city));
    }
}
