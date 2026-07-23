package com.ragdemo.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 问答任务结果（供前端轮询）：status 为 pending/completed/failed，
 * 完成后携带 answer 与 sources（引用来源）。
 */
@Data
public class ChatTaskResult {
    /** 任务状态：pending=生成中 / completed=完成 / failed=失败 */
    private String status;
    /** 抽取式答案（拼接 Top-K 片段） */
    private String answer;
    /** 引用来源（含相似度与原文） */
    private List<ChunkSearchResult> sources;
    /** 问题是否成功向量化 */
    private boolean queryEmbedded;
}
