import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

// 创建根应用，注册全局能力：状态管理(Pinia)、路由、Element Plus 组件库、全部图标
const app = createApp(App)

// 把 @element-plus/icons-vue 里的所有图标注册为全局组件，模板里可直接用 <xxx-icon/>
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
