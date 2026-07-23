// 与后端 common/Result 对应：统一响应包络，code=0 表示成功
export interface Result<T> {
  code: number
  message: string
  data: T
}

// 分页包络：与后端 common/PageResult 对应
export interface PageResult<T> {
  list: T[]
  page: number
  size: number
  total: number
}

// 文档视图对象（列表/上传返回，轻量）
export interface DocumentVO {
  docId: string       // 业务主键（不是自增 id）
  name: string        // 文件名
  status: string      // READY
  chunkCount: number  // 分块数量
  createdAt: string   // 上传时间（ISO 字符串）
}

// 文档详情中的单个分块
export interface ChunkDetail {
  chunkIndex: number
  section: string
  content: string
  tokenCount: number
}

// 文档详情：文档元信息 + 其全部分块
export interface DocumentDetailVO extends DocumentVO {
  chunks: ChunkDetail[]
}

// 向量检索命中结果（来自后端 ChunkSearchResult）
export interface ChunkSearchResult {
  docId: string
  docName: string
  section: string
  chunkIndex: number
  content: string
  distance: number   // 余弦距离，越小越相似
  score?: number     // 相似度 = 1 - distance（0~1）
}

// 提交问答的返回：后端同步生成答案后立即返回（M1 抽取式，非流式）
export interface ChatSubmitResponse {
  taskId: string
  answer: string
  sources: ChunkSearchResult[]
  queryEmbedded: boolean
}

// 轮询问答结果：前端按 taskId 轮询拿到的最终状态与答案
export interface ChatTaskResult {
  status: 'pending' | 'generating' | 'completed' | 'failed'
  answer: string
  sources: ChunkSearchResult[]
  queryEmbedded: boolean
}

// 登录返回：携带 JWT
export interface LoginResponse {
  token: string
  username: string
  expiresIn: number
}

// 前端聊天消息（对话历史用）
export interface ChatMessage {
  role: 'user' | 'ai'
  content: string
  sources?: ChunkSearchResult[]    // 仅 ai 消息携带引用来源
  queryEmbedded?: boolean
  status?: 'pending' | 'generating' | 'completed' | 'failed'  // 异步任务状态：排队/生成中/完成/失败
  taskId?: string                  // 关联的问答任务 ID（轮询用）
}
