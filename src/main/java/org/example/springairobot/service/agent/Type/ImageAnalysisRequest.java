package org.example.springairobot.service.agent.Type;

import org.springframework.web.multipart.MultipartFile;

public record ImageAnalysisRequest(String input, MultipartFile image) {}
