package org.example.springairobot.PO.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationResult {
    private boolean pass;
    private boolean relevancyPass;
    private boolean factualityPass;
    private String failReason;  // "retrieval", "generation", "both", "evaluation_error"
    private double relevancyScore;
    private double factualityScore;
}