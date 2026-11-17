<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { productsApi, type Category, type ProductInput } from '@/services/products'
import { errorMessage } from '@/services/http'

const props = defineProps<{ open: boolean; productId: string | null; categories: Category[] }>()
const emit = defineEmits<{ close: []; saved: [] }>()
const loading = ref(false)
const saving = ref(false)
const failure = ref('')
const form = ref<ProductInput>(empty())
let sequence = 0
function empty(): ProductInput {
  return { goodsCode: '', name: '', subtitle: '', category: '', coverUrl: '',
    description: '', price: 0, originalPrice: null, stockWarning: 10, sortOrder: 0 }
}
function setDescription(value: string) { form.value.description = value }
async function load() {
  const current = ++sequence
  failure.value = ''
  loading.value = true
  form.value = empty()
  try {
    if (props.productId) {
      const product = await productsApi.detail(props.productId)
      if (current !== sequence) return
      Object.assign(form.value, {
        goodsCode: product.goodsCode, name: product.name, subtitle: product.subtitle,
        category: product.category || '', coverUrl: product.coverUrl,
        description: product.description || '',
        price: product.price, originalPrice: product.originalPrice,
        stockWarning: product.stockWarning, sortOrder: product.sortOrder, lockVersion: product.lockVersion,
      })
    }
  } catch (error) {
    if (current === sequence) failure.value = errorMessage(error)
  } finally {
    if (current === sequence) loading.value = false
  }
}
watch(() => [props.open, props.productId], () => {
  if (props.open) void load()
  else sequence++
}, { immediate: true })
function imageUrl(value: string) {
  if (!value || value.length > 512 || /[\s\\\u0000-\u001f]/.test(value)) return false
  if (value.startsWith('/') && !value.startsWith('//')) return true
  try { const url = new URL(value); return ['http:', 'https:'].includes(url.protocol) && !!url.hostname && !url.username && !url.password }
  catch { return false }
}
async function save() {
  if (loading.value || saving.value || failure.value) return
  const input: ProductInput = { ...form.value, name: form.value.name.trim(), goodsCode: form.value.goodsCode.trim(),
    subtitle: form.value.subtitle.trim(), coverUrl: form.value.coverUrl.trim() }
  if (!input.goodsCode || !input.name || !input.category || !input.coverUrl) {
    ElMessage.warning('请填写商品编码、名称、分类和主图'); return
  }
  if (!props.categories.some(c => c.value === input.category)) {
    ElMessage.warning('请选择有效的商品分类'); return
  }
  if (!Number.isFinite(input.price) || input.price <= 0 ||
      (input.originalPrice !== null && (!Number.isFinite(input.originalPrice) || input.originalPrice < input.price))) {
    ElMessage.warning('售价必须大于 0，划线价不能低于售价'); return
  }
  if (!imageUrl(input.coverUrl)) {
    ElMessage.warning('图片需使用 http(s) 地址或站内路径'); return
  }
  saving.value = true
  try {
    if (props.productId) await productsApi.update(props.productId, input)
    else await productsApi.create(input)
    ElMessage.success(props.productId ? '商品已保存' : '商品已创建，请调整库存后按需上架')
    emit('saved'); emit('close')
  } catch (error) { ElMessage.error(errorMessage(error)) }
  finally { saving.value = false }
}
</script>

<template>
  <el-dialog :model-value="open" :title="productId ? '编辑商品' : '新增商品'" width="760px" top="5vh" body-class="product-editor-body"
    :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving"
    @update:model-value="!saving && emit('close')">
    <el-alert v-if="failure" :title="failure" type="error" :closable="false" show-icon />
    <el-button v-if="failure" @click="load">重新加载</el-button>
    <el-form v-else v-loading="loading" label-position="top" :disabled="loading || saving" @submit.prevent="save">
      <div class="product-form-grid">
        <el-form-item label="商品编码（创建后不可修改）" required>
          <el-input v-model="form.goodsCode" maxlength="64" :disabled="!!productId" />
        </el-form-item>
        <el-form-item label="商品名称" required><el-input v-model="form.name" maxlength="128" /></el-form-item>
        <el-form-item label="商品分类" required>
          <el-select v-model="form.category" placeholder="请选择商品分类" style="width:100%">
            <el-option v-if="form.category && !categories.some(c => c.value === form.category)" :value="form.category" label="分类已失效，请重新选择" disabled />
            <el-option v-for="category in categories" :key="category.value" :label="category.label" :value="category.value" />
          </el-select>
          <small v-if="!categories.length">分类选项尚未加载，请关闭后重试。</small>
        </el-form-item>
        <el-form-item label="排序（数值越小越靠前）">
          <el-input-number v-model="form.sortOrder" :min="0" :max="2147483647" :precision="0" />
        </el-form-item>
        <el-form-item label="售价（元）" required>
          <el-input-number v-model="form.price" :min="0" :max="999999999.99" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="划线价（元，可不填）">
          <el-input-number :model-value="form.originalPrice" @update:model-value="form.originalPrice = $event ?? null"
            :min="0.01" :max="999999999.99" :precision="2" />
        </el-form-item>
        <el-form-item label="库存预警阈值（库存 ≤ 此值时预警）">
          <el-input-number v-model="form.stockWarning" :min="0" :max="2147483647" :precision="0" />
        </el-form-item>
      </div>
      <el-form-item label="商品简介"><el-input v-model="form.subtitle" maxlength="255" show-word-limit /></el-form-item>
      <el-form-item label="主图地址" required><el-input v-model="form.coverUrl" maxlength="512" placeholder="https://… 或 /images/…" /></el-form-item>
      <el-form-item label="商品详情（纯文本）">
        <el-input :model-value="form.description" @update:model-value="setDescription" type="textarea" :rows="6" maxlength="20000" show-word-limit />
      </el-form-item>
      <el-alert title="库存通过列表中的“调整库存”单独修改，新商品默认下架、库存为 0。" type="info" :closable="false" />
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="emit('close')">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="loading || !!failure" @click="save">保存商品</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.product-form-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 0 24px; }
:global(.product-editor-body) { max-height: 70vh; overflow-y: auto; padding-right: 8px; }
small { color: #909399; }
@media(max-width:640px) { .product-form-grid { grid-template-columns: 1fr; } }
</style>
