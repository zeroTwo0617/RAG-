<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadDocument, listDocuments, getDocument, deleteDocument } from '@/api/document'
import { useDocumentStore } from '@/store/document'
import type { DocumentDetailVO } from '@/types'

const store = useDocumentStore()
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref<DocumentDetailVO | null>(null)
const detailLoading = ref(false)

async function load(page = 1) {
  loading.value = true
  try {
    const res = await listDocuments(page, 20)
    store.set(res.data.list, res.data.total)
  } finally {
    loading.value = false
  }
}

async function onUpload(file: { raw: File }) {
  try {
    const res = await uploadDocument(file.raw)
    ElMessage.success(`入库成功：${res.data.name}，共 ${res.data.chunkCount} 个分块`)
    load()
  } catch (e) {
    // 错误已由请求拦截器提示
  }
}

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

async function remove(id: string) {
  try {
    await ElMessageBox.confirm('确定删除该文档及其所有分块？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteDocument(id)
  ElMessage.success('已删除')
  load()
}

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
