package org.example.springairobot.service.memory;

import org.example.springairobot.DAO.UserProfileRepository;
import org.example.springairobot.PO.DTO.UserProfileExtraction;
import org.example.springairobot.PO.Tables.ConversationMessage;
import org.example.springairobot.PO.Tables.UserProfile;
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

@Service
public class MemoryEnhancementService {

    private final VectorStore vectorStore;
    private final UserProfileRepository userProfileRepo;
    private final ChatClient profileExtractionChatClient;
    private final ConversationService conversationService;

    public MemoryEnhancementService(VectorStore vectorStore,
                                    UserProfileRepository userProfileRepo,
                                    @Qualifier("profileExtractionChatClient") ChatClient profileExtractionChatClient,
                                    ConversationService conversationService) {
        this.vectorStore = vectorStore;
        this.userProfileRepo = userProfileRepo;
        this.profileExtractionChatClient = profileExtractionChatClient;
        this.conversationService = conversationService;
    }

    @Async
    public void updateUserProfileAndMemory(String userId) {
        UserProfile existing = userProfileRepo.findById(userId).orElse(null);
        if (existing != null && existing.getUpdatedAt() != null &&
                existing.getUpdatedAt().isAfter(LocalDateTime.now().minusHours(1))) {
            return;
        }

        List<ConversationMessage> allMessages = conversationService.getHistoryByUserId(userId);
        if (allMessages.isEmpty()) {
            return;
        }

        String recentConversation = allMessages.stream()
                .skip(Math.max(0, allMessages.size() - 20))
                .map(msg -> (msg.getRole().equals("user") ? "用户: " : "助手: ") + msg.getContent())
                .collect(Collectors.joining("\n"));

        BeanOutputConverter<UserProfileExtraction> converter =
                new BeanOutputConverter<>(UserProfileExtraction.class);

        String prompt = """
                根据以下对话内容，提取用户的关键偏好和特征。
                重要：只返回 JSON 数据，不要包含任何解释、Schema 或 Markdown 代码块标记
                
                对话内容：
                %s
                
                %s
                """.formatted(recentConversation, converter.getFormat());

        try {
            String response = profileExtractionChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            UserProfileExtraction extracted = converter.convert(response);

            UserProfile profile = existing != null ? existing : new UserProfile();
            profile.setUserId(userId);
            profile.setPreferredUnits(extracted.getPreferredUnits());
            profile.setLanguage(extracted.getLanguage());
            profile.setInterests(extracted.getInterests() != null ?
                    String.join(",", extracted.getInterests()) : ""); // 转换
            profile.setLocation(extracted.getLocation());
            profile.setSummary(extracted.getSummary());
            userProfileRepo.save(profile);
        } catch (Exception e) {
            System.err.println("Failed to extract user profile: " + e.getMessage());
        }

        for (ConversationMessage msg : allMessages) {
            if (!"user".equals(msg.getRole()) || msg.getContent().length() < 10) {
                continue;
            }
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
                        "type", "long_term_memory"
                ));
                vectorStore.add(List.of(doc));
            }
        }
    }

    public List<Document> retrieveRelevantMemories(String userId, String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .filterExpression("user_id == '%s' && type == 'long_term_memory'".formatted(userId))
                        .build()
        );
    }

    public UserProfile getUserProfile(String userId) {
        return userProfileRepo.findById(userId).orElse(null);
    }

    public void deleteUserMemory(String userId) {
        vectorStore.delete("user_id == '%s' && type == 'long_term_memory'".formatted(userId));
        userProfileRepo.deleteById(userId);
    }
}
