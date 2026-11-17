<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Box } from '@element-plus/icons-vue'
const route = useRoute()
const filter = computed(
  () =>
    ({
      'pending-payment': '待付款订单',
      'pending-shipment': '待发货订单',
      'after-sales': '售后待处理',
      'low-stock': '库存预警商品',
    })[String(route.query.status)],
)
</script>

<template>
  <div class="page-heading">
    <div>
      <h1>{{ route.meta.title }}</h1>
      <p>{{ route.meta.description }}</p>
    </div>
  </div>
  <section class="panel module-placeholder">
    <el-tag v-if="filter" class="module-filter">当前入口：{{ filter }}</el-tag
    ><el-empty :description="`${route.meta.title}功能尚未接入`"
      ><template #image
        ><el-icon :size="100" color="#a9bde8"><Box /></el-icon
      ></template>
      <p class="muted">已配置模块路由，当前阶段提供工作台与账号登录功能。</p>
      <el-button type="primary" @click="$router.push('/dashboard')"
        >返回工作台</el-button
      ></el-empty
    >
  </section>
</template>
