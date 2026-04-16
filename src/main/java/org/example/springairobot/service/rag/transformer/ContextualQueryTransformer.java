package org.example.springairobot.service.rag.transformer;

import org.example.springairobot.service.ConversationService;
import org.example.springairobot.constants.AppConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 上下文感知查询转换器
 * 
 * 将用户的原始查询转换为更适合检索的格式，考虑对话上下文
 * 
 * 功能特点：
 * - 结合对话历史理解指代词（如"它"、"这个"）
 * - 将模糊查询转换为具体查询
 * - 提高检索准确性和相关性
 * 
 * 处理流程：
 * 1. 从查询上下文中获取 sessionId
 * 2. 加载对话历史
 * 3. 使用 LLM 重写查询，使其独立于上下文
 * 4. 如果重写失败，回退到原始查询
 */
public class ContextualQueryTransformer implements QueryTransformer {

    /** ChatClient 用于查询改写 */
    private final ChatClient chatClient;
    
    /** 会话服务，用于加载历史消息 */
    private final ConversationService conversationService;

    public ContextualQueryTransformer(ChatClient.Builder chatClientBuilder,
                                      ConversationService conversationService) {
        this.chatClient = chatClientBuilder.build();
        this.conversationService = conversationService;
    }

    @Override
    public Query transform(Query query) {
        // 1. 获取 sessionId
        String sessionId = (String) query.context().get(AppConstants.QueryTransformerConstants.CONTEXT_SESSION_ID_KEY);
        String originalText = query.text();

        if (!StringUtils.hasText(sessionId)) {
            System.out.println(AppConstants.QueryTransformerConstants.LOG_NO_SESSION);
            return query;
        }

        // 2. 加载对话历史
        List<Message> historyMessages = conversationService.loadHistoryMessages(sessionId);
        if (historyMessages.isEmpty()) {
            System.out.println(AppConstants.QueryTransformerConstants.LOG_NO_HISTORY);
            return query;
        }

        // 3. 构建历史文本
        String historyText = historyMessages.stream()
                .map(msg -> (msg instanceof org.springframework.ai.chat.messages.UserMessage ? 
                        AppConstants.QueryTransformerConstants.ROLE_USER_LABEL : 
                        AppConstants.QueryTransformerConstants.ROLE_ASSISTANT_LABEL)
                        + ": " + msg.getText())
                .collect(Collectors.joining(AppConstants.QueryTransformerConstants.HISTORY_SEPARATOR));

        // 4. 构建改写提示词
        String prompt = String.format(
                AppConstants.QueryTransformerConstants.QUERY_REWRITE_PROMPT,
                historyText, originalText);

        // 5. 调用 LLM 改写查询
        String rewritten = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (!StringUtils.hasText(rewritten)) {
            System.out.println(AppConstants.QueryTransformerConstants.LOG_REWRITE_FAILED);
            return query;
        }

        // 6. 返回改写后的查询
        return Query.builder()
                .text(rewritten.trim())
                .history(query.history())
                .context(query.context())
                .build();
    }
}
