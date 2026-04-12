package org.example.springairobot.controller;

import org.example.springairobot.service.vision.VisionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/vision")
public class VisionController {

    private final VisionService visionService;

    public VisionController(VisionService visionService) {
        this.visionService = visionService;
    }

    @PostMapping("/analyze")
    public String analyzeImage(@RequestParam(required = false) String sessionId,
                               @RequestParam String question,
                               @RequestPart("image") MultipartFile image) throws IOException {
        return visionService.analyzeMedia(sessionId, question, image);
    }
}
