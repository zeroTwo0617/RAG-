package com.ragdemo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 问答请求体：POST /api/chat
 */
@Data
public class ChatRequest {

    /** 用户问题（必填，@NotBlank 校验） */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 召回数量，1~10，默认 5；越大召回越广但可能越杂 */
    private Integer topK;
}
