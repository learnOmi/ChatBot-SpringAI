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

    // 流式接口，返回Server-Sent Events类型的数据
    @GetMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> streamChat(@RequestParam String message) {
        return chatService.chatStream(message);
    }
}