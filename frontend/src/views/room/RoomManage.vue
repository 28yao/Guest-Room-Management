<template>
  <div>
    <div class="toolbar">
      <h3>客房管理</h3>
      <el-button type="primary" @click="openCreate">新增客房</el-button>
    </div>
    <el-table :data="rooms" border>
      <el-table-column prop="roomNo" label="房号" width="100" />
      <el-table-column prop="roomTypeName" label="房型" />
      <el-table-column prop="floorNo" label="楼层" width="80" />
      <el-table-column label="占用态" width="100">
        <template #default="{ row }">
          <el-tag>{{ occupancyLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="保洁态" width="80">
        <template #default="{ row }">
          <el-tag type="info">{{ cleanLabel(row.cleanStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="dialogTitle" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="房号">
          <el-input v-model="form.roomNo" />
        </el-form-item>
        <el-form-item label="房型">
          <el-select v-model="form.roomTypeId" style="width: 100%">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层">
          <el-input-number v-model="form.floorNo" :min="1" :max="99" />
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
import { listRoomTypesApi, type RoomTypeVO } from '@/api/roomType'
import {
  listRoomsApi,
  createRoomApi,
  updateRoomApi,
  OCCUPANCY_STATUS_LABEL,
  CLEAN_STATUS_LABEL,
  type RoomVO,
  type RoomForm
} from '@/api/room'

const rooms = ref<RoomVO[]>([])
const types = ref<RoomTypeVO[]>([])
const visible = ref(false)
const dialogTitle = ref('新增客房')
const editingId = ref<number | null>(null)
const saving = ref(false)

const form = reactive({
  roomNo: '',
  roomTypeId: undefined as number | undefined,
  floorNo: 2
})

function occupancyLabel(s: string) {
  return OCCUPANCY_STATUS_LABEL[s] || s
}

function cleanLabel(s: string) {
  return CLEAN_STATUS_LABEL[s] || s
}

async function load() {
  const [r, t] = await Promise.all([listRoomsApi(), listRoomTypesApi()])
  rooms.value = r.data.data
  types.value = t.data.data
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增客房'
  form.roomNo = ''
  form.roomTypeId = types.value[0]?.id
  form.floorNo = 2
  visible.value = true
}

function openEdit(row: RoomVO) {
  editingId.value = row.id
  dialogTitle.value = '编辑客房'
  form.roomNo = row.roomNo
  form.roomTypeId = row.roomTypeId
  form.floorNo = row.floorNo
  visible.value = true
}

async function save() {
  if (!form.roomNo || !form.roomTypeId) {
    ElMessage.warning('请填写房号并选择房型')
    return
  }
  const payload: RoomForm = {
    roomNo: form.roomNo,
    roomTypeId: form.roomTypeId,
    floorNo: form.floorNo
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateRoomApi(editingId.value, payload)
    } else {
      await createRoomApi(payload)
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

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
</style>
