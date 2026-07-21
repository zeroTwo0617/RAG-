package com.ragdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * chunk 实体只映射结构化字段，向量列 embedding 不加载到内存（检索时由 Mapper 直接计算距离）。
 */
@Data
@TableName("chunk")
public class Chunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String docId;
    private String section;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private LocalDateTime createdAt;
}
