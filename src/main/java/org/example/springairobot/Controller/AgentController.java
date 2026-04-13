package org.example.springairobot.Controller;

import org.example.springairobot.service.AgentService;
import org.example.springairobot.service.CoordinatorAgent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;
    private final CoordinatorAgent coordinatorAgent;

    public AgentController(AgentService agentService, CoordinatorAgent coordinatorAgent) {
        this.agentService = agentService;
        this.coordinatorAgent = coordinatorAgent;
    }

    @PostMapping("/chat")
    public String chat(@RequestParam(required = false) String sessionId,
                       @RequestParam String userId,
                       @RequestParam String message) {
        return agentService.execute(sessionId, userId, message);
    }

    // 协调模式：多智能体协作
    @PostMapping("/coordinate")
    public String coordinateChat(@RequestParam(required = false) String sessionId,
                                 @RequestParam(required = false) String userId,
                                 @RequestParam String message,
                                 @RequestPart(required = false) MultipartFile image) {
        return coordinatorAgent.execute(sessionId, userId, message, image);
    }
}