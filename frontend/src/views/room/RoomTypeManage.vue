<template>
  <div>
    <div class="toolbar">
      <h3>房型管理</h3>
      <el-button type="primary" @click="openCreate">新增房型</el-button>
    </div>
    <el-table :data="types" border>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="rackRate" label="门市价" width="120">
        <template #default="{ row }">¥{{ row.rackRate }}</template>
      </el-table-column>
      <el-table-column prop="bedType" label="床型" width="100" />
      <el-table-column prop="maxGuests" label="最多入住" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="dialogTitle" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="门市价">
          <el-input-number v-model="form.rackRate" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="床型">
          <el-input v-model="form.bedType" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listRoomTypesApi,
  createRoomTypeApi,
  updateRoomTypeApi,
  type RoomTypeVO,
  type RoomTypeForm
} from '@/api/roomType'

const types = ref<RoomTypeVO[]>([])
const visible = ref(false)
const dialogTitle = ref('新增房型')
const editingId = ref<number | null>(null)
const saving = ref(false)

const form = reactive({
  name: '',
  rackRate: 299,
  bedType: '',
  description: '',
  enabled: true
})

async function load() {
  const res = await listRoomTypesApi()
  types.value = res.data.data
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增房型'
  form.name = ''
  form.rackRate = 299
  form.bedType = '大床'
  form.description = ''
  form.enabled = true
  visible.value = true
}

function openEdit(row: RoomTypeVO) {
  editingId.value = row.id
  dialogTitle.value = '编辑房型'
  form.name = row.name
  form.rackRate = Number(row.rackRate)
  form.bedType = row.bedType || ''
  form.description = row.description || ''
  form.enabled = row.status === 1
  visible.value = true
}

function toPayload(): RoomTypeForm {
  return {
    name: form.name,
    rackRate: form.rackRate,
    bedType: form.bedType,
    description: form.description,
    status: form.enabled ? 1 : 0
  }
}

async function save() {
  if (!form.name) {
    ElMessage.warning('请填写房型名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateRoomTypeApi(editingId.value, toPayload())
    } else {
      await createRoomTypeApi(toPayload())
    }
    ElMessage.success('保存成功')
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

