package org.example.springairobot.DAO;

import org.example.springairobot.PO.Tables.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {
    List<ConversationMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    List<ConversationMessage> findByUserIdOrderByCreatedAtAsc(String userId);
    void deleteBySessionId(String sessionId);
}
