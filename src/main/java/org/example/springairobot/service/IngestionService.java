package org.example.springairobot.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * IngestionService 类负责处理文档的摄入过程，包括加载文档、分割文档、向量化存储等步骤。
 * 该类被标记为 Spring 组件，将被 Spring 容器管理。
 */
@Component
public class IngestionService {
    // 向量存储对象，用于存储和检索文档向量
    private final VectorStore vectorStore;
    // 文本分割器，用于将大文档分割为更小的块
    private final TokenTextSplitter textSplitter;

    // 从类路径加载知识库文件
    @Value("classpath:knowledge-base.txt")
    private Resource knowledgeBase;

    /**
     * 构造函数，注入向量存储对象
     * @param vectorStore 向量存储对象
     */
    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.textSplitter = new TokenTextSplitter();
    }

    /**
     * 初始化方法，在组件构造完成后自动执行
     * 完成文档的完整摄入流程：加载、分割、向量化存储
     */
    @PostConstruct
    public void runIngestion() {
        // 1. 加载文档
        TextReader textReader = new TextReader(knowledgeBase);
        List<Document> documents = textReader.get();
        // 2. 分割文档为更小的块
        List<Document> chunks = textSplitter.apply(documents);
        // 3. 将文档块向量化并存储到PGVector
        vectorStore.accept(chunks);
        // 输出摄入完成的提示信息，包含文档块数量
        System.out.println("知识库摄入完成！共 " + chunks.size() + " 个文档块。");
    }
}