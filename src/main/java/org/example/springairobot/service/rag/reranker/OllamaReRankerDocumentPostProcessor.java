package org.example.springairobot.service.rag.reranker;

import org.example.springairobot.constants.AppConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Ollama 文档重排序器
 * 
 * 使用 LLM 对检索到的文档进行相关性重排序
 * 
 * 功能特点：
 * - 使用 LLM 理解查询和文档的语义相关性
 * - 对文档进行 0-1 分的相关性评分
 * - 按评分降序返回文档，提高 RAG 质量
 * - 支持并行处理，提高重排序速度
 * 
 * 重排序流程：
 * 1. 对每篇文档并行计算相关性分数
 * 2. 按分数降序排序
 * 3. 返回 topN 篇文档
 */
public class OllamaReRankerDocumentPostProcessor implements DocumentPostProcessor {

    /** ChatClient 用于相关性评分 */
    private final ChatClient chatClient;
    
    /** 返回的文档数量 */
    private final int topN;

    public OllamaReRankerDocumentPostProcessor(ChatClient.Builder chatClientBuilder, int topN) {
        this.chatClient = chatClientBuilder.build();
        this.topN = topN;
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }

        String userQuestion = query.text();

        // 并行计算每篇文档的相关性分数
        Map<Document, Double> scoreMap = new ConcurrentHashMap<>();
        documents.parallelStream().forEach(doc -> {
            double score = scoreDocument(userQuestion, doc.getText());
            scoreMap.put(doc, score);
        });

        // 按分数降序排序并返回 topN
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Document, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 对单篇文档进行相关性评分
     * 
     * @param query 用户查询
     * @param documentText 文档内容
     * @return 相关性分数（0-1）
     */
    private double scoreDocument(String query, String documentText) {
        // 构建评分提示词
        String prompt = String.format(
                AppConstants.RagRetrieverConstants.RERANK_SCORING_PROMPT,
                query, documentText);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // 解析分数
        try {
            return Double.parseDouble(response.trim());
        } catch (NumberFormatException e) {
            return AppConstants.RagRetrieverConstants.DEFAULT_RELEVANCE_SCORE;
        }
    }
}
