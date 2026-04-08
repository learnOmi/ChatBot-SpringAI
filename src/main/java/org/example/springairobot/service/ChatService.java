package org.example.springairobot.service;

import org.example.springairobot.PO.ConversationMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private final ChatClient chatClient;
    private final ChatClient ragChatClient;
    private final VectorStore vectorStore;
    private final ConversationService conversationService;

    public ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ConversationService conversationService) {
        this.chatClient = chatClientBuilder.build();
        this.ragChatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.conversationService = conversationService;
    }

    // 同步对话
    public String chat(String sessionId, String userMessage) {
        // 1. 获取历史消息
        List<ConversationMessage> history = conversationService.getHistory(sessionId);

        // 2. 构建 Prompt
        Prompt prompt = buildPromptWithHistory(history, userMessage);

        // 3. 调用模型
        String assistantMessage = chatClient.prompt(prompt).call().content();

        // 4. 保存用户消息和助手回复
        conversationService.saveMessage(sessionId, "user", userMessage, null);
        conversationService.saveMessage(sessionId, "assistant", assistantMessage, null);

        return assistantMessage;
    }

    // 流式对话
    public Flux<String> chatStream(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .stream()
                .content();
    }

    /**
     * RAG 问答（基于知识库）
     */
    public String ragChat(String sessionId, String userMessage) {
        List<ConversationMessage> history = conversationService.getHistory(sessionId);
        // 构建 Prompt 时增加检索增强
        String context = retrieveContext(userMessage); // 向量检索
        Prompt prompt = buildRagPromptWithHistory(history, userMessage, context);
        String assistantMessage = chatClient.prompt(prompt).call().content();

        conversationService.saveMessage(sessionId, "user", userMessage, null);
        conversationService.saveMessage(sessionId, "assistant", assistantMessage, null);
        return assistantMessage;
    }

    /**
     * RAG 流式问答
     */
    public Flux<String> ragChatStream(String userMessage) {
        return ragChatClient.prompt()
                .user(userMessage)
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .stream()
                .content();
    }

    private Prompt buildPromptWithHistory(List<ConversationMessage> history, String currentMessage) {
        List<Message> messages = new ArrayList<>();
        for (ConversationMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        messages.add(new UserMessage(currentMessage));
        return new Prompt(messages);
    }

    private String retrieveContext(String question) {
        // 构建检索请求
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(3)                    // 返回最相似的3个文档
                .similarityThreshold(0.7)   // 相似度阈值
                .build();

        // 执行向量检索
        List<Document> docs = vectorStore.similaritySearch(request);

        // ✅ 添加空值检查
        if (docs == null || docs.isEmpty()) {
            return "未找到相关资料";
        }

        // 合并文档内容
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
    }

    private Prompt buildRagPromptWithHistory(List<ConversationMessage> history,
                                             String question, String context) {
        // 构建包含历史、上下文、当前问题的 Prompt
        StringBuilder sb = new StringBuilder();
        sb.append("以下是相关资料：\n").append(context).append("\n\n");
        sb.append("对话历史：\n");
        for (ConversationMessage msg : history) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        sb.append("user: ").append(question).append("\n");
        sb.append("assistant:");

        return new Prompt(new UserMessage(sb.toString()));
    }
}