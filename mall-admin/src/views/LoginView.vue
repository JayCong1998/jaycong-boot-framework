<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Lock,
  User,
  Right,
  TrendCharts,
  ShoppingBag,
  Check,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { errorMessage } from '@/services/http'
import { safeRedirect } from '@/router/navigation'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const form = reactive({ account: '', password: '' })
const submitting = ref(false)
const error = ref('')
const needsLogout = computed(() =>
  Boolean(auth.user && auth.user.role !== 'admin'),
)
const notice = computed(
  () =>
    ({
      expired: '登录已失效，请重新登录。',
      unavailable: '暂时无法验证会话，请确认服务可用后重试登录。',
      forbidden: '当前账号没有后台管理权限，请使用管理员账号登录。',
      'forbidden-logout':
        '当前账号没有后台管理权限，且会话退出失败，请重试退出当前会话。',
    })[String(route.query.reason)] || '',
)
const rules: FormRules = {
  account: [
    {
      required: true,
      whitespace: true,
      message: '请输入手机号或邮箱',
      trigger: 'blur',
    },
    { max: 254, message: '账号长度不能超过 254 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度为 8–64 个字符', trigger: 'blur' },
  ],
}

async function submit() {
  if (
    needsLogout.value ||
    submitting.value ||
    !(await formRef.value?.validate().catch(() => false))
  )
    return
  submitting.value = true
  error.value = ''
  try {
    await auth.login(form)
    form.password = ''
    await router.replace(safeRedirect(route.query.redirect))
    ElMessage.success('登录成功，欢迎回来')
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    submitting.value = false
  }
}
async function retryLogout() {
  try {
    await auth.logout()
    error.value = '当前会话已退出，请使用管理员账号登录。'
    form.password = ''
  } catch (cause) {
    error.value = `退出当前会话失败：${errorMessage(cause)}，请重试`
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-story" aria-label="电商运营管理平台">
      <a class="brand" href="/login"
        ><span class="brand-mark">M</span
        ><span>mall-admin<small>电商运营管理平台</small></span></a
      >
      <div class="story-copy">
        <span class="eyebrow">YOUR BUSINESS, CONNECTED</span>
        <h1>让每一笔生意<br />井然有序<span>。</span></h1>
        <p>
          从商品到订单，从会员到增长。<br />一个工作台，掌握经营的每一个重要时刻。
        </p>
        <div class="login-illustration" aria-hidden="true">
          <div class="illustration-title">
            <span class="illustration-icon"
              ><el-icon><TrendCharts /></el-icon></span
            ><span>经营概览<small>让增长清晰可见</small></span
            ><span class="live-dot"></span>
          </div>
          <svg viewBox="0 0 400 110" class="illustration-chart">
            <defs>
              <linearGradient id="login-fill" x1="0" y1="0" x2="0" y2="1">
                <stop stop-color="#648dff" stop-opacity=".35" />
                <stop offset="1" stop-color="#648dff" stop-opacity="0" />
              </linearGradient>
            </defs>
            <path
              d="M0 92 L55 72 L110 82 L165 45 L220 58 L275 25 L330 40 L400 4 V110 H0Z"
              fill="url(#login-fill)"
            />
            <path
              d="M0 92 L55 72 L110 82 L165 45 L220 58 L275 25 L330 40 L400 4"
              fill="none"
              stroke="#82a3ff"
              stroke-width="3"
            />
          </svg>
          <div class="illustration-bottom">
            <span
              ><el-icon><ShoppingBag /></el-icon> 订单有序流转</span
            ><span
              ><el-icon><Check /></el-icon> 运营尽在掌握</span
            >
          </div>
        </div>
      </div>
      <span class="story-footer">MALL ADMIN / 让经营更简单</span>
    </section>
    <section class="login-form-side">
      <div class="login-form-wrap">
        <span class="login-badge">管理控制台</span>
        <h2>欢迎回来</h2>
        <p class="muted login-subtitle">登录你的管理员账号，开始今天的工作。</p>
        <el-alert
          v-if="error || notice"
          :title="error || notice"
          :type="error ? 'error' : 'warning'"
          show-icon
          :closable="false"
          class="login-alert"
          role="alert"
        />
        <el-button
          v-if="needsLogout"
          class="logout-retry"
          type="warning"
          :loading="auth.loggingOut"
          @click="retryLogout"
          >退出当前会话</el-button
        >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="submit"
        >
          <el-form-item label="登录账号" prop="account">
            <el-input
              v-model="form.account"
              :prefix-icon="User"
              placeholder="请输入手机号或邮箱"
              autocomplete="username"
              :maxlength="254"
              :disabled="submitting || needsLogout"
            />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              :maxlength="64"
              :disabled="submitting || needsLogout"
            />
          </el-form-item>
          <div class="login-security">
            <el-icon><Lock /></el-icon> 仅限授权管理员访问
          </div>
          <el-button
            type="primary"
            native-type="submit"
            class="login-submit"
            :loading="submitting"
            :disabled="needsLogout"
            >{{ submitting ? '正在登录' : '登 录'
            }}<el-icon v-if="!submitting"><Right /></el-icon
          ></el-button>
        </el-form>
        <p class="login-help">账号无法登录？请联系系统管理员</p>
      </div>
      <footer>
        © {{ new Date().getFullYear() }} mall-admin · 电商运营管理平台
      </footer>
    </section>
  </main>
</template>
