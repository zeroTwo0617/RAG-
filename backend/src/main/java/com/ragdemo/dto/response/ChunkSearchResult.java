package com.ragdemo.dto.response;

import lombok.Data;

/**
 * 向量检索命中结果（含距离，需转换为相似度 score = 1 - distance）。
 */
@Data
public class ChunkSearchResult {

    private String docId;
    private String docName;
    private String section;
    private Integer chunkIndex;
    private String content;
    private Double distance;

    /** 余弦相似度：1 - 余弦距离，范围 0~1 */
    public Double getScore() {
        return distance == null ? null : 1 - distance;
    }
}
