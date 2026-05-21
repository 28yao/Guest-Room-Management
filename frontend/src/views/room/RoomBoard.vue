<template>
  <div>
    <div class="toolbar">
      <h3>房态图</h3>
      <el-date-picker
        v-model="viewDate"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="查看日期"
        style="width: 150px"
        @change="load"
      />
      <el-button link type="primary" @click="setToday">今天</el-button>
      <el-select v-model="floorFilter" clearable placeholder="全部楼层" style="width: 140px" @change="load">
        <el-option v-for="f in floors" :key="f" :label="`${f} 层`" :value="f" />
      </el-select>
      <el-button @click="load">刷新</el-button>
      <span class="date-hint">展示态按 {{ viewDate }} 与预订/在住日期计算；操作以库内实时状态为准</span>
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

    <el-dialog v-model="actionVisible" :title="`客房 ${selected?.roomNo || ''}`" width="720px" @open="onDialogOpen">
      <p class="room-meta">
        <span>{{ selected?.roomTypeName }}</span>
        <el-tag size="small" class="meta-tag">展示 {{ statusLabel(selected?.status || '') }}</el-tag>
        <el-tag size="small" type="info">库内 {{ statusLabel(schedule?.actualStatus || selected?.actualStatus || '') }}</el-tag>
        <span class="meta-date">查看日 {{ viewDate }}</span>
      </p>

      <div class="section-title">当前及未来订单（自 {{ viewDate }} 起）</div>
      <el-table v-loading="scheduleLoading" :data="schedule?.orders || []" border size="small" max-height="220">
        <el-table-column label="类型" width="72">
          <template #default="{ row }">
            <el-tag size="small" :type="row.orderType === 'STAY' ? 'warning' : 'primary'">
              {{ row.orderType === 'STAY' ? '在住' : '预订' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="单号" width="150" />
        <el-table-column prop="guestName" label="客人" width="90" />
        <el-table-column label="入住/离店" min-width="200">
          <template #default="{ row }">{{ formatOrderRange(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="88">
          <template #default="{ row }">{{ orderStatusLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.editable" link type="primary" @click="openEditOrder(row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!schedule?.occupiedOnViewDate" class="quick-actions">
        <span class="section-title">快速办理（查看日无占用）</span>
        <el-button
          v-if="auth.hasPermission('reservation:manage')"
          type="primary"
          size="small"
          @click="openQuickReserve"
        >
          快速预订
        </el-button>
        <el-button
          v-if="auth.hasPermission('stay:checkin') && schedule?.actualStatus === 'VACANT_CLEAN'"
          type="success"
          size="small"
          @click="openQuickWalkIn"
        >
          快速 Walk-in 入住
        </el-button>
        <span v-if="auth.hasPermission('stay:checkin') && schedule?.actualStatus !== 'VACANT_CLEAN'" class="hint-inline">
          仅空净房可 Walk-in（当前库内 {{ statusLabel(schedule?.actualStatus || '') }}）
        </span>
      </div>

      <el-divider />
      <div class="section-title">房态操作</div>
      <el-button
        v-if="auth.hasPermission('room:status:maintenance') && selected?.actualStatus !== 'OUT_OF_ORDER'"
        type="warning"
        @click="openMaintenance"
      >
        设维修
      </el-button>
      <el-button
        v-if="auth.hasPermission('room:status:maintenance') && selected?.actualStatus === 'OUT_OF_ORDER'"
        type="success"
        @click="openEndMaintenance"
      >
        结束维修
      </el-button>
      <el-button
        v-if="auth.hasPermission('room:status:dirty') && canMarkDirty(selected?.actualStatus)"
        type="warning"
        @click="submitMarkDirty"
      >
        设为脏房
      </el-button>
      <el-button
        v-if="auth.hasPermission('room:status:clean') && canMarkClean(selected?.actualStatus)"
        type="success"
        @click="submitMarkClean"
      >
        设为空净
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

    <el-dialog v-model="resEditVisible" :title="resEditTitle" width="520px">
      <el-form :model="resForm" label-width="90px">
        <el-form-item label="客人姓名" required>
          <el-input v-model="resForm.guestName" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="resForm.guestPhone" />
        </el-form-item>
        <el-form-item label="房型" required>
          <el-select v-model="resForm.roomTypeId" style="width: 100%">
            <el-option v-for="t in roomTypes" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入住日期" required>
          <el-date-picker v-model="resForm.arrivalDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入住时刻">
          <el-time-picker v-model="resForm.arrivalTime" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="离店日期" required>
          <el-date-picker v-model="resForm.departureDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="离店时刻">
          <el-time-picker v-model="resForm.departureTime" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="resForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveReservationEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stayRemarkVisible" title="修改在住备注" width="440px">
      <el-form label-width="80px">
        <el-form-item label="单号">
          <span>{{ stayRemarkOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stayRemarkText" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stayRemarkVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStayRemark">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quickResVisible" title="快速预订" width="520px">
      <el-form :model="quickResForm" label-width="90px">
        <el-form-item label="房号">
          <span>{{ schedule?.roomNo }}</span>
        </el-form-item>
        <el-form-item label="客人姓名" required>
          <el-input v-model="quickResForm.guestName" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="quickResForm.guestPhone" />
        </el-form-item>
        <el-form-item label="入住日期" required>
          <el-date-picker v-model="quickResForm.arrivalDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入住时刻">
          <el-time-picker v-model="quickResForm.arrivalTime" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="离店日期" required>
          <el-date-picker v-model="quickResForm.departureDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="离店时刻">
          <el-time-picker v-model="quickResForm.departureTime" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="quickResForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickResVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitQuickReserve">创建并预排本房</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="quickWalkVisible" title="快速 Walk-in 入住" width="520px">
      <el-form :model="quickWalkForm" label-width="100px">
        <el-form-item label="房号">
          <span>{{ schedule?.roomNo }}</span>
        </el-form-item>
        <el-form-item label="客人姓名" required>
          <el-input v-model="quickWalkForm.guestName" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="quickWalkForm.guestPhone" />
        </el-form-item>
        <el-form-item label="入住日期" required>
          <el-date-picker v-model="quickWalkForm.arrivalDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入住时刻">
          <el-time-picker v-model="quickWalkForm.arrivalTime" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="离店日期" required>
          <el-date-picker v-model="quickWalkForm.departureDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="离店时刻">
          <el-time-picker v-model="quickWalkForm.departureTime" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="协议房价">
          <el-input-number v-model="quickWalkForm.agreedDailyRate" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="quickWalkForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickWalkVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitQuickWalkIn">确认入住</el-button>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  getRoomBoardApi,
  getRoomScheduleApi,
  listRoomFloorsApi,
  startMaintenanceApi,
  endMaintenanceApi,
  forceRoomStatusApi,
  markRoomDirtyApi,
  markRoomCleanApi,
  MARK_DIRTY_FROM,
  MARK_CLEAN_FROM,
  ROOM_STATUS_LABEL,
  ROOM_STATUS_COLOR,
  type RoomBoardItem,
  type RoomScheduleVO,
  type RoomScheduleOrderVO
} from '@/api/room'
import { listRoomTypesApi, type RoomTypeVO } from '@/api/roomType'
import {
  RES_STATUS_LABEL,
  createReservationApi,
  updateReservationApi,
  assignRoomApi
} from '@/api/reservation'
import { getCurrentShift } from '@/api/shift'
import { walkInCheckIn } from '@/api/stay'
import { updateStayRemark } from '@/api/stay'
import { combineDateTime, DEFAULT_ARRIVAL_TIME, DEFAULT_DEPARTURE_TIME } from '@/utils/datetime'

const auth = useAuthStore()
const items = ref<RoomBoardItem[]>([])
const floors = ref<number[]>([])
const floorFilter = ref<number | undefined>()
const viewDate = ref(todayString())
const actionVisible = ref(false)
const selected = ref<RoomBoardItem | null>(null)
const schedule = ref<RoomScheduleVO | null>(null)
const scheduleLoading = ref(false)
const roomTypes = ref<RoomTypeVO[]>([])
const resEditVisible = ref(false)
const resEditTitle = ref('修改预订')
const editingResId = ref<number | null>(null)
const stayRemarkVisible = ref(false)
const stayRemarkOrder = ref<RoomScheduleOrderVO | null>(null)
const stayRemarkText = ref('')
const quickResVisible = ref(false)
const quickWalkVisible = ref(false)
const maintVisible = ref(false)
const endMaintVisible = ref(false)
const forceVisible = ref(false)
const saving = ref(false)

const maintForm = reactive({ reason: '', expectedRecoveryAt: '' })
const endMaintForm = reactive({ targetStatus: 'DIRTY' })
const forceForm = reactive({ targetStatus: 'VACANT_CLEAN', reason: '' })

const resForm = reactive({
  guestName: '',
  guestPhone: '',
  roomTypeId: undefined as number | undefined,
  arrivalDate: '',
  departureDate: '',
  arrivalTime: DEFAULT_ARRIVAL_TIME,
  departureTime: DEFAULT_DEPARTURE_TIME,
  remark: ''
})

const quickResForm = reactive({
  guestName: '',
  guestPhone: '',
  arrivalDate: '',
  departureDate: '',
  arrivalTime: DEFAULT_ARRIVAL_TIME,
  departureTime: DEFAULT_DEPARTURE_TIME,
  remark: ''
})

const quickWalkForm = reactive({
  guestName: '',
  guestPhone: '',
  arrivalDate: '',
  departureDate: '',
  arrivalTime: DEFAULT_ARRIVAL_TIME,
  departureTime: DEFAULT_DEPARTURE_TIME,
  agreedDailyRate: undefined as number | undefined,
  remark: ''
})

function statusLabel(s: string) {
  return ROOM_STATUS_LABEL[s] || s
}

function statusColor(s: string) {
  return ROOM_STATUS_COLOR[s] || '#dcdfe6'
}

function todayString() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function setToday() {
  viewDate.value = todayString()
  load()
}

async function loadFloors() {
  const res = await listRoomFloorsApi()
  floors.value = res.data.data || []
}

async function load() {
  const res = await getRoomBoardApi(floorFilter.value, viewDate.value)
  items.value = res.data.data.map((r) => ({
    ...r,
    actualStatus: r.actualStatus || r.status
  }))
}

function addDays(dateStr: string, days: number) {
  const d = new Date(dateStr + 'T12:00:00')
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}

function parseTimeFromIso(iso?: string, fallback = DEFAULT_ARRIVAL_TIME) {
  if (!iso) return fallback
  const part = iso.includes('T') ? iso.split('T')[1] : iso
  return part.length >= 8 ? part.substring(0, 8) : fallback
}

function formatOrderRange(row: RoomScheduleOrderVO) {
  const start = row.arrivalAt || combineDateTime(row.arrivalDate, DEFAULT_ARRIVAL_TIME)
  const end = row.departureAt || combineDateTime(row.departureDate, DEFAULT_DEPARTURE_TIME)
  return `${start.replace('T', ' ').slice(0, 16)} ~ ${end.replace('T', ' ').slice(0, 16)}`
}

function orderStatusLabel(row: RoomScheduleOrderVO) {
  if (row.orderType === 'STAY') {
    return row.status === 'IN_HOUSE' ? '在住' : row.status
  }
  return RES_STATUS_LABEL[row.status] || row.status
}

async function loadSchedule() {
  if (!selected.value) return
  scheduleLoading.value = true
  try {
    const res = await getRoomScheduleApi(selected.value.id, viewDate.value)
    schedule.value = res.data.data
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载客房日程失败')
    schedule.value = null
  } finally {
    scheduleLoading.value = false
  }
}

function openActions(room: RoomBoardItem) {
  selected.value = room
  schedule.value = null
  actionVisible.value = true
}

async function onDialogOpen() {
  if (roomTypes.value.length === 0) {
    const t = await listRoomTypesApi()
    roomTypes.value = t.data.data || []
  }
  await loadSchedule()
}

function openEditOrder(row: RoomScheduleOrderVO) {
  if (row.orderType === 'STAY') {
    stayRemarkOrder.value = row
    stayRemarkText.value = row.remark || ''
    stayRemarkVisible.value = true
    return
  }
  editingResId.value = row.orderId
  resEditTitle.value = `修改预订 ${row.orderNo}`
  resForm.guestName = row.guestName
  resForm.guestPhone = row.guestPhone
  resForm.roomTypeId = schedule.value?.roomTypeId
  resForm.arrivalDate = row.arrivalDate
  resForm.departureDate = row.departureDate
  resForm.arrivalTime = parseTimeFromIso(row.arrivalAt, DEFAULT_ARRIVAL_TIME)
  resForm.departureTime = parseTimeFromIso(row.departureAt, DEFAULT_DEPARTURE_TIME)
  resForm.remark = row.remark || ''
  resEditVisible.value = true
}

async function saveReservationEdit() {
  if (!editingResId.value) return
  if (!resForm.guestName || !resForm.guestPhone || !resForm.roomTypeId || !resForm.arrivalDate || !resForm.departureDate) {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  try {
    await updateReservationApi(editingResId.value, {
      guestName: resForm.guestName,
      guestPhone: resForm.guestPhone,
      roomTypeId: resForm.roomTypeId,
      arrivalDate: resForm.arrivalDate,
      departureDate: resForm.departureDate,
      arrivalAt: combineDateTime(resForm.arrivalDate, resForm.arrivalTime),
      departureAt: combineDateTime(resForm.departureDate, resForm.departureTime),
      remark: resForm.remark
    })
    ElMessage.success('预订已更新')
    resEditVisible.value = false
    await loadSchedule()
    await load()
  } finally {
    saving.value = false
  }
}

async function saveStayRemark() {
  if (!stayRemarkOrder.value) return
  saving.value = true
  try {
    await updateStayRemark(stayRemarkOrder.value.orderId, stayRemarkText.value)
    ElMessage.success('备注已更新')
    stayRemarkVisible.value = false
    await loadSchedule()
  } finally {
    saving.value = false
  }
}

function openQuickReserve() {
  if (!schedule.value) return
  quickResForm.guestName = ''
  quickResForm.guestPhone = ''
  quickResForm.arrivalDate = viewDate.value
  quickResForm.departureDate = addDays(viewDate.value, 1)
  quickResForm.arrivalTime = DEFAULT_ARRIVAL_TIME
  quickResForm.departureTime = DEFAULT_DEPARTURE_TIME
  quickResForm.remark = ''
  quickResVisible.value = true
}

async function submitQuickReserve() {
  if (!schedule.value) return
  if (!quickResForm.guestName || !quickResForm.guestPhone || !quickResForm.arrivalDate || !quickResForm.departureDate) {
    ElMessage.warning('请填写客人姓名、电话与日期')
    return
  }
  saving.value = true
  try {
    const created = await createReservationApi({
      guestName: quickResForm.guestName,
      guestPhone: quickResForm.guestPhone,
      roomTypeId: schedule.value.roomTypeId,
      arrivalDate: quickResForm.arrivalDate,
      departureDate: quickResForm.departureDate,
      arrivalAt: combineDateTime(quickResForm.arrivalDate, quickResForm.arrivalTime),
      departureAt: combineDateTime(quickResForm.departureDate, quickResForm.departureTime),
      remark: quickResForm.remark
    })
    const resId = created.data.data.id
    await assignRoomApi(resId, schedule.value.roomId)
    ElMessage.success('预订已创建并预排至本房')
    quickResVisible.value = false
    await loadSchedule()
    await load()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '快速预订失败')
  } finally {
    saving.value = false
  }
}

function openQuickWalkIn() {
  if (!schedule.value) return
  quickWalkForm.guestName = ''
  quickWalkForm.guestPhone = ''
  quickWalkForm.arrivalDate = viewDate.value
  quickWalkForm.departureDate = addDays(viewDate.value, 1)
  quickWalkForm.arrivalTime = DEFAULT_ARRIVAL_TIME
  quickWalkForm.departureTime = DEFAULT_DEPARTURE_TIME
  quickWalkForm.agreedDailyRate = schedule.value.rackRate != null ? Number(schedule.value.rackRate) : undefined
  quickWalkForm.remark = ''
  quickWalkVisible.value = true
}

async function submitQuickWalkIn() {
  if (!schedule.value) return
  if (!quickWalkForm.guestName || !quickWalkForm.guestPhone) {
    ElMessage.warning('请填写客人信息')
    return
  }
  try {
    const shiftRes = await getCurrentShift()
    if (!shiftRes.data.data) {
      ElMessage.warning('请先开班后再办理入住')
      return
    }
  } catch {
    ElMessage.warning('请先开班后再办理入住')
    return
  }
  saving.value = true
  try {
    await walkInCheckIn({
      roomId: schedule.value.roomId,
      guestName: quickWalkForm.guestName,
      guestPhone: quickWalkForm.guestPhone,
      arrivalDate: quickWalkForm.arrivalDate,
      departureDate: quickWalkForm.departureDate,
      arrivalAt: combineDateTime(quickWalkForm.arrivalDate, quickWalkForm.arrivalTime),
      departureAt: combineDateTime(quickWalkForm.departureDate, quickWalkForm.departureTime),
      agreedDailyRate: quickWalkForm.agreedDailyRate,
      remark: quickWalkForm.remark
    })
    ElMessage.success('入住成功')
    quickWalkVisible.value = false
    actionVisible.value = false
    await load()
    await loadFloors()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '入住失败')
  } finally {
    saving.value = false
  }
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

function canMarkDirty(status?: string) {
  return !!status && (MARK_DIRTY_FROM as readonly string[]).includes(status)
}

function canMarkClean(status?: string) {
  return !!status && (MARK_CLEAN_FROM as readonly string[]).includes(status)
}

async function submitMarkDirty() {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm('确认将该客房设为脏房？', '设为脏房', { type: 'warning' })
  } catch {
    return
  }
  saving.value = true
  try {
    await markRoomDirtyApi(selected.value.id, { version: selected.value.version })
    ElMessage.success('已设为脏房')
    actionVisible.value = false
    await load()
    await loadFloors()
  } finally {
    saving.value = false
  }
}

async function submitMarkClean() {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm('确认打扫完成，设为空净？', '设为空净', { type: 'info' })
  } catch {
    return
  }
  saving.value = true
  try {
    await markRoomCleanApi(selected.value.id, { version: selected.value.version })
    ElMessage.success('已设为空净')
    actionVisible.value = false
    await load()
    await loadFloors()
    if (actionVisible.value) {
      await loadSchedule()
    }
  } finally {
    saving.value = false
  }
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
    await loadFloors()
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
    await loadFloors()
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
    await loadFloors()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadFloors()
  await load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.date-hint {
  font-size: 12px;
  color: #909399;
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
.room-meta {
  margin: 0 0 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.meta-tag {
  margin-left: 4px;
}
.meta-date {
  color: #909399;
  font-size: 12px;
}
.section-title {
  font-weight: 600;
  margin: 8px 0;
  font-size: 14px;
}
.quick-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.hint-inline {
  font-size: 12px;
  color: #909399;
}
</style>
