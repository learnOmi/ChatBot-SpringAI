package org.example.springairobot.service.file;

import org.example.springairobot.PO.DTO.BatchProcessResult;
import org.example.springairobot.PO.DTO.BatchProcessResponse;
import org.example.springairobot.service.file.audio.AudioTranscriptionService;
import org.example.springairobot.service.file.document.DocumentParserService;
import org.example.springairobot.service.file.ocr.OcrService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class UnifiedFileProcessingService {

    private final DocumentParserService documentParser;
    private final OcrService ocrService;
    private final AudioTranscriptionService audioService;
    private final Executor fileProcessingExecutor;

    public UnifiedFileProcessingService(DocumentParserService documentParser,
                                        OcrService ocrService,
                                        AudioTranscriptionService audioService,
                                        @Qualifier("fileProcessingExecutor") Executor fileProcessingExecutor) {
        this.documentParser = documentParser;
        this.ocrService = ocrService;
        this.audioService = audioService;
        this.fileProcessingExecutor = fileProcessingExecutor;
    }

    public String processFile(MultipartFile file) throws Exception {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("无法识别文件类型");
        }

        if (isDocumentType(contentType)) {
            return documentParser.parseDocument(file);
        } else if (isImageType(contentType)) {
            return ocrService.recognizeText(file);
        } else if (isAudioType(contentType)) {
            try {
                return audioService.transcribe(file);
            } catch (UnsupportedAudioFileException e) {
                throw new IOException("不支持的音频格式：" + e.getMessage(), e);
            }
        } else if (isTextType(contentType)) {
            return new String(file.getBytes());
        } else {
            throw new IllegalArgumentException("不支持的文件类型：" + contentType);
        }
    }

    public BatchProcessResponse processBatchFiles(List<MultipartFile> files) {
        Instant startTime = Instant.now();

        List<CompletableFuture<BatchProcessResult>> futures = files.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> processFileWithResult(file), fileProcessingExecutor))
                .toList();

        List<BatchProcessResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        long totalTime = Duration.between(startTime, Instant.now()).toMillis();
        long successCount = results.stream().filter(BatchProcessResult::isSuccess).count();

        return BatchProcessResponse.builder()
                .totalFiles(files.size())
                .successCount((int) successCount)
                .failureCount(files.size() - (int) successCount)
                .totalProcessingTimeMs(totalTime)
                .results(results)
                .build();
    }

    private BatchProcessResult processFileWithResult(MultipartFile file) {
        Instant start = Instant.now();
        String fileName = file.getOriginalFilename();
        String fileType = file.getContentType();

        try {
            String content = processFile(file);
            long timeMs = Duration.between(start, Instant.now()).toMillis();
            return BatchProcessResult.builder()
                    .fileName(fileName)
                    .success(true)
                    .content(content)
                    .fileType(fileType)
                    .processingTimeMs(timeMs)
                    .build();
        } catch (Exception e) {
            long timeMs = Duration.between(start, Instant.now()).toMillis();
            return BatchProcessResult.builder()
                    .fileName(fileName)
                    .success(false)
                    .error(e.getMessage())
                    .fileType(fileType)
                    .processingTimeMs(timeMs)
                    .build();
        }
    }

    private boolean isDocumentType(String contentType) {
        return contentType.equals("application/pdf") ||
               contentType.equals("application/msword") ||
               contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
               contentType.equals("application/vnd.ms-excel") ||
               contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
               contentType.equals("application/vnd.ms-powerpoint") ||
               contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation") ||
               contentType.equals("application/rtf") ||
               contentType.equals("application/vnd.oasis.opendocument.text");
    }

    private boolean isImageType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private boolean isAudioType(String contentType) {
        return contentType != null && (contentType.startsWith("audio/") ||
                contentType.equals("video/mp4") ||
                contentType.equals("video/mpeg"));
    }

    private boolean isTextType(String contentType) {
        return contentType != null && contentType.startsWith("text/");
    }
}
