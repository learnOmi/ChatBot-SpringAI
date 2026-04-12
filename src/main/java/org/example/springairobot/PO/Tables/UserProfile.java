package org.example.springairobot.PO.Tables;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile")
@Data
public class UserProfile {
    @Id
    private String userId;

    private String preferredUnits;  // metric / imperial / unknown
    private String language;        // zh / en
    private String interests;
    private String location;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}