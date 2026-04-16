package org.example.springairobot.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.example.springairobot.DAO.KnowledgeBaseMetadataRepository;
import org.example.springairobot.PO.Tables.KnowledgeBaseMetadata;
import org.example.springairobot.constants.AppConstants;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 知识库导入服务
 * 
 * 负责将知识库文件导入向量数据库
 * 
 * 功能特点：
 * - 应用启动时自动加载知识库
 * - 检测文件变化，仅在文件更新时重新导入
 * - 使用MD5校验避免重复导入
 * - 自动分割长文本为合适的块大小
 */
@Component
public class IngestionService {
    
    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final KnowledgeBaseMetadataRepository metadataRepo;
    @PersistenceContext
    private EntityManager entityManager;

    @Value("classpath:knowledge-base.txt")
    private Resource knowledgeBase;

    public IngestionService(VectorStore vectorStore, KnowledgeBaseMetadataRepository metadataRepo) {
        this.vectorStore = vectorStore;
        this.textSplitter = new TokenTextSplitter();
        this.metadataRepo = metadataRepo;
    }

    /**
     * 执行知识库导入
     * 
     * 检测文件变化，仅在文件更新时重新导入：
     * 1. 计算文件的最后修改时间和MD5哈希
     * 2. 与数据库中存储的元数据比较
     * 3. 如果文件有变化，删除旧数据并导入新数据
     * 4. 更新元数据记录
     */
    @Transactional
    public void runIngestion() {
        try {
            String fileId = AppConstants.IngestionConstants.KNOWLEDGE_BASE_FILE;
            long currentLastModified = knowledgeBase.getFile().lastModified();
            String currentHash = DigestUtils.md5Hex(knowledgeBase.getInputStream().readAllBytes());

            Optional<KnowledgeBaseMetadata> existing = metadataRepo.findById(fileId);
            boolean needUpdate = true;

            // 检查文件是否有变化
            if (existing.isPresent()) {
                KnowledgeBaseMetadata meta = existing.get();
                if (meta.getLastModified().toEpochMilli() == currentLastModified &&
                        currentHash.equals(meta.getContentHash())) {
                    needUpdate = false;
                    System.out.println(AppConstants.IngestionConstants.LOG_NO_CHANGE);
                }
            }

            if (needUpdate) {
                System.out.println(AppConstants.IngestionConstants.LOG_DETECTED_CHANGE);

                // 删除旧数据
                if(existing.isPresent()) deleteDocumentsBySource(fileId);

                // 读取并分割文档
                TextReader reader = new TextReader(knowledgeBase);
                List<Document> documents = reader.get();
                List<Document> chunks = textSplitter.apply(documents);
                
                // 添加元数据
                List<Document> chunksWithMetadata = chunks.stream()
                        .map(chunk -> new Document(chunk.getText(), Map.of(
                                AppConstants.IngestionConstants.METADATA_KEY_SOURCE, fileId,
                                AppConstants.IngestionConstants.METADATA_KEY_TYPE, AppConstants.IngestionConstants.METADATA_VALUE_KNOWLEDGE
                        )))
                        .collect(Collectors.toList());
                
                // 存入向量数据库
                vectorStore.add(chunksWithMetadata);
                System.out.println(String.format(AppConstants.IngestionConstants.LOG_LOAD_COMPLETE, chunks.size()));

                // 更新元数据
                KnowledgeBaseMetadata meta = new KnowledgeBaseMetadata();
                meta.setId(fileId);
                meta.setLastModified(Instant.ofEpochMilli(currentLastModified));
                meta.setContentHash(currentHash);
                metadataRepo.save(meta);
            }
        } catch (Exception e) {
            System.err.println(AppConstants.IngestionConstants.LOG_ERROR + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据来源删除文档
     * 
     * @param source 来源标识
     */
    private void deleteDocumentsBySource(String source) {
        entityManager.createNativeQuery(AppConstants.IngestionConstants.SQL_DELETE_BY_SOURCE)
                .setParameter(1, source)
                .executeUpdate();
    }
}
