<script setup lang="ts">
import type { ChunkSearchResult } from '@/types'

defineProps<{ sources: ChunkSearchResult[] }>()

function scoreOf(s: ChunkSearchResult): number {
  const v = s.score ?? 1 - (s.distance ?? 1)
  return Math.round(v * 100)
}
</script>

<template>
  <div class="sources">
    <div class="sources-title">引用来源（{{ sources.length }}）</div>
    <el-collapse>
      <el-collapse-item v-for="(s, i) in sources" :key="i" :name="i">
        <template #title>
          <span class="src-title">
            <b>{{ s.docName }}</b>
            <span class="src-section" v-if="s.section"> · {{ s.section }}</span>
            <span class="src-score">相似度 {{ scoreOf(s) }}%</span>
          </span>
        </template>
        <div class="src-content">{{ s.content }}</div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<style scoped>
.sources {
  margin-top: 10px;
  border-top: 1px dashed #ebeef5;
  padding-top: 8px;
}
.sources-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.src-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.src-section {
  color: #606266;
}
.src-score {
  margin-left: auto;
  color: #67c23a;
  font-size: 12px;
}
.src-content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
  font-size: 13px;
}
</style>
