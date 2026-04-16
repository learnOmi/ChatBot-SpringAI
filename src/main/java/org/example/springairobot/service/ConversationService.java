package org.example.springairobot.service;

import org.example.springairobot.DAO.ConversationMessageRepository;
import org.example.springairobot.DAO.ConversationSessionRepository;
import org.example.springairobot.PO.Tables.ConversationMessage;
import org.example.springairobot.PO.Tables.ConversationSession;
import org.example.springairobot.constants.AppConstants;
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

/**
 * 会话服务
 * 
 * 管理对话会话和消息的持久化
 * 
 * 功能特点：
 * - 创建和管理对话会话
 * - 保存和检索对话消息
 * - 支持会话历史加载
 * - 支持按用户查询会话列表
 */
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

    /**
     * 创建新会话
     * 
     * @param userId 用户ID
     * @param title 会话标题，可为null
     * @return 新创建的会话ID
     */
    public String createSession(String userId, String title) {
        String id = UUID.randomUUID().toString();
        ConversationSession session = new ConversationSession();
        session.setId(id);
        session.setUserId(userId);
        session.setTitle(title != null ? title : AppConstants.ConversationConstants.DEFAULT_SESSION_TITLE);
        sessionRepo.save(session);
        return id;
    }

    /**
     * 获取或创建会话
     * 
     * 如果sessionId存在且有效，返回该会话；否则创建新会话
     * 
     * @param sessionId 会话ID，可为null
     * @param userId 用户ID
     * @param title 会话标题
     * @return 有效会话ID
     */
    public String getOrCreateSession(String sessionId, String userId, String title) {
        if (sessionId != null && sessionRepo.existsById(sessionId)) {
            return sessionId;
        }
        return createSession(userId, title);
    }

    /**
     * 保存单条消息
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param role 角色（user/assistant）
     * @param content 消息内容
     * @param tokensUsed 使用的token数
     */
    public void saveMessage(String sessionId, String userId, String role, String content, Integer tokensUsed) {
        ConversationMessage msg = new ConversationMessage();
        msg.setSessionId(sessionId);
        msg.setUserId(userId);
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

    /**
     * 保存一对消息（用户消息和助手回复）
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param userMessage 用户消息
     * @param assistantMessage 助手回复
     * @param tokensUsed 使用的token数
     */
    public void savePair(String sessionId, String userId, String userMessage, String assistantMessage, Integer tokensUsed) {
        saveMessage(sessionId, userId, AppConstants.ConversationConstants.ROLE_USER, userMessage, tokensUsed);
        saveMessage(sessionId, userId, AppConstants.ConversationConstants.ROLE_ASSISTANT, assistantMessage, tokensUsed);
    }

    /**
     * 保存消息并返回持久化后的消息对象
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param role 角色
     * @param content 消息内容
     * @param tokensUsed 使用的token数
     * @return 持久化后的消息对象
     */
    public ConversationMessage saveMessageAndReturn(String sessionId, String userId, String role, String content, Integer tokensUsed) {
        ConversationMessage msg = new ConversationMessage();
        msg.setSessionId(sessionId);
        msg.setUserId(userId);
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

    /**
     * 获取会话历史消息
     * 
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<ConversationMessage> getHistory(String sessionId) {
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * 获取用户所有历史消息
     * 
     * @param userId 用户ID
     * @return 消息列表
     */
    public List<ConversationMessage> getHistoryByUserId(String userId) {
        return messageRepo.findByUserIdOrderByCreatedAtAsc(userId);
    }

    /**
     * 加载历史消息（转换为Spring AI Message格式）
     * 
     * @param sessionId 会话ID
     * @return Spring AI Message列表
     */
    public List<Message> loadHistoryMessages(String sessionId) {
        List<ConversationMessage> messages = getHistory(sessionId);
        List<Message> springMessages = new ArrayList<>();
        for (ConversationMessage msg : messages) {
            if (AppConstants.ConversationConstants.ROLE_USER.equals(msg.getRole())) {
                springMessages.add(new UserMessage(msg.getContent()));
            } else if (AppConstants.ConversationConstants.ROLE_ASSISTANT.equals(msg.getRole())) {
                springMessages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return springMessages;
    }

    /**
     * 删除会话及其所有消息
     * 
     * @param sessionId 会话ID
     */
    public void deleteSession(String sessionId) {
        messageRepo.deleteBySessionId(sessionId);
        sessionRepo.deleteById(sessionId);
    }

    /**
     * 获取所有会话列表
     * 
     * @return 会话列表，按更新时间降序
     */
    public List<ConversationSession> listSessions() {
        return sessionRepo.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    /**
     * 列出用户的所有会话
     * 
     * @param userId 用户ID
     * @return 会话列表，按更新时间降序
     */
    public List<ConversationSession> listSessionsByUser(String userId) {
        return sessionRepo.findByUserIdOrderByUpdatedAtDesc(userId);
    }
}
