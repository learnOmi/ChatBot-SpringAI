package org.example.springairobot.tool;

import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.WeatherService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 天气工具类
 * 
 * 提供天气查询相关的工具函数，供AI Agent自动调用
 * 
 * 功能特点：
 * - 获取实时天气信息
 * - 获取天气预报
 * - 自动集成和风天气API
 * - 支持降级到模拟数据
 */
@Component
public class WeatherTools {

    private final WeatherService weatherService;

    public WeatherTools(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * 获取实时天气
     * 
     * @param city 城市名称
     * @return 天气信息字符串
     */
    @Tool(description = AppConstants.WeatherConstants.TOOL_DESC_GET_WEATHER)
    public String getWeather(@ToolParam(description = AppConstants.WeatherConstants.TOOL_PARAM_CITY) String city) {
        return weatherService.getWeather(city);
    }

    /**
     * 获取天气预报
     * 
     * @param city 城市名称
     * @return 天气预报字符串
     */
    @Tool(description = AppConstants.WeatherConstants.TOOL_DESC_GET_FORECAST)
    public String getForecast(@ToolParam(description = AppConstants.WeatherConstants.TOOL_PARAM_CITY) String city) {
        return weatherService.getForecast(city);
    }
}
