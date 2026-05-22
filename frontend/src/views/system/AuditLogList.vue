<template>
  <div>
    <div class="toolbar">
      <h3>操作审计</h3>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-form :inline="true" class="filter-form">
      <el-form-item label="业务类型">
        <el-select v-model="filters.bizType" clearable placeholder="全部" style="width: 140px">
          <el-option label="预订" value="RESERVATION" />
          <el-option label="在住" value="STAY" />
          <el-option label="账单" value="FOLIO" />
          <el-option label="客房" value="ROOM" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作类型">
        <el-input v-model="filters.operationType" clearable placeholder="如 FOLIO_ADJUST_PRICE" style="width: 180px" />
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          value-format="YYYY-MM-DD"
          start-placeholder="开始"
          end-placeholder="结束"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" border size="small" @row-click="onRowClick">
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column prop="operatorName" label="操作人" width="100" />
      <el-table-column prop="bizType" label="业务" width="100" />
      <el-table-column prop="bizId" label="业务ID" width="80" />
      <el-table-column prop="operationType" label="操作" width="160" />
      <el-table-column prop="summary" label="摘要" min-width="140" show-overflow-tooltip />
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      @current-change="load"
      @size-change="load"
    />

    <el-drawer v-model="detailVisible" title="审计详情" size="480px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="时间">{{ detail.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="业务">{{ detail.bizType }} #{{ detail.bizId }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.operationType }}</el-descriptions-item>
        <el-descriptions-item label="摘要">{{ detail.summary || '—' }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detail" class="json-block">
        <p class="json-title">变更前</p>
        <pre>{{ formatJson(detail.beforeValue) }}</pre>
        <p class="json-title">变更后</p>
        <pre>{{ formatJson(detail.afterValue) }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listAuditLogs, type OperationLogVO } from '@/api/audit'

const loading = ref(false)
const records = ref<OperationLogVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const dateRange = ref<[string, string] | null>(null)
const filters = reactive({ bizType: '', operationType: '' })
const detailVisible = ref(false)
const detail = ref<OperationLogVO | null>(null)

function formatJson(raw?: string) {
  if (!raw) return '—'
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

async function load() {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      page: page.value,
      size: size.value
    }
    if (filters.bizType) params.bizType = filters.bizType
    if (filters.operationType) params.operationType = filters.operationType
    if (dateRange.value && dateRange.value.length === 2) {
      params.from = dateRange.value[0]
      params.to = dateRange.value[1]
    }
    const res = await listAuditLogs(params)
    records.value = res.data.data.records
    total.value = res.data.data.total
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function onRowClick(row: OperationLogVO) {
  detail.value = row
  detailVisible.value = true
}

onMounted(() => load())
</script>

<style scoped>
.json-block {
  margin-top: 16px;
}
.json-title {
  font-weight: 600;
  margin: 8px 0 4px;
}
pre {
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
  font-size: 12px;
  overflow: auto;
}
</style>
