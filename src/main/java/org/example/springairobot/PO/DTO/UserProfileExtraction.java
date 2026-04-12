package org.example.springairobot.PO.DTO;

import lombok.Data;

@Data
public class UserProfileExtraction {
    private String preferredUnits;
    private String language;
    private String interests;
    private String location;
    private String summary;
}