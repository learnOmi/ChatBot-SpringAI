package org.example.springairobot.service;

import org.example.springairobot.DAO.UserProfileRepository;
import org.example.springairobot.PO.DTO.UserProfileExtraction;
import org.example.springairobot.PO.Tables.ConversationMessage;
import org.example.springairobot.PO.Tables.UserProfile;
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

@Service
public class MemoryEnhancementService {

    private final VectorStore vectorStore;
    private final UserProfileRepository userProfileRepo;
    private final ChatClient chatClient;
    private final ConversationService conversationService;

    public MemoryEnhancementService(VectorStore vectorStore,
                                    UserProfileRepository userProfileRepo,
                                    @Qualifier("chatClient") ChatClient chatClient,
                                    ConversationService conversationService) {
        this.vectorStore = vectorStore;
        this.userProfileRepo = userProfileRepo;
        this.chatClient = chatClient;
        this.conversationService = conversationService;
    }

    /**
     * 异步更新用户画像和长期记忆（带缓存、去重）
     */
    @Async
    public void updateUserProfileAndMemory(String userId) {
        // 1. 画像缓存：1小时内不重复提取
        UserProfile existing = userProfileRepo.findById(userId).orElse(null);
        if (existing != null && existing.getUpdatedAt() != null &&
                existing.getUpdatedAt().isAfter(LocalDateTime.now().minusHours(1))) {
            return;
        }

        // 2. 从 ConversationService 获取所有历史消息
        List<ConversationMessage> allMessages = conversationService.getHistory(userId);
        if (allMessages.isEmpty()) {
            return;
        }

        // 3. 提取画像（基于最近 20 条消息）
        String recentConversation = allMessages.stream()
                .skip(Math.max(0, allMessages.size() - 20))
                .map(msg -> (msg.getRole().equals("user") ? "用户: " : "助手: ") + msg.getContent())
                .collect(Collectors.joining("\n"));

        // 4. 结构化提取画像
        BeanOutputConverter<UserProfileExtraction> converter =
                new BeanOutputConverter<>(UserProfileExtraction.class);

        String prompt = """
                根据以下对话内容，提取用户的关键偏好和特征。
                
                对话内容：
                %s
                
                %s
                """.formatted(recentConversation, converter.getFormat());

        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            UserProfileExtraction extracted = converter.convert(response);

            UserProfile profile = existing != null ? existing : new UserProfile();
            profile.setUserId(userId);
            profile.setPreferredUnits(extracted.getPreferredUnits());
            profile.setLanguage(extracted.getLanguage());
            profile.setInterests(extracted.getInterests());
            profile.setLocation(extracted.getLocation());
            profile.setSummary(extracted.getSummary());
            userProfileRepo.save(profile);
        } catch (Exception e) {
            System.err.println("Failed to extract user profile: " + e.getMessage());
        }

        // 5. 向量化长期记忆（带去重）
        for (ConversationMessage msg : allMessages) {
            if (!"user".equals(msg.getRole()) || msg.getContent().length() < 10) {
                continue;
            }
            // 去重：检查相似记忆
            List<Document> existingDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(msg.getContent())
                            .topK(1)
                            .similarityThreshold(0.9)
                            .filterExpression("user_id == '%s' && type == 'long_term_memory'".formatted(userId))
                            .build()
            );
            if (existingDocs.isEmpty()) {
                Document doc = new Document(msg.getContent(), Map.of(
                        "user_id", userId,
                        "role", "user",
                        "type", "long_term_memory"   // 关键：标记为长期记忆
                ));
                vectorStore.add(List.of(doc));
            }
        }
    }

    /**
     * 检索相关长期记忆
     */
    public List<Document> retrieveRelevantMemories(String userId, String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .filterExpression("user_id == '%s' && type == 'long_term_memory'".formatted(userId))
                        .build()
        );
    }

    /**
     * 获取用户画像
     */
    public UserProfile getUserProfile(String userId) {
        return userProfileRepo.findById(userId).orElse(null);
    }

    /**
     * 隐私保护：删除用户所有长期记忆和画像
     */
    public void deleteUserMemory(String userId) {
        vectorStore.delete("user_id == '%s' && type == 'long_term_memory'".formatted(userId));
        userProfileRepo.deleteById(userId);
    }
}