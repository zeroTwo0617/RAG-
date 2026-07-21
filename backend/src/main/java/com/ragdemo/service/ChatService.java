package com.ragdemo.service;

import com.ragdemo.common.BusinessException;
import com.ragdemo.common.ErrorCode;
import com.ragdemo.dto.response.ChatResponse;
import com.ragdemo.dto.response.ChunkSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 问答服务（M1 抽取式）。
 * 检索 Top-K 片段后，直接拼接片段内容作为答案；sources 携带引用来源与相似度。
 * 后续里程碑在此接入 LLM：用检索片段作为上下文，流式生成答案。
 */
@Service
public class ChatService {

    private final RetrievalService retrievalService;
    private final EmbeddingService embeddingService;

    public ChatService(RetrievalService retrievalService, EmbeddingService embeddingService) {
        this.retrievalService = retrievalService;
        this.embeddingService = embeddingService;
    }

    public ChatResponse ask(String question, int topK) {
        if (question == null || question.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "问题为空");
        }
        List<ChunkSearchResult> sources = retrievalService.retrieve(question, topK);

        StringBuilder answer = new StringBuilder();
        for (ChunkSearchResult s : sources) {
            answer.append(s.getContent()).append("\n\n");
        }

        ChatResponse response = new ChatResponse();
        response.setAnswer(answer.toString().trim());
        response.setSources(sources);
        response.setQueryEmbedded(true);
        return response;
    }
}
