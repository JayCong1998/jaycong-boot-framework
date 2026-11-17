<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productsApi, type Category, type Product, type ProductQuery } from '@/services/products'
import { errorMessage } from '@/services/http'
import ProductEditor from '@/components/products/ProductEditor.vue'
import ProductDetail from '@/components/products/ProductDetail.vue'

const route = useRoute()
const router = useRouter()
const filters = reactive({ name: '', goodsCode: '', category: '', status: '' as '' | number,
  lowStock: route.query.filter === 'low-stock' || route.query.status === 'low-stock' })
const query = ref<ProductQuery>({ page: 1, size: 20, lowStock: filters.lowStock || undefined })
const rows = ref<Product[]>([])
const total = ref(0)
const loading = ref(false)
const failure = ref('')
const categories = ref<Category[]>([])
const categoryFailure = ref('')
const editOpen = ref(false)
const editId = ref<string | null>(null)
const detailOpen = ref(false)
const detailId = ref('')
const acting = ref(false)
const stockOpen = ref(false)
const stockProduct = ref<Product | null>(null)
const stockLoading = ref(false)
const stockFailure = ref('')
const quantity = ref(1)
let listSequence = 0
let stockSequence = 0
let alive = true
const money = (value: number) => `¥${value.toFixed(2)}`
async function load() {
  const current = ++listSequence
  loading.value = true; failure.value = ''
  try {
    const result = await productsApi.list({ ...query.value })
    if (current !== listSequence || !alive) return
    rows.value = result.rows; total.value = result.total
    if (!result.rows.length && query.value.page > 1 && result.total) {
      query.value.page = Math.ceil(result.total / query.value.size); await load()
    }
  } catch (error) {
    if (current === listSequence && alive) { rows.value = []; total.value = 0; failure.value = errorMessage(error) }
  } finally { if (current === listSequence && alive) loading.value = false }
}
async function loadCategories() {
  categoryFailure.value = ''
  try { const result = await productsApi.categories(); if (alive) categories.value = result }
  catch (error) { if (alive) categoryFailure.value = errorMessage(error) }
}
function search() {
  query.value = { page: 1, size: query.value.size, name: filters.name.trim() || undefined,
    goodsCode: filters.goodsCode.trim() || undefined, category: filters.category || undefined,
    status: filters.status === '' ? undefined : filters.status, lowStock: filters.lowStock || undefined }
  void load()
}
function reset() {
  Object.assign(filters, { name: '', goodsCode: '', category: '', status: '', lowStock: false })
  if (route.query.filter || route.query.status === 'low-stock') void router.replace({ path: '/products' })
  search()
}
function edit(product?: Product) { editId.value = product?.id || null; editOpen.value = true; void loadCategories() }
function showDetail(product: Product) { detailId.value = product.id; detailOpen.value = true }
async function action(product: Product, kind: 'status' | 'delete') {
  if (acting.value) return
  acting.value = true
  try {
    await ElMessageBox.confirm(kind === 'delete' ? `确认删除“${product.name}”？商品将被逻辑删除。` :
      `确认${product.status === 1 ? '下架' : '上架'}“${product.name}”？`, '操作确认', { type: 'warning' })
    if (kind === 'delete') await productsApi.remove(product)
    else await productsApi.status(product, product.status === 1 ? 0 : 1)
    ElMessage.success('操作成功'); await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') { ElMessage.error(errorMessage(error)); await load() }
  } finally { acting.value = false }
}
async function openStock(product: Product) {
  stockOpen.value = true; stockProduct.value = null; quantity.value = 1
  await loadStock(product.id)
}
async function loadStock(id: string) {
  const current = ++stockSequence
  stockLoading.value = true; stockFailure.value = ''
  try { const result = await productsApi.detail(id); if (current === stockSequence && stockOpen.value) stockProduct.value = result }
  catch (error) { if (current === stockSequence) stockFailure.value = errorMessage(error) }
  finally { if (current === stockSequence) stockLoading.value = false }
}
async function saveStock() {
  const product = stockProduct.value
  if (!product || acting.value || stockLoading.value || stockFailure.value) return
  if (!Number.isInteger(quantity.value) || quantity.value === 0 || product.stock + quantity.value < 0 || product.stock + quantity.value > 2147483647) {
    ElMessage.warning('调整数量必须为非零整数，调整后库存需在 0～2147483647 之间'); return
  }
  acting.value = true
  try {
    await productsApi.stock(product, quantity.value)
    ElMessage.success('库存已调整'); stockOpen.value = false; await load()
  } catch (error) {
    ElMessage.error(errorMessage(error))
    // 失败后要求刷新，避免超时重试导致同一增量被重复提交。
    stockFailure.value = '请关闭后重新打开库存调整，确认最新库存后再操作。'
    await load()
  } finally { acting.value = false }
}
watch(() => [route.query.filter, route.query.status], () => {
  filters.lowStock = route.query.filter === 'low-stock' || route.query.status === 'low-stock'; search()
})
onMounted(() => { void load(); void loadCategories() })
onBeforeUnmount(() => { alive = false; listSequence++; stockSequence++ })
</script>

