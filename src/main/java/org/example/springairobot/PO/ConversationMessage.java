package org.example.springairobot.PO;

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
    private Long id; // 声明一个Long类型的id属性，作为实体类的主键字段
    private String sessionId;
    private String role;   // user / assistant
    @Column(columnDefinition = "TEXT")
    private String content;
    private Integer tokensUsed;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}