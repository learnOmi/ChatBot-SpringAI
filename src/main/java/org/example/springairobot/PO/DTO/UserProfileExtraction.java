package org.example.springairobot.PO.DTO;

import lombok.Data;

import java.util.List;

@Data
public class UserProfileExtraction {
    private String preferredUnits;
    private String language;
    private List<String> interests;   // 改为 List<String>
    private String location;
    private String summary;
}