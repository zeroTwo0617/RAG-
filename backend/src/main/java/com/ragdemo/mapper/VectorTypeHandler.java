package com.ragdemo.mapper;

import com.pgvector.PGvector;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * float[] <-> pgvector 类型处理器。
 * 用官方 PGvector 类把 float[] 转成向量字面量字符串 "[0.1,0.2,...]"，
 * 以 setString 写入；SQL 侧通过 `?::vector` 强转为 vector 类型（无需 registerTypes，连接池安全）。
 * 检索时把查询向量同样通过该处理器转为文本参数传给 <-> 算子。
 */
public class VectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, new PGvector(parameter).toString());
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) {
        return null;
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) {
        return null;
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) {
        return null;
    }
}
