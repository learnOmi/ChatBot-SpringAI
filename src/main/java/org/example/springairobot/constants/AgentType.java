package org.example.springairobot.constants;

/**
 * Agent 类型枚举
 */
public enum AgentType {
    WEATHER("weather_agent"),
    SEARCH("search_agent"),
    KNOWLEDGE("knowledge_agent");

    private final String value;

    AgentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