<template>
  <section class="products-page">
    <header class="products-heading">
      <div><h1>商品管理</h1><p>管理商品资料、价格、库存与上架状态</p></div>
      <div><el-button type="primary" @click="edit()">新增商品</el-button></div>
    </header>
    <div class="products-card">
      <el-form :inline="true" class="product-filters" @submit.prevent="search">
        <el-form-item label="商品名称"><el-input v-model="filters.name" placeholder="输入商品名称" maxlength="128" clearable /></el-form-item>
        <el-form-item label="商品编码"><el-input v-model="filters.goodsCode" placeholder="输入完整商品编码" maxlength="64" clearable /></el-form-item>
        <el-form-item label="分类"><el-select v-model="filters.category" clearable placeholder="全部分类" style="width:170px"><el-option v-for="category in categories" :key="category.value" :label="category.label" :value="category.value" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="filters.status" clearable placeholder="全部状态" style="width:130px" @clear="filters.status = ''"><el-option label="已上架" :value="1" /><el-option label="已下架" :value="0" /></el-select></el-form-item>
        <el-form-item><el-checkbox v-model="filters.lowStock">仅看低库存</el-checkbox></el-form-item>
        <el-form-item><el-button type="primary" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
      <el-alert v-if="categoryFailure" :title="`分类加载失败：${categoryFailure}`" type="warning" :closable="false"><template #default><el-button link @click="loadCategories">重试</el-button></template></el-alert>
      <el-alert v-if="failure" :title="failure" type="error" :closable="false" style="margin-bottom:16px"><template #default><el-button link @click="load">重新加载</el-button></template></el-alert>
      <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无商品，可以新增商品或调整筛选条件">
        <el-table-column label="商品" min-width="270"><template #default="{ row }"><div class="product-cell"><el-image :src="row.coverUrl" fit="cover"><template #error><span class="image-placeholder">暂无图片</span></template></el-image><div><strong>{{ row.name }}</strong><small>{{ row.goodsCode }}</small></div></div></template></el-table-column>
        <el-table-column label="分类" min-width="120"><template #default="{ row }">{{ row.category || '未分类' }}</template></el-table-column>
        <el-table-column label="售价" width="130"><template #default="{ row }">{{ money(row.price) }}</template></el-table-column>
        <el-table-column label="库存" width="130"><template #default="{ row }"><span :class="{ 'stock-warning': row.stock <= row.stockWarning }">{{ row.stock }}</span><el-tag v-if="row.stock <= row.stockWarning" size="small" type="warning" style="margin-left:6px">预警</el-tag></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已上架' : '已下架' }}</el-tag></template></el-table-column>
        <el-table-column label="更新时间" width="175"><template #default="{ row }">{{ row.updateTime?.replace('T', ' ') }}</template></el-table-column>
        <el-table-column label="操作" width="300" fixed="right"><template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">详情</el-button><el-button link type="primary" @click="edit(row)">编辑</el-button>
          <el-button link type="primary" :disabled="acting" @click="openStock(row)">调整库存</el-button>
          <el-button link :type="row.status === 1 ? 'warning' : 'success'" :disabled="acting" @click="action(row, 'status')">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
          <el-button link type="danger" :disabled="acting || row.status === 1" @click="action(row, 'delete')">删除</el-button>
        </template></el-table-column>
      </el-table>
      <div class="products-pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="query.page = 1; load()" /></div>
    </div>
    <ProductEditor :open="editOpen" :product-id="editId" :categories="categories" @close="editOpen = false" @saved="load" />
    <ProductDetail :open="detailOpen" :product-id="detailId" @close="detailOpen = false" />
    <el-dialog v-model="stockOpen" title="调整库存" width="480px" :close-on-click-modal="false" :close-on-press-escape="!acting" :show-close="!acting">
      <el-alert v-if="stockFailure" :title="stockFailure" type="error" :closable="false" />
      <div v-loading="stockLoading" style="min-height:150px">
        <template v-if="stockProduct"><p>{{ stockProduct.name }}</p><p>当前库存：<strong>{{ stockProduct.stock }}</strong></p>
          <el-form label-position="top" :disabled="acting || !!stockFailure"><el-form-item label="调整数量（正数增加，负数减少）"><el-input-number v-model="quantity" :min="-2147483647" :max="2147483647" :precision="0" /></el-form-item></el-form>
          <p>调整后库存：{{ stockProduct.stock + (quantity || 0) }}</p>
        </template>
      </div>
      <template #footer><el-button :disabled="acting" @click="stockOpen = false">取消</el-button><el-button type="primary" :loading="acting" :disabled="stockLoading || !!stockFailure || !stockProduct" @click="saveStock">确认调整</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.products-heading { display:flex; justify-content:space-between; gap:20px; align-items:center; margin-bottom:24px; }
.products-heading h1 { font-size:24px; margin:0 0 8px; }.products-heading p { margin:0; color:#7a8495; }
.products-card { padding:24px; background:var(--el-bg-color,#fff); border:1px solid #edf0f5; border-radius:16px; }
.product-cell { display:flex; align-items:center; gap:12px; }.product-cell .el-image { width:52px; height:52px; flex-shrink:0; border-radius:8px; background:#f5f7fa; }
.product-cell small { display:block; color:#909399; margin-top:4px; }.product-cell strong { overflow-wrap:anywhere; }.image-placeholder { font-size:10px; color:#909399; }
.stock-warning { color:#b77916; font-weight:600; }.products-pagination { display:flex; justify-content:flex-end; margin-top:24px; overflow-x:auto; }
@media(max-width:768px) { .products-heading { align-items:flex-start; flex-direction:column; }.products-card { padding:16px; } }
</style>
