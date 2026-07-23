import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { ChatMessage } from '@/types'

// 本地持久化 key：单用户体验，刷新页面后从 localStorage 恢复会话
const STORAGE_KEY = 'rag-chat-history'

// 从 localStorage 读取历史消息（解析失败时安全回退为空）
function load(): ChatMessage[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const list = raw ? (JSON.parse(raw) as ChatMessage[]) : []
    // 刷新/重新打开页面后，原先的轮询任务已不可能继续。
    // 把仍停留在 pending 且内容为空（说明上次刷新/关闭时答案尚未回来）的 AI 消息
    // 标记为失败并给出提示，避免永久显示「思考中…」
    return list.map((m) => {
      if (m.role === 'ai' && !m.content && (!m.status || m.status === 'pending')) {
        return { ...m, status: 'failed', content: '上一次查询未完成（页面已刷新），请重新提问。' }
      }
      return m
    })
  } catch {
    return []
  }
}

// 聊天状态：用 Pinia 管理整个会话的消息列表（跨组件共享），并持久化到 localStorage
export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>(load())

  // 追加一条消息（用户提问或 AI 回答）。
  // 注意：必须返回「store 内的 reactive 代理引用」，而非原始裸对象。
  // 若返回原始 msg，调用方后续直接对其赋值（如轮询回填 content）不会经过 Vue 的
  // 响应式 set trap，依赖不会被通知，UI 不会刷新 —— 需组件卸载重挂载才显示，
  // 这正是此前「答案要切换页面才出现」的根因。返回数组元素访问得到的代理，
  // 调用方再修改该字段即可实时触发视图与持久化更新。
  function add(msg: ChatMessage): ChatMessage {
    messages.value.push(msg)
    return messages.value[messages.value.length - 1]
  }

  // 清空对话（如"新对话"按钮），同时清掉本地持久化
  function reset() {
    messages.value = []
    localStorage.removeItem(STORAGE_KEY)
  }

  // 主动持久化当前消息列表。
  // 轮询回填答案完成后立即调用，确保结果同步落盘，不依赖 watch 的异步 flush 时机
  // （避免极端情况下刷新恰好发生在 deep watch 回调之前，导致存的是旧占位状态）。
  function persist() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(messages.value))
    } catch {
      // 隐私模式禁用存储或容量已满时静默忽略，不影响内存中的会话
    }
  }

  // 任意消息变化都写回 localStorage（deep 监听覆盖 content/sources 的后续修改）
  watch(messages, persist, { deep: true })

  return { messages, add, reset, persist }
})
