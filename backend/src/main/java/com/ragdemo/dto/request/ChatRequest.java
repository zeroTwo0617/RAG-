package com.ragdemo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "问题不能为空")
    private String question;

    /** 召回数量，1~10，默认 5 */
    private Integer topK;
}
