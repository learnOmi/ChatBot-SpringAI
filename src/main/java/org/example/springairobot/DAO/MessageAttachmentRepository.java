package org.example.springairobot.DAO;

import org.example.springairobot.PO.Tables.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
}