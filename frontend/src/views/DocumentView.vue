<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadDocument, listDocuments, getDocument, deleteDocument } from '@/api/document'
import { useDocumentStore } from '@/store/document'
import type { DocumentDetailVO } from '@/types'

const store = useDocumentStore()
const router = useRouter()
const loading = ref(false)          // 列表加载中
const detailVisible = ref(false)    // 详情弹窗显隐
const detail = ref<DocumentDetailVO | null>(null)  // 当前查看的文档详情
const detailLoading = ref(false)    // 详情加载中

// 拉取文档列表（默认第 1 页，每页 20），写入 store
async function load(page = 1) {
  loading.value = true
  try {
    const res = await listDocuments(page, 20)
    store.set(res.data.list, res.data.total)
  } finally {
    loading.value = false
  }
}

// 上传回调：el-upload 设了 auto-upload=false，这里手动拿原始文件调后端入库
async function onUpload(file: { raw: File }) {
  try {
    const res = await uploadDocument(file.raw)
    // 入库只是第一步（拆解分块），真正出答案在「问答」页。上传成功后明确引导用户去提问，
    // 避免"拆完就停住、不知道下一步"的体验断点
    ElMessageBox.confirm(
      `已成功入库「${res.data.name}」，共 ${res.data.chunkCount} 个分块。\n现在去问答页提问吗？`,
      '入库成功',
      { confirmButtonText: '去提问 →', cancelButtonText: '稍后', type: 'success' }
    ).then(() => router.push('/chat')).catch(() => {})
    load()  // 刷新列表
  } catch (e) {
    // 错误已由请求拦截器统一弹提示，这里无需重复处理
  }
}

// 打开详情弹窗并加载该文档的分块预览
async function openDetail(id: string) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    const res = await getDocument(id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

// 删除：先二次确认，再调后端删除并刷新列表
async function remove(id: string) {
  try {
    await ElMessageBox.confirm('确定删除该文档及其所有分块？', '提示', { type: 'warning' })
  } catch {
    return  // 用户取消
  }
  await deleteDocument(id)
  ElMessage.success('已删除')
  load()
}

// 页面挂载即加载一次列表
onMounted(load)
</script>

<template>
  <div class="doc-page">
    <div class="doc-header">
      <h3>文档管理</h3>
      <el-upload accept=".md" :auto-upload="false" :show-file-list="false" :on-change="onUpload">
        <el-button type="primary" :loading="loading">上传 Markdown</el-button>
      </el-upload>
    </div>

    <el-table :data="store.documents" v-loading="loading" stripe>
      <el-table-column prop="name" label="文件名" min-width="200" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'READY' ? 'success' : 'warning'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="分块数" width="90" />
      <el-table-column prop="createdAt" label="上传时间" min-width="160" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.docId)">详情</el-button>
          <el-button link type="danger" @click="remove(row.docId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" title="文档分块预览" width="720px">
      <div v-if="detail">
        <p class="detail-meta">{{ detail.name }} · 共 {{ detail.chunkCount }} 个分块</p>
        <el-collapse v-loading="detailLoading">
          <el-collapse-item v-for="(c, i) in detail.chunks" :key="i" :name="i">
            <template #title>
              <span>
                #{{ c.chunkIndex + 1 }}
                <span class="c-section" v-if="c.section">{{ c.section }}</span>
                <span class="c-token">~{{ c.tokenCount }} tokens</span>
              </span>
            </template>
            <div class="c-content">{{ c.content }}</div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.doc-page {
  max-width: 1000px;
  margin: 0 auto;
}
.doc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.detail-meta {
  color: #909399;
  font-size: 13px;
}
.c-section {
  color: #606266;
  margin-left: 6px;
}
.c-token {
  color: #c0c4cc;
  font-size: 12px;
  margin-left: 8px;
}
.c-content {
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
