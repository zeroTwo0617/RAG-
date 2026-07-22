import request from './request'
import type { Result, ChatResponse } from '@/types'

// 单轮问答：POST /chat。topK 默认 5，表示召回前 5 个最相似分块
export function ask(question: string, topK = 5) {
  return request.post('/chat', { question, topK }) as Promise<Result<ChatResponse>>
}
