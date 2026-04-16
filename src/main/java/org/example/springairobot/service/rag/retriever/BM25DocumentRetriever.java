package org.example.springairobot.service.rag.retriever;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.springairobot.constants.AppConstants;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * BM25 文档检索器
 * 
 * 基于 BM25 算法的关键词检索，适合精确匹配和关键词搜索
 * 
 * 功能特点：
 * - 使用 PostgreSQL 的 tsvector/tsquery 全文搜索
 * - BM25 评分算法计算相关性
 * - 与向量检索互补，提高检索覆盖率
 * 
 * BM25 算法参数：
 * - k1=1.5：控制词频饱和度的参数
 * 
 * 检索流程：
 * 1. 将查询转换为 PostgreSQL 全文搜索语法
 * 2. 执行 SQL 查询，计算 BM25 分数
 * 3. 按相关性降序返回文档
 */
public class BM25DocumentRetriever implements DocumentRetriever {

    /** JDBC 模板用于数据库查询 */
    private final JdbcTemplate jdbcTemplate;
    
    /** 返回的文档数量 */
    private final int topK;
    
    /** JSON 解析器 */
    private final ObjectMapper objectMapper;

    public BM25DocumentRetriever(JdbcTemplate jdbcTemplate, int topK) {
        this.jdbcTemplate = jdbcTemplate;
        this.topK = topK;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Document> retrieve(Query query) {
        // 执行 BM25 查询
        return jdbcTemplate.query(
                AppConstants.RagRetrieverConstants.BM25_SQL_QUERY_TEMPLATE,
                (rs, rowNum) -> {
                    String id = rs.getString("id");
                    String content = rs.getString("content");
                    String metadataJson = rs.getString("metadata");
                    
                    // 解析元数据
                    Map<String, Object> metadata = parseMetadata(metadataJson);
                    metadata.put(AppConstants.RagRetrieverConstants.BM25_SCORE_KEY, rs.getDouble("rank"));
                    
                    return new Document(id, content, metadata);
                },
                query.text(), query.text(), this.topK);
    }

    /**
     * 解析元数据 JSON
     * 
     * @param metadataJson 元数据 JSON 字符串
     * @return 解析后的 Map，失败时返回包含原始 JSON 的 Map
     */
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of(AppConstants.RagRetrieverConstants.BM25_RAW_METADATA_KEY, metadataJson);
        }
    }
}
