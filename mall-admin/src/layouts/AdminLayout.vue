<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Fold,
  Expand,
  ArrowDown,
  SwitchButton,
  User,
  Close,
  Menu,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { navigation } from '@/router/navigation'
import { useAuthStore } from '@/stores/auth'
import { errorMessage } from '@/services/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const mobileOpen = ref(false)
const nickname = computed(() => auth.user?.nickname || '管理员')

async function onCommand(command: string) {
  if (command === 'profile') {
    await router.push('/settings')
    return
  }
  if (command !== 'logout' || auth.loggingOut) return
  try {
    await ElMessageBox.confirm(
      '退出后需要重新登录才能访问管理后台。',
      '确认退出登录？',
      {
        confirmButtonText: '退出登录',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }
  try {
    await auth.logout()
    await router.replace('/login')
    ElMessage.success('已退出登录')
  } catch (error) {
    ElMessage.error(`${errorMessage(error)}；退出未完成，请重试`)
  }
}
</script>

<template>
  <div
    class="admin-shell"
    :class="{ 'sidebar-collapsed': collapsed, 'mobile-open': mobileOpen }"
  >
    <div
      v-if="mobileOpen"
      class="sidebar-backdrop"
      @click="mobileOpen = false"
    ></div>
    <aside class="admin-sidebar">
      <router-link class="brand" to="/dashboard" aria-label="mall-admin 工作台"
        ><span class="brand-mark">M</span
        ><span class="brand-copy"
          >mall-admin<small>电商运营管理平台</small></span
        ></router-link
      >
      <button
        class="icon-button mobile-close"
        aria-label="关闭导航"
        @click="mobileOpen = false"
      >
        <el-icon><Close /></el-icon>
      </button>
      <div class="nav-caption">工作空间</div>
      <el-menu
        :default-active="route.path"
        :default-openeds="['group-/members']"
        :collapse="collapsed && !mobileOpen"
        :collapse-transition="false"
        router
        class="sidebar-menu"
        @select="mobileOpen = false"
      >
        <template v-for="item in navigation" :key="item.path">
          <el-sub-menu v-if="item.children" :index="'group-' + item.path">
            <template #title><el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span></template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">{{ child.title }}</el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon><template #title><span>{{ item.title }}</span></template>
          </el-menu-item>
        </template>
      </el-menu>
      <div class="sidebar-bottom">
        <div class="sidebar-tip">
          <strong>运营小贴士</strong>
          <p>及时处理订单和库存预警<br />让每一笔生意井然有序</p>
        </div>
        <span>让经营更简单 · mall-admin</span>
      </div>
    </aside>
    <div class="admin-main">
      <header class="admin-header">
        <button
          class="icon-button desktop-toggle"
          :aria-label="collapsed ? '展开导航' : '折叠导航'"
          @click="collapsed = !collapsed"
        >
          <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
        </button>
        <button
          class="icon-button mobile-toggle"
          aria-label="打开导航"
          @click="mobileOpen = true"
        >
          <el-icon><Menu /></el-icon>
        </button>
        <el-breadcrumb separator="/"
          ><el-breadcrumb-item :to="{ path: '/dashboard' }"
            >首页</el-breadcrumb-item
          ><el-breadcrumb-item>{{
            route.meta.title
          }}</el-breadcrumb-item></el-breadcrumb
        >
        <div class="header-right">
          <span class="environment-label">运营管理后台</span>
          <el-dropdown trigger="click" @command="onCommand">
            <button class="profile-trigger" :disabled="auth.loggingOut">
              <el-avatar :size="36">{{ nickname.slice(0, 1) }}</el-avatar
              ><span class="profile-text"
                ><strong>{{ nickname }}</strong
                ><small>{{
                  auth.loggingOut ? '正在退出…' : '管理员'
                }}</small></span
              ><el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown
              ><el-dropdown-menu
                ><el-dropdown-item command="profile" :icon="User"
                  >账号信息</el-dropdown-item
                ><el-dropdown-item
                  command="logout"
                  :icon="SwitchButton"
                  divided
                  :disabled="auth.loggingOut"
                  >退出登录</el-dropdown-item
                ></el-dropdown-menu
              ></template
            >
          </el-dropdown>
        </div>
      </header>
      <main class="page-content"><router-view /></main>
      <footer class="admin-footer">
        <span>MALL ADMIN / 运营管理平台</span
        ><span>© {{ new Date().getFullYear() }} mall-admin</span>
      </footer>
    </div>
  </div>
</template>
