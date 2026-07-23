# RAG 与向量检索学习笔记

## 一、什么是向量检索

向量检索（Vector Search）把文本通过 Embedding 模型映射成高维向量，再用距离度量（余弦相似度、欧氏距离）找出语义最接近的条目。它弥补了关键词检索无法理解同义词的短板——搜"如何登录"也能命中讲解"认证"的文档段落。

## 二、PGVector 安装与建表

PostgreSQL 通过 pgvector 扩展支持向量列。先启用扩展，再把字段声明为 `vector(维度)`：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536)
);
CREATE INDEX ON knowledge_chunk
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

注意 `ivfflat` 索引在写入数据后再建效果更好，`lists` 一般取 总行数/1000。

## 三、Embedding 模型选型

中文场景可选 bge-large-zh（1024 维）或 OpenAI text-embedding-3-small（1536 维）。维度越高表达力越强，但存储与检索成本也越高。本项目采用 1536 维以匹配 OpenAI 接口规范。

## 四、分块策略

长文档需切分为语义完整的块再向量化。常见做法：先按 Markdown 标题层级切分，单块超过 400 字再按长度二次切分，相邻块保留 80 字重叠以减少边界信息丢失。块太小会丢失上下文，太大则检索粒度变粗。

## 五、余弦相似度与向量检索的距离计算

PGVector 用 `<=>` 算子计算余弦距离（值越小越相似，0 表示完全相同）。向量检索的核心就是按该距离排序取最近邻：

```sql
SELECT content, embedding <=> :query_vec AS distance
FROM knowledge_chunk
ORDER BY distance
LIMIT 5;
```

## 六、Spring Boot 集成 MyBatis-Plus

用 MyBatis-Plus 的 `BaseMapper` 完成基础 CRUD，向量字段通过自定义 `TypeHandler` 在 `float[]` 与 `vector` 之间互转；写入时用 `?::vector` 把字符串强转为向量类型。分页需注册 `MybatisPlusInterceptor` 并加入 `PaginationInnerInterceptor`。

## 七、常见问题

- 索引不生效：检查是否使用 `vector_cosine_ops` 且查询用了 `<=>` 算子。
- 维度不匹配：Embedding 输出维度必须与表定义 `vector(N)` 一致，否则写入报错。
- 排序反了：`<=>` 返回的是距离而非相似度，应按升序取最近的记录。
