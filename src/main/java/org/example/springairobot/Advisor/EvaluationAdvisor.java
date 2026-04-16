package org.example.springairobot.Advisor;

import org.example.springairobot.PO.DTO.EvaluationResult;
import org.example.springairobot.constants.AppConstants;
import org.example.springairobot.constants.EvaluationFailReason;
import org.example.springairobot.service.rag.RagEvaluatorService;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 评估顾问
 * 在LLM生成答案后，评估回答的相关性和事实准确性
 * 执行顺序：应在检索顾问(RAG)之后，确保先完成检索和生成
 */
@Component
public class EvaluationAdvisor implements CallAdvisor {

    private final RagEvaluatorService evaluatorService;

    public EvaluationAdvisor(RagEvaluatorService evaluatorService) {
        this.evaluatorService = evaluatorService;
    }

    @Override
    public String getName() {
        return AppConstants.AdvisorConstants.EVALUATION_ADVISOR_NAME;
    }

    @Override
    public int getOrder() {
        return AppConstants.AdvisorConstants.EVALUATION_ADVISOR_ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 先让链继续执行，生成答案
        ChatClientResponse response = chain.nextCall(request);

        // 答案已生成，现在进行评估
        String userQuery = request.prompt().getUserMessage().getText();
        @SuppressWarnings("unchecked")
        List<Document> retrievedDocs = (List<Document>) request.context()
                .getOrDefault(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, Collections.emptyList());
        String answer = response.chatResponse().getResult().getOutput().getText();

        boolean relevancyPass = false;
        boolean factualityPass = false;
        String failReason = EvaluationFailReason.NONE.getValue();

        if (!retrievedDocs.isEmpty()) {
            // RAG场景：有检索文档，评估相关性和事实准确性
            try {
                relevancyPass = evaluatorService.evaluateRelevancy(userQuery, retrievedDocs, answer).isPass();
                factualityPass = evaluatorService.evaluateFactuality(retrievedDocs, answer).isPass();
            } catch (Exception e) {
                failReason = EvaluationFailReason.EVALUATION_ERROR.getValue();
            }
        } else {
            // Agent 场景：无检索文档，评估工具调用和答案质量
            // 由于工具调用后 getToolCalls() 已被清空，通过答案内容判断是否调用了工具
            boolean hasToolResult = hasToolResultInAnswer(answer);
            if (!hasToolResult) {
                // Agent 没有使用工具结果，直接标记生成失败
                failReason = EvaluationFailReason.GENERATION.getValue();
            } else {
                // Agent 调用了工具并使用了工具结果，只评估答案是否回答了用户问题
                try {
                    relevancyPass = evaluateAgentAnswerQuality(userQuery, answer);
                    factualityPass = true;
                } catch (Exception e) {
                    failReason = EvaluationFailReason.EVALUATION_ERROR.getValue();
                }
            }
        }

        // 确定失败原因
        boolean pass = relevancyPass && factualityPass;
        if (!pass) {
            if (!relevancyPass && !factualityPass) {
                failReason = EvaluationFailReason.BOTH.getValue();
            } else if (!relevancyPass) {
                failReason = EvaluationFailReason.RETRIEVAL.getValue();
            } else {
                failReason = EvaluationFailReason.GENERATION.getValue();
            }
        }

        // 构建评估结果并存入上下文，供后续Advisor使用
        EvaluationResult result = EvaluationResult.builder()
                .pass(pass)
                .relevancyPass(relevancyPass)
                .factualityPass(factualityPass)
                .failReason(failReason)
                .build();

        request.context().put("evaluationResult", result);
        request.context().put("originalAnswer", answer);
        request.context().put("originalQuery", userQuery);
        request.context().put("retrievedDocs", retrievedDocs);

        return response;
    }

    private boolean hasToolResultInAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return false;
        }
        return answer.contains("\"" + AppConstants.AdvisorConstants.TOOL_RESULT_KEY_IN_ANSWER + "\"") ||
               answer.contains(AppConstants.AdvisorConstants.TOOL_RESULT_KEY_IN_ANSWER);
    }

    private boolean evaluateAgentAnswerQuality(String userQuery, String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return false;
        }
        String lowerAnswer = answer.toLowerCase();
        for (String keyword : AppConstants.AdvisorConstants.NEGATIVE_QUALITY_KEYWORDS) {
            if (lowerAnswer.contains(keyword)) {
                return false;
            }
        }
        return true;
    }
}
