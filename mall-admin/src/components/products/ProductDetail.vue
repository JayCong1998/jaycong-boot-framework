<script setup lang="ts">
import { ref, watch } from 'vue'
import { productsApi, type Product } from '@/services/products'
import { errorMessage } from '@/services/http'
const props = defineProps<{ open: boolean; productId: string }>()
const emit = defineEmits<{ close: [] }>()
const product = ref<Product | null>(null)
const loading = ref(false)
const failure = ref('')
let sequence = 0
async function load() {
  const current = ++sequence
  product.value = null; failure.value = ''; loading.value = true
  try { const result = await productsApi.detail(props.productId); if (current === sequence) product.value = result }
  catch (error) { if (current === sequence) failure.value = errorMessage(error) }
  finally { if (current === sequence) loading.value = false }
}
watch(() => [props.open, props.productId], () => { if (props.open) void load(); else sequence++ }, { immediate: true })
const money = (n: number | null) => n === null ? '—' : `¥${n.toFixed(2)}`
</script>
<template>
  <el-drawer :model-value="open" title="商品详情" size="min(680px, 100vw)" @update:model-value="emit('close')">
    <el-button :loading="loading" @click="load" style="margin-bottom:20px">刷新详情</el-button>
    <el-alert v-if="failure" :title="failure" type="error" :closable="false" />
    <div v-loading="loading" style="min-height:160px">
      <template v-if="product">
        <el-image v-if="product.coverUrl" :src="product.coverUrl" fit="contain" style="width:100%;height:220px" />
        <h2>{{ product.name }}</h2><p>{{ product.subtitle }}</p>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="商品 ID">{{ product.id }}</el-descriptions-item>
          <el-descriptions-item label="商品编码">{{ product.goodsCode }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ product.category || '未分类' }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="product.status === 1 ? 'success' : 'info'">{{ product.status === 1 ? '已上架' : '已下架' }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="售价 / 划线价">{{ money(product.price) }} / {{ money(product.originalPrice) }}</el-descriptions-item>
          <el-descriptions-item label="库存 / 预警阈值">{{ product.stock }} / {{ product.stockWarning }}</el-descriptions-item>
          <el-descriptions-item label="排序">{{ product.sortOrder }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ product.createTime?.replace('T', ' ') }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ product.updateTime?.replace('T', ' ') }}</el-descriptions-item>
        </el-descriptions>
        <h3>商品详情</h3><p class="description">{{ product.description || '暂无详情' }}</p>
      </template>
    </div>
  </el-drawer>
</template>
<style scoped>
.description { white-space:pre-wrap; overflow-wrap:anywhere; line-height:1.8; }
</style>
