package org.example.springairobot.Exception;

import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.stereotype.Component;

@Component
public class AgentExceptionHandler implements ToolExecutionExceptionProcessor {

    @Override
    public String process(ToolExecutionException exception) {
        Throwable cause = exception.getCause();
        String toolName = exception.getToolDefinition().name();

        if (cause instanceof IllegalArgumentException) {
            return String.format("工具 '%s' 调用失败：参数不正确。原因：%s", toolName, cause.getMessage());
        } else if (cause instanceof RuntimeException) {
            return String.format("工具 '%s' 执行时发生错误：%s", toolName, cause.getMessage());
        } else {
            return String.format("工具 '%s' 调用失败，请稍后重试。", toolName);
        }
    }
}