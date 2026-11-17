<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { membersApi, type Member, type MemberQuery } from '@/services/members'
import { errorMessage } from '@/services/http'

const filters = reactive({ keyword: '', id: '', status: '' as '' | number })
const dates = ref<[string, string] | null>(null)
const query = ref<MemberQuery>({ page: 1, size: 20 })
const rows = ref<Member[]>([])
const total = ref(0)
const loading = ref(false)
const failure = ref('')
const selected = ref<Member | null>(null)
const drawer = ref(false)
const detailLoading = ref(false)
const detailId = ref('')
const detailFailure = ref('')
const editing = ref<Member | null>(null)
const editOpen = ref(false)
const editId = ref('')
const editLoading = ref(false)
const editFailure = ref('')
const nickname = ref('')
const saving = ref(false)
const acting = ref(false)
let listSequence = 0
let detailSequence = 0
let editSequence = 0
const time = (value: string | null) => value ? value.replace('T', ' ') : '—'

async function load() {
  const sequence = ++listSequence
  loading.value = true
  failure.value = ''
  try {
    const result = await membersApi.list({ ...query.value })
    if (sequence !== listSequence) return
    rows.value = result.rows
    total.value = result.total
    if (!result.rows.length && query.value.page > 1 && result.total > 0) {
      query.value.page = Math.ceil(result.total / query.value.size)
      await load()
    }
  } catch (error) {
    if (sequence === listSequence) {
      rows.value = []
      total.value = 0
      failure.value = errorMessage(error)
    }
  } finally {
    if (sequence === listSequence) loading.value = false
  }
}
function search() {
  if (filters.id && !/^[1-9]\d{0,18}$/.test(filters.id.trim())) {
    ElMessage.warning('请输入有效的会员 ID')
    return
  }
  query.value = {
    page: 1, size: query.value.size,
    keyword: filters.keyword.trim() || undefined,
    id: filters.id.trim() || undefined,
    status: filters.status === '' ? undefined : filters.status,
    createTimeStart: dates.value?.[0], createTimeEnd: dates.value?.[1],
  }
  void load()
}
function reset() {
  Object.assign(filters, { keyword: '', id: '', status: '' })
  dates.value = null
  search()
}
async function showDetail(member: Member) {
  detailId.value = member.id
  drawer.value = true
  await loadDetail()
}
async function loadDetail() {
  if (!drawer.value || !detailId.value) return
  const sequence = ++detailSequence
  selected.value = null
  detailFailure.value = ''
  detailLoading.value = true
  try {
    const result = await membersApi.detail(detailId.value)
    if (sequence === detailSequence) selected.value = result
  } catch (error) {
    if (sequence === detailSequence) {
      detailFailure.value = errorMessage(error)
    }
  } finally {
    if (sequence === detailSequence) detailLoading.value = false
  }
}
async function edit(member: Member) {
  if (saving.value || acting.value) return
  editId.value = member.id
  editOpen.value = true
  await loadEdit()
}
async function loadEdit() {
  if (!editOpen.value || !editId.value) return
  const sequence = ++editSequence
  editing.value = null
  nickname.value = ''
  editFailure.value = ''
  editLoading.value = true
  try {
    const result = await membersApi.detail(editId.value)
    if (sequence !== editSequence) return
    editing.value = result
    nickname.value = result.nickname
  } catch (error) {
    if (sequence === editSequence) editFailure.value = errorMessage(error)
  } finally {
    if (sequence === editSequence) editLoading.value = false
  }
}
async function save() {
  if (!editing.value || editLoading.value || saving.value || acting.value) return
  if (!nickname.value.trim() || nickname.value.trim().length > 64) {
    ElMessage.warning('昵称不能为空，且最多 64 个字符')
    return
  }
  saving.value = true
  try {
    const result = await membersApi.update(editing.value, nickname.value.trim())
    if (selected.value?.id === result.id) selected.value = result
    editOpen.value = false
    ElMessage.success('会员资料已更新')
    await Promise.all([load(), loadDetail()])
  } catch (error) {
    ElMessage.error(errorMessage(error))
    // 并发冲突后关闭旧表单，重新读取版本，避免重复提交旧快照。
    editOpen.value = false
    await load()
    await loadDetail()
  } finally { saving.value = false }
}
async function changeStatus(member: Member) {
  if (acting.value || saving.value) return
  const status = member.status === 1 ? 0 : 1
  acting.value = true
  try {
    try {
      await ElMessageBox.confirm(
        status === 0 ? `禁用“${member.nickname}”后将撤销其全部登录会话。是否继续？`
          : `确认启用“${member.nickname}”？启用后会员需要重新登录。`,
        status === 0 ? '禁用会员' : '启用会员',
        { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
      )
    } catch { return }
    try {
      const result = await membersApi.status(member, status)
      if (selected.value?.id === result.id) selected.value = result
      ElMessage.success(status === 0 ? '会员已禁用，登录会话已撤销' : '会员已启用')
    } catch (error) {
      ElMessage.error(errorMessage(error))
    }
    await Promise.all([load(), loadDetail()])
  } finally { acting.value = false }
}
async function revokeAgain(member: Member) {
  if (acting.value || saving.value) return
  acting.value = true
  try {
    const result = await membersApi.status(member, 0)
    if (selected.value?.id === result.id) selected.value = result
    ElMessage.success('禁用会员的登录会话已撤销')
  } catch (error) { ElMessage.error(errorMessage(error)) }
  finally {
    await Promise.all([load(), loadDetail()])
    acting.value = false
  }
}
// 关闭和卸载立即使旧读取失效，防止迟到响应回填已关闭或已切换的表单。
watch(drawer, (open) => {
  if (open) return
  ++detailSequence
  selected.value = null
  detailId.value = ''
  detailFailure.value = ''
  detailLoading.value = false
}, { flush: 'sync' })
watch(editOpen, (open) => {
  if (open) return
  ++editSequence
  editing.value = null
  editId.value = ''
  editFailure.value = ''
  editLoading.value = false
}, { flush: 'sync' })
onBeforeUnmount(() => {
  ++listSequence
  ++detailSequence
  ++editSequence
})
onMounted(load)
</script>

<template>
  <div class="page-heading"><div><h1>会员列表</h1><p>查询会员档案，维护资料与账号状态</p></div></div>
  <section class="panel members-panel">
    <el-form inline class="member-filters" @submit.prevent="search">
      <el-form-item label="关键词"><el-input v-model="filters.keyword" placeholder="昵称 / 手机号 / 邮箱" maxlength="254" clearable @keyup.enter="search" /></el-form-item>
      <el-form-item label="会员 ID"><el-input v-model="filters.id" placeholder="精确查询" maxlength="19" clearable @keyup.enter="search" /></el-form-item>
      <el-form-item label="状态"><el-select v-model="filters.status" style="width: 130px"><el-option label="全部" value="" /><el-option label="正常" :value="1" /><el-option label="禁用" :value="0" /></el-select></el-form-item>
      <el-form-item label="注册时间"><el-date-picker v-model="dates" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
    </el-form>
    <el-alert v-if="failure" :title="failure" type="error" show-icon :closable="false"><el-button link @click="load">重新加载</el-button></el-alert>
    <div class="member-table" v-loading="loading">
      <el-table :data="rows" row-key="id" empty-text="暂无符合条件的会员">
        <el-table-column prop="id" label="会员 ID" min-width="190" />
        <el-table-column prop="nickname" label="昵称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="mobile" label="手机号" min-width="140"><template #default="{ row }">{{ row.mobile || '—' }}</template></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="170"><template #default="{ row }">{{ row.email || '—' }}</template></el-table-column>
        <el-table-column label="状态" width="85"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="注册时间" min-width="185"><template #default="{ row }">{{ time(row.createTime) }}</template></el-table-column>
        <el-table-column label="最近登录" min-width="185"><template #default="{ row }">{{ time(row.lastLoginTime) }}</template></el-table-column>
        <el-table-column label="操作" width="185" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="showDetail(row)">详情</el-button><el-button link type="primary" :disabled="saving || acting" @click="edit(row)">编辑</el-button><el-button link :type="row.status === 1 ? 'danger' : 'success'" :disabled="acting || saving" @click="changeStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button></template></el-table-column>
      </el-table>
    </div>
    <div class="member-pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next" @size-change="query.page = 1; load()" @current-change="load" /></div>
  </section>
  <el-drawer v-model="drawer" title="会员档案" size="min(560px, 100vw)">
    <el-button :loading="detailLoading" :disabled="saving || acting" @click="loadDetail">刷新资料</el-button>
    <div v-loading="detailLoading" style="min-height: 240px">
      <el-alert v-if="detailFailure" :title="detailFailure" type="error" show-icon :closable="false"><el-button link @click="loadDetail">重新加载</el-button></el-alert>
      <template v-if="selected">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="会员 ID">{{ selected.id }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ selected.nickname }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ selected.mobile || '—' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ selected.email || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ selected.status === 1 ? '正常' : '禁用' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ time(selected.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ time(selected.updateTime) }}</el-descriptions-item>
          <el-descriptions-item label="最近登录">{{ time(selected.lastLoginTime) }}</el-descriptions-item>
        </el-descriptions>
        <p class="member-note muted">联系方式已脱敏。登录手机号、邮箱和密码不在此页面修改。</p>
        <el-button @click="$router.push({ path: '/members/operations', query: { memberId: selected.id } })">操作记录</el-button>
        <el-button type="primary" :disabled="saving || acting" @click="edit(selected)">编辑资料</el-button>
        <el-button :disabled="acting || saving" @click="changeStatus(selected)">{{ selected.status === 1 ? '禁用会员' : '启用会员' }}</el-button>
        <el-button v-if="selected.status === 0" :disabled="acting || saving" @click="revokeAgain(selected)">重试撤销会话</el-button>
      </template>
    </div>
  </el-drawer>
  <el-dialog v-model="editOpen" title="编辑会员资料" width="min(460px, 94vw)" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving">
    <div v-loading="editLoading" style="min-height: 100px">
      <el-alert v-if="editFailure" :title="editFailure" type="error" show-icon :closable="false"><el-button link @click="loadEdit">重新加载</el-button></el-alert>
      <el-form v-if="editing" label-position="top" @submit.prevent="save"><el-form-item label="昵称" required><el-input v-model="nickname" maxlength="64" show-word-limit :disabled="saving" /></el-form-item></el-form>
    </div>
    <template #footer><el-button :disabled="saving" @click="editOpen = false">取消</el-button><el-button type="primary" :disabled="editLoading || !editing" :loading="saving" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>
.members-panel { padding: 24px; min-width: 0; }
.member-table { width: 100%; min-height: 240px; overflow: auto; }
.member-pagination { display: flex; justify-content: flex-end; overflow: auto; margin-top: 24px; }
.member-note { margin: 20px 0; line-height: 1.7; }
@media (max-width: 700px) {
  .members-panel { padding: 16px; }
  .member-filters :deep(.el-form-item) { display: flex; margin-right: 0; }
  .member-filters :deep(.el-form-item__content) { min-width: 0; }
  .member-filters :deep(.el-date-editor) { width: 100%; min-width: 0; }
  .member-pagination { justify-content: flex-start; }
}
</style>
