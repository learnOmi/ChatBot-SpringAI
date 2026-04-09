package org.example.springairobot.Advisor;

import org.example.springairobot.service.ConversationService;
import org.springframework.ai.chat.client.advisor.*;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

public class ConversationLoggingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private final ConversationService conversationService;

    public ConversationLoggingAdvisor(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Override
    public String getName() {
        return "conversationLoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return 0; // 高优先级，先于其他 Advisor（如 RAG）
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        // 1. 获取或创建 sessionId
        String sessionId = getSessionId(request);
        String userMessage = extractUserMessage(request);

        // 2. 加载历史消息并合并到请求
        AdvisedRequest newRequest = enhanceRequestWithHistory(request, sessionId);

        // 3. 调用下一个 Advisor（最终调用模型）
        AdvisedResponse response = chain.nextAroundCall(newRequest);

        // 4. 提取助手回复并保存
        String assistantMessage = extractAssistantMessage(response);
        conversationService.savePair(sessionId, userMessage, assistantMessage, null); // tokens 可选

        return response;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        String sessionId = getSessionId(request);
        String userMessage = extractUserMessage(request);

        AdvisedRequest newRequest = enhanceRequestWithHistory(request, sessionId);

        // 用于累积完整回复的 StringBuilder（线程安全，因为 Flux 是顺序的）
        StringBuilder assistantReplyBuilder = new StringBuilder();

        return chain.nextAroundStream(newRequest)
                .doOnNext(response -> {
                    String chunk = extractAssistantMessage(response);
                    assistantReplyBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    String fullReply = assistantReplyBuilder.toString();
                    conversationService.savePair(sessionId, userMessage, fullReply, null);
                });
    }

    private String getSessionId(AdvisedRequest request) {
        String sessionId = (String) request.adviseContext().get("sessionId");
        // 如果没有传递 sessionId，自动创建一个新的
        return conversationService.getOrCreateSession(sessionId, null);
    }

    private String extractUserMessage(AdvisedRequest request) {
        // 尝试从请求的消息列表中提取最后一条用户消息
        List<Message> messages = request.messages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage) {
                return msg.getText();
            }
        }
        // 尝试从userText中获取
        String userText = request.userText();
        if (userText.isEmpty()) {
            throw new IllegalStateException("No user message found in request");
        }
        return userText;
    }

    private String extractAssistantMessage(AdvisedResponse response) {
        ChatResponse chatResponse = response.response();
        if (chatResponse != null && !chatResponse.getResults().isEmpty()) {
            return chatResponse.getResult().getOutput().getText();
        }
        return "";
    }

    private AdvisedRequest enhanceRequestWithHistory(AdvisedRequest request, String sessionId) {
        List<Message> historyMessages = conversationService.loadHistoryMessages(sessionId);
        if (historyMessages.isEmpty()) {
            return request; // 无历史，直接返回原请求
        }

        // 构建新的消息列表：保留系统消息（如果有），然后插入历史消息，最后是当前用户消息
        List<Message> newMessages = new ArrayList<>();
        List<Message> original = request.messages();

        // 提取系统消息（如果有）
        for (Message msg : original) {
            if (msg instanceof SystemMessage) {
                newMessages.add(msg);
            } else {
                break; // 遇到非系统消息就停止，因为通常系统消息在最前面
            }
        }

        // 添加历史消息
        newMessages.addAll(historyMessages);

        // 添加当前用户消息（最后一条用户消息，可能不是最后一条，但实际请求中最后一条通常是用户消息）
        // 找到最后一条用户消息并添加
        for (Message msg : original) {
            if (msg instanceof UserMessage) {
                newMessages.add(msg);
                break; // 只添加一次，假设只有一个用户消息
            }
        }

        // 注意：如果请求中还有其他消息（比如函数调用结果），这里简化处理，实际可能需要更复杂的合并。
        // 但大多数简单对话场景，请求中只有 [SystemMessage?, UserMessage] 或者 [UserMessage]

        return AdvisedRequest.from(request)
                .messages(newMessages)
                .build();
    }
}