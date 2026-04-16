package org.example.springairobot.Controller;

import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.AgentService;
import org.example.springairobot.service.CoordinatorAgent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Agent控制器
 * 
 * 提供智能体对话API接口：
 * - 工具调用对话：自动调用天气、搜索、知识库等工具
 * - 多模态协调对话：支持图片输入的多专家协作
 * 
 * 功能特点：
 * - 自动识别用户意图并调用相应工具
 * - 结合用户画像和历史记忆增强对话
 * - 支持多专家协调工作
 */
@RestController
@RequestMapping(AppConstants.ApiPaths.AGENT_BASE)
public class AgentController {

    private final AgentService agentService;
    private final CoordinatorAgent coordinatorAgent;

    public AgentController(AgentService agentService, CoordinatorAgent coordinatorAgent) {
        this.agentService = agentService;
        this.coordinatorAgent = coordinatorAgent;
    }

    /**
     * Agent对话
     * 
     * 自动调用工具（天气、搜索、知识库等）回答用户问题
     * 
     * @param sessionId 会话ID（可选）
     * @param userId 用户ID
     * @param message 用户消息
     * @return AI回复
     */
    @PostMapping(AppConstants.ApiPaths.AGENT_CHAT)
    public String chat(@RequestParam(required = false) String sessionId,
                       @RequestParam String userId,
                       @RequestParam String message) {
        return agentService.execute(sessionId, userId, message);
    }

    /**
     * 多模态协调对话
     * 
     * 支持图片输入的多专家协作对话，自动分配给合适的专家处理
     * 
     * @param sessionId 会话ID（可选）
     * @param userId 用户ID（可选）
     * @param message 用户消息
     * @param image 图片文件（可选）
     * @return AI回复
     */
    @PostMapping(AppConstants.ApiPaths.AGENT_COORDINATE)
    public String coordinateChat(@RequestParam(required = false) String sessionId,
                                 @RequestParam(required = false) String userId,
                                 @RequestParam String message,
                                 @RequestPart(required = false) MultipartFile image) {
        return coordinatorAgent.execute(sessionId, userId, message, image);
    }
}
