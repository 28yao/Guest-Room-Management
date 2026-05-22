<template>
  <el-container class="layout">
    <el-aside width="228px" class="aside">
      <div class="logo">
        <span class="logo-mark">GR</span>
        <span class="logo-text">客房管理</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          class="side-menu"
          :default-active="activeMenu"
          background-color="transparent"
          text-color="#cbd5e1"
          active-text-color="#ffffff"
          router
        >
          <el-menu-item v-if="auth.hasPermission('room:board:view')" index="/rooms/board">
            <span>房态图</span>
          </el-menu-item>
          <el-menu-item v-if="auth.hasPermission('reservation:manage')" index="/reservations">
            <span>预订管理</span>
          </el-menu-item>
          <el-menu-item
            v-if="auth.hasAnyPermission(['shift:open', 'shift:close'])"
            index="/shift"
          >
            <span>开班/结班</span>
          </el-menu-item>
          <el-menu-item v-if="auth.hasPermission('stay:checkin')" index="/check-in">
            <span>办理入住</span>
          </el-menu-item>
          <el-menu-item v-if="auth.hasPermission('stay:in_house:view')" index="/in-house">
            <span>在住管理</span>
          </el-menu-item>
          <el-menu-item v-if="auth.hasPermission('hk:view')" index="/housekeeping">
            <span>保洁任务</span>
          </el-menu-item>
          <el-menu-item v-if="auth.hasPermission('stat:view')" index="/stats">
            <span>经营统计</span>
          </el-menu-item>
          <el-menu-item v-if="auth.hasPermission('room:manage')" index="/rooms">
            <span>客房管理</span>
          </el-menu-item>
          <el-menu-item v-if="auth.hasPermission('room:type:manage')" index="/room-types">
            <span>房型管理</span>
          </el-menu-item>
          <el-menu-item index="/home">
            <span>工作台</span>
          </el-menu-item>
          <el-sub-menu v-if="showSystemMenu" index="system">
            <template #title>系统管理</template>
            <el-menu-item v-if="auth.hasPermission('system:user:manage')" index="/system/users">
              用户管理
            </el-menu-item>
            <el-menu-item v-if="auth.hasPermission('system:role:manage')" index="/system/roles">
              角色权限
            </el-menu-item>
            <el-menu-item
              v-if="auth.hasPermission('system:permission:grant')"
              index="/system/user-permissions"
            >
              敏感权限直授
            </el-menu-item>
            <el-menu-item v-if="auth.hasPermission('audit:view')" index="/system/audit">
              操作审计
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>
    <el-container class="main-wrap">
      <el-header class="header" height="56px">
        <div class="header-title">酒店客房管理系统</div>
        <div class="header-user">
          <span class="user-badge">{{ auth.username }}</span>
          <el-button class="logout-btn" type="primary" link @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="grms-main">
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
  auth.hasAnyPermission([
    'system:user:manage',
    'system:role:manage',
    'system:permission:grant',
    'audit:view'
  ])
)

async function onLogout() {
  await auth.logout()
  router.replace('/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: var(--grms-bg);
}

.aside {
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--grms-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: -0.05em;
}

.logo-text {
  color: #f8fafc;
  font-weight: 600;
  font-size: 15px;
  letter-spacing: 0.02em;
}

.menu-scroll {
  flex: 1;
}

.side-menu {
  border-right: none;
  padding: 8px 0 16px;
}

.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  margin: 2px 10px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.06) !important;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, var(--grms-sidebar-active) 0%, #2563eb 100%) !important;
  color: #fff !important;
  font-weight: 500;
}

.main-wrap {
  flex-direction: column;
  min-width: 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--grms-surface);
  border-bottom: 1px solid var(--grms-border);
  box-shadow: var(--grms-shadow-sm);
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--grms-text-secondary);
}

.header-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-badge {
  font-size: 13px;
  color: var(--grms-text);
  padding: 4px 12px;
  background: var(--grms-primary-soft);
  border-radius: 20px;
  border: 1px solid #bfdbfe;
}

.logout-btn {
  cursor: pointer;
  font-weight: 500;
}

.logout-btn:hover {
  color: var(--grms-primary-hover) !important;
}
</style>
