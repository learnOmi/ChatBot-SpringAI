package org.example.springairobot.PO.Tables;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_base_metadata")
@Data
public class KnowledgeBaseMetadata {
    @Id
    private String id;               // 文件标识，如 "knowledge-base.txt"

    @Column(name = "last_modified", nullable = false)
    private Instant lastModified;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}