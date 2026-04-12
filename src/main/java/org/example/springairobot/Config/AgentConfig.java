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
                .defaultAdvisors(memoryAdvisor, new SimpleLoggerAdvisor())
                .defaultTools(agentTools)   // 注册所有 @Tool 方法
                .build();
    }
}