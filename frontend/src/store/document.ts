import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DocumentVO } from '@/types'

export const useDocumentStore = defineStore('document', () => {
  const documents = ref<DocumentVO[]>([])
  const total = ref(0)

  function set(list: DocumentVO[], t: number) {
    documents.value = list
    total.value = t
  }

  return { documents, total, set }
})
