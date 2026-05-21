<template>
  <div>
    <div class="toolbar">
      <h3>办理入住</h3>
      <div class="shift-bar">
        <el-tag v-if="shift" type="success">已开班 {{ formatTime(shift.openedAt) }}</el-tag>
        <el-tag v-else type="warning">未开班</el-tag>
        <el-button v-if="!shift && canOpenShift" type="primary" size="small" @click="onOpenShift">开班</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="Walk-in" name="walkin">
        <el-form :model="walkIn" label-width="100px" style="max-width: 520px">
          <el-form-item label="空净客房" required>
            <el-select
              v-model="walkIn.roomId"
              filterable
              placeholder="先选日期再加载可售房"
              style="width: 100%"
              @focus="loadWalkInRooms"
              @change="onWalkInRoomChange"
            >
              <el-option
                v-for="r in walkInRooms"
                :key="r.roomId"
                :label="`${r.roomNo} (${r.roomTypeName})`"
                :value="r.roomId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="客人姓名" required>
            <el-input v-model="walkIn.guestName" />
          </el-form-item>
          <el-form-item label="联系电话" required>
            <el-input v-model="walkIn.guestPhone" />
          </el-form-item>
          <el-form-item label="证件号">
            <el-input v-model="walkIn.idCard" />
          </el-form-item>
          <el-form-item label="入住日期" required>
            <el-date-picker v-model="walkIn.arrivalDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" @change="onWalkInDateChange" />
          </el-form-item>
          <el-form-item label="入住时刻">
            <el-time-picker v-model="walkInArrivalTime" value-format="HH:mm:ss" style="width: 100%" @change="onWalkInDateChange" />
            <div class="hint">默认 18:00，可改</div>
          </el-form-item>
          <el-form-item label="离店日期" required>
            <el-date-picker v-model="walkIn.departureDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" @change="onWalkInDateChange" />
          </el-form-item>
          <el-form-item label="离店时刻">
            <el-time-picker v-model="walkInDepartureTime" value-format="HH:mm:ss" style="width: 100%" @change="onWalkInDateChange" />
            <div class="hint">默认离店日 12:00，可改</div>
          </el-form-item>
          <el-form-item label="协议房价">
            <el-input-number v-model="walkIn.agreedDailyRate" :min="0" :precision="2" style="width: 100%" />
            <div class="hint">默认房型门市价，可改</div>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="walkIn.remark" type="textarea" />
          </el-form-item>
          <el-divider content-position="left">入住结账</el-divider>
          <el-form-item label="应付房费">
            <span class="charge-total">¥{{ walkInChargeable }}</span>
            <span class="hint">（{{ walkInNights }} 晚 × 协议价）</span>
          </el-form-item>
          <el-form-item label="收款" required>
            <div class="pay-row">
              <el-select v-model="walkInPayMethod" style="width: 120px">
                <el-option label="现金" value="CASH" />
                <el-option label="微信" value="WECHAT" />
                <el-option label="支付宝" value="ALIPAY" />
              </el-select>
              <el-input-number v-model="walkInPayAmount" :min="0.01" :precision="2" style="width: 160px" />
              <el-button link type="primary" @click="walkInPayAmount = walkInChargeable">收齐</el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="!shift" @click="submitWalkIn">确认入住并结账</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="预订入住" name="reservation">
        <el-form :inline="true" class="filters">
          <el-form-item label="客人姓名">
            <el-input v-model="resQuery" clearable placeholder="姓名/手机/单号" style="width: 180px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="searchReservations">查询预订</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="resList" border highlight-current-row @row-click="selectReservation">
          <el-table-column prop="resNo" label="预订单号" width="160" />
          <el-table-column prop="guestName" label="客人" width="100" />
          <el-table-column prop="guestPhone" label="电话" width="120" />
          <el-table-column prop="roomTypeName" label="房型" width="100" />
          <el-table-column prop="roomNo" label="房号" width="80">
            <template #default="{ row }">{{ row.roomNo || '—' }}</template>
          </el-table-column>
          <el-table-column label="门市价" width="90">
            <template #default="{ row }">{{ row.rackRate ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="入住/离店" width="220">
            <template #default="{ row }">{{ row.arrivalDate }} ~ {{ row.departureDate }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag>{{ row.status === 'CONFIRMED' ? '已确认' : row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-form v-if="resCheckIn.reservationId" :model="resCheckIn" label-width="100px" style="max-width: 520px; margin-top: 16px">
          <el-form-item label="客人">
            <span>{{ selectedRes?.guestName }} / {{ selectedRes?.guestPhone }}</span>
          </el-form-item>
          <el-form-item label="入住客房" required>
            <el-select v-model="resCheckIn.roomId" style="width: 100%" @change="onResRoomChange">
              <el-option
                v-for="r in resRooms"
                :key="r.roomId"
                :label="`${r.roomNo} (${r.roomTypeName})`"
                :value="r.roomId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="协议房价">
            <el-input-number v-model="resCheckIn.agreedDailyRate" :min="0" :precision="2" style="width: 100%" />
            <div class="hint">默认房型门市价 {{ selectedRes?.rackRate ?? '' }}，可改</div>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="resCheckIn.remark" type="textarea" />
          </el-form-item>
          <template v-if="selectedRes">
            <el-divider content-position="left">入住结账</el-divider>
            <el-form-item label="应付房费">
              <span class="charge-total">¥{{ resChargeable }}</span>
              <span class="hint">（{{ resNights }} 晚 × 协议价）</span>
            </el-form-item>
            <el-form-item label="收款" required>
              <div class="pay-row">
                <el-select v-model="resPayMethod" style="width: 120px">
                  <el-option label="现金" value="CASH" />
                  <el-option label="微信" value="WECHAT" />
                  <el-option label="支付宝" value="ALIPAY" />
                </el-select>
                <el-input-number v-model="resPayAmount" :min="0.01" :precision="2" style="width: 160px" />
                <el-button link type="primary" @click="resPayAmount = resChargeable">收齐</el-button>
              </div>
            </el-form-item>
          </template>
          <el-form-item>
            <el-button type="primary" :disabled="!shift || !resCheckIn.reservationId" @click="submitResCheckIn">
              确认入住并结账
            </el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getCurrentShift, openShift, type ShiftSessionVO } from '@/api/shift'
