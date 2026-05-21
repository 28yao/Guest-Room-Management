<template>
  <div>
    <div class="toolbar">
      <h3>用户管理</h3>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>
    <el-table :data="users" border>
      <el-table-column prop="username" label="用户名" />
      <el-table-column label="角色">
        <template #default="{ row }">
          {{ (row.roleNames || []).join('、') || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item v-if="!editingId" label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" :placeholder="editingId ? '留空则不修改' : ''" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.statusEnabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listUsersApi, createUserApi, updateUserApi, type UserVO } from '@/api/user'
import { listRolesApi, type RoleVO } from '@/api/role'

const users = ref<UserVO[]>([])
const roles = ref<RoleVO[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const editingId = ref<number | null>(null)
const saving = ref(false)

const form = reactive({
  username: '',
  password: '',
  statusEnabled: true,
  roleIds: [] as number[]
})

async function load() {
  const [u, r] = await Promise.all([listUsersApi(), listRolesApi()])
  users.value = u.data.data
  roles.value = r.data.data
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增用户'
  form.username = ''
  form.password = ''
  form.statusEnabled = true
  form.roleIds = []
  dialogVisible.value = true
}

function openEdit(row: UserVO) {
  editingId.value = row.id
  dialogTitle.value = '编辑用户'
  form.username = row.username
  form.password = ''
  form.statusEnabled = row.status === 1
  form.roleIds = row.roleIds ? [...row.roleIds] : []
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const status = form.statusEnabled ? 1 : 0
    if (editingId.value) {
      await updateUserApi(editingId.value, {
        password: form.password || undefined,
        status,
        roleIds: form.roleIds
      })
    } else {
      if (!form.username || !form.password) {
        ElMessage.warning('请填写用户名和密码')
        return
      }
      await createUserApi({
        username: form.username,
        password: form.password,
        status,
        roleIds: form.roleIds
      })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
