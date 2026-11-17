<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { memberOperationsApi, type MemberOperation, type OperationQuery } from '@/services/memberOperations'
import { errorMessage } from '@/services/http'

const route = useRoute()
const actions: Record<string, string> = { UPDATE_NICKNAME: '修改昵称', ENABLE: '启用会员', DISABLE: '禁用会员', REVOKE_SESSIONS: '重试撤销会话' }
const outcomes: Record<string, string> = { SUCCESS: '成功', FAILED: '失败', PARTIAL: '部分完成', PROCESSING: '处理中 / 待核实' }
const actorTypes: Record<string, string> = { ADMIN: '管理员', MEMBER: '会员本人' }
const sources: Record<string, string> = { ADMIN_WEB: '管理后台', MEMBER_WEB: '会员端' }
const filters = reactive({ memberId: '', operatorId: '', action: '', operatorType: '', outcome: '' })
const dates = ref<[string, string] | null>(null)
const query = ref<OperationQuery>({ page: 1, size: 20 })
const rows = ref<MemberOperation[]>([])
const total = ref(0)
const loading = ref(false)
const failure = ref('')
const drawer = ref(false)
const detailId = ref('')
const detail = ref<MemberOperation | null>(null)
const detailLoading = ref(false)
const detailFailure = ref('')
let listSequence = 0
let detailSequence = 0
const time = (value: string) => value.replace('T', ' ')
const tone = (outcome: string) => outcome === 'SUCCESS' ? 'success' : outcome === 'FAILED' ? 'danger' : 'warning'
async function load() {
  const sequence = ++listSequence
  loading.value = true
  failure.value = ''
  try {
    const result = await memberOperationsApi.list({ ...query.value })
    if (sequence !== listSequence) return
    rows.value = result.rows
    total.value = result.total
  } catch (error) {
    if (sequence === listSequence) {
      rows.value = []
      total.value = 0
      failure.value = errorMessage(error)
    }
  } finally { if (sequence === listSequence) loading.value = false }
}
function search() {
  for (const id of [filters.memberId.trim(), filters.operatorId.trim()]) {
    if (id && (!/^[1-9]\d{0,18}$/.test(id) || BigInt(id) > 9223372036854775807n)) {
      ElMessage.warning('请输入有效的会员或操作人 ID')
      return
    }
  }
  query.value = {
    page: 1, size: query.value.size,
    memberId: filters.memberId.trim() || undefined,
    operatorId: filters.operatorId.trim() || undefined,
    action: filters.action || undefined, operatorType: filters.operatorType || undefined,
    outcome: filters.outcome || undefined, start: dates.value?.[0], end: dates.value?.[1],
  }
  void load()
}
function reset() {
  Object.assign(filters, { memberId: '', operatorId: '', action: '', operatorType: '', outcome: '' })
  dates.value = null
  search()
}
async function loadDetail() {
  if (!drawer.value || !detailId.value) return
  const sequence = ++detailSequence
  detailLoading.value = true
  detailFailure.value = ''
  detail.value = null
  try {
    const result = await memberOperationsApi.detail(detailId.value)
    if (sequence === detailSequence) detail.value = result
  } catch (error) {
    if (sequence === detailSequence) detailFailure.value = errorMessage(error)
  } finally { if (sequence === detailSequence) detailLoading.value = false }
}
function showDetail(row: MemberOperation) {
  detailId.value = row.id
  drawer.value = true
  void loadDetail()
}
watch(drawer, (open) => {
  if (!open) { ++detailSequence; detail.value = null; detailId.value = ''; detailLoading.value = false }
}, { flush: 'sync' })
function readRoute() {
  filters.memberId = typeof route.query.memberId === 'string' ? route.query.memberId : ''
  search()
}
watch(() => route.query.memberId, readRoute)
onMounted(readRoute)
onBeforeUnmount(() => { ++listSequence; ++detailSequence })
</script>

