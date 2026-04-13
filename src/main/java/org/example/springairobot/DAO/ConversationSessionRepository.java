package org.example.springairobot.DAO;

import org.example.springairobot.PO.Tables.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, String> {
    List<ConversationSession> findByUserIdOrderByUpdatedAtDesc(String userId);
}

