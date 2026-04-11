package org.example.springairobot.RagOpt.Retriever;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MultiQueryDocumentRetrieverAdapter implements DocumentRetriever {

    private final MultiQueryExpander queryExpander;
    private final List<DocumentRetriever> delegateRetrievers; // 修改为检索器列表


    public MultiQueryDocumentRetrieverAdapter(MultiQueryExpander queryExpander,
                                              List<DocumentRetriever> delegateRetrievers) {
        this.queryExpander = queryExpander;
        this.delegateRetrievers = delegateRetrievers;
    }

    @Override
    public List<Document> retrieve(Query query) {
        // 1. 扩展查询：生成多个语义变体
        List<Query> expandedQueries = queryExpander.expand(query);

        // 2. 并行检索与融合
        List<Document> allDocuments = expandedQueries.stream()
                .flatMap(expandedQuery -> delegateRetrievers.stream() // 对每个扩展查询，调用所有检索器
                        .flatMap(retriever -> retriever.retrieve(expandedQuery).stream())
                )
                .collect(Collectors.toList());

        // 3. 去重：基于文档ID（若无ID则用文本内容），使用 LinkedHashMap 保持顺序
        Map<String, Document> uniqueDocs = allDocuments.stream()
                .collect(Collectors.toMap(
                        doc -> StringUtils.hasText(doc.getId()) ? doc.getId() : doc.getText(),
                        doc -> doc,
                        (existing, replacement) -> existing, // 冲突时保留第一个
                        LinkedHashMap::new                  // 使用 LinkedHashMap 保持原始顺序
                ));

        return new ArrayList<>(uniqueDocs.values());
    }
}