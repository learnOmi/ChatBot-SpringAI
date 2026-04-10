package org.example.springairobot.RagOpt.Transformer;

import org.example.springairobot.service.ConversationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class ContextualQueryTransformer implements QueryTransformer {

    private final ChatClient chatClient;
    private final ConversationService conversationService;

    public ContextualQueryTransformer(ChatClient.Builder chatClientBuilder,
                                      ConversationService conversationService) {
        this.chatClient = chatClientBuilder.build();
        this.conversationService = conversationService;
    }

    @Override
    public Query transform(Query query) {
        // 从 Query 上下文中获取 sessionId
        String sessionId = (String) query.context().get("sessionId");
        String originalText = query.text();

        // 如果没有 sessionId 或历史消息为空，直接返回原查询
        if (!StringUtils.hasText(sessionId)) {
            return query;
        }

        List<Message> historyMessages = conversationService.loadHistoryMessages(sessionId);
        if (historyMessages.isEmpty()) {
            return query;
        }

        // 将历史消息格式化为文本
        String historyText = historyMessages.stream()
                .map(msg -> (msg instanceof org.springframework.ai.chat.messages.UserMessage ? "User" : "Assistant")
                        + ": " + msg.getText())
                .collect(Collectors.joining("\n"));

        String prompt = """
                你是一个查询改写助手。请根据以下对话历史，将用户当前的问题改写成一个独立、完整的查询。
                如果问题本身已经完整，可以直接返回原问题。
                
                ## 对话历史
                %s
                
                ## 当前问题
                %s
                
                ## 改写后的查询
                """.formatted(historyText, originalText);

        String rewritten = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // 如果改写失败或返回为空，则回退到原查询
        if (!StringUtils.hasText(rewritten)) {
            return query;
        }

        // 返回一个新的 Query 对象，只包含改写后的文本和原始上下文
        return Query.builder()
                .text(rewritten.trim())
                .history(query.history())   // 保留原历史记录 (如有)
                .context(query.context())   // 保留上下文，以便后续使用
                .build();
    }
}