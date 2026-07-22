import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DocumentVO } from '@/types'

// 文档列表状态：缓存当前页文档与总条数，供文档管理页渲染
export const useDocumentStore = defineStore('document', () => {
  const documents = ref<DocumentVO[]>([])
  const total = ref(0)

  // 拉取列表后调用，更新缓存
  function set(list: DocumentVO[], t: number) {
    documents.value = list
    total.value = t
  }

  return { documents, total, set }
})
