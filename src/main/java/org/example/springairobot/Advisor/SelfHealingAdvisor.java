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
 * 在LLM生成答案且评估完成后，根据评估结果决定是否进行修复
 * 修复策略：查询改写（retrieval失败）或重新生成（generation失败）
 * 执行顺序：应在评估顾问之后
 */
@Component
public class SelfHealingAdvisor implements CallAdvisor {

    private final ChatClient ragChatClient;

    public SelfHealingAdvisor(@Qualifier("evaluationChatClient") ChatClient ragChatClient) {
        this.ragChatClient = ragChatClient;
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

        // 根据失败原因选择修复策略
        String failReason = evalResult.getFailReason();
        String originalQuery = (String) request.context().get("originalQuery");
        boolean hasToolCalls = request.context().containsKey("toolResult");

        String healingStrategy;
        if ("generation".equals(failReason)) {
            healingStrategy = "regenerate";
        } else {
            healingStrategy = "query_rewrite";
            String rewritten = rewriteQuery(originalQuery);
            request.context().put("rewrittenQuery", rewritten);
            if (hasToolCalls) {
                request.context().put("replayTools", true);
            }
        }

        request.context().put("healingStrategy", healingStrategy);
        request.context().put("retryCount", retryCount + 1);
        request.context().put("healed", true);

        // 如果是查询改写，使用改写后的查询重新发起请求
        if ("query_rewrite".equals(healingStrategy)) {
            String rewrittenQuery = (String) request.context().get("rewrittenQuery");
            ChatClientRequest newRequest = ChatClientRequest.builder()
                    .prompt(request.prompt().augmentUserMessage(rewrittenQuery))
                    .context(request.context())
                    .build();
            return chain.nextCall(newRequest);
        }

        // 重新生成策略：用相同请求再次调用链
        return chain.nextCall(request);
    }

    private String rewriteQuery(String originalQuery) {
        String prompt = """
                你是一个查询优化专家。请将以下用户问题改写为更适合检索的关键词形式。
                要求：只返回改写后的查询文本，不要任何解释。
                
                原始问题：%s
                改写后的查询：""".formatted(originalQuery);
        return ragChatClient.prompt().user(prompt).call().content().trim();
    }
}
