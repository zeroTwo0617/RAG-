import request from './request'
import type { Result, ChatResponse } from '@/types'

export function ask(question: string, topK = 5) {
  return request.post('/chat', { question, topK }) as Promise<Result<ChatResponse>>
}
