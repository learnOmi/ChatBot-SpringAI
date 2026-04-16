package org.example.springairobot.Controller;

import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.vision.VisionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视觉控制器
 * 
 * 提供多模态分析API接口
 * 
 * 功能特点：
 * - 支持图片、视频、音频等多种媒体类型
 * - 自动生成缩略图用于存储
 * - 结合对话记忆进行上下文分析
 */
@RestController
@RequestMapping(AppConstants.ApiPaths.VISION_BASE)
public class VisionController {

    private final VisionService visionService;

    public VisionController(VisionService visionService) {
        this.visionService = visionService;
    }

    /**
     * 分析媒体文件
     * 
     * 支持图片、视频、音频等多种媒体类型的分析
     * 
     * @param sessionId 会话ID（可选）
     * @param userId 用户ID
     * @param question 用户问题
     * @param mediaFile 媒体文件
     * @return AI分析结果
     * @throws Exception 文件处理或AI调用异常
     */
    @PostMapping(AppConstants.ApiPaths.VISION_ANALYZE)
    public String analyzeMedia(@RequestParam(required = false) String sessionId,
                               @RequestParam String userId,
                               @RequestParam String question,
                               @RequestPart("media") MultipartFile mediaFile) throws Exception {
        return visionService.analyzeMedia(sessionId, userId, question, mediaFile);
    }
}
