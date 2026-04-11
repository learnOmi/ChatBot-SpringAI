package org.example.springairobot.PO.Tables;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_attachment")
@Data
public class MessageAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;  // 关联 conversation_message.id

    @Column(name = "attachment_type")
    private String attachmentType;  // 如 "image/jpeg"

    @Column(name = "data", columnDefinition = "bytea")
    private byte[] data;  // 二进制数据（缩略图）

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}