import {
  listReservationsApi,
  listAvailabilityApi,
  type ReservationVO,
  type AvailableRoomVO
} from '@/api/reservation'
import { walkInCheckIn, checkInFromReservation, type WalkInForm, type ReservationCheckInForm } from '@/api/stay'
import { combineDateTime, DEFAULT_ARRIVAL_TIME, DEFAULT_DEPARTURE_TIME, toIsoDateTime } from '@/utils/datetime'
import { computeCheckInChargeable, computeStayNights } from '@/utils/billing'

const auth = useAuthStore()
const activeTab = ref('walkin')
const shift = ref<ShiftSessionVO | null>(null)
const canOpenShift = auth.hasPermission('shift:open')

const walkIn = reactive<WalkInForm>({
  roomId: undefined,
  guestName: '',
  guestPhone: '',
  idCard: '',
  arrivalDate: '',
  departureDate: '',
  agreedDailyRate: undefined,
  remark: '',
  payments: [{ method: 'CASH', amount: 0 }]
})
const walkInPayMethod = ref('CASH')
const walkInPayAmount = ref(0)
const walkInRooms = ref<AvailableRoomVO[]>([])
const walkInArrivalTime = ref(DEFAULT_ARRIVAL_TIME)
const walkInDepartureTime = ref(DEFAULT_DEPARTURE_TIME)

const resQuery = ref('')
const resList = ref<ReservationVO[]>([])
const selectedRes = ref<ReservationVO | null>(null)
const resRooms = ref<AvailableRoomVO[]>([])
const resCheckIn = reactive<ReservationCheckInForm>({
  reservationId: undefined,
  roomId: undefined,
  agreedDailyRate: undefined,
  remark: '',
  payments: [{ method: 'CASH', amount: 0 }]
})
const resPayMethod = ref('CASH')
const resPayAmount = ref(0)

