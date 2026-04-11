package org.example.springairobot.RagOpt.Retriever;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * BM25DocumentRetriever 类实现了 DocumentRetriever 接口，用于基于 BM25 算法的文档检索
 * 该类使用 PostgreSQL 的全文搜索功能来实现文档检索和排序
 */
public class BM25DocumentRetriever implements DocumentRetriever {

    private final JdbcTemplate jdbcTemplate;
    private final int topK; // 固定值，在构造时传入，表示检索结果的最大数量
    private final ObjectMapper objectMapper; // 用于处理 JSON 数据的对象映射器

    public BM25DocumentRetriever(JdbcTemplate jdbcTemplate, int topK) {
        this.jdbcTemplate = jdbcTemplate;
        this.topK = topK;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Document> retrieve(Query query) {
        // 使用 PostgreSQL 的全文搜索功能，构建 SQL 查询语句
        // 查询包含指定文本的文档，并按相关性排序
        String sql = """
                SELECT id, content, metadata,
                       ts_rank(content_tsv, plainto_tsquery('english', ?)) AS rank
                FROM vector_store
                WHERE content_tsv @@ plainto_tsquery('english', ?)
                ORDER BY rank DESC
                LIMIT ?
                """;

        // 使用 jdbcTemplate 执行查询，并将结果映射为 Document 对象列表
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> {
                    String id = rs.getString("id");
                    String content = rs.getString("content");
                    // 从结果集中提取元数据 JSON 字符串并解析为 Map
                    String metadataJson = rs.getString("metadata");
                    Map<String, Object> metadata = parseMetadata(metadataJson);
                    // 将相关性评分添加到元数据中
                    metadata.put("bm25_score", rs.getDouble("rank"));
                    return new Document(id, content, metadata);
                },
                query.text(), query.text(), this.topK);
    }

    /**
     * 解析元数据JSON字符串，并将其转换为Map对象
     * @param metadataJson 包含元数据的JSON字符串
     * @return 解析后的Map对象，如果输入为空或解析失败则返回空Map或包含原始JSON的Map
     */
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
        // 使用objectMapper将JSON字符串解析为Map对象
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        } catch (Exception e) {
        // 如果解析过程中出现异常，返回包含原始JSON字符串的Map
            return Map.of("raw", metadataJson);
        }
    }
}