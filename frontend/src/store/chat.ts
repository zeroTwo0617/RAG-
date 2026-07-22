import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage } from '@/types'

// 聊天状态：用 Pinia 管理整个会话的消息列表（跨组件共享）
export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])

  // 追加一条消息（用户提问或 AI 回答）
  function add(msg: ChatMessage) {
    messages.value.push(msg)
  }

  // 清空对话（如"新对话"按钮）
  function reset() {
    messages.value = []
  }

  return { messages, add, reset }
})
