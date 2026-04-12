package org.example.springairobot.controller;

import org.example.springairobot.service.AgentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public String chat(@RequestParam(required = false) String sessionId,
                       @RequestBody String message) {
        return agentService.execute(sessionId, message);
    }
}