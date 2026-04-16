package org.example.springairobot.service.memory;

import org.example.springairobot.DAO.UserProfileRepository;
import org.example.springairobot.PO.DTO.UserProfileExtraction;
import org.example.springairobot.PO.Tables.ConversationMessage;
import org.example.springairobot.PO.Tables.UserProfile;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.service.ConversationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记忆增强服务
 * 
 * 提供用户画像提取和长期记忆功能
 * 
 * 功能特点：
 * - 从对话历史中提取用户偏好和特征
 * - 将重要对话内容存储到向量数据库作为长期记忆
 * - 支持基于用户 ID 的记忆检索
 * - 异步更新，不影响主对话流程
 * 
 * 更新策略：
 * - 每 1 小时更新一次用户画像
 * - 仅保存长度超过 10 个字符的用户消息
 * - 避免重复存储相似内容
 */
@Service
public class MemoryEnhancementService {

    /** 向量存储，用于长期记忆 */
    private final VectorStore vectorStore;
    
    /** 用户画像仓库 */
    private final UserProfileRepository userProfileRepo;
    
    /** 画像提取专用 ChatClient */
    private final ChatClient profileExtractionChatClient;
    
    /** 会话服务 */
    private final ConversationService conversationService;

    public MemoryEnhancementService(VectorStore vectorStore,
                                    UserProfileRepository userProfileRepo,
                                    @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_PROFILE_EXTRACTION_CHAT_CLIENT) ChatClient profileExtractionChatClient,
                                    ConversationService conversationService) {
        this.vectorStore = vectorStore;
        this.userProfileRepo = userProfileRepo;
        this.profileExtractionChatClient = profileExtractionChatClient;
        this.conversationService = conversationService;
    }

    /**
     * 异步更新用户画像和长期记忆
     * 
     * 处理流程：
     * 1. 检查是否需要更新（距离上次更新超过 1 小时）
     * 2. 获取用户最近的对话历史
     * 3. 使用 LLM 提取用户画像（偏好、兴趣、位置等）
     * 4. 保存或更新用户画像
     * 5. 将有价值的对话内容存储到向量数据库
     * 
     * @param userId 用户 ID
     */
    @Async
    public void updateUserProfileAndMemory(String userId) {
        // 检查是否需要更新
        UserProfile existing = userProfileRepo.findById(userId).orElse(null);
        if (existing != null && existing.getUpdatedAt() != null &&
                existing.getUpdatedAt().isAfter(LocalDateTime.now().minusHours(AppConstants.MemoryConstants.PROFILE_UPDATE_HOURS))) {
            return;
        }

        // 获取用户所有对话历史
        List<ConversationMessage> allMessages = conversationService.getHistoryByUserId(userId);
        if (allMessages.isEmpty()) {
            return;
        }

        // 提取最近对话
        String recentConversation = allMessages.stream()
                .skip(Math.max(0, allMessages.size() - AppConstants.MemoryConstants.RECENT_MESSAGE_COUNT))
                .map(msg -> (msg.getRole().equals(AppConstants.MemoryConstants.ROLE_USER) ? 
                        AppConstants.MemoryConstants.ROLE_USER_LABEL : AppConstants.MemoryConstants.ROLE_ASSISTANT_LABEL) 
                        + msg.getContent())
                .collect(Collectors.joining("\n"));

        // 提取用户画像
        BeanOutputConverter<UserProfileExtraction> converter =
                new BeanOutputConverter<>(UserProfileExtraction.class);

        String prompt = String.format(
                AppConstants.MemoryConstants.PROFILE_EXTRACTION_PROMPT,
                recentConversation, converter.getFormat());

        try {
            String response = profileExtractionChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            UserProfileExtraction extracted = converter.convert(response);

            // 保存用户画像
            UserProfile profile = existing != null ? existing : new UserProfile();
            profile.setUserId(userId);
            profile.setPreferredUnits(extracted.getPreferredUnits());
            profile.setLanguage(extracted.getLanguage());
            profile.setInterests(extracted.getInterests() != null ?
                    String.join(",", extracted.getInterests()) : "");
            profile.setLocation(extracted.getLocation());
            profile.setSummary(extracted.getSummary());
            userProfileRepo.save(profile);
        } catch (Exception e) {
            System.err.println(AppConstants.MemoryConstants.ERROR_PROFILE_EXTRACTION_FAILED + e.getMessage());
        }

        // 存储长期记忆
        for (ConversationMessage msg : allMessages) {
            // 只保存用户消息，且长度超过阈值
            if (!AppConstants.MemoryConstants.ROLE_USER.equals(msg.getRole()) || 
                    msg.getContent().length() < AppConstants.MemoryConstants.MIN_MESSAGE_LENGTH) {
                continue;
            }
            
            // 检查是否已存在相似记忆
            List<Document> existingDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(msg.getContent())
                            .topK(AppConstants.MemoryConstants.MEMORY_TOP_K)
                            .similarityThreshold(AppConstants.MemoryConstants.MEMORY_SIMILARITY_THRESHOLD)
                            .filterExpression(String.format(AppConstants.MemoryConstants.FILTER_USER_MEMORY, userId))
                            .build()
            );
            
            // 不存在则添加
            if (existingDocs.isEmpty()) {
                Document doc = new Document(msg.getContent(), Map.of(
                        AppConstants.MemoryConstants.METADATA_KEY_USER_ID, userId,
                        AppConstants.MemoryConstants.METADATA_KEY_ROLE, AppConstants.MemoryConstants.ROLE_USER,
                        AppConstants.MemoryConstants.METADATA_KEY_TYPE, AppConstants.MemoryConstants.METADATA_VALUE_LONG_TERM_MEMORY
                ));
                vectorStore.add(List.of(doc));
            }
        }
    }

    /**
     * 检索与查询相关的用户记忆
     * 
     * @param userId 用户 ID
     * @param query 查询内容
     * @param topK 返回数量
     * @return 相关记忆列表
     */
    public List<Document> retrieveRelevantMemories(String userId, String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .filterExpression(String.format(AppConstants.MemoryConstants.FILTER_USER_MEMORY, userId))
                        .build()
        );
    }

    /**
     * 获取用户画像
     * 
     * @param userId 用户 ID
     * @return 用户画像对象
     */
    public UserProfile getUserProfile(String userId) {
        return userProfileRepo.findById(userId).orElse(null);
    }

    /**
     * 删除用户的所有记忆
     * 
     * @param userId 用户 ID
     */
    public void deleteUserMemory(String userId) {
        vectorStore.delete(String.format(AppConstants.MemoryConstants.FILTER_USER_MEMORY, userId));
        userProfileRepo.deleteById(userId);
    }
}