const walkInNights = computed(() =>
  walkIn.arrivalDate && walkIn.departureDate
    ? computeStayNights(walkIn.arrivalDate, walkIn.departureDate)
    : 0
)
const walkInChargeable = computed(() =>
  computeCheckInChargeable(Number(walkIn.agreedDailyRate ?? 0), walkIn.arrivalDate, walkIn.departureDate)
)

const resNights = computed(() =>
  selectedRes.value
    ? computeStayNights(selectedRes.value.arrivalDate, selectedRes.value.departureDate)
    : 0
)
const resChargeable = computed(() => {
  if (!selectedRes.value) return 0
  const rate = Number(resCheckIn.agreedDailyRate ?? selectedRes.value.rackRate ?? 0)
  return computeCheckInChargeable(rate, selectedRes.value.arrivalDate, selectedRes.value.departureDate)
})

watch(walkInChargeable, (v) => {
  walkInPayAmount.value = v
})
watch(resChargeable, (v) => {
  resPayAmount.value = v
})

onMounted(async () => {
  const today = new Date().toISOString().slice(0, 10)
  walkIn.arrivalDate = today
  walkIn.departureDate = addDays(today, 1)
  await loadShift()
})

function addDays(dateStr: string, days: number) {
  const d = new Date(dateStr)
  d.setDate(d.getDate() + days)
  return d.toISOString().slice(0, 10)
}

function formatTime(iso: string) {
  return iso ? iso.replace('T', ' ').slice(0, 16) : ''
}

function apiErrorMessage(e: unknown, fallback: string) {
  const err = e as { message?: string; response?: { data?: { message?: string } } }
  return err.message || err.response?.data?.message || fallback
}

async function loadShift() {
  try {
    const res = await getCurrentShift()
    shift.value = res.data.data
  } catch {
    shift.value = null
  }
}

async function onOpenShift() {
  const res = await openShift()
  shift.value = res.data.data
  ElMessage.success('开班成功')
}

function onWalkInDateChange() {
  walkIn.roomId = undefined
  walkIn.agreedDailyRate = undefined
  loadWalkInRooms()
}

function onWalkInRoomChange(roomId: number) {
  const room = walkInRooms.value.find((r) => r.roomId === roomId)
  if (room?.rackRate != null) {
    walkIn.agreedDailyRate = Number(room.rackRate)
  }
}

function walkInTimeRange() {
  return {
    arrivalAt: combineDateTime(walkIn.arrivalDate, walkInArrivalTime.value || DEFAULT_ARRIVAL_TIME),
    departureAt: combineDateTime(walkIn.departureDate, walkInDepartureTime.value || DEFAULT_DEPARTURE_TIME)
  }
}

async function loadWalkInRooms() {
  if (!walkIn.arrivalDate || !walkIn.departureDate) return
  const times = walkInTimeRange()
  try {
    const res = await listAvailabilityApi({
      arrival: walkIn.arrivalDate,
      departure: walkIn.departureDate,
      arrivalAt: times.arrivalAt,
      departureAt: times.departureAt
    })
    walkInRooms.value = res.data.data || []
    if (walkIn.roomId) {
      onWalkInRoomChange(walkIn.roomId)
    }
  } catch (e: unknown) {
    ElMessage.error(apiErrorMessage(e, '加载可售房失败'))
  }
}

async function submitWalkIn() {
  if (!walkIn.roomId) {
    ElMessage.warning('请选择客房')
    return
  }
  if (Math.abs(walkInPayAmount.value - walkInChargeable.value) > 0.009) {
    ElMessage.warning('收款金额须等于应付房费')
    return
  }
  const times = walkInTimeRange()
  walkIn.arrivalAt = times.arrivalAt
  walkIn.departureAt = times.departureAt
  walkIn.payments = [{ method: walkInPayMethod.value, amount: walkInPayAmount.value }]
  try {
    await walkInCheckIn(walkIn)
    ElMessage.success('入住成功')
    walkIn.guestName = ''
    walkIn.guestPhone = ''
    walkIn.roomId = undefined
    walkIn.agreedDailyRate = undefined
    await loadWalkInRooms()
  } catch (e: unknown) {
    ElMessage.error(apiErrorMessage(e, '入住失败'))
  }
}

