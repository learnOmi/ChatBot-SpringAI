package org.example.springairobot.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.example.springairobot.DAO.KnowledgeBaseMetadataRepository;
import org.example.springairobot.PO.Tables.KnowledgeBaseMetadata;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    // 知识库元数据存储库，用于存储和检索知识库元数据
    private final KnowledgeBaseMetadataRepository metadataRepo;
    /**
     * 使用JPA注解注入EntityManager对象
     * EntityManager是JPA规范中的核心接口，用于对实体类进行持久化操作
     * 包括保存、更新、删除、查询等数据库操作
     * 它能够执行 原生 SQL (Native SQL) 并直接操作 非实体表，这是普通 JpaRepository 难以做到的
     */
    @PersistenceContext
    private EntityManager entityManager;


    // 从类路径加载知识库文件
    @Value("classpath:knowledge-base.txt")
    private Resource knowledgeBase;

    /**
     * 构造函数，注入向量存储对象
     * @param vectorStore 向量存储对象
     * @param metadataRepo 知识库元数据存储库
     */
    public IngestionService(VectorStore vectorStore, KnowledgeBaseMetadataRepository metadataRepo) {
        this.vectorStore = vectorStore;
        this.textSplitter = new TokenTextSplitter();
        this.metadataRepo = metadataRepo;
    }

    /**
     * 初始化方法，在组件构造完成后自动执行
     * 完成文档的完整摄入流程：加载、分割、向量化存储
     */
    @PostConstruct
    public void runIngestion() {
        try {
            String fileId = "knowledge-base.txt";
            long currentLastModified = knowledgeBase.getFile().lastModified();
            String currentHash = DigestUtils.md5Hex(knowledgeBase.getInputStream().readAllBytes());

            // 使用 JPA 查询
            Optional<KnowledgeBaseMetadata> existing = metadataRepo.findById(fileId);
            boolean needUpdate = true;

            if (existing.isPresent()) {
                KnowledgeBaseMetadata meta = existing.get();
                if (meta.getLastModified().toEpochMilli() == currentLastModified &&
                        currentHash.equals(meta.getContentHash())) {
                    needUpdate = false;
                    System.out.println("✅ 知识库无变化，跳过加载。");
                }
            }

            if (needUpdate) {
                System.out.println("🔄 检测到知识库变化，开始重新加载...");

                // 1. 删除该来源的所有旧文档（ native query ）
                // 注意：vector_store 表不是 JPA 实体管理的，所以删除操作用 native query 更简单。
                // 但我们可以注入 EntityManager 执行原生删除。
                if(existing.isPresent()) deleteDocumentsBySource(fileId);

                // 2. 重新解析并插入
                TextReader reader = new TextReader(knowledgeBase);
                List<Document> documents = reader.get();
                List<Document> chunks = textSplitter.apply(documents);
                List<Document> chunksWithMetadata = chunks.stream()
                        .map(chunk -> new Document(chunk.getText(), Map.of(
                                "source", fileId,
                                "type", "knowledge"      // 标记为知识库
                        )))
                        .collect(Collectors.toList());
                vectorStore.add(chunksWithMetadata);
                System.out.println("✅ 知识库加载完成，共 " + chunks.size() + " 个文档块。");

                // 3. 保存/更新元数据
                KnowledgeBaseMetadata meta = new KnowledgeBaseMetadata();
                meta.setId(fileId);
                meta.setLastModified(Instant.ofEpochMilli(currentLastModified));
                meta.setContentHash(currentHash);
                metadataRepo.save(meta);
            }
        } catch (Exception e) {
            System.err.println("❌ 知识库处理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据来源(source)删除文档记录
     * @param source 文档来源标识符
     */
    private void deleteDocumentsBySource(String source) {
        // 因为 vector_store 表不是 JPA 实体，所以用原生 SQL 删除
        entityManager.createNativeQuery(
                        "DELETE FROM vector_store WHERE metadata->>'source' = ?")
                .setParameter(1, source)
                .executeUpdate();
    }
}