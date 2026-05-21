<template>
  <el-card>
    <h3>工作台</h3>
    <p>欢迎，{{ auth.username }}。客房、预订等模块将在后续任务中接入。</p>
    <p>当前权限数：{{ auth.permissions.length }}</p>
  </el-card>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>
