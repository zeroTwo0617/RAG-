package com.ragdemo.mapper;

import io.github.pgvector.PGvector;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * float[] <-> pgvector 类型处理器。
 * pgvector 接受文本格式 "[0.1,0.2,...]"，故以 setString 写入；
 * 检索时把查询向量同样通过该处理器转为文本参数传给 <-> 算子。
 */
public class VectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, PGvector.toSql(parameter));
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
