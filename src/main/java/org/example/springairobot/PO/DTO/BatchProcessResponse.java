package org.example.springairobot.PO.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BatchProcessResponse {
    private int totalFiles;
    private int successCount;
    private int failureCount;
    private long totalProcessingTimeMs;
    private List<BatchProcessResult> results;
}
