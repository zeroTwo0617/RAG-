<script setup lang="ts">
import { useRoute } from 'vue-router'

// 根布局：顶部 header（品牌 + 导航菜单）+ 主体区（路由出口）
// useRoute 拿到当前路由，用于高亮当前菜单项
const route = useRoute()
</script>

<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="brand">📚 RAG 知识库问答</div>
      <el-menu mode="horizontal" :default-active="route.path" router class="nav-menu">
        <el-menu-item index="/chat">问答</el-menu-item>
        <el-menu-item index="/documents">文档管理</el-menu-item>
      </el-menu>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
}
.app-container {
  height: 100%;
}
.app-header {
  display: flex;
  align-items: center;
  gap: 24px;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.brand {
  font-weight: 600;
  font-size: 16px;
  white-space: nowrap;
}
.nav-menu {
  flex: 1;
  border-bottom: none;
}
.app-main {
  background: #f5f7fa;
  padding: 16px;
}
</style>
