package org.example.springairobot.constants;

/**
 * 消息类型枚举
 */
public enum MessageType {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
