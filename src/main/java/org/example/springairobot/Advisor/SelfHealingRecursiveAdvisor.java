package org.example.springairobot.Advisor;

import org.example.springairobot.constants.AppConstants;
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

    private ChatClient.Builder retryBuilder;

    public void setRetryBuilder(ChatClient.Builder retryBuilder) {
        this.retryBuilder = retryBuilder;
    }

    @Override
    public String getName() {
        return AppConstants.AdvisorConstants.SELF_HEALING_RECURSIVE_ADVISOR_NAME;
    }

    @Override
    public int getOrder() {
        return AppConstants.AdvisorConstants.SELF_HEALING_RECURSIVE_ADVISOR_ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        int retryCount = (Integer) request.context()
                .getOrDefault(AppConstants.AdvisorConstants.CONTEXT_KEY_RETRY_COUNT, 0);
        if (retryCount == 0) {
            request.context().put(AppConstants.AdvisorConstants.CONTEXT_KEY_MAX_RETRIES, 
                    AppConstants.AdvisorConstants.DEFAULT_MAX_RETRIES);
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

        Boolean fallback = (Boolean) request.context()
                .get(AppConstants.AdvisorConstants.CONTEXT_KEY_FALLBACK);
        if (fallback != null && fallback) {
            return response;
        }

        int currentRetry = (Integer) request.context()
                .getOrDefault(AppConstants.AdvisorConstants.CONTEXT_KEY_RETRY_COUNT, 0);
        int maxRetries = (Integer) request.context()
                .getOrDefault(AppConstants.AdvisorConstants.CONTEXT_KEY_MAX_RETRIES, 
                              AppConstants.AdvisorConstants.DEFAULT_MAX_RETRIES);

        if (currentRetry >= maxRetries) {
            request.context().put(AppConstants.AdvisorConstants.CONTEXT_KEY_FALLBACK, true);
            return response;
        }

        request.context().put(AppConstants.AdvisorConstants.CONTEXT_KEY_RETRY_COUNT, currentRetry + 1);

        return adviseCall(request, chain);
    }

    private boolean checkEvaluationPass(ChatClientRequest request) {
        Object evalResultObj = request.context()
                .get(AppConstants.AdvisorConstants.CONTEXT_KEY_EVALUATION_RESULT);
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
        Boolean needsHealing = (Boolean) request.context()
                .get(AppConstants.AdvisorConstants.CONTEXT_KEY_NEEDS_HEALING);
        if (needsHealing != null && needsHealing) {
            String strategy = (String) request.context()
                    .get(AppConstants.AdvisorConstants.CONTEXT_KEY_HEALING_STRATEGY);
            if (AppConstants.AdvisorConstants.HEALING_STRATEGY_QUERY_REWRITE.equals(strategy)) {
                String rewrittenQuery = (String) request.context()
                        .get(AppConstants.AdvisorConstants.CONTEXT_KEY_REWRITTEN_QUERY);
                if (rewrittenQuery != null) {
                    return ChatClientRequest.builder()
                            .prompt(request.prompt().augmentUserMessage(rewrittenQuery))
                            .context(request.context())
                            .build();
                }
            }
            request.context().remove(AppConstants.AdvisorConstants.CONTEXT_KEY_NEEDS_HEALING);
            request.context().remove(AppConstants.AdvisorConstants.CONTEXT_KEY_HEALING_STRATEGY);
        }
        return request;
    }

    private ChatClientResponse executeRetry(ChatClientRequest request) {
        if (retryBuilder == null) {
            throw new IllegalStateException(
                    AppConstants.SelfHealingConstants.ERROR_RETRY_BUILDER_NOT_CONFIGURED);
        }

        ChatClient retryClient = retryBuilder.build();
        String userText = request.prompt().getUserMessage().getText();

        ChatResponse chatResponse = retryClient.prompt()
                .user(userText)
                .advisors(a -> {
                    for (Map.Entry<String, Object> entry : request.context().entrySet()) {
                        if (!entry.getKey().equals(AppConstants.AdvisorConstants.CONTEXT_KEY_RETRY_COUNT)) {
                            a.param(entry.getKey(), entry.getValue());
                        }
                    }
                    a.param(AppConstants.AdvisorConstants.CONTEXT_KEY_RETRY_COUNT, 
                            request.context().get(AppConstants.AdvisorConstants.CONTEXT_KEY_RETRY_COUNT));
                })
                .call()
                .chatResponse();

        return new ChatClientResponse(chatResponse, Map.of());
    }
}
