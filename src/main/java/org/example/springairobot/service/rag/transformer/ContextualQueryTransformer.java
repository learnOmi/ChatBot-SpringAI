package org.example.springairobot.service.rag.transformer;

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
        String sessionId = (String) query.context().get("sessionId");
        String originalText = query.text();

        if (!StringUtils.hasText(sessionId)) {
            return query;
        }

        List<Message> historyMessages = conversationService.loadHistoryMessages(sessionId);
        if (historyMessages.isEmpty()) {
            return query;
        }

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

        if (!StringUtils.hasText(rewritten)) {
            return query;
        }

        return Query.builder()
                .text(rewritten.trim())
                .history(query.history())
                .context(query.context())
                .build();
    }
}
