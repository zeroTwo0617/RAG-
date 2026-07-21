export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  page: number
  size: number
  total: number
}

export interface DocumentVO {
  docId: string
  name: string
  status: string
  chunkCount: number
  createdAt: string
}

export interface ChunkDetail {
  chunkIndex: number
  section: string
  content: string
  tokenCount: number
}

export interface DocumentDetailVO extends DocumentVO {
  chunks: ChunkDetail[]
}

export interface ChunkSearchResult {
  docId: string
  docName: string
  section: string
  chunkIndex: number
  content: string
  distance: number
  score?: number
}

export interface ChatResponse {
  answer: string
  sources: ChunkSearchResult[]
  queryEmbedded: boolean
}

export interface LoginResponse {
  token: string
  username: string
  expiresIn: number
}

export interface ChatMessage {
  role: 'user' | 'ai'
  content: string
  sources?: ChunkSearchResult[]
  queryEmbedded?: boolean
}
