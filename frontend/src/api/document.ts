import request from './request'
import type { Result, PageResult, DocumentVO, DocumentDetailVO } from '@/types'

// 上传 Markdown：用 FormData 包裹文件，POST /documents/upload（后端解析入库）
export function uploadDocument(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/documents/upload', form) as Promise<Result<DocumentVO>>
}

// 文档列表：GET /documents?page=&size=，返回分页包络
export function listDocuments(page = 1, size = 20) {
  return request.get('/documents', { params: { page, size } }) as Promise<Result<PageResult<DocumentVO>>>
}

// 文档详情（含分块预览）：GET /documents/:id
export function getDocument(id: string) {
  return request.get(`/documents/${id}`) as Promise<Result<DocumentDetailVO>>
}

// 删除文档（级联删分块）：DELETE /documents/:id
export function deleteDocument(id: string) {
  return request.delete(`/documents/${id}`) as Promise<Result<null>>
}
