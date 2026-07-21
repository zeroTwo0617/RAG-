# RAG 知识库问答系统（简历项目）

> 基于个人技术笔记的 RAG（检索增强生成）问答 Demo，用于展示「Vue3 + Spring Boot + PostgreSQL/PGVector」全栈能力与 RAG 工程深度。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3（Composition API + `<script setup>`）+ Vite + TypeScript + Pinia + Vue Router + Element Plus + Axios |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + Spring Security(JWT) + SpringDoc(OpenAPI) + Flyway |
| 数据 | PostgreSQL + **pgvector** 向量扩展（同一库同时存结构化数据与向量） |
| RAG | Markdown 解析 → 按标题分块 + 长度二次切 + 重叠窗口 → Embedding → 向量召回 → 引用溯源 |
| 部署 | Docker Compose（PostgreSQL+pgvector）、Vercel/Nginx（前端）、Docker（后端） |

## 目录结构

```
rag-kb-demo/
├── frontend/          # Vue3 前端工程
│   └── src/
│       ├── api/       # Axios 请求层（按契约调用后端）
│       ├── components/# 通用组件（消息气泡、来源 chip 等）
│       ├── router/    # 前端路由
│       ├── store/     # Pinia 状态（对话、文档列表）
│       ├── views/     # 页面（ChatView / DocumentView）
│       └── utils/     # 工具（流式解析、格式化）
├── backend/           # Spring Boot 后端工程
│   └── src/main/java/com/ragdemo/
│       ├── config/    # Cors / OpenAPI / Security 配置
│       ├── controller/# 接口层（Document / Chat / Auth）
│       ├── service/   # 业务层（RAG 管线、Embedding、检索）
│       ├── mapper/    # MyBatis 数据访问 + 向量类型处理器
│       ├── entity/    # 实体（Document / Chunk）
│       ├── dto/       # 请求/响应对象
│       └── common/    # 统一响应、错误码
│   └── src/main/resources/db/migration/  # Flyway 建表脚本
├── docker-compose.yml # 本地一键起 PostgreSQL + pgvector
├── api-contract.md    # 前后端接口契约（开发依据）
└── 功能策划.md         # 完整功能策划书
```

## 快速开始

详见 `docs/运行说明.md`。简述：

```bash
# 1. 起数据库（需本机 Docker）
docker compose up -d

# 2. 后端
cd backend && cp .env.example .env   # 填数据库密码等
./mvnw spring-boot:run               # 或 mvn spring-boot:run

# 3. 前端
cd frontend && npm install && npm run dev
```

Swagger 文档：http://localhost:8080/swagger-ui.html

## 当前里程碑（M1）

- ✅ PGVector 建表 + 索引
- ✅ Markdown 上传 → 解析 → 分块 → 向量化入库
- ✅ 单轮抽取式问答（向量召回 Top-K + 引用溯源）
- ⏳ 后续：混合检索 + Rerank、LLM 流式生成、多语言、评测闭环

## 简历话术

> 独立搭建 RAG 知识库问答系统（Vue3 + Spring Boot + PGVector），实现基于 Markdown 结构的分块策略（按标题切 + 长度二次切 + 重叠窗口）、向量召回与引用溯源，覆盖前后端分离架构、SQL 向量检索与统一接口契约。
