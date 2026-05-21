<template>
  <div>
    <h3>角色权限配置</h3>
    <el-select v-model="selectedRoleId" placeholder="选择角色" style="width: 240px; margin: 16px 0" @change="loadPerms">
      <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
    </el-select>
    <el-checkbox-group v-if="selectedRoleId" v-model="checkedIds">
      <div v-for="p in items" :key="p.id" class="perm-row">
        <el-checkbox :label="p.id">{{ p.name }}（{{ p.code }}）</el-checkbox>
      </div>
    </el-checkbox-group>
    <el-button v-if="selectedRoleId" type="primary" style="margin-top: 16px" :loading="saving" @click="save">
      保存
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listRolesApi, listRolePermissionsApi, saveRolePermissionsApi, type RoleVO } from '@/api/role'
import type { PermissionItem } from '@/api/user'

const roles = ref<RoleVO[]>([])
const selectedRoleId = ref<number | null>(null)
const items = ref<PermissionItem[]>([])
const checkedIds = ref<number[]>([])
const saving = ref(false)

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

async function save() {
  if (!selectedRoleId.value) return
  saving.value = true
  try {
    await saveRolePermissionsApi(selectedRoleId.value, checkedIds.value)
    ElMessage.success('已保存')
    await loadPerms()
  } finally {
    saving.value = false
  }
}

onMounted(loadRoles)
</script>

<style scoped>
.perm-row {
  margin: 8px 0;
}
</style>
