package org.example.springairobot.service.rag.retriever;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MultiQueryDocumentRetrieverAdapter implements DocumentRetriever {

    private final MultiQueryExpander queryExpander;
    private final List<DocumentRetriever> delegateRetrievers;

    public MultiQueryDocumentRetrieverAdapter(MultiQueryExpander queryExpander,
                                              List<DocumentRetriever> delegateRetrievers) {
        this.queryExpander = queryExpander;
        this.delegateRetrievers = delegateRetrievers;
    }

    @Override
    public List<Document> retrieve(Query query) {
        List<Query> expandedQueries = queryExpander.expand(query);

        List<Document> allDocuments = expandedQueries.stream()
                .flatMap(expandedQuery -> delegateRetrievers.stream()
                        .flatMap(retriever -> retriever.retrieve(expandedQuery).stream())
                )
                .collect(Collectors.toList());

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
