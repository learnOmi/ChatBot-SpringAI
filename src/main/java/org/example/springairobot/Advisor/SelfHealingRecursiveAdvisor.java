package org.example.springairobot.Advisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 递归自修复顾问
 * 在LLM生成答案后，控制自修复的迭代过程
 * 最多尝试3次修复（包括首次执行）
 * 执行顺序：应在最外层，包裹整个评估+修复流程
 */
@Component
public class SelfHealingRecursiveAdvisor implements CallAdvisor {

    private static final String RETRY_COUNT_KEY = "retryCount";
    private static final String MAX_RETRIES_KEY = "maxRetries";

    private ChatClient.Builder retryBuilder;

    public void setRetryBuilder(ChatClient.Builder retryBuilder) {
        this.retryBuilder = retryBuilder;
    }

    @Override
    public String getName() {
        return "selfHealingRecursiveAdvisor";
    }

    @Override
    public int getOrder() {
        return 180;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        int retryCount = (Integer) request.context().getOrDefault(RETRY_COUNT_KEY, 0);
        if (retryCount == 0) {
            request.context().put(MAX_RETRIES_KEY, 2);
        }

        ChatClientResponse response;
        if (retryCount == 0) {
            response = chain.nextCall(request);
        } else {
            request = applyHealingStrategy(request);
            response = executeRetry(request);
        }

        boolean pass = checkEvaluationPass(request);

        if (pass) {
            return response;
        }

        Boolean fallback = (Boolean) request.context().get("fallback");
        if (fallback != null && fallback) {
            return response;
        }

        int currentRetry = (Integer) request.context().getOrDefault(RETRY_COUNT_KEY, 0);
        int maxRetries = (Integer) request.context().getOrDefault(MAX_RETRIES_KEY, 2);

        if (currentRetry >= maxRetries) {
            request.context().put("fallback", true);
            return response;
        }

        request.context().put(RETRY_COUNT_KEY, currentRetry + 1);

        return adviseCall(request, chain);
    }

    private boolean checkEvaluationPass(ChatClientRequest request) {
        Object evalResultObj = request.context().get("evaluationResult");
        if (evalResultObj != null) {
            try {
                return (boolean) evalResultObj.getClass().getMethod("isPass").invoke(evalResultObj);
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private ChatClientRequest applyHealingStrategy(ChatClientRequest request) {
        Boolean needsHealing = (Boolean) request.context().get("needsHealing");
        if (needsHealing != null && needsHealing) {
            String strategy = (String) request.context().get("healingStrategy");
            if ("query_rewrite".equals(strategy)) {
                String rewrittenQuery = (String) request.context().get("rewrittenQuery");
                if (rewrittenQuery != null) {
                    return ChatClientRequest.builder()
                            .prompt(request.prompt().augmentUserMessage(rewrittenQuery))
                            .context(request.context())
                            .build();
                }
            }
            request.context().remove("needsHealing");
            request.context().remove("healingStrategy");
        }
        return request;
    }

    private ChatClientResponse executeRetry(ChatClientRequest request) {
        if (retryBuilder == null) {
            throw new IllegalStateException("retryBuilder not configured. Call setRetryBuilder() during initialization.");
        }

        ChatClient retryClient = retryBuilder.build();
        String userText = request.prompt().getUserMessage().getText();

        ChatResponse chatResponse = retryClient.prompt()
                .user(userText)
                .advisors(a -> {
                    for (Map.Entry<String, Object> entry : request.context().entrySet()) {
                        if (!entry.getKey().equals(RETRY_COUNT_KEY)) {
                            a.param(entry.getKey(), entry.getValue());
                        }
                    }
                    a.param(RETRY_COUNT_KEY, request.context().get(RETRY_COUNT_KEY));
                })
                .call()
                .chatResponse();

        return new ChatClientResponse(chatResponse, Map.of());
    }
}
