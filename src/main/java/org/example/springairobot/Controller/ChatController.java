package org.example.springairobot.Controller;

import org.example.springairobot.PO.DTO.EntityExtraction;
import org.example.springairobot.PO.DTO.RagAnswer;
import org.example.springairobot.PO.Tables.ConversationMessage;
import org.example.springairobot.PO.Tables.ConversationSession;
import org.example.springairobot.service.ChatService;
import org.example.springairobot.service.ConversationService;
import org.example.springairobot.service.memory.MemoryEnhancementService;
import org.example.springairobot.service.vision.VisionService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final ConversationService conversationService;
    private final VisionService visionService;
    private final MemoryEnhancementService memoryEnhancementService;

    public ChatController(ChatService chatService, ConversationService conversationService, VisionService visionService, MemoryEnhancementService memoryEnhancementService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
        this.visionService = visionService;
        this.memoryEnhancementService = memoryEnhancementService;
    }

    // 同步接口
    @GetMapping("/sync")
    public String syncChat(@RequestParam String message, @RequestParam(required = false) String userId, @RequestParam(required = false) String sessionId) {
        return chatService.chat(sessionId, userId, message);
    }

    // 直接返回HTML
    @GetMapping(value = "/stream", produces = "text/html; charset=UTF-8")
    public Flux<String> streamChat(@RequestParam String message, @RequestParam(required = false) String userId, @RequestParam(required = false) String sessionId) {
        return chatService.chatStream(sessionId, userId, message);
    }

    @GetMapping("/rag")
    public String ragChat(@RequestParam String message, @RequestParam(required = false) String userId, @RequestParam(required = false) String sessionId) {
        return chatService.ragChat(sessionId, userId, message);
    }

    @GetMapping(value = "/rag/stream", produces = "text/html; charset=UTF-8")
    public Flux<String> ragChatStream(@RequestParam String message, @RequestParam(required = false) String userId, @RequestParam(required = false) String sessionId) {
        return chatService.ragChatStream(sessionId, userId, message);
    }

    @GetMapping("/rag/structured")
    public RagAnswer ragChatStructured(@RequestParam String message,
                                       @RequestParam(required = false) String userId,
                                       @RequestParam(required = false) String sessionId) {
        return chatService.ragChatStructured(sessionId, userId, message);
    }

    @GetMapping("/rag/extractentities")
    public List<EntityExtraction> extractEntities(@RequestParam String query,
                                                  @RequestParam(required = false) String userId,
                                                  @RequestParam(required = false) String sessionId) {
        return chatService.extractEntities(sessionId, userId, query);
    }

    // 获取会话历史
    @GetMapping("/history/{sessionId}")
    public List<ConversationMessage> history(@PathVariable String sessionId) {
        return conversationService.getHistory(sessionId);
    }

    // 获取用户会话消息
    @GetMapping("/user-history/{userId}")
    public List<ConversationMessage> userHistory(@PathVariable String userId) {
        return conversationService.getHistoryByUserId(userId);
    }

    // 列出所有会话
    @GetMapping("/sessions")
    public List<ConversationSession> sessions() {
        return conversationService.listSessions();
    }

    // 列出用户会话
    @GetMapping("/user-sessions")
    public List<ConversationSession> userSessions(@RequestParam String userId) {
        return conversationService.listSessionsByUser(userId);
    }

    // 删除会话
    @DeleteMapping("/session/{sessionId}")
    public void deleteSession(@PathVariable String sessionId) {
        conversationService.deleteSession(sessionId);
    }

    // 创建新会话
    @PostMapping("/session")
    public String newSession(@RequestParam(required = false) String userId, @RequestParam(required = false) String title) {
        return conversationService.createSession(userId, title);
    }
}