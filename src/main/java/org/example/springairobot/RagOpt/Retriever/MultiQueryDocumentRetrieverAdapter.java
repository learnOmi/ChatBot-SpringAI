package org.example.springairobot.RagOpt.Retriever;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MultiQueryDocumentRetrieverAdapter implements DocumentRetriever {

    private final MultiQueryExpander queryExpander;
    private final VectorStoreDocumentRetriever delegateRetriever;

    public MultiQueryDocumentRetrieverAdapter(MultiQueryExpander queryExpander,
                                              VectorStoreDocumentRetriever delegateRetriever) {
        this.queryExpander = queryExpander;
        this.delegateRetriever = delegateRetriever;
    }

    @Override
    public List<Document> retrieve(Query query) {
        // 1. 扩展查询：生成多个语义变体
        List<Query> expandedQueries = queryExpander.expand(query);

        // 2. 并行检索：对每个扩展查询执行向量检索，并收集所有文档
        List<Document> allDocuments = expandedQueries.stream()
                .flatMap(expandedQuery -> delegateRetriever.retrieve(expandedQuery).stream())
                .collect(Collectors.toList());

        // 3. 融合去重：基于文档内容去重，保留首次出现的文档并维持顺序
        // 使用 LinkedHashMap 保持插入顺序
        Map<String, Document> uniqueDocuments = new LinkedHashMap<>();
        for (Document doc : allDocuments) {
            String content = doc.getText();
            if (!uniqueDocuments.containsKey(content)) {
                uniqueDocuments.put(content, doc);
            }
        }

        return new ArrayList<>(uniqueDocuments.values());
    }
}