# 接口契约（API Contract）

> 本文件是前后端联调的唯一依据。字段名、类型、错误码、分页、鉴权、SSE 格式在此锁死，任何一方不得私自改动。
> 后端使用 SpringDoc 注解，启动后 `/swagger-ui.html` 自动生成在线文档，与本文保持一致。

## 通用约定

- **Base URL**：`http://localhost:8080/api`
- **字段命名**：JSON 一律 `camelCase`
- **日期**：ISO-8601 字符串，如 `2026-07-21T18:30:00`
- **鉴权**：请求头 `Authorization: Bearer <jwt>`（M1 演示接口默认放行，生产由 SecurityConfig 开启）
- **Content-Type**：`application/json`（上传接口为 `multipart/form-data`）

### 统一响应包络

```json
{ "code": 0, "message": "success", "data": { } }
```

- `code = 0` 表示成功；非 0 表示业务/系统错误
- `data` 为 `null` 时表示无返回体（如删除）

### 错误码

| code | 含义 | HTTP 状态 |
|------|------|-----------|
| 0    | 成功 | 200 |
| 400  | 参数错误 | 400 |
| 401  | 未登录 / 无权限 | 401 |
| 404  | 资源不存在 | 404 |
| 409  | 资源冲突（如用户名已存在） | 409 |
| 500  | 服务器内部错误 | 500 |

### 分页格式

列表接口统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [ ],
    "page": 1,
    "size": 20,
    "total": 100
  }
}
```

---

## 1. 文档上传

`POST /api/documents/upload` · `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File(.md) | 是 | Markdown 文件 |

**响应 200**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "docId": "a1b2c3d4",
    "name": "Vue3笔记.md",
    "status": "READY",
    "chunkCount": 12,
    "createdAt": "2026-07-21T18:30:00"
  }
}
```

- `status`：`PROCESSING` / `READY` / `FAILED`
- M1 同步处理：上传即完成解析入库，返回 `READY` 及真实 `chunkCount`

---

## 2. 文档列表

`GET /api/documents?page=1&size=20`

**响应 200**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      { "docId": "a1b2c3d4", "name": "Vue3笔记.md", "status": "READY", "chunkCount": 12, "createdAt": "2026-07-21T18:30:00" }
    ],
    "page": 1, "size": 20, "total": 1
  }
}
```

---

## 3. 文档详情（含分块预览）

`GET /api/documents/{id}`

**响应 200**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "docId": "a1b2c3d4",
    "name": "Vue3笔记.md",
    "status": "READY",
    "chunkCount": 12,
    "createdAt": "2026-07-21T18:30:00",
    "chunks": [
      { "chunkIndex": 0, "section": "## 响应式基础", "content": "Vue3 的响应式基于 Proxy...", "tokenCount": 256 }
    ]
  }
}
```

---

## 4. 删除文档

`DELETE /api/documents/{id}`

**响应 200** `{ "code": 0, "message": "success", "data": null }`

---

## 5. 单轮问答（抽取式，M1）

`POST /api/chat` · `application/json`

**请求体**

```json
{ "question": "Vue3 的响应式原理是什么", "topK": 5 }
```

- `topK` 选填，默认 5，范围 1–10

**响应 200**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "answer": "Vue3 的响应式基于 Proxy...（拼接 Top-K 片段）",
    "sources": [
      {
        "docId": "a1b2c3d4",
        "docName": "Vue3笔记.md",
        "chunkIndex": 0,
        "section": "## 响应式基础",
        "score": 0.873,
        "content": "Vue3 的响应式基于 Proxy..."
      }
    ],
    "queryEmbedded": true
  }
}
```

- `score`：余弦相似度 `1 - distance`，范围 0–1
- `answer`（M1）：直接拼接 Top-K 片段内容（抽取式）；后续里程碑改为 LLM 生成
- `queryEmbedded`：问题是否成功向量化（无 Embedding key 时本地兜底为 true）

---

## 6. 注册（鉴权骨架）

`POST /api/auth/register` · `application/json`

```json
{ "username": "alice", "password": "123456" }
```

**响应 200** `{ "code":0, "data": { "userId": 1, "username": "alice" } }`

---

## 7. 登录（返回 JWT）

`POST /api/auth/login` · `application/json`

```json
{ "username": "alice", "password": "123456" }
```

**响应 200**

```json
{ "code": 0, "data": { "token": "<jwt>", "username": "alice", "expiresIn": 86400 } }
```

---

## 8. 流式问答（M2+，SSE 约定）

`GET /api/chat/stream?question=...` · `text/event-stream`

事件格式（每个 `data:` 一行，以 `\n\n` 分隔）：

```
data: {"type":"retrieval","sources":[ ... ]}

data: {"type":"token","content":"Vue"}

data: {"type":"token","content":"3 的"}

data: {"type":"done"}

```

- `retrieval`：先回传引用来源
- `token`：逐字流式生成内容
- `done`：结束标记

前端用 `EventSource` 或 `fetch` + `ReadableStream` 解析。
