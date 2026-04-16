package org.example.springairobot.Advisor;

import org.example.springairobot.PO.DTO.EvaluationResult;
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

    public SelfHealingAdvisor(@Qualifier("evaluationChatClient") ChatClient queryRewriteClient) {
        this.queryRewriteClient = queryRewriteClient;
    }

    @Override
    public String getName() {
        return "selfHealingAdvisor";
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 先让链继续执行，生成答案并完成评估
        ChatClientResponse response = chain.nextCall(request);

        // 检查评估结果
        EvaluationResult evalResult = (EvaluationResult) request.context().get("evaluationResult");

        // 评估通过或无评估结果，直接返回
        if (evalResult == null || evalResult.isPass()) {
            return response;
        }

        // 检查重试次数
        Integer retryCount = (Integer) request.context().getOrDefault("retryCount", 0);
        int maxRetries = (Integer) request.context().getOrDefault("maxRetries", 2);

        // 超过最大重试次数，标记兜底
        if (retryCount >= maxRetries) {
            request.context().put("fallback", true);
            return response;
        }

        // 根据失败原因选择修复策略，只标记不重试
        String failReason = evalResult.getFailReason();
        String originalQuery = (String) request.context().get("originalQuery");

        if ("generation".equals(failReason)) {
            request.context().put("healingStrategy", "regenerate");
        } else {
            request.context().put("healingStrategy", "query_rewrite");
            String rewritten = rewriteQuery(originalQuery);
            request.context().put("rewrittenQuery", rewritten);
        }

        request.context().put("needsHealing", true);

        return response;
    }

    private String rewriteQuery(String originalQuery) {
        String prompt = """
                你是一个查询优化专家。请将以下用户问题改写为更适合检索的关键词形式。
                要求：只返回改写后的查询文本，不要任何解释。
                
                原始问题：%s
                改写后的查询：""".formatted(originalQuery);
        return queryRewriteClient.prompt().user(prompt).call().content().trim();
    }
}
