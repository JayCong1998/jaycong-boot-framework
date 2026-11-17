<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
</script>
<template>
  <div class="page-heading">
    <div>
      <h1>系统设置</h1>
      <p>当前登录账号与系统信息</p>
    </div>
  </div>
  <section class="panel account-panel">
    <div class="panel-heading">
      <h2>账号信息</h2>
      <el-tag type="success">已认证</el-tag>
    </div>
    <el-descriptions :column="1" border
      ><el-descriptions-item label="用户 ID">{{
        auth.user?.id
      }}</el-descriptions-item
      ><el-descriptions-item label="昵称">{{
        auth.user?.nickname
      }}</el-descriptions-item
      ><el-descriptions-item label="手机号">{{
        auth.user?.mobile || '未绑定'
      }}</el-descriptions-item
      ><el-descriptions-item label="邮箱">{{
        auth.user?.email || '未绑定'
      }}</el-descriptions-item
      ><el-descriptions-item label="角色"
        >管理员（{{ auth.user?.role }}）</el-descriptions-item
      ></el-descriptions
    >
    <p class="muted account-note">
      修改密码与个人资料功能尚未开放。如需变更账号信息，请联系系统维护人员。
    </p>
  </section>
</template>
