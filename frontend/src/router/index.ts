import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '@/views/ChatView.vue'
import DocumentView from '@/views/DocumentView.vue'

// 前端路由表：默认进 / 重定向到 /chat（问答页）；/documents 为文档管理页
// 使用 HTML5 history 模式（URL 无 #），需要后端/部署做 fallback 配置
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/chat' },
    { path: '/chat', name: 'chat', component: ChatView },
    { path: '/documents', name: 'documents', component: DocumentView }
  ]
})

export default router
