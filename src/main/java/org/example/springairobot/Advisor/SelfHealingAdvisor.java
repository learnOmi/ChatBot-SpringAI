package org.example.springairobot.Advisor;

import org.example.springairobot.PO.DTO.EvaluationResult;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.constants.EvaluationFailReason;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 自修复顾问
 * 在LLM生成答案且评估完成后，根据评估结果决定修复策略
 * 只标记修复策略和改写后的查询，不自行重试
 * 重试由 SelfHealingRecursiveAdvisor 统一控制
 */
@Component
public class SelfHealingAdvisor implements CallAdvisor {

    private final ChatClient queryRewriteClient;

    public SelfHealingAdvisor(@Qualifier(AppConstants.AiConfigConstants.QUALIFIER_EVALUATION_CHAT_CLIENT) ChatClient queryRewriteClient) {
        this.queryRewriteClient = queryRewriteClient;
    }

    @Override
    public String getName() {
        return AppConstants.AdvisorConstants.SELF_HEALING_ADVISOR_NAME;
    }

    @Override
    public int getOrder() {
        return AppConstants.AdvisorConstants.SELF_HEALING_ADVISOR_ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 先让链继续执行，生成答案并完成评估
        ChatClientResponse response = chain.nextCall(request);

        // 检查评估结果
        EvaluationResult evalResult = (EvaluationResult) request.context()
                .get(AppConstants.AdvisorConstants.CONTEXT_KEY_EVALUATION_RESULT);

        // 评估通过或无评估结果，直接返回
        if (evalResult == null || evalResult.isPass()) {
            return response;
        }

        // 检查重试次数
        Integer retryCount = (Integer) request.context()
                .getOrDefault(AppConstants.AdvisorConstants.CONTEXT_KEY_RETRY_COUNT, 0);
        int maxRetries = (Integer) request.context()
                .getOrDefault(AppConstants.AdvisorConstants.CONTEXT_KEY_MAX_RETRIES, 
                              AppConstants.AdvisorConstants.DEFAULT_MAX_RETRIES);

        // 超过最大重试次数，标记兜底
        if (retryCount >= maxRetries) {
            request.context().put(AppConstants.AdvisorConstants.CONTEXT_KEY_FALLBACK, true);
            return response;
        }

        // 根据失败原因选择修复策略，只标记不重试
        String failReason = evalResult.getFailReason();
        String originalQuery = (String) request.context()
                .get(AppConstants.AdvisorConstants.CONTEXT_KEY_ORIGINAL_QUERY);

        if (EvaluationFailReason.GENERATION.getValue().equals(failReason)) {
            request.context().put(AppConstants.AdvisorConstants.CONTEXT_KEY_HEALING_STRATEGY, 
                    AppConstants.AdvisorConstants.HEALING_STRATEGY_REGENERATE);
        } else {
            request.context().put(AppConstants.AdvisorConstants.CONTEXT_KEY_HEALING_STRATEGY, 
                    AppConstants.AdvisorConstants.HEALING_STRATEGY_QUERY_REWRITE);
            String rewritten = rewriteQuery(originalQuery);
            request.context().put(AppConstants.AdvisorConstants.CONTEXT_KEY_REWRITTEN_QUERY, rewritten);
        }

        request.context().put(AppConstants.AdvisorConstants.CONTEXT_KEY_NEEDS_HEALING, true);

        return response;
    }

    private String rewriteQuery(String originalQuery) {
        String prompt = String.format(
                AppConstants.SelfHealingConstants.QUERY_REWRITE_PROMPT_TEMPLATE, 
                originalQuery);
        return queryRewriteClient.prompt().user(prompt).call().content().trim();
    }
}
