package com.ragdemo.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentVO {

    private String docId;
    private String name;
    private String status;
    private Integer chunkCount;
    private LocalDateTime createdAt;
}
