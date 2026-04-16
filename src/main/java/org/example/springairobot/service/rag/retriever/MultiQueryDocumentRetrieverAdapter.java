package org.example.springairobot.service.rag.retriever;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 多查询文档检索适配器
 * 
 * 结合多查询扩展和多个检索器的混合检索策略
 * 
 * 功能特点：
 * - 将单个查询扩展为多个不同角度的查询
 * - 并行使用多个检索器（向量检索、BM25 检索）
 * - 自动去重，保留最相关的文档
 * 
 * 检索流程：
 * 1. 使用 MultiQueryExpander 将原始查询扩展为多个查询
 * 2. 对每个扩展查询，使用所有检索器进行检索
 * 3. 合并所有检索结果
 * 4. 去重（基于文档 ID 或内容）
 * 5. 返回唯一文档列表
 */
public class MultiQueryDocumentRetrieverAdapter implements DocumentRetriever {

    /** 多查询扩展器 */
    private final MultiQueryExpander queryExpander;
    
    /** 委托检索器列表（向量检索器、BM25 检索器等） */
    private final List<DocumentRetriever> delegateRetrievers;

    public MultiQueryDocumentRetrieverAdapter(MultiQueryExpander queryExpander,
                                              List<DocumentRetriever> delegateRetrievers) {
        this.queryExpander = queryExpander;
        this.delegateRetrievers = delegateRetrievers;
    }

    @Override
    public List<Document> retrieve(Query query) {
        // 1. 扩展查询
        List<Query> expandedQueries = queryExpander.expand(query);

        // 2. 对所有扩展查询和检索器进行检索
        List<Document> allDocuments = expandedQueries.stream()
                .flatMap(expandedQuery -> delegateRetrievers.stream()
                        .flatMap(retriever -> retriever.retrieve(expandedQuery).stream())
                )
                .collect(Collectors.toList());

        // 3. 去重（优先保留排在前面的文档）
        Map<String, Document> uniqueDocs = allDocuments.stream()
                .collect(Collectors.toMap(
                        doc -> StringUtils.hasText(doc.getId()) ? doc.getId() : doc.getText(),
                        doc -> doc,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        return new ArrayList<>(uniqueDocs.values());
    }
}
