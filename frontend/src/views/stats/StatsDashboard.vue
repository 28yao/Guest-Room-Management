<template>
  <div>
    <div class="toolbar">
      <h3>经营统计</h3>
      <el-button @click="refresh">刷新</el-button>
    </div>

    <el-row :gutter="16" v-loading="occupancyLoading">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">客房总数</div>
          <div class="stat-value">{{ occupancy?.totalRooms ?? '—' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">可售间数</div>
          <div class="stat-value">{{ occupancy?.sellableRooms ?? '—' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">在住间数</div>
          <div class="stat-value">{{ occupancy?.inHouseRooms ?? '—' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">出租率</div>
          <div class="stat-value accent">{{ formatRate(occupancy?.occupancyRate) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card" v-loading="revenueLoading">
      <template #header>
        <div class="revenue-header">
          <span>房费营收（支付流水净额）</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledFuture"
            @change="loadRevenue"
          />
        </div>
      </template>
      <el-descriptions v-if="revenue" :column="4" border>
        <el-descriptions-item label="合计">¥{{ formatMoney(revenue.totalRevenue) }}</el-descriptions-item>
        <el-descriptions-item label="现金">¥{{ formatMoney(revenue.cashTotal) }}</el-descriptions-item>
        <el-descriptions-item label="微信">¥{{ formatMoney(revenue.wechatTotal) }}</el-descriptions-item>
        <el-descriptions-item label="支付宝">¥{{ formatMoney(revenue.alipayTotal) }}</el-descriptions-item>
      </el-descriptions>
      <el-table
        v-if="revenue?.dailyItems?.length"
        :data="revenue.dailyItems"
        border
        size="small"
        class="daily-table"
      >
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column label="营收">
          <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getOccupancyStats,
  getRevenueStats,
  type OccupancyStatsVO,
  type RevenueStatsVO
} from '@/api/stats'

const occupancyLoading = ref(false)
const revenueLoading = ref(false)
const occupancy = ref<OccupancyStatsVO | null>(null)
const revenue = ref<RevenueStatsVO | null>(null)
const dateRange = ref<[string, string]>(defaultRange())

function defaultRange(): [string, string] {
  const today = formatDate(new Date())
  return [today, today]
}

function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function disabledFuture(d: Date) {
  return d.getTime() > Date.now()
}

function formatMoney(v?: number) {
  if (v == null || Number.isNaN(v)) return '0.00'
  return Number(v).toFixed(2)
}

function formatRate(v?: number) {
  if (v == null || Number.isNaN(v)) return '—'
  return `${Number(v).toFixed(2)}%`
}

async function loadOccupancy() {
  occupancyLoading.value = true
  try {
    const res = await getOccupancyStats()
    occupancy.value = res.data.data
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载出租率失败')
  } finally {
    occupancyLoading.value = false
  }
}

async function loadRevenue() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  revenueLoading.value = true
  try {
    const res = await getRevenueStats(dateRange.value[0], dateRange.value[1])
    revenue.value = res.data.data
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载营收失败')
  } finally {
    revenueLoading.value = false
  }
}

async function refresh() {
  await loadOccupancy()
  await loadRevenue()
}

onMounted(() => {
  refresh()
})
</script>

<style scoped>
.revenue-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.daily-table {
  margin-top: 16px;
}
</style>
