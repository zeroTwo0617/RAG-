import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage } from '@/types'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])

  function add(msg: ChatMessage) {
    messages.value.push(msg)
  }

  function reset() {
    messages.value = []
  }

  return { messages, add, reset }
})
