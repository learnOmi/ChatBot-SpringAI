package org.example.springairobot.Controller;

import org.example.springairobot.PO.DTO.BatchProcessResponse;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.ChatService;
import org.example.springairobot.service.file.UnifiedFileProcessingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(AppConstants.ApiPaths.FILE_BASE)
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
    @PostMapping(AppConstants.ApiPaths.FILE_PROCESS)
    public String processFile(@RequestPart("file") MultipartFile file) throws Exception {
        return fileProcessingService.processFile(file);
    }

    /**
     * 批量文件处理
     */
    @PostMapping(AppConstants.ApiPaths.FILE_BATCH)
    public BatchProcessResponse processBatchFiles(@RequestPart("files") List<MultipartFile> files) {
        return fileProcessingService.processBatchFiles(files);
    }

    /**
     * 处理文件并直接用于对话
     */
    @PostMapping(AppConstants.ApiPaths.FILE_CHAT)
    public String chatWithFile(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String userId,
            @RequestParam String question,
            @RequestPart("file") MultipartFile file) throws Exception {

        String fileContent = fileProcessingService.processFile(file);
        String enhancedQuestion = String.format(
                AppConstants.ControllerMessages.FILE_CHAT_PROMPT_TEMPLATE,
                fileContent, question);

        return chatService.chat(sessionId, userId, enhancedQuestion);
    }

    /**
     * 批量处理文件并结合对话
     */
    @PostMapping(AppConstants.ApiPaths.FILE_BATCH_CHAT)
    public String chatWithBatchFiles(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String userId,
            @RequestParam String question,
            @RequestPart("files") List<MultipartFile> files) {

        BatchProcessResponse batchResult = fileProcessingService.processBatchFiles(files);
        StringBuilder fileContents = new StringBuilder();
        for (var result : batchResult.getResults()) {
            if (result.isSuccess()) {
                fileContents.append(String.format(
                        AppConstants.ControllerMessages.FILE_CONTENT_FORMAT,
                        result.getFileName(), result.getContent()));
            }
        }

        String enhancedQuestion = String.format(
                AppConstants.ControllerMessages.BATCH_FILE_CHAT_PROMPT_TEMPLATE,
                batchResult.getTotalFiles(), batchResult.getSuccessCount(),
                fileContents.toString(), question);

        return chatService.chat(sessionId, userId, enhancedQuestion);
    }
}
