<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">GRMS</div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/home">工作台</el-menu-item>
        <el-sub-menu v-if="showSystemMenu" index="system">
          <template #title>系统管理</template>
          <el-menu-item v-if="auth.hasPermission('system:user:manage')" index="/system/users">
            用户管理
          </el-menu-item>
          <el-menu-item v-if="auth.hasPermission('system:role:manage')" index="/system/roles">
            角色权限
          </el-menu-item>
          <el-menu-item v-if="auth.hasPermission('system:permission:grant')" index="/system/user-permissions">
            敏感权限直授
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>{{ auth.username }}</span>
        <el-button type="primary" link @click="onLogout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)

const showSystemMenu = computed(() =>
  auth.hasAnyPermission(['system:user:manage', 'system:role:manage', 'system:permission:grant'])
)

async function onLogout() {
  await auth.logout()
  router.replace('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.aside {
  background: #304156;
}
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-weight: bold;
  font-size: 18px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}
</style>
