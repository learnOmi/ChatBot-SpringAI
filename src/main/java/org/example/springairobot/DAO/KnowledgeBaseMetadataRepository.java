package org.example.springairobot.DAO;

import org.example.springairobot.PO.KnowledgeBaseMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KnowledgeBaseMetadataRepository extends JpaRepository<KnowledgeBaseMetadata, String> {
    Optional<KnowledgeBaseMetadata> findById(String id);
}