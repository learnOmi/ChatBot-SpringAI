package org.example.springairobot.constants;

/**
 * 评估失败原因枚举
 */
public enum EvaluationFailReason {
    NONE("none"),
    BOTH("both"),
    RETRIEVAL("retrieval"),
    GENERATION("generation"),
    EVALUATION_ERROR("evaluation_error");

    private final String value;

    EvaluationFailReason(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
