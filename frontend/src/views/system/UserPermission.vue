<template>
  <div>
    <h3>敏感权限直授</h3>
    <p class="hint">用于将改价等敏感权限下放给指定账号（如前台）。</p>
    <el-select v-model="selectedUserId" placeholder="选择用户" style="width: 240px; margin: 16px 0" @change="loadPerms">
      <el-option v-for="u in users" :key="u.id" :label="u.username" :value="u.id" />
    </el-select>
    <el-checkbox-group v-if="selectedUserId" v-model="checkedIds">
      <div v-for="p in items" :key="p.id" class="perm-row">
        <el-checkbox :label="p.id">{{ p.name }}（{{ p.code }}）</el-checkbox>
      </div>
    </el-checkbox-group>
    <el-button v-if="selectedUserId" type="primary" style="margin-top: 16px" :loading="saving" @click="save">
      保存
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listUsersApi, listUserPermissionsApi, saveUserPermissionsApi, type UserVO, type PermissionItem } from '@/api/user'

const users = ref<UserVO[]>([])
const selectedUserId = ref<number | null>(null)
const items = ref<PermissionItem[]>([])
const checkedIds = ref<number[]>([])
const saving = ref(false)

async function loadUsers() {
  const res = await listUsersApi()
  users.value = res.data.data
}

async function loadPerms() {
  if (!selectedUserId.value) return
  const res = await listUserPermissionsApi(selectedUserId.value)
  items.value = res.data.data
  checkedIds.value = items.value.filter((i) => i.assigned).map((i) => i.id)
}

async function save() {
  if (!selectedUserId.value) return
  saving.value = true
  try {
    await saveUserPermissionsApi(selectedUserId.value, checkedIds.value)
    ElMessage.success('已保存')
    await loadPerms()
  } finally {
    saving.value = false
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.hint {
  color: #909399;
  font-size: 13px;
}
.perm-row {
  margin: 8px 0;
}
</style>
