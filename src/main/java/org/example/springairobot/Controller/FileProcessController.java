package org.example.springairobot.Controller;

import org.example.springairobot.PO.DTO.BatchProcessResponse;
import org.example.springairobot.service.ChatService;
import org.example.springairobot.service.file.UnifiedFileProcessingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/file")
public class FileProcessController {

    private final UnifiedFileProcessingService fileProcessingService;
    private final ChatService chatService;

    public FileProcessController(UnifiedFileProcessingService fileProcessingService, ChatService chatService) {
        this.fileProcessingService = fileProcessingService;
        this.chatService = chatService;
    }

    /**
     * 单文件处理
     */
    @PostMapping("/process")
    public String processFile(@RequestPart("file") MultipartFile file) throws Exception {
        return fileProcessingService.processFile(file);
    }

    /**
     * 批量文件处理
     */
    @PostMapping("/batch")
    public BatchProcessResponse processBatchFiles(@RequestPart("files") List<MultipartFile> files) {
        return fileProcessingService.processBatchFiles(files);
    }

    /**
     * 处理文件并直接用于对话
     */
    @PostMapping("/chat")
    public String chatWithFile(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String userId,
            @RequestParam String question,
            @RequestPart("file") MultipartFile file) throws Exception {

        String fileContent = fileProcessingService.processFile(file);
        String enhancedQuestion = String.format("""
                用户上传了一个文件，内容如下：
                ---
                %s
                ---
                用户的问题：%s
                """, fileContent, question);

        return chatService.chat(sessionId, userId, enhancedQuestion);
    }

    /**
     * 批量处理文件并结合对话
     */
    @PostMapping("/batch-chat")
    public String chatWithBatchFiles(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String userId,
            @RequestParam String question,
            @RequestPart("files") List<MultipartFile> files) {

        BatchProcessResponse batchResult = fileProcessingService.processBatchFiles(files);
        StringBuilder fileContents = new StringBuilder();
        for (var result : batchResult.getResults()) {
            if (result.isSuccess()) {
                fileContents.append("【文件: ").append(result.getFileName()).append("】\n")
                        .append(result.getContent()).append("\n\n");
            }
        }

        String enhancedQuestion = String.format("""
                用户上传了 %d 个文件，成功处理 %d 个。
                文件内容如下：
                ---
                %s
                ---
                用户的问题：%s
                """, batchResult.getTotalFiles(), batchResult.getSuccessCount(),
                fileContents.toString(), question);

        return chatService.chat(sessionId, userId, enhancedQuestion);
    }
}
