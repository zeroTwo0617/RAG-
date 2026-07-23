import request from './request'
import type { Result, ChatSubmitResponse, ChatTaskResult, ChunkSearchResult } from '@/types'

// 提交问题：POST /chat，后端异步生成，立即返回 taskId（M2 用于订阅 SSE）
export function ask(question: string, topK = 5) {
  return request.post('/chat', { question, topK }) as Promise<Result<ChatSubmitResponse>>
}

// 轮询任务结果：GET /chat/result?taskId=xxx（SSE 不可用时的降级路径）
export function getChatResult(taskId: string) {
  return request.get('/chat/result', { params: { taskId } }) as Promise<Result<ChatTaskResult>>
}

/**
 * 订阅 SSE 流式问答（M2）：按 taskId 打开 /chat/stream，逐字接收生成内容。
 * 后端 SSE 事件 data 为 JSON 字符串，形如：
 *   {"type":"delta","content":"...片段..."}   每个增量文本
 *   {"type":"done","sources":[...],"answer":"完整答案"}  生成结束
 *   {"type":"error","message":"..."}           出错
 * 返回 AbortController，调用方可 abort() 主动中断（如整体超时）。
 */
export function openChatStream(
  taskId: string,
  handlers: {
    onDelta: (text: string) => void
    onDone: (payload: { sources: ChunkSearchResult[]; answer: string }) => void
    onError: (msg: string) => void
  }
): AbortController {
  const ctrl = new AbortController()
  const base = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'
  const token = localStorage.getItem('token')
  const headers: Record<string, string> = {}
  if (token) headers['Authorization'] = `Bearer ${token}`

  // finished 防止 done/error 之后又触发 onError（连接关闭时的重复回调）
  let finished = false
  const finish = (fn: () => void) => {
    if (finished) return
    finished = true
    fn()
  }

  fetch(`${base}/chat/stream?taskId=${encodeURIComponent(taskId)}`, {
    headers,
    signal: ctrl.signal
  })
    .then((res) => {
      if (!res.ok || !res.body) {
        finish(() => handlers.onError('流式连接失败'))
        return
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      const pump = () => {
        reader
          .read()
          .then(({ done, value }) => {
            if (done) {
              // 后端已 complete，正常结束；若还没收到 done 则补一次 error
              finish(() => handlers.onError('连接已关闭'))
              return
            }
            buffer += decoder.decode(value, { stream: true })
            // SSE 事件以空行(\n\n)分隔，逐条解析 data 行
            let idx
            while ((idx = buffer.indexOf('\n\n')) >= 0) {
              const raw = buffer.slice(0, idx)
              buffer = buffer.slice(idx + 2)
              const dataLines = raw.split('\n').filter((l) => l.startsWith('data:'))
              for (const line of dataLines) {
                const jsonStr = line.slice(5).trim()
                if (!jsonStr) continue
                try {
                  const evt = JSON.parse(jsonStr)
                  if (evt.type === 'delta') {
                    handlers.onDelta(evt.content || '')
                  } else if (evt.type === 'done') {
                    finish(() => handlers.onDone({ sources: evt.sources || [], answer: evt.answer || '' }))
                  } else if (evt.type === 'error') {
                    finish(() => handlers.onError(evt.message || '生成出错'))
                  }
                } catch {
                  // 跳过无法解析的行
                }
              }
            }
            pump()
          })
          .catch(() => finish(() => handlers.onError('读取流失败')))
      }
      pump()
    })
    .catch(() => finish(() => handlers.onError('无法连接流式接口')))

  return ctrl
}
