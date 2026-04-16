package org.example.springairobot.Controller;

import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.memory.MemoryEnhancementService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.ApiPaths.MEMORY_BASE)
public class MemoryController {

    private final MemoryEnhancementService memoryService;

    public MemoryController(MemoryEnhancementService memoryService) {
        this.memoryService = memoryService;
    }

    @DeleteMapping("/{userId}")
    public String deleteMemory(@PathVariable String userId) {
        memoryService.deleteUserMemory(userId);
        return AppConstants.ControllerMessages.MEMORY_DELETED;
    }
}