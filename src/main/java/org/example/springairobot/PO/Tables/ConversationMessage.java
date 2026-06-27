package org.example.springairobot.PO.Tables;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sessionId;
    private String userId;
    private String role;
    @Column(columnDefinition = "TEXT")
    private String content;
    private Integer tokensUsed;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}