<template>
  <div>
    <div class="toolbar">
      <h3>房态图</h3>
      <el-select v-model="floorFilter" clearable placeholder="全部楼层" style="width: 140px" @change="load">
        <el-option v-for="f in floors" :key="f" :label="`${f} 层`" :value="f" />
      </el-select>
      <el-button @click="load">刷新</el-button>
    </div>
    <div v-if="items.length === 0" class="empty">暂无客房，请先在「客房管理」中维护房号</div>
    <div v-else class="board">
      <div
        v-for="room in items"
        :key="room.id"
        class="room-card"
        :style="{ borderColor: statusColor(room.status) }"
        @click="openActions(room)"
      >
        <div class="room-no">{{ room.roomNo }}</div>
        <div class="room-type">{{ room.roomTypeName }}</div>
        <el-tag size="small" :color="statusColor(room.status)" effect="dark">
          {{ statusLabel(room.status) }}
        </el-tag>
        <div class="tags">
          <el-tag v-if="room.dailyTags?.includes('EXPECTED_ARRIVAL')" size="small" type="warning">预抵</el-tag>
          <el-tag v-if="room.dailyTags?.includes('EXPECTED_DEPARTURE')" size="small" type="danger">预离</el-tag>
        </div>
      </div>
    </div>

    <el-dialog v-model="actionVisible" title="客房操作" width="480px">
      <p><strong>{{ selected?.roomNo }}</strong> — {{ statusLabel(selected?.status || '') }}</p>
      <el-button
        v-if="auth.hasPermission('room:status:maintenance') && selected?.status !== 'OUT_OF_ORDER'"
        type="warning"
        @click="openMaintenance"
      >
        设维修
      </el-button>
      <el-button
        v-if="auth.hasPermission('room:status:maintenance') && selected?.status === 'OUT_OF_ORDER'"
        type="success"
        @click="openEndMaintenance"
      >
        结束维修
      </el-button>
      <el-button v-if="auth.hasPermission('room:status:force')" type="danger" @click="openForce">
        强制改态
      </el-button>
    </el-dialog>

    <el-dialog v-model="maintVisible" title="设维修" width="440px">
      <el-form label-width="100px">
        <el-form-item label="原因" required>
          <el-input v-model="maintForm.reason" type="textarea" />
        </el-form-item>
        <el-form-item label="预计恢复" required>
          <el-date-picker
            v-model="maintForm.expectedRecoveryAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="maintVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitMaintenance">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="endMaintVisible" title="结束维修" width="400px">
      <el-form label-width="100px">
        <el-form-item label="恢复为">
          <el-radio-group v-model="endMaintForm.targetStatus">
            <el-radio label="DIRTY">脏房</el-radio>
            <el-radio label="VACANT_CLEAN">空净</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="endMaintVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEndMaintenance">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="forceVisible" title="强制改房态" width="440px">
      <el-alert type="warning" title="此操作将跳过正常状态机，请填写原因" :closable="false" show-icon />
      <el-form label-width="80px" style="margin-top: 12px">
        <el-form-item label="目标态">
          <el-select v-model="forceForm.targetStatus" style="width: 100%">
            <el-option v-for="(label, key) in ROOM_STATUS_LABEL" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="forceForm.reason" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forceVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="submitForce">确认改态</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  getRoomBoardApi,
  startMaintenanceApi,
  endMaintenanceApi,
  forceRoomStatusApi,
  ROOM_STATUS_LABEL,
  ROOM_STATUS_COLOR,
  type RoomBoardItem
} from '@/api/room'

const auth = useAuthStore()
const items = ref<RoomBoardItem[]>([])
const floorFilter = ref<number | undefined>()
const actionVisible = ref(false)
const selected = ref<RoomBoardItem | null>(null)
const maintVisible = ref(false)
const endMaintVisible = ref(false)
const forceVisible = ref(false)
const saving = ref(false)

const maintForm = reactive({ reason: '', expectedRecoveryAt: '' })
const endMaintForm = reactive({ targetStatus: 'DIRTY' })
const forceForm = reactive({ targetStatus: 'VACANT_CLEAN', reason: '' })

const floors = computed(() => {
  const set = new Set<number>()
  for (const r of items.value) {
    set.add(r.floorNo)
  }
  return Array.from(set).sort((a, b) => a - b)
})

function statusLabel(s: string) {
  return ROOM_STATUS_LABEL[s] || s
}

function statusColor(s: string) {
  return ROOM_STATUS_COLOR[s] || '#dcdfe6'
}

async function load() {
  const res = await getRoomBoardApi(floorFilter.value)
  items.value = res.data.data
}

function openActions(room: RoomBoardItem) {
  selected.value = room
  actionVisible.value = true
}

function openMaintenance() {
  actionVisible.value = false
  maintForm.reason = ''
  maintForm.expectedRecoveryAt = ''
  maintVisible.value = true
}

function openEndMaintenance() {
  actionVisible.value = false
  endMaintForm.targetStatus = 'DIRTY'
  endMaintVisible.value = true
}

function openForce() {
  actionVisible.value = false
  forceForm.targetStatus = 'VACANT_CLEAN'
  forceForm.reason = ''
  forceVisible.value = true
}

async function submitMaintenance() {
  if (!selected.value || !maintForm.reason || !maintForm.expectedRecoveryAt) {
    ElMessage.warning('请填写维修原因与预计恢复时间')
    return
  }
  saving.value = true
  try {
    await startMaintenanceApi(selected.value.id, {
      reason: maintForm.reason,
      expectedRecoveryAt: maintForm.expectedRecoveryAt,
      version: selected.value.version
    })
    ElMessage.success('已设为维修')
    maintVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function submitEndMaintenance() {
  if (!selected.value) return
  saving.value = true
  try {
    await endMaintenanceApi(selected.value.id, {
      targetStatus: endMaintForm.targetStatus,
      version: selected.value.version
    })
    ElMessage.success('维修已结束')
    endMaintVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function submitForce() {
  if (!selected.value || !forceForm.reason) {
    ElMessage.warning('请填写改态原因')
    return
  }
  await ElMessageBox.confirm('确认强制修改房态？', '二次确认', { type: 'warning' })
  saving.value = true
  try {
    await forceRoomStatusApi(selected.value.id, {
      targetStatus: forceForm.targetStatus,
      reason: forceForm.reason,
      version: selected.value.version
    })
    ElMessage.success('房态已更新')
    forceVisible.value = false
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
  gap: 12px;
  margin-bottom: 16px;
}
.board {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.room-card {
  width: 120px;
  padding: 10px;
  border: 3px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  background: #fff;
}
.room-no {
  font-size: 18px;
  font-weight: bold;
}
.room-type {
  font-size: 12px;
  color: #666;
  margin: 4px 0;
}
.tags {
  margin-top: 6px;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.empty {
  color: #909399;
  padding: 24px;
}
</style>
