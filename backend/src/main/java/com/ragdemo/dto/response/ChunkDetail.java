package com.ragdemo.dto.response;

import lombok.Data;

/**
 * 文档详情中的分块预览项。
 */
@Data
public class ChunkDetail {

    private Integer chunkIndex;
    private String section;
    private String content;
    private Integer tokenCount;
}
