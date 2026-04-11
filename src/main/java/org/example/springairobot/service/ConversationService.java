package org.example.springairobot.service;

import org.example.springairobot.DAO.ConversationMessageRepository;
import org.example.springairobot.DAO.ConversationSessionRepository;
import org.example.springairobot.PO.Tables.ConversationMessage;
import org.example.springairobot.PO.Tables.ConversationSession;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    // 获取或创建会话
    public String getOrCreateSession(String sessionId, String title) {
        if (sessionId != null && sessionRepo.existsById(sessionId)) {
            return sessionId;
        }
        String newId = UUID.randomUUID().toString();
        ConversationSession session = new ConversationSession();
        session.setId(newId);
        session.setTitle(title != null ? title : "新对话");
        sessionRepo.save(session);
        return newId;
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

    // 保存一对消息（用户和助手），通常在响应完成后调用
    public void savePair(String sessionId, String userMessage, String assistantMessage, Integer tokensUsed) {
        saveMessage(sessionId, "user", userMessage, tokensUsed);
        saveMessage(sessionId, "assistant", assistantMessage, tokensUsed);
    }

    /**
     * 保存消息并返回持久化后的消息对象（用于获取自增ID）
     */
    public ConversationMessage saveMessageAndReturn(String sessionId, String role, String content, Integer tokensUsed) {
        ConversationMessage msg = new ConversationMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setTokensUsed(tokensUsed);
        ConversationMessage saved = messageRepo.save(msg);

        sessionRepo.findById(sessionId).ifPresent(session -> {
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepo.save(session);
        });
        return saved;
    }

    // 获取会话历史（用于构建对话上下文）
    public List<ConversationMessage> getHistory(String sessionId) {
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    // 加载历史消息（转换为 Spring AI Message 列表）
    public List<Message> loadHistoryMessages(String sessionId) {
        List<ConversationMessage> messages = getHistory(sessionId);
        List<Message> springMessages = new ArrayList<>();
        for (ConversationMessage msg : messages) {
            if ("user".equals(msg.getRole())) {
                springMessages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                springMessages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return springMessages;
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
