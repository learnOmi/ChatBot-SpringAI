package org.example.springairobot.service.rag.retriever;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

public class BM25DocumentRetriever implements DocumentRetriever {

    private final JdbcTemplate jdbcTemplate;
    private final int topK;
    private final ObjectMapper objectMapper;

    public BM25DocumentRetriever(JdbcTemplate jdbcTemplate, int topK) {
        this.jdbcTemplate = jdbcTemplate;
        this.topK = topK;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Document> retrieve(Query query) {
        String sql = """
                SELECT id, content, metadata,
                       ts_rank(content_tsv, plainto_tsquery('english', ?)) AS rank
                FROM vector_store
                WHERE content_tsv @@ plainto_tsquery('english', ?)
                ORDER BY rank DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    String id = rs.getString("id");
                    String content = rs.getString("content");
                    String metadataJson = rs.getString("metadata");
                    Map<String, Object> metadata = parseMetadata(metadataJson);
                    metadata.put("bm25_score", rs.getDouble("rank"));
                    return new Document(id, content, metadata);
                },
                query.text(), query.text(), this.topK);
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("raw", metadataJson);
        }
    }
}
