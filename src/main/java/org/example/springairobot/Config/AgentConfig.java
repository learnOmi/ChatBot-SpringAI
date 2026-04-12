package org.example.springairobot.Config;

import org.example.springairobot.FunctionCalling.Tools.AgentTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    @Qualifier("agentChatClient")
    public ChatClient agentChatClient(ChatClient.Builder builder,
                                      MessageChatMemoryAdvisor memoryAdvisor,
                                      AgentTools agentTools) {
        return builder
                .defaultSystem("""
                    你是一个智能助手，必须通过调用工具来获取实时信息。
                    
                    **重要规则**：
                    - 当用户询问天气、搜索、知识库内容时，你必须调用相应的工具。
                    - 绝对禁止编造或猜测答案，所有信息必须来自工具返回的结果。
                    - 如果工具调用失败，请如实告知用户服务暂时不可用。
                    - 回答时，请引用工具返回的具体数据。
                    """)
                .defaultAdvisors(memoryAdvisor, new SimpleLoggerAdvisor())
                .defaultTools(agentTools)   // 注册所有 @Tool 方法
                .build();
    }
}