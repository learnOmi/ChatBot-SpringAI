package org.example.springairobot.controller;

import org.example.springairobot.PO.DTO.EntityExtraction;
import org.example.springairobot.PO.DTO.RagAnswer;
import org.example.springairobot.PO.Tables.ConversationMessage;
import org.example.springairobot.PO.Tables.ConversationSession;
import org.example.springairobot.service.ChatService;
import org.example.springairobot.service.ConversationService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final ConversationService conversationService;

    public ChatController(ChatService chatService, ConversationService conversationService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }

    // 同步接口
    @GetMapping("/sync")
    public String syncChat(@RequestParam String message, @RequestParam(required = false) String sessionId) {
        return chatService.chat(sessionId, message);
    }

    // 直接返回HTML
    @GetMapping(value = "/stream", produces = "text/html; charset=UTF-8")
    public Flux<String> streamChat(@RequestParam String message, @RequestParam(required = false) String sessionId) {
        return chatService.chatStream(sessionId, message);
    }

    @GetMapping("/tools")
    public String chatWithTools(@RequestParam String message,
                                @RequestParam(required = false) String sessionId) {
        return chatService.chatWithTools(sessionId, message);
    }

    @GetMapping(value = "/tools/stream", produces = "text/html; charset=UTF-8")
    public Flux<String> chatWithToolsStream(@RequestParam String message,
                                            @RequestParam(required = false) String sessionId) {
        return chatService.chatWithToolsStream(sessionId, message);
    }

    @GetMapping("/rag")
    public String ragChat(@RequestParam String message, @RequestParam(required = false) String sessionId) {
        return chatService.ragChat(sessionId, message);
    }

    @GetMapping(value = "/rag/stream", produces = "text/html; charset=UTF-8")
    public Flux<String> ragChatStream(@RequestParam String message, @RequestParam(required = false) String sessionId) {
        return chatService.ragChatStream(sessionId, message);
    }

    @GetMapping("/rag/structured")
    public RagAnswer ragChatStructured(@RequestParam String message,
                                       @RequestParam(required = false) String sessionId) {
        return chatService.ragChatStructured(sessionId, message);
    }

    @GetMapping("/rag/extractentities")
    public List<EntityExtraction> extractEntities(@RequestParam String query,
                                                  @RequestParam(required = false) String sessionId) {
        return chatService.extractEntities(sessionId, query);
    }

    // 获取会话历史
    @GetMapping("/history/{sessionId}")
    public List<ConversationMessage> history(@PathVariable String sessionId) {
        return conversationService.getHistory(sessionId);
    }

    // 列出所有会话
    @GetMapping("/sessions")
    public List<ConversationSession> sessions() {
        return conversationService.listSessions();
    }

    // 删除会话
    @DeleteMapping("/session/{sessionId}")
    public void deleteSession(@PathVariable String sessionId) {
        conversationService.deleteSession(sessionId);
    }

    // 创建新会话
    @PostMapping("/session")
    public String newSession(@RequestParam(required = false) String title) {
        return conversationService.createSession(title);
    }
}