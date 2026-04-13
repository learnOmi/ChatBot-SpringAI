package org.example.springairobot.service;

import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.example.springairobot.PO.Context.AgentContextHolder;
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

    public CoordinatorAgent(@Qualifier("coordinatorChatClient") ChatClient coordinatorChatClient,
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
                String imageDescription = visionService.analyzeMedia(sessionId, userId, "请详细描述这张图片的内容", image);
                // 将视觉描述作为上下文注入用户输入
                enhancedUserInput = String.format("""
                用户上传了一张图片，视觉分析结果如下：
                %s
                
                用户的问题：%s
                """, imageDescription, userInput);
            } catch (IOException e) {
                enhancedUserInput = userInput + "\n（图片分析失败：" + e.getMessage() + "）";
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
                    .name("coordinator")
                    .chatClient(coordinatorChatClient)
                    .tools(agentTools)
                    .build();

            String response;
            try {
                response = coordinator.call(enhancedUserInput).getText();
            } catch (GraphRunnerException e) {
                response = "智能体执行过程中发生错误: " + e.getMessage();
            }

            conversationService.savePair(effectiveSessionId, userId, enhancedUserInput, response, null);
            return response;
        } finally {
            AgentContextHolder.clear();
        }
    }
}