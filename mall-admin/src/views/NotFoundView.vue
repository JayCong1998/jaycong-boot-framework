<template>
  <main class="not-found">
    <span>404</span>
    <h1>页面走丢了</h1>
    <p class="muted">访问的页面不存在，请检查地址或返回工作台。</p>
    <el-button type="primary" @click="$router.replace('/dashboard')"
      >返回工作台</el-button
    >
  </main>
</template>
