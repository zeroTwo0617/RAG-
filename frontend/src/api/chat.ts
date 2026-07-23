import request from './request'
import type { Result, ChatSubmitResponse, ChatTaskResult } from '@/types'

// 提交问题：POST /chat，后端同步生成答案并立即返回 taskId 与结果（M1 抽取式，非流式）
export function ask(question: string, topK = 5) {
  return request.post('/chat', { question, topK }) as Promise<Result<ChatSubmitResponse>>
}

// 轮询任务结果：GET /chat/result?taskId=xxx，前端按固定间隔拉取直到 completed/failed
export function getChatResult(taskId: string) {
  return request.get('/chat/result', { params: { taskId } }) as Promise<Result<ChatTaskResult>>
}
