package org.example.springairobot.Controller;

import org.example.springairobot.PO.DTO.EntityExtraction;
import org.example.springairobot.PO.DTO.RagAnswer;
import org.example.springairobot.PO.Tables.ConversationMessage;
import org.example.springairobot.PO.Tables.ConversationSession;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.ChatService;
import org.example.springairobot.service.ConversationService;
import org.example.springairobot.service.memory.MemoryEnhancementService;
import org.example.springairobot.service.vision.VisionService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天控制器
 * 
 * 提供多种对话API接口：
 * - 普通对话：基础AI对话功能
 * - RAG对话：基于知识库的增强对话
 * - 流式对话：支持流式输出的对话
 * - 实体抽取：从知识库中提取实体信息
 * - 会话管理：创建、查询、删除会话
 */
@RestController
@RequestMapping(AppConstants.ApiPaths.CHAT_BASE)
public class ChatController {
    
    private final ChatService chatService;
    private final ConversationService conversationService;

    public ChatController(ChatService chatService, ConversationService conversationService, VisionService visionService, MemoryEnhancementService memoryEnhancementService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }

    /**
     * 同步普通对话
     * 
     * @param message 用户消息
     * @param userId 用户ID（可选）
     * @param sessionId 会话ID（可选）
     * @return AI回复
     */
    @GetMapping(AppConstants.ApiPaths.CHAT_SYNC)
    public String syncChat(@RequestParam String message, @RequestParam(required = false) String userId, @RequestParam(required = false) String sessionId) {
        return chatService.chat(sessionId, userId, message);
    }

    /**
     * 流式普通对话
     * 
     * @param message 用户消息
     * @param userId 用户ID（可选）
     * @param sessionId 会话ID（可选）
     * @return 流式AI回复
     */
    @GetMapping(value = AppConstants.ApiPaths.CHAT_STREAM, produces = AppConstants.ControllerMessages.CONTENT_TYPE_TEXT_HTML)
    public Flux<String> streamChat(@RequestParam String message, @RequestParam(required = false) String userId, @RequestParam(required = false) String sessionId) {
        return chatService.chatStream(sessionId, userId, message);
    }

    /**
     * 同步RAG对话
     * 
     * 基于知识库的增强对话，自动检索相关文档辅助回答
     * 
     * @param message 用户消息
     * @param userId 用户ID（可选）
     * @param sessionId 会话ID（可选）
     * @return AI回复
     */
    @GetMapping(AppConstants.ApiPaths.CHAT_RAG)
    public String ragChat(@RequestParam String message, @RequestParam(required = false) String userId, @RequestParam(required = false) String sessionId) {
        return chatService.ragChat(sessionId, userId, message);
    }

    /**
     * 流式RAG对话
     * 
     * @param message 用户消息
     * @param userId 用户ID（可选）
     * @param sessionId 会话ID（可选）
     * @return 流式AI回复
     */
    @GetMapping(value = AppConstants.ApiPaths.CHAT_RAG_STREAM, produces = AppConstants.ControllerMessages.CONTENT_TYPE_TEXT_HTML)
    public Flux<String> ragChatStream(@RequestParam String message, @RequestParam(required = false) String userId, @RequestParam(required = false) String sessionId) {
        return chatService.ragChatStream(sessionId, userId, message);
    }

    /**
     * 结构化RAG回答
     * 
     * 返回包含答案、引用来源和置信度的结构化结果
     * 
     * @param message 用户消息
     * @param userId 用户ID（可选）
     * @param sessionId 会话ID（可选）
     * @return 结构化答案对象
     */
    @GetMapping(AppConstants.ApiPaths.CHAT_RAG_STRUCTURED)
    public RagAnswer ragChatStructured(@RequestParam String message,
                                       @RequestParam(required = false) String userId,
                                       @RequestParam(required = false) String sessionId) {
        return chatService.ragChatStructured(sessionId, userId, message);
    }

    /**
     * 实体抽取
     * 
     * 从知识库中提取与查询相关的实体信息
     * 
     * @param query 查询内容
     * @param userId 用户ID（可选）
     * @param sessionId 会话ID（可选）
     * @return 实体列表
     */
    @GetMapping(AppConstants.ApiPaths.CHAT_RAG_EXTRACT_ENTITIES)
    public List<EntityExtraction> extractEntities(@RequestParam String query,
                                                  @RequestParam(required = false) String userId,
                                                  @RequestParam(required = false) String sessionId) {
        return chatService.extractEntities(sessionId, userId, query);
    }

    /**
     * 获取会话历史
     * 
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @GetMapping(AppConstants.ApiPaths.CHAT_HISTORY)
    public List<ConversationMessage> history(@PathVariable String sessionId) {
        return conversationService.getHistory(sessionId);
    }

    /**
     * 获取用户所有历史消息
     * 
     * @param userId 用户ID
     * @return 消息列表
     */
    @GetMapping(AppConstants.ApiPaths.CHAT_USER_HISTORY)
    public List<ConversationMessage> userHistory(@PathVariable String userId) {
        return conversationService.getHistoryByUserId(userId);
    }

    /**
     * 获取所有会话列表
     * 
     * @return 会话列表
     */
    @GetMapping(AppConstants.ApiPaths.CHAT_SESSIONS)
    public List<ConversationSession> sessions() {
        return conversationService.listSessions();
    }

    /**
     * 获取用户的所有会话
     * 
     * @param userId 用户ID
     * @return 会话列表
     */
    @GetMapping(AppConstants.ApiPaths.CHAT_USER_SESSIONS)
    public List<ConversationSession> userSessions(@RequestParam String userId) {
        return conversationService.listSessionsByUser(userId);
    }

    /**
     * 删除会话
     * 
     * @param sessionId 会话ID
     */
    @DeleteMapping(AppConstants.ApiPaths.CHAT_SESSION)
    public void deleteSession(@PathVariable String sessionId) {
        conversationService.deleteSession(sessionId);
    }

    /**
     * 创建新会话
     * 
     * @param userId 用户ID（可选）
     * @param title 会话标题（可选）
     * @return 新创建的会话ID
     */
    @PostMapping(AppConstants.ApiPaths.CHAT_SESSION)
    public String newSession(@RequestParam(required = false) String userId, @RequestParam(required = false) String title) {
        return conversationService.createSession(userId, title);
    }
}
