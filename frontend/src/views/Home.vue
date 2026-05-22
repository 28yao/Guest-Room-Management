<template>
  <div class="home-page">
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-head">
        <h2>欢迎回来，{{ auth.username }}</h2>
        <p>从前台日常操作到经营数据，可在左侧菜单快速进入各模块。</p>
      </div>
      <div class="grms-stat-grid">
        <div class="grms-stat-item">
          <div class="label">已授权功能</div>
          <div class="value">{{ auth.permissions.length }}</div>
        </div>
        <div class="grms-stat-item">
          <div class="label">建议入口</div>
          <div class="value value-sm">房态图</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>

<style scoped>
.home-page {
  max-width: 960px;
}

.welcome-card {
  border-radius: var(--grms-radius);
}

.welcome-head h2 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--grms-text);
}

.welcome-head p {
  margin: 0;
  color: var(--grms-text-secondary);
  font-size: 14px;
}

.value-sm {
  font-size: 18px !important;
}
</style>
