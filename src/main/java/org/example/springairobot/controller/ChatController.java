package org.example.springairobot.controller;

import org.example.springairobot.service.ChatService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // 同步接口
    @GetMapping("/sync")
    public String syncChat(@RequestParam String message) {
        return chatService.chat(message);
    }

    // 直接返回HTML
    @GetMapping(value = "/stream", produces = "text/html; charset=UTF-8")
    public Flux<String> streamChat(@RequestParam String message) {
        return chatService.chatStream(message);
    }

    @GetMapping("/rag")
    public String ragChat(@RequestParam String message) {
        return chatService.ragChat(message);
    }

    @GetMapping(value = "/rag/stream", produces = "text/html; charset=UTF-8")
    public Flux<String> ragChatStream(@RequestParam String message) {
        return chatService.ragChatStream(message);
    }
}