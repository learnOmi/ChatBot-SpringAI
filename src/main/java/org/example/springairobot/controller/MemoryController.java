package org.example.springairobot.controller;

import org.example.springairobot.service.memory.MemoryEnhancementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryEnhancementService memoryService;

    public MemoryController(MemoryEnhancementService memoryService) {
        this.memoryService = memoryService;
    }

    @DeleteMapping("/{userId}")
    public String deleteMemory(@PathVariable String userId) {
        memoryService.deleteUserMemory(userId);
        return "记忆已删除";
    }
}