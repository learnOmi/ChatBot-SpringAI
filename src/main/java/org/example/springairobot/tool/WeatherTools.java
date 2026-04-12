package org.example.springairobot.tool;

import org.example.springairobot.service.WeatherService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTools {

    private final WeatherService weatherService;

    public WeatherTools(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Tool(description = "获取指定城市的实时天气信息")
    public String getWeather(@ToolParam(description = "城市名称，如北京、上海") String city) {
        return weatherService.getWeather(city);
    }

    @Tool(description = "获取指定城市的未来三天天气预报")
    public String getForecast(@ToolParam(description = "城市名称") String city) {
        return weatherService.getForecast(city);
    }
}
