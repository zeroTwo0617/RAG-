package com.ragdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ragdemo.dto.response.ChunkDetail;
import com.ragdemo.dto.response.ChunkSearchResult;
import com.ragdemo.entity.Chunk;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChunkMapper extends BaseMapper<Chunk> {

    @Insert("INSERT INTO chunk (doc_id, section, chunk_index, content, token_count, embedding) " +
            "VALUES (#{docId}, #{section}, #{chunkIndex}, #{content}, #{tokenCount}, " +
            "#{embedding, typeHandler=com.ragdemo.mapper.VectorTypeHandler})")
    void insertChunk(@Param("docId") String docId,
                     @Param("section") String section,
                     @Param("chunkIndex") int chunkIndex,
                     @Param("content") String content,
                     @Param("tokenCount") Integer tokenCount,
                     @Param("embedding") float[] embedding);

    /**
     * 向量余弦检索：embedding <=> queryVec 得到余弦距离，排序取 Top-K。
     * 关联 document 取文档名，便于前端展示引用来源。
     */
    @Select("SELECT c.doc_id, d.name AS doc_name, c.section, c.chunk_index, c.content, " +
            "(c.embedding <=> #{queryVec, typeHandler=com.ragdemo.mapper.VectorTypeHandler}) AS distance " +
            "FROM chunk c JOIN document d ON c.doc_id = d.doc_id " +
            "ORDER BY distance LIMIT #{topK}")
    @Results({
            @Result(column = "doc_id", property = "docId"),
            @Result(column = "doc_name", property = "docName"),
            @Result(column = "section", property = "section"),
            @Result(column = "chunk_index", property = "chunkIndex"),
            @Result(column = "content", property = "content"),
            @Result(column = "distance", property = "distance")
    })
    List<ChunkSearchResult> search(@Param("queryVec") float[] queryVec, @Param("topK") int topK);

    @Select("SELECT chunk_index, section, content, token_count " +
            "FROM chunk WHERE doc_id = #{docId} ORDER BY chunk_index")
    @Results({
            @Result(column = "chunk_index", property = "chunkIndex"),
            @Result(column = "section", property = "section"),
            @Result(column = "content", property = "content"),
            @Result(column = "token_count", property = "tokenCount")
    })
    List<ChunkDetail> listByDocId(@Param("docId") String docId);
}
