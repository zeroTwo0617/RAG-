package com.ragdemo.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentDetailVO extends DocumentVO {

    private List<ChunkDetail> chunks;
}
