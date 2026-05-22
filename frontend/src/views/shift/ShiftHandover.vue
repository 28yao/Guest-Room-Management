<template>
  <div>
    <div class="toolbar">
      <h3>开班 / 结班</h3>
      <div class="toolbar-actions">
        <el-button v-if="canOpen && !shift" type="primary" @click="onOpenShift">开班</el-button>
        <el-button @click="refresh">刷新</el-button>
      </div>
    </div>

    <el-card class="status-card">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="当前班次">
          <el-tag v-if="shift" type="success">已开班 #{{ shift.id }}</el-tag>
          <el-tag v-else type="warning">未开班</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开班时间">
          {{ shift ? formatTime(shift.openedAt) : '—' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <template v-if="shift && canClose">
      <el-card v-loading="previewLoading" class="section-card">
        <template #header>本班收款汇总</template>
        <el-descriptions v-if="preview" :column="3" border>
          <el-descriptions-item label="现金">¥{{ formatMoney(preview.cashTotal) }}</el-descriptions-item>
          <el-descriptions-item label="微信">¥{{ formatMoney(preview.wechatTotal) }}</el-descriptions-item>
          <el-descriptions-item label="支付宝">¥{{ formatMoney(preview.alipayTotal) }}</el-descriptions-item>
        </el-descriptions>
        <p v-else class="hint">加载预览中…</p>
      </el-card>

      <el-card v-if="preview" class="section-card">
        <template #header>
          待办交接
          <el-tag v-if="preview.pendingCount > 0" type="danger" size="small" style="margin-left: 8px">
            {{ preview.pendingCount }} 项
          </el-tag>
          <el-tag v-else type="success" size="small" style="margin-left: 8px">无待办</el-tag>
        </template>
        <el-table v-if="preview.pendingItems.length" :data="preview.pendingItems" border size="small">
          <el-table-column prop="title" label="类型" width="100" />
          <el-table-column prop="detail" label="说明" />
        </el-table>
        <p v-else class="hint">当前无未退房、待打扫或未释放预订。</p>
      </el-card>

      <div class="close-row">
        <el-checkbox v-if="canForceClose && preview?.pendingCount" v-model="forceClose">
          强制结班（忽略待办）
        </el-checkbox>
        <el-button
          type="primary"
          :loading="closing"
          :disabled="closeDisabled"
          @click="confirmClose"
        >
          确认结班
        </el-button>
      </div>
    </template>

    <el-alert v-else-if="!canClose" type="info" :closable="false" title="无结班权限，请联系管理员。" />

    <el-card v-if="lastHandover" class="section-card">
      <template #header>最近一次结班</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="结班单号">{{ lastHandover.handoverId }}</el-descriptions-item>
        <el-descriptions-item label="结班时间">{{ formatTime(lastHandover.closedAt) }}</el-descriptions-item>
        <el-descriptions-item label="现金">¥{{ formatMoney(lastHandover.cashTotal) }}</el-descriptions-item>
        <el-descriptions-item label="微信">¥{{ formatMoney(lastHandover.wechatTotal) }}</el-descriptions-item>
        <el-descriptions-item label="支付宝">¥{{ formatMoney(lastHandover.alipayTotal) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  openShift,
  getCurrentShift,
  getHandoverPreview,
  closeShift,
  type ShiftSessionVO,
  type ShiftHandoverPreviewVO,
  type ShiftHandoverVO
} from '@/api/shift'

const auth = useAuthStore()
const canOpen = computed(() => auth.hasPermission('shift:open'))
const canClose = computed(() => auth.hasPermission('shift:close'))
const canForceClose = computed(() => auth.hasPermission('shift:force_close'))

const shift = ref<ShiftSessionVO | null>(null)
const preview = ref<ShiftHandoverPreviewVO | null>(null)
const previewLoading = ref(false)
const closing = ref(false)
const forceClose = ref(false)
const lastHandover = ref<ShiftHandoverVO | null>(null)

const closeDisabled = computed(() => {
  if (!preview.value) return true
  if (preview.value.pendingCount > 0 && preview.value.blockCloseOnPending && !forceClose.value) {
    return true
  }
  return false
})

function formatTime(iso?: string) {
  if (!iso) return '—'
  return iso.replace('T', ' ').slice(0, 16)
}

function formatMoney(v?: number) {
  return Number(v ?? 0).toFixed(2)
}

async function refresh() {
  const res = await getCurrentShift()
  shift.value = res.data.data
  preview.value = null
  if (shift.value && canClose.value) {
    await loadPreview()
  }
}

async function loadPreview() {
  if (!shift.value) return
  previewLoading.value = true
  try {
    const res = await getHandoverPreview(shift.value.id)
    preview.value = res.data.data
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载结班预览失败')
  } finally {
    previewLoading.value = false
  }
}

async function onOpenShift() {
  const res = await openShift()
  shift.value = res.data.data
  ElMessage.success('开班成功')
  await loadPreview()
}

async function confirmClose() {
  if (!shift.value || !preview.value) return
  const pendingHint =
    preview.value.pendingCount > 0
      ? `仍有 ${preview.value.pendingCount} 项待办。${forceClose.value ? '将强制结班。' : ''}`
      : ''
  try {
    await ElMessageBox.confirm(`确认结班？${pendingHint}`, '结班', { type: 'warning' })
  } catch {
    return
  }
  closing.value = true
  try {
    const res = await closeShift(shift.value.id, forceClose.value)
    lastHandover.value = res.data.data
    ElMessage.success('结班成功')
    shift.value = null
    preview.value = null
    forceClose.value = false
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '结班失败')
  } finally {
    closing.value = false
  }
}

watch(forceClose, () => {
  /* 触发 closeDisabled 重算 */
})

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.close-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}
</style>
