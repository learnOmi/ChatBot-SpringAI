package org.example.springairobot.PO.DTO;

import lombok.Data;

@Data
public class EntityExtraction {
    private String name;        // 实体名称，如 "秦锋"
    private String type;        // 实体类型，如 "人物"、"地点"、"事件"
    private String description; // 简要描述，如 "主角，想享受生活却被迫面对敌人"
}