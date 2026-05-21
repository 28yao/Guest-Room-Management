<template>
  <div>
    <div class="toolbar">
      <h3>在住管理</h3>
      <div class="toolbar-actions">
        <el-input
          v-model="guestNameQuery"
          placeholder="客人姓名"
          clearable
          style="width: 160px"
          @keyup.enter="load"
          @clear="load"
        />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="load">刷新</el-button>
      </div>
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
      <el-table-column label="账单" width="140">
        <template #default="{ row }">
          应付 ¥{{ row.folioTotalAmount ?? 0 }}
          <span v-if="(row.folioPaidAmount ?? 0) > 0" class="paid-hint"> / 已收 ¥{{ row.folioPaidAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canCheckout" link type="primary" @click="confirmCheckout(row)">退房</el-button>
          <el-button v-if="canChangeRoom" link type="primary" @click="openChangeRoom(row)">换房</el-button>
          <el-button v-if="canVoidCheckout" link type="danger" @click="openVoidCheckout(row)">退款</el-button>
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

    <el-dialog v-model="voidVisible" title="退款 — 提前结束住宿" width="480px">
      <el-alert type="info" :closable="false" show-icon title="将按计费截止日重算房费；多收部分可退款并记入当班。" />
      <el-form label-width="110px" style="margin-top: 12px">
        <el-form-item label="在住单号">
          <span>{{ voidTarget?.stayNo }}</span>
        </el-form-item>
        <el-form-item label="计费截止日" required>
          <el-date-picker
            v-model="voidForm.chargeThroughDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="refreshVoidRefund"
          />
        </el-form-item>
        <el-form-item v-if="voidPreview" label="应付预览">
          <span>{{ voidPreview.nights }} 晚，应付 ¥{{ voidPreview.chargeable }}（已收 ¥{{ voidTarget?.folioPaidAmount ?? 0 }}）</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input-number v-model="voidForm.refundAmount" :min="0" :precision="2" style="width: 100%" />
          <div class="form-hint">已按「已收 − 应付」自动填写，可手工修改</div>
        </el-form-item>
        <el-form-item label="退款方式" required>
          <el-select v-model="voidForm.refundMethod" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="voidForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="voidVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="submitVoidCheckout">确认退订</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { listInHouse, changeRoom, updateStayRemark, voidCheckout, checkoutStay, type StayVO } from '@/api/stay'
import { listAvailabilityApi, type AvailableRoomVO } from '@/api/reservation'
import { getCurrentShift } from '@/api/shift'
import { combineDateTime, DEFAULT_ARRIVAL_TIME, DEFAULT_DEPARTURE_TIME } from '@/utils/datetime'
import { computeRefundPreview } from '@/utils/billing'

const auth = useAuthStore()
const canChangeRoom = auth.hasPermission('stay:change_room')
const canCheckout = auth.hasPermission('billing:checkout')
const canVoidCheckout = auth.hasPermission('billing:checkout')

async function confirmCheckout(row: StayVO) {
  try {
    await ElMessageBox.confirm(
      `确认为 ${row.guestName}（${row.roomNo}）办理退房？客房将置为脏房。房费已在入住时结清。`,
      '退房',
      { type: 'warning' }
    )
  } catch {
    return
  }
  saving.value = true
  try {
    await checkoutStay(row.id)
    ElMessage.success('已退房')
    await load()
  } catch (e: unknown) {
    const err = e as { message?: string; response?: { data?: { message?: string } } }
    ElMessage.error(err.message || err.response?.data?.message || '退房失败')
  } finally {
    saving.value = false
  }
}
const list = ref<StayVO[]>([])
const guestNameQuery = ref('')
const changeVisible = ref(false)
const remarkVisible = ref(false)
const voidVisible = ref(false)
const saving = ref(false)
const currentStay = ref<StayVO | null>(null)
const voidTarget = ref<StayVO | null>(null)
const changeForm = ref({ targetRoomId: undefined as number | undefined })
const targetRooms = ref<AvailableRoomVO[]>([])
const remarkText = ref('')

const voidPreview = ref<{ nights: number; chargeable: number; refund: number } | null>(null)

const voidForm = ref({
  chargeThroughDate: '',
  refundAmount: 0,
  refundMethod: 'CASH',
  remark: ''
})

function todayString() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

onMounted(() => load())

async function load() {
  const name = guestNameQuery.value.trim()
  const res = await listInHouse(name || undefined)
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

function refreshVoidRefund() {
  const row = voidTarget.value
  if (!row || !voidForm.value.chargeThroughDate) {
    voidForm.value.refundAmount = 0
    voidPreview.value = null
    return
  }
  const preview = computeRefundPreview(
    Number(row.folioPaidAmount ?? 0),
    Number(row.agreedDailyRate ?? 0),
    row.arrivalDate,
    row.departureDate,
    voidForm.value.chargeThroughDate
  )
  voidPreview.value = preview
  voidForm.value.refundAmount = preview.refund
}

function openVoidCheckout(row: StayVO) {
  voidTarget.value = row
  voidForm.value = {
    chargeThroughDate: todayString(),
    refundAmount: 0,
    refundMethod: 'CASH',
    remark: ''
  }
  refreshVoidRefund()
  voidVisible.value = true
}

async function submitVoidCheckout() {
  if (!voidTarget.value || !voidForm.value.chargeThroughDate) {
    ElMessage.warning('请选择计费截止日')
    return
  }
  try {
    const shiftRes = await getCurrentShift()
    if (!shiftRes.data.data) {
      ElMessage.warning('请先开班后再办理退款')
      return
    }
  } catch {
    ElMessage.warning('请先开班后再办理退订退款')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确认提前退房？客房将置为脏房，账单将按截止日结清。',
      '退款',
      { type: 'warning' }
    )
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = {
      chargeThroughDate: voidForm.value.chargeThroughDate,
      refundMethod: voidForm.value.refundMethod,
      remark: voidForm.value.remark,
      refundAmount: voidForm.value.refundAmount ?? 0
    }
    await voidCheckout(voidTarget.value.id, payload)
    ElMessage.success('已办理退款')
    voidVisible.value = false
    await load()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
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
.toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.paid-hint {
  color: #909399;
  font-size: 12px;
}
.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
