package org.example.springairobot.RagOpt.ReRanker;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OllamaReRankerDocumentPostProcessor implements DocumentPostProcessor {

    private final ChatClient chatClient;
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

        String userQuestion = query.text(); // 直接从 Query 对象获取查询文本，无需 ThreadLocal

        // 并发调用 Ollama 为每个文档打分（简单起见，也可串行）
        Map<Document, Double> scoreMap = new ConcurrentHashMap<>();
        documents.parallelStream().forEach(doc -> {
            double score = scoreDocument(userQuestion, doc.getText());
            scoreMap.put(doc, score);
        });

        // 按分数降序排序，取前 topN
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Document, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double scoreDocument(String query, String documentText) {
        String prompt = """
                你是一个专业的相关性评分助手。请根据用户问题对文档内容的相关性进行评分，分值范围0到1，保留两位小数。
                仅输出数字，不要有任何其他文字。
                
                用户问题：%s
                文档内容：%s
                相关性分数：
                """.formatted(query, documentText);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        try {
            return Double.parseDouble(response.trim());
        } catch (NumberFormatException e) {
            return 0.0; // 解析失败视为不相关
        }
    }
}