import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { ChatMessage } from '@/types'

// 本地持久化 key：单用户体验，刷新页面后从 localStorage 恢复会话
const STORAGE_KEY = 'rag-chat-history'

// 从 localStorage 读取历史消息（解析失败时安全回退为空）
function load(): ChatMessage[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as ChatMessage[]) : []
  } catch {
    return []
  }
}

// 聊天状态：用 Pinia 管理整个会话的消息列表（跨组件共享），并持久化到 localStorage
export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>(load())

  // 追加一条消息（用户提问或 AI 回答），返回被存入数组的同一引用，
  // 便于调用方后续直接修改该对象（如流式逐字回填）且能实时反映到视图
  function add(msg: ChatMessage): ChatMessage {
    messages.value.push(msg)
    return msg
  }

  // 清空对话（如"新对话"按钮），同时清掉本地持久化
  function reset() {
    messages.value = []
    localStorage.removeItem(STORAGE_KEY)
  }

  // 任意消息变化都写回 localStorage（deep 监听覆盖 content/sources 的后续修改）
  watch(
    messages,
    (val) => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(val))
    },
    { deep: true }
  )

  return { messages, add, reset }
})
