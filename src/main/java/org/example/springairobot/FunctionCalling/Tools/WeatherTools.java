package org.example.springairobot.FunctionCalling.Tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WeatherTools {

    // 模拟天气数据，实际应调用第三方 API
    private static final Map<String, String> MOCK_WEATHER = Map.of(
            "北京", "晴，温度 22°C，湿度 40%",
            "上海", "多云，温度 25°C，湿度 60%",
            "广州", "雷阵雨，温度 28°C，湿度 80%"
    );

    @Tool(description = "获取指定城市的实时天气信息")
    public String getWeather(@ToolParam(description = "城市名称，如北京、上海") String city) {
        // 实际项目中可替换为 RestTemplate 调用天气 API
        return MOCK_WEATHER.getOrDefault(city, "抱歉，暂时无法获取该城市的天气信息。");
    }

    @Tool(description = "获取指定城市的未来三天天气预报")
    public String getForecast(@ToolParam(description = "城市名称") String city) {
        // 模拟数据
        return city + "未来三天：周一晴，周二多云，周三小雨";
    }
}