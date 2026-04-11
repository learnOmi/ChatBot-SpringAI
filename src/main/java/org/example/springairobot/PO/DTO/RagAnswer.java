package org.example.springairobot.PO.DTO;

import lombok.Data;
import java.util.List;

@Data
public class RagAnswer {
    private String answer;              // 回答正文
    private List<String> sources;       // 引用来源（文档片段或标题）
    private double confidence;          // 置信度 0.0~1.0
}