# Vue3 学习笔记

## 响应式基础

Vue3 的响应式系统基于 ES6 的 Proxy 实现。当我们把一个普通对象传给 `reactive()` 时，Vue 会返回一个代理对象。读取属性时收集依赖（track），修改属性时触发更新（trigger）。相比 Vue2 基于 `Object.defineProperty` 的实现，Proxy 可以监听属性的新增与删除，也天然支持数组索引和 length 变化。

## ref 与 reactive 的区别

`ref` 用于包装基本类型，内部通过 `.value` 访问；在模板中会自动解包。`reactive` 用于对象/数组，直接返回代理。一般来说，基本类型用 `ref`，对象用 `reactive`。需要整体替换对象时，`ref` 更合适，因为 `reactive` 直接赋值会丢失响应性。

## 组合式 API

组合式 API 通过 `setup` 或 `<script setup>` 把逻辑按功能聚合，而不是像选项式 API 那样按选项（data/methods/computed）拆分。逻辑复用通过"组合式函数"（以 `use` 开头的函数）实现，比 mixin 更清晰、无命名冲突。

## 生命周期钩子

常用的钩子有 `onMounted`、`onUpdated`、`onUnmounted`。在 `<script setup>` 中直接调用即可，Vue 会自动绑定当前组件实例。需要清理的副作用（如定时器、事件监听）应在 `onUnmounted` 中移除。

## 组件通信

父传子用 `props`，子传父用 `emit`。跨层级用 `provide` / `inject`。复杂全局状态交给 Pinia 管理。v-model 在 Vue3 中支持多个，且通过 `modelValue` + `update:modelValue` 实现。

## 性能优化

使用 `computed` 缓存派生值；用 `shallowRef` / `shallowReactive` 跳过深层响应以降低开销；列表用 `v-memo` 或虚拟列表处理大数据；路由级组件用异步 `defineAsyncComponent` 做代码分割。
