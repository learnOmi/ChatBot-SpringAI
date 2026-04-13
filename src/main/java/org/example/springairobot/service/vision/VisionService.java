package org.example.springairobot.service.vision;

import net.coobird.thumbnailator.Thumbnails;
import org.example.springairobot.DAO.MessageAttachmentRepository;
import org.example.springairobot.PO.Tables.ConversationMessage;
import org.example.springairobot.PO.Tables.MessageAttachment;
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

@Service
public class VisionService {

    private final ChatClient visionChatClient;
    private final ConversationService conversationService;
    private final MessageAttachmentRepository attachmentRepo;

    public VisionService(@Qualifier("visionChatClient") ChatClient visionChatClient,
                         ConversationService conversationService,
                         MessageAttachmentRepository attachmentRepo) {
        this.visionChatClient = visionChatClient;
        this.conversationService = conversationService;
        this.attachmentRepo = attachmentRepo;
    }

    public String analyzeMedia(String sessionId, String userId, String question, MultipartFile mediaFile) throws IOException {
        String effectiveSessionId = conversationService.getOrCreateSession(sessionId, userId, null);
        String fileContentType = mediaFile.getContentType();
        byte[] thumbnailBytes = null;
        String attachmentType = null;

        if (fileContentType == null || !isSupportedMediaType(fileContentType)) {
            throw new IllegalArgumentException("不支持的文件类型: " + fileContentType);
        }

        try {
            if (fileContentType.startsWith("image/")) {
                ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
                Thumbnails.of(mediaFile.getInputStream())
                        .size(200, 200)
                        .outputFormat("jpg")
                        .toOutputStream(thumbnailOutputStream);
                thumbnailBytes = thumbnailOutputStream.toByteArray();
                attachmentType = "image/jpeg";
            }

            String base64Media = Base64.getEncoder().encodeToString(mediaFile.getBytes());

            Media media = Media.builder()
                    .mimeType(MimeTypeUtils.parseMimeType(fileContentType))
                    .data(base64Media)
                    .build();

            UserMessage userMessage = UserMessage.builder()
                    .text(question)
                    .media(media)
                    .build();

            String response = visionChatClient.prompt()
                    .messages(userMessage)
                    .advisors(a -> a.param("chat_memory_conversation_id", effectiveSessionId))
                    .call()
                    .content();

            ConversationMessage userMsg = conversationService.saveMessageAndReturn(
                    effectiveSessionId, userId, "user", "[媒体文件] " + question, null);

            if (thumbnailBytes != null) {
                MessageAttachment attachment = new MessageAttachment();
                attachment.setMessageId(userMsg.getId());
                attachment.setAttachmentType(attachmentType);
                attachment.setData(thumbnailBytes);
                attachmentRepo.save(attachment);
            }

            conversationService.saveMessageAndReturn(effectiveSessionId, userId, "assistant", response, null);

            return response;

        } catch (IOException e) {
            System.err.println("文件处理失败: " + e.getMessage());
            throw new IOException("文件处理失败，请检查文件是否损坏", e);
        } catch (Exception e) {
            System.err.println("调用多模态模型失败: " + e.getMessage());
            throw new RuntimeException("AI服务暂时不可用，请稍后重试", e);
        }
    }

    private boolean isSupportedMediaType(String contentType) {
        return contentType.startsWith("image/") ||
                contentType.startsWith("video/") ||
                contentType.startsWith("audio/");
    }
}