<template>
  <div class="page-heading"><div><h1>操作记录</h1><p>追溯会员资料、账号状态与会话操作</p></div></div>
  <section class="panel operations-panel">
    <el-form inline class="filters" @submit.prevent="search">
      <el-form-item label="会员 ID"><el-input v-model="filters.memberId" maxlength="19" clearable placeholder="精确查询" @keyup.enter="search" /></el-form-item>
      <el-form-item label="操作人 ID"><el-input v-model="filters.operatorId" maxlength="19" clearable placeholder="精确查询" @keyup.enter="search" /></el-form-item>
      <el-form-item label="操作类型"><el-select v-model="filters.action" clearable placeholder="全部" style="width: 170px"><el-option v-for="(label, key) in actions" :key="key" :label="label" :value="key" /></el-select></el-form-item>
      <el-form-item label="操作身份"><el-select v-model="filters.operatorType" clearable placeholder="全部" style="width: 140px"><el-option v-for="(label, key) in actorTypes" :key="key" :label="label" :value="key" /></el-select></el-form-item>
      <el-form-item label="结果"><el-select v-model="filters.outcome" clearable placeholder="全部" style="width: 175px"><el-option v-for="(label, key) in outcomes" :key="key" :label="label" :value="key" /></el-select></el-form-item>
      <el-form-item label="操作时间"><el-date-picker v-model="dates" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
    </el-form>
    <el-alert title="仅记录功能上线后的操作；“处理中 / 待核实”记录可能需要刷新资料核实实际结果。" type="info" :closable="false" show-icon />
    <el-alert v-if="failure" :title="failure" type="error" :closable="false"><el-button link @click="load">重新加载</el-button></el-alert>
    <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无符合条件的操作记录" style="margin-top: 16px">
      <el-table-column label="操作时间" min-width="190"><template #default="{ row }">{{ time(row.createTime) }}</template></el-table-column>
      <el-table-column prop="memberId" label="会员 ID" min-width="190" />
      <el-table-column label="操作人" min-width="200"><template #default="{ row }"><div>{{ row.operatorName }}</div><small class="muted">{{ row.operatorId }}</small></template></el-table-column>
      <el-table-column label="身份 / 来源" min-width="150"><template #default="{ row }">{{ actorTypes[row.operatorType] || row.operatorType }} / {{ sources[row.source] || row.source }}</template></el-table-column>
      <el-table-column label="操作" min-width="145"><template #default="{ row }">{{ actions[row.action] || row.action }}</template></el-table-column>
      <el-table-column label="结果" min-width="170"><template #default="{ row }"><el-tag :type="tone(row.outcome)">{{ outcomes[row.outcome] || row.outcome }}</el-tag></template></el-table-column>
      <el-table-column prop="message" label="结果说明" min-width="240" show-overflow-tooltip />
      <el-table-column label="操作" fixed="right" width="80"><template #default="{ row }"><el-button link type="primary" @click="showDetail(row)">详情</el-button></template></el-table-column>
    </el-table>
    <div class="pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="query.page = 1; load()" /></div>
  </section>
  <el-drawer v-model="drawer" title="操作记录详情" size="min(600px, 100vw)">
    <el-button :loading="detailLoading" @click="loadDetail">刷新记录</el-button>
    <div v-loading="detailLoading" class="detail">
      <el-alert v-if="detailFailure" :title="detailFailure" type="error" :closable="false"><el-button link @click="loadDetail">重新加载</el-button></el-alert>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="流水 ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="会员 ID">{{ detail.memberId }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operatorName }}（{{ detail.operatorId }}）</el-descriptions-item>
        <el-descriptions-item label="操作身份">{{ actorTypes[detail.operatorType] || detail.operatorType }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ sources[detail.source] || detail.source }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ actions[detail.action] || detail.action }}</el-descriptions-item>
        <el-descriptions-item label="结果"><el-tag :type="tone(detail.outcome)">{{ outcomes[detail.outcome] || detail.outcome }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="变更前">{{ detail.beforeValue ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="变更后">{{ detail.afterValue ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="结果说明">{{ detail.message }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ time(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="记录更新时间">{{ time(detail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </el-drawer>
</template>

<style scoped>
.operations-panel { padding: 24px; min-width: 0; }
.pagination { display: flex; justify-content: flex-end; margin-top: 24px; overflow: auto; }
.detail { min-height: 220px; margin-top: 20px; overflow-wrap: anywhere; }
@media (max-width: 700px) {
  .operations-panel { padding: 16px; }
  .filters :deep(.el-form-item) { display: flex; margin-right: 0; }
  .filters :deep(.el-form-item__content) { min-width: 0; }
  .filters :deep(.el-date-editor) { width: 100%; min-width: 0; }
  .pagination { justify-content: flex-start; }
}
</style>
