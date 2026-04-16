package org.example.springairobot.service.rag;

import org.example.springairobot.constants.AppConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 评估服务
 * 
 * 对 RAG 生成的答案进行质量评估
 * 
 * 功能特点：
 * - 相关性评估：答案是否与查询相关
 * - 事实性评估：答案是否基于检索的文档，无幻觉
 * - 可配置评估提示词
 * - 支持单独评估和组合评估
 * 
 * 评估流程：
 * 1. 相关性评估：检查答案是否回答了用户问题
 * 2. 事实性评估：检查答案是否基于检索的文档
 * 3. 两项都通过才认为是高质量答案
 */
@Service
public class RagEvaluatorService {
    
    /** 相关性评估器 */
    private final RelevancyEvaluator relevancyEvaluator;
    
    /** 事实性评估器 */
    private final FactCheckingEvaluator factCheckingEvaluator;

    public RagEvaluatorService(
            @Qualifier(AppConstants.AiConfigConstants.QUALIFIER_EVALUATION_CHAT_CLIENT) ChatClient chatClient) {
        this.relevancyEvaluator = RelevancyEvaluator.builder()
                .promptTemplate(new PromptTemplate(AppConstants.RagEvaluationPrompts.CUSTOM_RELEVANCY_PROMPT))
                .chatClientBuilder(chatClient.mutate())
                .build();
        this.factCheckingEvaluator = FactCheckingEvaluator.builder(chatClient.mutate())
                .evaluationPrompt(AppConstants.RagEvaluationPrompts.CUSTOM_FACT_CHECK_PROMPT)
                .build();
    }

    /**
     * 综合评估（相关性 + 事实性）
     * 
     * @param userQuery 用户查询
     * @param context 检索的文档上下文
     * @param response 生成的答案
     * @return 是否通过评估
     */
    public boolean evaluate(String userQuery, List<Document> context, String response) {
        EvaluationRequest evaluationRequest = new EvaluationRequest(
                userQuery,
                context,
                response
        );

        // 1. 相关性评估
        EvaluationResponse relevancyResponse = relevancyEvaluator.evaluate(evaluationRequest);
        if (!relevancyResponse.isPass()) {
            return false;
        }

        // 2. 事实性评估
        EvaluationResponse factCheckingResponse = factCheckingEvaluator.evaluate(evaluationRequest);
        return factCheckingResponse.isPass();
    }

    /**
     * 单独评估相关性
     * 
     * @param userQuery 用户查询
     * @param context 检索的文档上下文
     * @param response 生成的答案
     * @return 评估结果
     */
    public EvaluationResponse evaluateRelevancy(String userQuery, List<Document> context, String response) {
        EvaluationRequest request = new EvaluationRequest(userQuery, context, response);
        return relevancyEvaluator.evaluate(request);
    }

    /**
     * 单独评估事实性
     * 
     * @param context 检索的文档上下文
     * @param response 生成的答案
     * @return 评估结果
     */
    public EvaluationResponse evaluateFactuality(List<Document> context, String response) {
        EvaluationRequest request = new EvaluationRequest(null, context, response);
        return factCheckingEvaluator.evaluate(request);
    }

    /**
     * 获取默认兜底答案
     * 
     * @return 兜底答案字符串
     */
    public String getDefaultUnknownAnswer() {
        return AppConstants.ChatMessages.DEFAULT_UNKNOWN_ANSWER;
    }
}
