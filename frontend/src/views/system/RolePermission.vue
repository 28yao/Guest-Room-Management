<template>
  <div>
    <h3>角色权限配置</h3>
    <p class="hint">
      保存后对该角色下<strong>已分配该角色</strong>的用户生效；请同时在「用户管理」中为用户勾选对应角色。
    </p>
    <el-select v-model="selectedRoleId" placeholder="选择角色" style="width: 240px; margin: 16px 0" @change="loadPerms">
      <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
    </el-select>
    <el-checkbox-group v-if="selectedRoleId" v-model="checkedIds">
      <div v-for="p in items" :key="p.id" class="perm-row">
        <el-checkbox :value="p.id">{{ p.name }}（{{ p.code }}）</el-checkbox>
      </div>
    </el-checkbox-group>
    <div v-if="selectedRoleId" class="actions">
      <el-button @click="restoreDefault" :loading="restoring">恢复默认</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRolesApi,
  listRolePermissionsApi,
  saveRolePermissionsApi,
  restoreRolePermissionsDefaultApi,
  type RoleVO
} from '@/api/role'
import type { PermissionItem } from '@/api/user'

const roles = ref<RoleVO[]>([])
const selectedRoleId = ref<number | null>(null)
const items = ref<PermissionItem[]>([])
const checkedIds = ref<number[]>([])
const saving = ref(false)
const restoring = ref(false)

async function loadRoles() {
  const res = await listRolesApi()
  roles.value = res.data.data
}

async function loadPerms() {
  if (!selectedRoleId.value) return
  const res = await listRolePermissionsApi(selectedRoleId.value)
  items.value = res.data.data
  checkedIds.value = items.value.filter((i) => i.assigned).map((i) => i.id)
}

function applyItems(data: PermissionItem[]) {
  items.value = data
  checkedIds.value = data.filter((i) => i.assigned).map((i) => i.id)
}

async function save() {
  if (!selectedRoleId.value) return
  saving.value = true
  try {
    await saveRolePermissionsApi(
      selectedRoleId.value,
      checkedIds.value.map((id) => Number(id))
    )
    ElMessage.success('已保存，相关用户刷新页面后即可使用新权限')
    await loadPerms()
  } finally {
    saving.value = false
  }
}

async function restoreDefault() {
  if (!selectedRoleId.value) return
  const role = roles.value.find((r) => r.id === selectedRoleId.value)
  await ElMessageBox.confirm(
    `将把「${role?.name || '该角色'}」的权限恢复为系统默认配置（与初始种子数据一致），是否继续？`,
    '恢复默认',
    { type: 'warning' }
  )
  restoring.value = true
  try {
    const res = await restoreRolePermissionsDefaultApi(selectedRoleId.value)
    applyItems(res.data.data)
    ElMessage.success('已恢复为默认权限')
  } finally {
    restoring.value = false
  }
}

onMounted(loadRoles)
</script>

<style scoped>
.hint {
  color: #909399;
  font-size: 13px;
  margin: 0 0 8px;
}
.perm-row {
  margin: 8px 0;
}
.actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
