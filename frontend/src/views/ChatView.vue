<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ask, getChatResult, openChatStream } from '@/api/chat'
import { useChatStore } from '@/store/chat'
import MessageBubble from '@/components/MessageBubble.vue'
import type { ChatMessage } from '@/types'

const chatStore = useChatStore()
const input = ref('')           // 输入框绑定值
const loading = ref(false)      // 是否正在请求（控制"思考中"与按钮 loading）
const scrollRef = ref<HTMLElement | null>(null)  // 聊天滚动容器，用于自动滚到底部

// 发送逻辑（M2）：先压入用户消息与一条空 AI 占位（带 taskId），再：
// ① 优先订阅 SSE 流式接口，后端逐字推送时实时 append 到 aiMsg.content（打字机效果）；
// ② 若 SSE 不可用（如浏览器/网络不支持），回退到轮询 /chat/result 拿最终结果。
// 必须接收 add() 返回的 reactive 代理引用，后续对其 content/status 的赋值才会触发实时刷新。
async function send() {
  const q = input.value.trim()
  if (!q || loading.value) return
  chatStore.add({ role: 'user', content: q })
  input.value = ''
  loading.value = true
  const aiMsg = chatStore.add({
    role: 'ai',
    content: '',
    status: 'pending',
    taskId: '',
  })
  scrollToBottom()
  try {
    // 1) 提交问题，后端立即返回 taskId（不阻塞生成）
    const submitRes = await ask(q, 5)
    const taskId = submitRes.data.taskId
    aiMsg.taskId = taskId
    aiMsg.queryEmbedded = true
    aiMsg.status = 'generating'
    // 2) SSE 流式优先；异常则降级为轮询
    try {
      await streamChat(taskId, aiMsg)
    } catch (e) {
      await pollFallback(taskId, aiMsg)
    }
  } catch (e) {
    aiMsg.content = '抱歉，提交失败，请稍后重试。'
    aiMsg.status = 'failed'
    loading.value = false
    chatStore.persist()
  }
}

// SSE 流式订阅：delta 实时追加，done 回填来源并收尾
function streamChat(taskId: string, aiMsg: ChatMessage): Promise<void> {
  return new Promise((resolve) => {
    // 整体超时保护（90s 无 done 则中断并降级）
    const timeout = setTimeout(() => ctrl.abort(), 90000)
    const ctrl = openChatStream(taskId, {
      onDelta: (text) => {
        aiMsg.content += text          // 逐字追加（响应式代理，实时刷新）
        scrollToBottom()
      },
      onDone: (payload) => {
        clearTimeout(timeout)
        aiMsg.sources = payload.sources
        aiMsg.content = payload.answer || aiMsg.content
        aiMsg.status = 'completed'
        loading.value = false
        scrollToBottom()
        chatStore.persist()             // 完成后立即落盘，刷新不再回退
        resolve()
      },
      onError: (msg) => {
        clearTimeout(timeout)
        aiMsg.content = msg
        aiMsg.status = 'failed'
        loading.value = false
        chatStore.persist()
        resolve()
      },
    })
  })
}

// 降级路径：SSE 不可用时轮询 /chat/result，直到 completed/failed（最多 40 次 ≈ 32s）
async function pollFallback(taskId: string, aiMsg: ChatMessage) {
  let ticks = 0
  while (ticks++ < 40) {
    await new Promise((r) => setTimeout(r, 800))
    try {
      const r = await getChatResult(taskId)
      const st = r.data.status
      if (st === 'completed') {
        const ans = r.data.answer
        aiMsg.content = ans && ans.trim()
          ? ans
          : '未在知识库中找到相关内容，请先上传笔记，或换个问法试试。'
        aiMsg.sources = r.data.sources
        aiMsg.status = 'completed'
        loading.value = false
        scrollToBottom()
        chatStore.persist()
        return
      } else if (st === 'failed') {
        aiMsg.content = '抱歉，查询出错，请稍后重试。'
        aiMsg.status = 'failed'
        loading.value = false
        chatStore.persist()
        return
      }
    } catch (e) {
      // 轮询间隔中偶发网络抖动，忽略继续
    }
  }
  aiMsg.content = '查询超时，请稍后重试。'
  aiMsg.status = 'failed'
  loading.value = false
  chatStore.persist()
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
