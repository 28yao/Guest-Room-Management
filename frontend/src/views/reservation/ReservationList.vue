<template>
  <div>
    <div class="toolbar">
      <h3>预订管理</h3>
      <el-button type="primary" @click="openCreate">新建预订</el-button>
    </div>

    <el-form :inline="true" class="filters">
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部" style="width: 120px">
          <el-option v-for="(label, key) in RES_STATUS_LABEL" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="入住日">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="起"
          end-placeholder="止"
        />
      </el-form-item>
      <el-form-item label="手机">
        <el-input v-model="filters.guestPhone" clearable placeholder="模糊查询" style="width: 140px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" border>
      <el-table-column prop="resNo" label="预订单号" width="160" />
      <el-table-column prop="guestName" label="客人" width="100" />
      <el-table-column prop="guestPhone" label="电话" width="120" />
      <el-table-column prop="roomTypeName" label="房型" />
      <el-table-column prop="roomNo" label="房号" width="80">
        <template #default="{ row }">{{ row.roomNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="入住/离店" width="260">
        <template #default="{ row }">
          {{ formatStayRange(row) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag>{{ RES_STATUS_LABEL[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canEdit(row)" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canAssign(row)" link type="primary" @click="openAssign(row)">预排房</el-button>
          <el-button v-if="canRelease(row)" link type="warning" @click="onRelease(row)">释放</el-button>
          <el-button v-if="canCancel(row)" link type="danger" @click="onCancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="pageSize"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      @current-change="load"
    />

    <el-dialog v-model="formVisible" :title="formTitle" width="520px" @closed="resetForm">
      <el-form :model="form" label-width="90px">
        <el-form-item label="客人姓名" required>
          <el-input v-model="form.guestName" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="form.guestPhone" />
        </el-form-item>
        <el-form-item label="房型" required>
          <el-select v-model="form.roomTypeId" style="width: 100%">
            <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入住日期" required>
          <el-date-picker v-model="form.arrivalDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入住时刻">
          <el-time-picker
            v-model="form.arrivalTime"
            value-format="HH:mm:ss"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="离店日期" required>
          <el-date-picker v-model="form.departureDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="离店时刻">
          <el-time-picker
            v-model="form.departureTime"
            value-format="HH:mm:ss"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="预排房" width="480px">
      <p class="hint">可售空净房（{{ assignRow ? formatStayRange(assignRow) : '' }}）</p>
      <el-select v-model="selectedRoomId" placeholder="选择房号" style="width: 100%" filterable>
        <el-option
          v-for="r in availableRooms"
          :key="r.roomId"
          :label="`${r.roomNo}（${r.floorNo}楼）`"
          :value="r.roomId"
        />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" :disabled="!selectedRoomId" @click="confirmAssign">
          确认排房
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRoomTypesApi, type RoomTypeVO } from '@/api/roomType'
import {
  RES_STATUS_LABEL,
  listReservationsApi,
  createReservationApi,
  updateReservationApi,
  assignRoomApi,
  cancelReservationApi,
  releaseReservationApi,
  listAvailabilityApi,
  type ReservationVO
} from '@/api/reservation'

const list = ref<ReservationVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const types = ref<RoomTypeVO[]>([])
const dateRange = ref<string[]>([])

const filters = reactive({
  status: '',
  guestPhone: ''
})

const formVisible = ref(false)
const formTitle = ref('新建预订')
const editingId = ref<number | null>(null)
const saving = ref(false)

const DEFAULT_ARRIVAL_TIME = '18:00:00'
const DEFAULT_DEPARTURE_TIME = '12:00:00'

const form = reactive({
  guestName: '',
  guestPhone: '',
  roomTypeId: undefined as number | undefined,
  arrivalDate: '',
  departureDate: '',
  arrivalTime: DEFAULT_ARRIVAL_TIME,
  departureTime: DEFAULT_DEPARTURE_TIME,
  remark: ''
})

const assignVisible = ref(false)
const assignRow = ref<ReservationVO | null>(null)
const availableRooms = ref<{ roomId: number; roomNo: string; floorNo: number }[]>([])
const selectedRoomId = ref<number | undefined>()
const assigning = ref(false)

function canEdit(row: ReservationVO) {
  return row.status === 'CONFIRMED' || row.status === 'PENDING'
}

function canAssign(row: ReservationVO) {
  return row.status === 'CONFIRMED' || row.status === 'PENDING'
}

function canRelease(row: ReservationVO) {
  return row.status === 'CONFIRMED' || row.status === 'PENDING'
}

function canCancel(row: ReservationVO) {
  return row.status === 'CONFIRMED' || row.status === 'PENDING'
}

function formatDate(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function defaultDates() {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  const dayAfter = new Date(tomorrow)
  dayAfter.setDate(dayAfter.getDate() + 1)
  form.arrivalDate = formatDate(tomorrow)
  form.departureDate = formatDate(dayAfter)
  form.arrivalTime = DEFAULT_ARRIVAL_TIME
  form.departureTime = DEFAULT_DEPARTURE_TIME
}

function combineDateTime(date: string, time: string) {
  const t = time && time.length >= 5 ? time.substring(0, 8) : DEFAULT_ARRIVAL_TIME
  return `${date}T${t.length === 5 ? t + ':00' : t}`
}

function formatStayRange(row: ReservationVO) {
  const start = row.arrivalAt || `${row.arrivalDate} ${DEFAULT_ARRIVAL_TIME}`
  const end = row.departureAt || `${row.departureDate} ${DEFAULT_DEPARTURE_TIME}`
  return `${start.replace('T', ' ')} ~ ${end.replace('T', ' ')}`
}

function parseTimeFromIso(iso?: string, fallback = DEFAULT_ARRIVAL_TIME) {
  if (!iso) return fallback
  const part = iso.includes('T') ? iso.split('T')[1] : iso
  return part.length >= 8 ? part.substring(0, 8) : fallback
}

async function load() {
  const params: Record<string, string | number> = {
    page: page.value,
    size: pageSize.value
  }
  if (filters.status) params.status = filters.status
  if (filters.guestPhone) params.guestPhone = filters.guestPhone
  if (dateRange.value?.length === 2) {
    params.arrivalFrom = dateRange.value[0]
    params.arrivalTo = dateRange.value[1]
  }
  const res = await listReservationsApi(params)
  list.value = res.data.data.records
  total.value = res.data.data.total
}

function openCreate() {
  editingId.value = null
  formTitle.value = '新建预订'
  resetForm()
  formVisible.value = true
}

function openEdit(row: ReservationVO) {
  editingId.value = row.id
  formTitle.value = '编辑预订'
  form.guestName = row.guestName
  form.guestPhone = row.guestPhone
  form.roomTypeId = row.roomTypeId
  form.arrivalDate = row.arrivalDate
  form.departureDate = row.departureDate
  form.arrivalTime = parseTimeFromIso(row.arrivalAt, DEFAULT_ARRIVAL_TIME)
  form.departureTime = parseTimeFromIso(row.departureAt, DEFAULT_DEPARTURE_TIME)
  form.remark = row.remark || ''
  formVisible.value = true
}

function resetForm() {
  form.guestName = ''
  form.guestPhone = ''
  form.roomTypeId = types.value[0]?.id
  form.remark = ''
  form.arrivalTime = DEFAULT_ARRIVAL_TIME
  form.departureTime = DEFAULT_DEPARTURE_TIME
  defaultDates()
}

async function saveForm() {
  if (!form.guestName || !form.guestPhone || !form.roomTypeId || !form.arrivalDate || !form.departureDate) {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  try {
    const payload = {
      guestName: form.guestName,
      guestPhone: form.guestPhone,
      roomTypeId: form.roomTypeId!,
      arrivalDate: form.arrivalDate,
      departureDate: form.departureDate,
      arrivalAt: combineDateTime(form.arrivalDate, form.arrivalTime),
      departureAt: combineDateTime(form.departureDate, form.departureTime),
      remark: form.remark
    }
    if (editingId.value) {
      await updateReservationApi(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await createReservationApi(payload)
      ElMessage.success('预订已创建')
    }
    formVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function openAssign(row: ReservationVO) {
  assignRow.value = row
  selectedRoomId.value = row.roomId
  assignVisible.value = true
  const res = await listAvailabilityApi({
    roomTypeId: row.roomTypeId,
    arrival: row.arrivalDate,
    departure: row.departureDate,
    arrivalAt: row.arrivalAt,
    departureAt: row.departureAt,
    excludeReservationId: row.id
  })
  availableRooms.value = res.data.data
  if (availableRooms.value.length === 0) {
    ElMessage.warning('当前日期段无可排空净房')
  }
}

async function confirmAssign() {
  if (!assignRow.value || !selectedRoomId.value) return
  assigning.value = true
  try {
    await assignRoomApi(assignRow.value.id, selectedRoomId.value)
    ElMessage.success('预排房成功')
    assignVisible.value = false
    await load()
  } finally {
    assigning.value = false
  }
}

async function onRelease(row: ReservationVO) {
  try {
    await ElMessageBox.confirm('确认释放该预订并解除房态锁定？无罚金。', '释放预订', { type: 'warning' })
    let noShow = false
    try {
      await ElMessageBox.confirm('是否标记为 No-show？', 'No-show', {
        confirmButtonText: '标记 No-show',
        cancelButtonText: '普通释放',
        distinguishCancelAndClose: true,
        type: 'info'
      })
      noShow = true
    } catch (action: unknown) {
      if (action !== 'cancel') return
    }
    await releaseReservationApi(row.id, noShow)
    ElMessage.success(noShow ? '已释放并标记 No-show' : '已释放')
    await load()
  } catch {
    /* 用户取消 */
  }
}

async function onCancel(row: ReservationVO) {
  try {
    await ElMessageBox.confirm('确认取消该预订？无罚金。', '取消预订', { type: 'warning' })
    await cancelReservationApi(row.id)
    ElMessage.success('已取消')
    await load()
  } catch {
    /* 用户取消 */
  }
}

onMounted(async () => {
  const t = await listRoomTypesApi()
  types.value = t.data.data
  resetForm()
  await load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.filters {
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.hint {
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
}
</style>
