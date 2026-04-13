package org.example.springairobot.Config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentConfig {

    @Bean
    public SimpleLoggerAdvisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    @Bean
    @Qualifier("agentChatClient")
    public ChatClient agentChatClient(ChatClient.Builder builder,
                                      MessageChatMemoryAdvisor memoryAdvisor,
                                      SimpleLoggerAdvisor loggerAdvisor,
                                      List<ToolCallback> allToolCallbacks) {
        return builder
                .defaultSystem("""
                    你是一个智能助手，必须通过调用工具来获取实时信息。
                    
                    **重要规则**：
                    - 当用户询问天气、搜索、知识库内容时，你必须调用相应的工具。
                    - 绝对禁止编造或猜测答案，所有信息必须来自工具返回的结果。
                    - 如果工具调用失败，请如实告知用户服务暂时不可用。
                    - 回答时，请引用工具返回的具体数据。
                    """)
                .defaultAdvisors(memoryAdvisor, loggerAdvisor)
                .defaultTools(allToolCallbacks.toArray(new ToolCallback[0]))
                .build();
    }

    // ========== 新模式：协调者 ChatClient ==========
    @Bean
    @Qualifier("coordinatorChatClient")
    public ChatClient coordinatorChatClient(ChatClient.Builder builder,
                                            MessageChatMemoryAdvisor memoryAdvisor,
                                            SimpleLoggerAdvisor loggerAdvisor) {
        return builder
                .defaultSystem("""
                        你是一个智能助理协调员。你的唯一职责是分析用户请求，并调用合适的专家Agent来获取信息。
                        
                        可用专家：
                        - weather_agent：查询天气，需传入 {"input": "城市名"}
                        - search_agent：网络搜索，需传入 {"input": "关键词"}
                        - knowledge_agent：知识库检索，需传入 {"input": "关于《秦锋》小说、知识库的的问题"
                        
                        规则：
                        - 必须调用工具获取信息，绝对禁止编造答案。
                        - 将专家的结果整合成简洁完整的回答。
                        - 不要输出任何无关的JSON或元数据。
                        - 调用任何Agent时，必须严格按照上述JSON格式传递参数，确保包含"input"字段等必须字段。
                        """)
                .defaultAdvisors(memoryAdvisor, loggerAdvisor)
                .build();
    }
}