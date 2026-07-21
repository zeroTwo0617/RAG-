package com.ragdemo.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果包络：与 api-contract.md 的分页格式一致。
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> list;
    private int page;
    private int size;
    private long total;

    public static <T> PageResult<T> of(List<T> list, int page, int size, long total) {
        PageResult<T> p = new PageResult<>();
        p.setList(list);
        p.setPage(page);
        p.setSize(size);
        p.setTotal(total);
        return p;
    }
}
