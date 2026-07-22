<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ask } from '@/api/chat'
import { useChatStore } from '@/store/chat'
import MessageBubble from '@/components/MessageBubble.vue'
import type { ChatMessage } from '@/types'

const chatStore = useChatStore()
const input = ref('')           // 输入框绑定值
const loading = ref(false)      // 是否正在请求（控制"思考中"与按钮 loading）
const scrollRef = ref<HTMLElement | null>(null)  // 聊天滚动容器，用于自动滚到底部

// 发送逻辑：先把自己消息压入 store，再调后端 /chat，把回答与引用来源填回同一条 AI 消息
async function send() {
  const q = input.value.trim()
  if (!q || loading.value) return
  chatStore.add({ role: 'user', content: q })
  input.value = ''
  loading.value = true
  // 先放一条空的 AI 消息占位，请求回来再回填（流式时代这里会变成逐字追加）
  const aiMsg: ChatMessage = { role: 'ai', content: '' }
  chatStore.add(aiMsg)
  scrollToBottom()
  try {
    const res = await ask(q, 5)   // topK=5，召回前 5 个最相似分块
    aiMsg.content = res.data.answer
    aiMsg.sources = res.data.sources
    aiMsg.queryEmbedded = res.data.queryEmbedded
  } catch (e) {
    aiMsg.content = '抱歉，查询出错，请稍后重试。'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 滚动到底部（nextTick 确保 DOM 已更新）
function scrollToBottom() {
  nextTick(() => {
    if (scrollRef.value) {
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight
    }
  })
}

// 键盘事件：Enter 发送，Shift+Enter 换行
function onEnter(e: KeyboardEvent) {
  if (!e.shiftKey) {
    e.preventDefault()
    send()
  }
}
</script>

<template>
  <div class="chat-page">
    <div class="chat-scroll" ref="scrollRef">
      <el-empty
        v-if="chatStore.messages.length === 0"
        description="上传笔记后，试着问：Vue3 的响应式原理是什么？"
      />
      <MessageBubble v-for="(m, i) in chatStore.messages" :key="i" :message="m" />
      <div v-if="loading" class="loading-row">思考中…</div>
    </div>
    <div class="chat-input">
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        placeholder="输入问题，Enter 发送，Shift+Enter 换行"
        @keydown="onEnter"
      />
      <el-button type="primary" :loading="loading" @click="send">发送</el-button>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 880px;
  margin: 0 auto;
}
.chat-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}
.chat-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: #fff;
  border-top: 1px solid #eee;
}
.loading-row {
  padding: 8px;
  color: #909399;
}
</style>
