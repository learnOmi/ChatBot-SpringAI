package org.example.springairobot.service;

import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.example.springairobot.PO.Context.AgentContextHolder;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.agent.SubAgentFactory;
import org.example.springairobot.service.vision.VisionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class CoordinatorAgent {

    private final ChatClient coordinatorChatClient;
    private final SubAgentFactory subAgentFactory;
    private final ConversationService conversationService;
    private final VisionService visionService;

    public CoordinatorAgent(@Qualifier(AppConstants.AiConfigConstants.QUALIFIER_COORDINATOR_CHAT_CLIENT) ChatClient coordinatorChatClient,
                            SubAgentFactory subAgentFactory,
                            ConversationService conversationService,
                            VisionService visionService) {
        this.coordinatorChatClient = coordinatorChatClient;
        this.subAgentFactory = subAgentFactory;
        this.conversationService = conversationService;
        this.visionService = visionService;
    }

    public String execute(String sessionId, String userId, String userInput, MultipartFile image) {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);

        String enhancedUserInput = userInput;
        if (image != null && !image.isEmpty()) {
            try {
                // 调用视觉服务获取图片描述
                String imageDescription = visionService.analyzeMedia(sessionId, userId,
                        AppConstants.CoordinatorConstants.IMAGE_DESCRIPTION_PROMPT, image);
                // 将视觉描述作为上下文注入用户输入
                enhancedUserInput = String.format(AppConstants.CoordinatorConstants.IMAGE_CONTEXT_TEMPLATE,
                        imageDescription, userInput);
            } catch (IOException e) {
                enhancedUserInput = userInput + String.format(
                        AppConstants.CoordinatorConstants.IMAGE_ANALYSIS_FAILED_TEMPLATE, e.getMessage());
            }
        }

        // 同时设置 sessionId 和 userId
        AgentContextHolder.setSessionId(effectiveSessionId);
        AgentContextHolder.setUserId(userId);
        try {
            // 创建子 Agent（包括视觉）
            ReactAgent weatherAgent = subAgentFactory.createWeatherAgent();
            ReactAgent searchAgent = subAgentFactory.createSearchAgent();
            ReactAgent knowledgeAgent = subAgentFactory.createKnowledgeAgent();
            //ReactAgent visualAgent = subAgentFactory.createVisualAgent(); // 视觉 Agent

            // 包装为工具
            List<ToolCallback> agentTools = List.of(
                    AgentTool.getFunctionToolCallback(weatherAgent),
                    AgentTool.getFunctionToolCallback(searchAgent),
                    AgentTool.getFunctionToolCallback(knowledgeAgent)
                    //AgentTool.getFunctionToolCallback(visualAgent)
            );

            // 构建协调者
            ReactAgent coordinator = ReactAgent.builder()
                    .name(AppConstants.CoordinatorConstants.COORDINATOR_AGENT_NAME)
                    .chatClient(coordinatorChatClient)
                    .tools(agentTools)
                    .build();

            String response;
            try {
                response = coordinator.call(enhancedUserInput).getText();
            } catch (GraphRunnerException e) {
                response = AppConstants.CoordinatorConstants.ERROR_AGENT_EXECUTION_FAILED + e.getMessage();
            }

            conversationService.savePair(effectiveSessionId, userId, enhancedUserInput, response, null);
            return response;
        } finally {
            AgentContextHolder.clear();
        }
    }
}