async function searchReservations() {
  const q = resQuery.value.trim()
  try {
    const res = await listReservationsApi({
      status: 'CONFIRMED',
      guestName: q || undefined,
      page: 1,
      size: 50
    })
    let rows = res.data.data?.records || []
    if (q) {
      rows = rows.filter(
        (r) => r.resNo.includes(q) || r.guestName.includes(q) || r.guestPhone.includes(q)
      )
    }
    resList.value = rows
    if (rows.length === 0) {
      ElMessage.info('未找到符合条件的已确认预订')
    }
  } catch (e: unknown) {
    ElMessage.error(apiErrorMessage(e, '查询预订失败'))
  }
}

async function selectReservation(row: ReservationVO) {
  if (row.status !== 'CONFIRMED') {
    ElMessage.warning('仅已确认预订可入住')
    return
  }
  selectedRes.value = row
  resCheckIn.reservationId = row.id
  resCheckIn.remark = ''
  try {
    const res = await listAvailabilityApi({
      roomTypeId: row.roomTypeId,
      arrival: row.arrivalDate,
      departure: row.departureDate,
      arrivalAt: toIsoDateTime(row.arrivalDate, row.arrivalAt, DEFAULT_ARRIVAL_TIME),
      departureAt: toIsoDateTime(row.departureDate, row.departureAt, DEFAULT_DEPARTURE_TIME),
      excludeReservationId: row.id
    })
    resRooms.value = res.data.data || []
    applyPreAssignedRoom(row)
    if (row.roomId && resCheckIn.roomId !== row.roomId) {
      ElMessage.warning('原预排房当前不可入住，请另选客房')
    }
    onResRoomChange(resCheckIn.roomId)
  } catch (e: unknown) {
    ElMessage.error(apiErrorMessage(e, '加载可售房失败'))
  }
}

function applyPreAssignedRoom(row: ReservationVO) {
  if (!row.roomId) {
    resCheckIn.roomId = undefined
    resCheckIn.agreedDailyRate = row.rackRate != null ? Number(row.rackRate) : undefined
    return
  }
  resCheckIn.roomId = row.roomId
  if (!resRooms.value.some((r) => r.roomId === row.roomId) && row.roomNo) {
    resRooms.value = [
      {
        roomId: row.roomId,
        roomNo: row.roomNo,
        roomTypeId: row.roomTypeId,
        roomTypeName: row.roomTypeName || '',
        rackRate: row.rackRate,
        floorNo: 0,
        version: 0
      },
      ...resRooms.value
    ]
  }
  resCheckIn.agreedDailyRate = row.rackRate != null ? Number(row.rackRate) : undefined
}

function onResRoomChange(roomId?: number) {
  if (!roomId) return
  const room = resRooms.value.find((r) => r.roomId === roomId)
  if (room?.rackRate != null) {
    resCheckIn.agreedDailyRate = Number(room.rackRate)
  } else if (selectedRes.value?.rackRate != null) {
    resCheckIn.agreedDailyRate = Number(selectedRes.value.rackRate)
  }
}

async function submitResCheckIn() {
  if (!resCheckIn.reservationId) return
  if (!resCheckIn.roomId) {
    ElMessage.warning('请选择入住客房')
    return
  }
  if (Math.abs(resPayAmount.value - resChargeable.value) > 0.009) {
    ElMessage.warning('收款金额须等于应付房费')
    return
  }
  resCheckIn.payments = [{ method: resPayMethod.value, amount: resPayAmount.value }]
  try {
    await checkInFromReservation(resCheckIn)
    ElMessage.success('预订入住成功')
    resCheckIn.reservationId = undefined
    selectedRes.value = null
    await searchReservations()
  } catch (e: unknown) {
    ElMessage.error(apiErrorMessage(e, '入住失败'))
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.shift-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filters {
  margin-bottom: 12px;
}
.hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  margin-left: 8px;
}
.charge-total {
  font-size: 18px;
  font-weight: 600;
  color: #e6a23c;
}
.pay-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
