package org.example.springairobot.DAO;

import org.example.springairobot.PO.Tables.ConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, String> {}

