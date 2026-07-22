<script setup lang="ts">
import type { ChatMessage } from '@/types'
import SourcePanel from './SourcePanel.vue'

// 单条消息气泡：根据 role 决定头像与左右布局；
// AI 消息若携带 sources 则额外渲染引用来源面板（RAG 的核心卖点）
defineProps<{ message: ChatMessage }>()
</script>

<template>
  <div class="msg-row" :class="message.role">
    <div class="avatar">{{ message.role === 'user' ? '🧑' : '🤖' }}</div>
    <div class="bubble">
      <div class="content" v-if="message.content">{{ message.content }}</div>
      <div class="content muted" v-else>…</div>
      <SourcePanel
        v-if="message.role === 'ai' && message.sources && message.sources.length"
        :sources="message.sources"
      />
    </div>
  </div>
</template>

<style scoped>
.msg-row {
  display: flex;
  gap: 10px;
  margin: 12px 0;
}
.msg-row.user {
  flex-direction: row-reverse;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  border: 1px solid #eee;
}
.bubble {
  max-width: 78%;
  background: #fff;
  border-radius: 10px;
  padding: 10px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.msg-row.user .bubble {
  background: #ecf5ff;
}
.content {
  white-space: pre-wrap;
  line-height: 1.6;
}
.muted {
  color: #c0c4cc;
}
</style>
