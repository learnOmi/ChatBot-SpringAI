package org.example.springairobot.service;

import org.example.springairobot.DAO.ConversationMessageRepository;
import org.example.springairobot.DAO.ConversationSessionRepository;
import org.example.springairobot.PO.ConversationMessage;
import org.example.springairobot.PO.ConversationSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ConversationService {
    private ConversationSessionRepository sessionRepo;
    private ConversationMessageRepository messageRepo;

    @Autowired
    public void setSessionRepo(ConversationSessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @Autowired
    public void setMessageRepo(ConversationMessageRepository messageRepo) {
        this.messageRepo = messageRepo;
    }

    // 创建新会话（生成 UUID）
    public String createSession(String title) {
        String id = UUID.randomUUID().toString();
        ConversationSession session = new ConversationSession();
        session.setId(id);
        session.setTitle(title != null ? title : "新对话");
        sessionRepo.save(session);
        return id;
    }

    // 保存消息
    public void saveMessage(String sessionId, String role, String content, Integer tokensUsed) {
        ConversationMessage msg = new ConversationMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setTokensUsed(tokensUsed);
        messageRepo.save(msg);

        // 更新会话的更新时间
        sessionRepo.findById(sessionId).ifPresent(session -> {
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepo.save(session);
        });
    }

    // 获取会话历史（用于构建对话上下文）
    public List<ConversationMessage> getHistory(String sessionId) {
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    // 删除会话及其所有消息
    public void deleteSession(String sessionId) {
        messageRepo.deleteBySessionId(sessionId);
        sessionRepo.deleteById(sessionId);
    }

    // 获取会话列表
    public List<ConversationSession> listSessions() {
        return sessionRepo.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }
}
