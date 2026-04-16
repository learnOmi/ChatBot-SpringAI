package org.example.springairobot.Config;

import org.example.springairobot.Advisor.EvaluationAdvisor;
import org.example.springairobot.Advisor.SelfHealingAdvisor;
import org.example.springairobot.Advisor.SelfHealingRecursiveAdvisor;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.tool.KnowledgeTools;
import org.example.springairobot.tool.SearchTools;
import org.example.springairobot.tool.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent配置类
 * 
 * 配置智能体相关的组件：
 * - Agent ChatClient：支持工具调用的对话客户端
 * - Coordinator ChatClient：多专家协调对话客户端
 * - 工具集：天气、搜索、知识库等工具
 * - Advisor链：记忆、日志、评估、自修复等
 */
@Configuration
public class AgentConfig {

    /**
     * 日志顾问
     * 
     * 记录对话过程中的请求和响应，用于调试和监控
     * 
     * @return 日志顾问实例
     */
    @Bean
    public SimpleLoggerAdvisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    /**
     * Agent对话客户端
     * 
     * 支持工具调用的智能对话客户端，可自动调用：
     * - 天气工具：查询实时天气和预报
     * - 搜索工具：网络搜索
     * - 知识库工具：本地知识库查询
     * 
     * Advisor链顺序：
     * 1. MemoryAdvisor：对话记忆
     * 2. LoggerAdvisor：日志记录
     * 3. RecursiveAdvisor：自修复重试（最外层）
     * 4. EvaluationAdvisor：答案评估
     * 5. SelfHealingAdvisor：自修复策略
     * 
     * @param builder ChatClient构建器
     * @param memoryAdvisor 记忆顾问
     * @param loggerAdvisor 日志顾问
     * @param evaluationAdvisor 评估顾问
     * @param selfHealingAdvisor 自修复顾问
     * @param recursiveAdvisor 递归自修复顾问
     * @param weatherTools 天气工具
     * @param searchTools 搜索工具
     * @param knowledgeTools 知识库工具
     * @return Agent对话客户端实例
     */
    @Bean
    @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_AGENT_CHAT_CLIENT)
    public ChatClient agentChatClient(ChatClient.Builder builder,
                                      @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_MESSAGE_CHAT_MEMORY_ADVISOR) MessageChatMemoryAdvisor memoryAdvisor,
                                      SimpleLoggerAdvisor loggerAdvisor,
                                      EvaluationAdvisor evaluationAdvisor,
                                      SelfHealingAdvisor selfHealingAdvisor,
                                      SelfHealingRecursiveAdvisor recursiveAdvisor,
                                      WeatherTools weatherTools,
                                      SearchTools searchTools,
                                      KnowledgeTools knowledgeTools) {
        ChatClient.Builder retryBuilder = builder.clone()
                .defaultSystem(AppConstants.AgentPrompts.AGENT_SYSTEM_PROMPT)
                .defaultAdvisors(
                    memoryAdvisor,
                    loggerAdvisor,
                    evaluationAdvisor,
                    selfHealingAdvisor
                )
                .defaultTools(weatherTools, searchTools, knowledgeTools);
        
        recursiveAdvisor.setRetryBuilder(retryBuilder);
        
        return builder.clone()
                .defaultSystem(AppConstants.AgentPrompts.AGENT_SYSTEM_PROMPT)
                .defaultAdvisors(
                    memoryAdvisor,
                    loggerAdvisor,
                    recursiveAdvisor,
                    evaluationAdvisor,
                    selfHealingAdvisor
                )
                .defaultTools(weatherTools, searchTools, knowledgeTools)
                .build();
    }

    /**
     * 协调器对话客户端
     * 
     * 多专家协调对话客户端，负责分配任务给合适的专家处理
     * 支持图片等多模态输入
     * 
     * Advisor链顺序：
     * 1. MemoryAdvisor：对话记忆
     * 2. LoggerAdvisor：日志记录
     * 3. RecursiveAdvisor：自修复重试（最外层）
     * 4. EvaluationAdvisor：答案评估
     * 5. SelfHealingAdvisor：自修复策略
     * 
     * @param builder ChatClient构建器
     * @param memoryAdvisor 记忆顾问
     * @param loggerAdvisor 日志顾问
     * @param evaluationAdvisor 评估顾问
     * @param selfHealingAdvisor 自修复顾问
     * @param recursiveAdvisor 递归自修复顾问
     * @return 协调器对话客户端实例
     */
    @Bean
    @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_COORDINATOR_CHAT_CLIENT)
    public ChatClient coordinatorChatClient(ChatClient.Builder builder,
                                            @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_MESSAGE_CHAT_MEMORY_ADVISOR) MessageChatMemoryAdvisor memoryAdvisor,
                                            SimpleLoggerAdvisor loggerAdvisor,
                                            EvaluationAdvisor evaluationAdvisor,
                                            SelfHealingAdvisor selfHealingAdvisor,
                                            SelfHealingRecursiveAdvisor recursiveAdvisor) {
        ChatClient.Builder retryBuilder = builder.clone()
                .defaultSystem(AppConstants.AgentPrompts.COORDINATOR_SYSTEM_PROMPT)
                .defaultAdvisors(
                    memoryAdvisor,
                    loggerAdvisor,
                    evaluationAdvisor,
                    selfHealingAdvisor
                );
        
        recursiveAdvisor.setRetryBuilder(retryBuilder);
        
        return builder.clone()
                .defaultSystem(AppConstants.AgentPrompts.COORDINATOR_SYSTEM_PROMPT)
                .defaultAdvisors(
                    memoryAdvisor,
                    loggerAdvisor,
                    recursiveAdvisor,
                    evaluationAdvisor,
                    selfHealingAdvisor
                )
                .build();
    }
}
