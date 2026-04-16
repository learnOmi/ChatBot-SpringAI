package org.example.springairobot.PO.DTO;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ParsedDocumentResult {
    private String text;
    private Map<String, Object> metadata;
}
