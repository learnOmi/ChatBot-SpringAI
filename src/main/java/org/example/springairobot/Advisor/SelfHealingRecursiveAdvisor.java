package org.example.springairobot.Advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

/**
 * 递归自修复顾问
 * 在LLM生成答案后，控制自修复的迭代过程
 * 最多尝试3次修复（包括首次执行）
 * 执行顺序：应在最外层，包裹整个评估+修复流程
 */
@Component
public class SelfHealingRecursiveAdvisor implements CallAdvisor {

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
        // 初始化重试参数
        request.context().put("maxRetries", 2);
        request.context().put("retryCount", 0);

        ChatClientResponse response = null;
        boolean pass = false;
        int iteration = 0;
        int maxIterations = 3;

        // 循环尝试：先生成答案，再检查评估结果，决定是否重试
        while (iteration < maxIterations) {
            // 执行链：生成答案 → 评估 → 自修复判断
            response = chain.nextCall(request);

            // 检查评估结果
            Object evalResultObj = request.context().get("evaluationResult");
            if (evalResultObj != null) {
                try {
                    pass = (boolean) evalResultObj.getClass().getMethod("isPass").invoke(evalResultObj);
                } catch (Exception e) {
                    pass = false;
                }
            }

            // 评估通过，跳出循环
            if (pass) {
                break;
            }

            // 检查是否已标记兜底（重试次数耗尽）
            Boolean fallback = (Boolean) request.context().get("fallback");
            if (fallback != null && fallback) {
                break;
            }

            // 增加重试次数，继续循环
            iteration++;
            request.context().put("retryCount", iteration);
        }

        return response;
    }
}
