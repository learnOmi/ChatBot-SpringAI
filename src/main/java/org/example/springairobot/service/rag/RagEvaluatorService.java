package org.example.springairobot.service.rag;

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

@Service
public class RagEvaluatorService {
    private final RelevancyEvaluator relevancyEvaluator;
    private final FactCheckingEvaluator factCheckingEvaluator;

    public RagEvaluatorService(@Qualifier("evaluationChatClient")ChatClient chatClient) {
        String customRelevancyPrompt = """
            你的任务是判断以下回答是否与用户问题和提供的上下文相关。
            
            用户问题：{query}
            上下文信息：{context}
            回答内容：{response}
            
            回答仅用 YES 或 NO
            """;

        String customFactCheckPrompt = """
            请检查以下陈述是否在提供的文档中有事实依据。
            
            文档内容：{document}
            陈述内容：{claim}
            
            回答仅用 YES 或 NO
            """;

        this.relevancyEvaluator = RelevancyEvaluator.builder().promptTemplate(new PromptTemplate(customRelevancyPrompt)).chatClientBuilder(chatClient.mutate()).build();
        this.factCheckingEvaluator = FactCheckingEvaluator.builder(chatClient.mutate()).evaluationPrompt(customFactCheckPrompt).build();
    }

    /**
     * 评估RAG回答的相关性和事实准确性
     * @param userQuery 用户查询
     * @param context 检索到的上下文
     * @param response AI生成的响应
     * @return 是否通过评估
     */
    public boolean evaluate(String userQuery, List<Document> context, String response) {
        EvaluationRequest evaluationRequest = new EvaluationRequest(
                userQuery,
                context,
                response
        );

        // 首先评估相关性
        EvaluationResponse relevancyResponse = relevancyEvaluator.evaluate(evaluationRequest);
        if (!relevancyResponse.isPass()) {
            return false;
        }

        // 然后评估事实准确性
        EvaluationResponse factCheckingResponse = factCheckingEvaluator.evaluate(evaluationRequest);
        return factCheckingResponse.isPass();
    }

    /**
     * 单独评估相关性
     */
    public EvaluationResponse evaluateRelevancy(String userQuery, List<Document> context, String response) {
        EvaluationRequest request = new EvaluationRequest(userQuery, context, response);
        return relevancyEvaluator.evaluate(request);
    }

    /**
     * 单独评估事实准确性
     */
    public EvaluationResponse evaluateFactuality(List<Document> context, String response) {
        EvaluationRequest request = new EvaluationRequest(null, context, response);
        return factCheckingEvaluator.evaluate(request);
    }

    /**
     * 获取默认的"不知道"答案
     */
    public String getDefaultUnknownAnswer() {
        return "抱歉，我暂时无法准确回答这个问题。";
    }
}
