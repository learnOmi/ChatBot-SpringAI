package org.example.springairobot.PO.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchProcessResult {
    private String fileName;
    private boolean success;
    private String content;
    private String error;
    private String fileType;
    private long processingTimeMs;
}
