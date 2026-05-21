<template>
  <div>
    <div class="toolbar">
      <h3>在住管理</h3>
      <el-button type="primary" @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="stayNo" label="在住单号" width="160" />
      <el-table-column prop="roomNo" label="房号" width="80" />
      <el-table-column prop="roomTypeName" label="房型" />
      <el-table-column prop="guestName" label="客人" width="100" />
      <el-table-column prop="guestPhone" label="电话" width="120" />
      <el-table-column label="入住/离店" width="200">
        <template #default="{ row }">{{ row.arrivalDate }} ~ {{ row.departureDate }}</template>
      </el-table-column>
      <el-table-column label="账单合计" width="100">
        <template #default="{ row }">¥{{ row.folioTotalAmount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canChangeRoom" link type="primary" @click="openChangeRoom(row)">换房</el-button>
          <el-button link type="primary" @click="openRemark(row)">备注</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="changeVisible" title="换房" width="420px">
      <el-form label-width="90px">
        <el-form-item label="目标客房" required>
          <el-select v-model="changeForm.targetRoomId" style="width: 100%" @focus="loadTargetRooms">
            <el-option
              v-for="r in targetRooms"
              :key="r.roomId"
              :label="`${r.roomNo} (${r.roomTypeName})`"
              :value="r.roomId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChangeRoom">确认换房</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remarkVisible" title="在住备注" width="420px">
      <el-input v-model="remarkText" type="textarea" rows="4" />
      <template #footer>
        <el-button @click="remarkVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRemark">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { listInHouse, changeRoom, updateStayRemark, type StayVO } from '@/api/stay'
import { listAvailabilityApi, type AvailableRoomVO } from '@/api/reservation'
import { combineDateTime, DEFAULT_ARRIVAL_TIME, DEFAULT_DEPARTURE_TIME } from '@/utils/datetime'

const auth = useAuthStore()
const canChangeRoom = auth.hasPermission('stay:change_room')
const list = ref<StayVO[]>([])
const changeVisible = ref(false)
const remarkVisible = ref(false)
const currentStay = ref<StayVO | null>(null)
const changeForm = ref({ targetRoomId: undefined as number | undefined })
const targetRooms = ref<AvailableRoomVO[]>([])
const remarkText = ref('')

onMounted(() => load())

async function load() {
  const res = await listInHouse()
  list.value = res.data.data || []
}

function openChangeRoom(row: StayVO) {
  currentStay.value = row
  changeForm.value.targetRoomId = undefined
  changeVisible.value = true
}

async function loadTargetRooms() {
  if (!currentStay.value) return
  const row = currentStay.value
  const res = await listAvailabilityApi({
    roomTypeId: row.roomTypeId,
    arrival: row.arrivalDate,
    departure: row.departureDate,
    arrivalAt: combineDateTime(row.arrivalDate, DEFAULT_ARRIVAL_TIME),
    departureAt: combineDateTime(row.departureDate, DEFAULT_DEPARTURE_TIME)
  })
  targetRooms.value = (res.data.data || []).filter((r) => r.roomId !== row.roomId)
}

async function submitChangeRoom() {
  if (!currentStay.value || !changeForm.value.targetRoomId) {
    ElMessage.warning('请选择目标客房')
    return
  }
  const target = targetRooms.value.find((r) => r.roomId === changeForm.value.targetRoomId)
  try {
    await changeRoom(currentStay.value.id, {
      targetRoomId: changeForm.value.targetRoomId,
      targetRoomVersion: target?.version
    })
    ElMessage.success('换房成功，账单已重算')
    changeVisible.value = false
    await load()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '换房失败')
  }
}

function openRemark(row: StayVO) {
  currentStay.value = row
  remarkText.value = row.remark || ''
  remarkVisible.value = true
}

async function submitRemark() {
  if (!currentStay.value) return
  await updateStayRemark(currentStay.value.id, remarkText.value)
  ElMessage.success('备注已保存')
  remarkVisible.value = false
  await load()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
</style>
