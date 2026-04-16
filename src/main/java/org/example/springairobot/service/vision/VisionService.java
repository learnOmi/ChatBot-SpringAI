package org.example.springairobot.service.vision;

import net.coobird.thumbnailator.Thumbnails;
import org.example.springairobot.DAO.MessageAttachmentRepository;
import org.example.springairobot.PO.Tables.ConversationMessage;
import org.example.springairobot.PO.Tables.MessageAttachment;
import org.example.springairobot.Config.properties.VisionProperties;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.ConversationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 视觉服务
 * 
 * 提供多模态（图片、视频、音频）分析功能
 * 
 * 功能特点：
 * - 支持图片、视频、音频等多种媒体类型
 * - 自动生成图片缩略图用于存储
 * - 结合对话记忆进行上下文分析
 * - 支持会话持久化
 */
@Service
public class VisionService {

    private final ChatClient visionChatClient;
    private final ConversationService conversationService;
    private final MessageAttachmentRepository attachmentRepo;
    private final VisionProperties visionProperties;

    public VisionService(@Qualifier(AppConstants.AiConfigConstants.QUALIFIER_VISION_CHAT_CLIENT) ChatClient visionChatClient,
                         ConversationService conversationService,
                         MessageAttachmentRepository attachmentRepo,
                         VisionProperties visionProperties) {
        this.visionChatClient = visionChatClient;
        this.conversationService = conversationService;
        this.attachmentRepo = attachmentRepo;
        this.visionProperties = visionProperties;
    }

    /**
     * 分析媒体文件
     * 
     * 支持图片、视频、音频等多种媒体类型的分析
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param question 用户问题
     * @param mediaFile 媒体文件
     * @return AI分析结果
     * @throws IOException 文件处理异常
     * @throws IllegalArgumentException 不支持的媒体类型
     */
    public String analyzeMedia(String sessionId, String userId, String question, MultipartFile mediaFile) throws IOException {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);
        String fileContentType = mediaFile.getContentType();
        byte[] thumbnailBytes = null;
        String attachmentType = null;

        // 检查媒体类型
        if (fileContentType == null || !isSupportedMediaType(fileContentType)) {
            throw new IllegalArgumentException(AppConstants.VisionMessages.ERROR_UNSUPPORTED_FILE_TYPE + fileContentType);
        }

        try {
            // 如果是图片，生成缩略图
            if (fileContentType.startsWith(AppConstants.FileTypes.CONTENT_TYPE_PREFIX_IMAGE)) {
                ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
                Thumbnails.of(mediaFile.getInputStream())
                        .size(visionProperties.getThumbnailWidth(), visionProperties.getThumbnailHeight())
                        .outputFormat(visionProperties.getThumbnailFormat())
                        .toOutputStream(thumbnailOutputStream);
                thumbnailBytes = thumbnailOutputStream.toByteArray();
                attachmentType = AppConstants.VisionConstants.DEFAULT_ATTACHMENT_TYPE;
            }

            // 编码媒体文件为Base64
            String base64Media = Base64.getEncoder().encodeToString(mediaFile.getBytes());

            // 构建媒体对象
            Media media = Media.builder()
                    .mimeType(MimeTypeUtils.parseMimeType(fileContentType))
                    .data(base64Media)
                    .build();

            // 构建用户消息
            UserMessage userMessage = UserMessage.builder()
                    .text(question)
                    .media(media)
                    .build();

            // 调用视觉模型
            String response = visionChatClient.prompt()
                    .messages(userMessage)
                    .advisors(a -> a.param(AppConstants.AdvisorConstants.CHAT_MEMORY_CONVERSATION_ID_KEY, effectiveSessionId))
                    .call()
                    .content();

            // 保存用户消息
            ConversationMessage userMsg = conversationService.saveMessageAndReturn(
                    effectiveSessionId, userId, AppConstants.ChatMessages.MESSAGE_TYPE_USER,
                    AppConstants.VisionConstants.MEDIA_FILE_PREFIX + question, null);

            // 保存缩略图附件
            if (thumbnailBytes != null) {
                MessageAttachment attachment = new MessageAttachment();
                attachment.setMessageId(userMsg.getId());
                attachment.setAttachmentType(attachmentType);
                attachment.setData(thumbnailBytes);
                attachmentRepo.save(attachment);
            }

            // 保存助手回复
            conversationService.saveMessageAndReturn(effectiveSessionId, userId,
                    AppConstants.ChatMessages.MESSAGE_TYPE_ASSISTANT, response, null);

            return response;

        } catch (IOException e) {
            System.err.println(AppConstants.VisionMessages.ERROR_FILE_PROCESSING_FAILED + e.getMessage());
            throw new IOException(AppConstants.VisionMessages.ERROR_FILE_CORRUPTED, e);
        } catch (Exception e) {
            System.err.println(AppConstants.VisionMessages.ERROR_MULTIMODEL_CALL_FAILED + e.getMessage());
            throw new RuntimeException(AppConstants.VisionMessages.ERROR_AI_SERVICE_UNAVAILABLE, e);
        }
    }

    /**
     * 检查是否为支持的媒体类型
     * 
     * @param contentType 内容类型
     * @return 是否支持
     */
    private boolean isSupportedMediaType(String contentType) {
        return contentType.startsWith(AppConstants.FileTypes.CONTENT_TYPE_PREFIX_IMAGE) ||
                contentType.startsWith(AppConstants.FileTypes.CONTENT_TYPE_PREFIX_VIDEO) ||
                contentType.startsWith(AppConstants.FileTypes.CONTENT_TYPE_PREFIX_AUDIO);
    }
}
