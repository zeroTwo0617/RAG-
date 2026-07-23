<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ask, getChatResult } from '@/api/chat'
import { useChatStore } from '@/store/chat'
import MessageBubble from '@/components/MessageBubble.vue'
import type { ChatMessage } from '@/types'

const chatStore = useChatStore()
const input = ref('')           // 输入框绑定值
const loading = ref(false)      // 是否正在请求（控制"思考中"与按钮 loading）
const scrollRef = ref<HTMLElement | null>(null)  // 聊天滚动容器，用于自动滚到底部

// 发送逻辑：先压入用户消息与一条空 AI 占位（带 taskId），再轮询任务状态异步回填答案。
// 任务轮询让发送与响应解耦——用户消息即时上屏，AI 气泡实时显示「检索中 / 思考中 / 完成」，
// 答案回来立即呈现，不必干等整段请求返回（也为 M2 流式生成预留了同样的轮询通道）。
async function send() {
  const q = input.value.trim()
  if (!q || loading.value) return
  chatStore.add({ role: 'user', content: q })
  input.value = ''
  loading.value = true
  // 占位 AI 消息：先给 taskId 与中间状态，轮询回来再回填内容
  const aiMsg: ChatMessage = {
    role: 'ai',
    content: '',
    status: 'pending',
    taskId: '',
  }
  chatStore.add(aiMsg)
  scrollToBottom()
  let timer: ReturnType<typeof setInterval> | null = null
  try {
    // 1) 提交问题，后端立即返回 taskId（不阻塞生成）
    const submitRes = await ask(q, 5)
    const taskId = submitRes.data.taskId
    aiMsg.taskId = taskId
    aiMsg.queryEmbedded = true
    // 2) 轮询任务状态，直到完成或失败
    timer = setInterval(async () => {
      try {
        const r = await getChatResult(taskId)
        const st = r.data.status
        aiMsg.status = st
        if (st === 'completed') {
          const ans = r.data.answer
          aiMsg.content = ans && ans.trim()
            ? ans
            : '未在知识库中找到相关内容，请先上传笔记，或换个问法试试。'
          aiMsg.sources = r.data.sources
          if (timer) clearInterval(timer)
          loading.value = false
          scrollToBottom()
        } else if (st === 'failed') {
          aiMsg.content = '抱歉，查询出错，请稍后重试。'
          if (timer) clearInterval(timer)
          loading.value = false
          scrollToBottom()
        }
      } catch (e) {
        aiMsg.content = '抱歉，查询出错，请稍后重试。'
        if (timer) clearInterval(timer)
        loading.value = false
      }
    }, 800)
  } catch (e) {
    aiMsg.content = '抱歉，查询出错，请稍后重试。'
    loading.value = false
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

// 键盘事件：仅在「Enter 且非 Shift」且不在输入法组合中时才发送；其余按键一律放行，
// 避免误拦截导致输入框无法复制/粘贴（Ctrl+C/V），也避免输入法选词过程误发送
function onKeydown(e: KeyboardEvent) {
  if (e.isComposing) return            // 拼音/输入法上屏过程中不拦截
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()                  // 仅拦截 Enter 的默认换行，改为发送
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
        @keydown="onKeydown"
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
