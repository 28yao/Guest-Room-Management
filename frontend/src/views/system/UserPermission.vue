<template>
  <div>
    <div class="toolbar">
      <h3>敏感权限直授</h3>
    </div>
    <p class="hint">
      用于将改价（<code>billing:price:adjust</code>）、房态图（<code>room:board:view</code>）、在住管理（<code>stay:in_house:view</code>）等权限下放给指定账号。默认无直授，仅继承角色权限。
    </p>
    <el-select v-model="selectedUserId" placeholder="选择用户" style="width: 240px; margin: 16px 0" @change="loadPerms">
      <el-option v-for="u in users" :key="u.id" :label="u.username" :value="u.id" />
    </el-select>
    <el-checkbox-group v-if="selectedUserId" v-model="checkedIds">
      <div v-for="p in items" :key="p.id" class="perm-row">
        <el-checkbox :value="p.id">{{ p.name }}（{{ p.code }}）</el-checkbox>
      </div>
    </el-checkbox-group>
    <div v-if="selectedUserId" class="actions">
      <el-button @click="restoreDefault" :loading="restoring">恢复默认</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listUsersApi,
  listUserPermissionsApi,
  saveUserPermissionsApi,
  restoreUserPermissionsDefaultApi,
  type UserVO,
  type PermissionItem
} from '@/api/user'

const users = ref<UserVO[]>([])
const selectedUserId = ref<number | null>(null)
const items = ref<PermissionItem[]>([])
const checkedIds = ref<number[]>([])
const saving = ref(false)
const restoring = ref(false)

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

function applyItems(data: PermissionItem[]) {
  items.value = data
  checkedIds.value = data.filter((i) => i.assigned).map((i) => i.id)
}

async function save() {
  if (!selectedUserId.value) return
  saving.value = true
  try {
    await saveUserPermissionsApi(
      selectedUserId.value,
      checkedIds.value.map((id) => Number(id))
    )
    ElMessage.success('已保存，该用户刷新页面后即可使用新权限')
    await loadPerms()
  } finally {
    saving.value = false
  }
}

async function restoreDefault() {
  if (!selectedUserId.value) return
  const user = users.value.find((u) => u.id === selectedUserId.value)
  await ElMessageBox.confirm(
    `将清空用户「${user?.username || ''}」的全部直授权限，仅保留角色权限，是否继续？`,
    '恢复默认',
    { type: 'warning' }
  )
  restoring.value = true
  try {
    const res = await restoreUserPermissionsDefaultApi(selectedUserId.value)
    applyItems(res.data.data)
    ElMessage.success('已恢复为默认（无直授）')
  } finally {
    restoring.value = false
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.hint {
  margin: 0 0 12px;
}
</style>
