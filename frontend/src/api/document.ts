import request from './request'
import type { Result, PageResult, DocumentVO, DocumentDetailVO } from '@/types'

export function uploadDocument(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/documents/upload', form) as Promise<Result<DocumentVO>>
}

export function listDocuments(page = 1, size = 20) {
  return request.get('/documents', { params: { page, size } }) as Promise<Result<PageResult<DocumentVO>>>
}

export function getDocument(id: string) {
  return request.get(`/documents/${id}`) as Promise<Result<DocumentDetailVO>>
}

export function deleteDocument(id: string) {
  return request.delete(`/documents/${id}`) as Promise<Result<null>>
}
