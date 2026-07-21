package com.ragdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ragdemo.entity.Document;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    @Select("SELECT * FROM document WHERE doc_id = #{docId}")
    Document selectByDocId(@Param("docId") String docId);

    @Delete("DELETE FROM document WHERE doc_id = #{docId}")
    int deleteByDocId(@Param("docId") String docId);
}
