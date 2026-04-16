package org.example.springairobot.Advisor;

import org.example.springairobot.PO.DTO.EvaluationResult;
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
        return "evaluationAdvisor";
    }

    @Override
    public int getOrder() {
        return 250;
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
        String failReason = "none";

        // 评估回答质量
        if (!retrievedDocs.isEmpty()) {
            try {
                relevancyPass = evaluatorService.evaluateRelevancy(userQuery, retrievedDocs, answer).isPass();
                factualityPass = evaluatorService.evaluateFactuality(retrievedDocs, answer).isPass();
            } catch (Exception e) {
                failReason = "evaluation_error";
            }
        } else {
            // 无检索文档时，尝试从上下文获取工具结果作为伪文档
            String toolResult = (String) request.context().get("toolResult");
            if (toolResult != null && !toolResult.isEmpty()) {
                Document pseudoDoc = new Document(toolResult);
                try {
                    factualityPass = evaluatorService.evaluateFactuality(List.of(pseudoDoc), answer).isPass();
                    relevancyPass = true;
                } catch (Exception e) {
                    failReason = "evaluation_error";
                }
            }
        }

        // 确定失败原因
        boolean pass = relevancyPass && factualityPass;
        if (!pass) {
            if (!relevancyPass && !factualityPass) {
                failReason = "both";
            } else if (!relevancyPass) {
                failReason = "retrieval";
            } else {
                failReason = "generation";
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
}
