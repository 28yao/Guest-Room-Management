<template>
  <div>
    <div class="toolbar">
      <h3>保洁任务</h3>
      <div class="toolbar-actions">
        <el-select v-model="floorFilter" clearable placeholder="全部楼层" style="width: 120px" @change="load">
          <el-option v-for="f in floors" :key="f" :label="`${f} 楼`" :value="f" />
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </div>

    <el-alert
      v-if="!canComplete"
      type="info"
      :closable="false"
      show-icon
      title="当前账号无「完成保洁」权限，仅可查看待扫列表。"
      style="margin-bottom: 12px"
    />

    <el-table v-loading="loading" :data="list" border empty-text="暂无待打扫任务">
      <el-table-column prop="roomNo" label="房号" width="90" />
      <el-table-column prop="floorNo" label="楼层" width="70" />
      <el-table-column prop="roomTypeName" label="房型" />
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag type="warning">{{ row.status === 'PENDING' ? '待打扫' : row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="canComplete"
            link
            type="primary"
            :loading="completingId === row.id"
            @click="confirmComplete(row)"
          >
            完成打扫
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { listRoomFloorsApi } from '@/api/room'
import { listHkTasksApi, completeHkTaskApi, type HkTaskVO } from '@/api/hk'

const auth = useAuthStore()
const canComplete = computed(() => auth.hasPermission('hk:complete'))

const loading = ref(false)
const completingId = ref<number | null>(null)
const list = ref<HkTaskVO[]>([])
const floors = ref<number[]>([])
const floorFilter = ref<number | undefined>()

function formatTime(iso?: string) {
  if (!iso) return '—'
  return iso.replace('T', ' ').slice(0, 16)
}

async function loadFloors() {
  const res = await listRoomFloorsApi()
  floors.value = res.data.data || []
}

async function load() {
  loading.value = true
  try {
    const res = await listHkTasksApi(floorFilter.value, 'PENDING')
    list.value = res.data.data || []
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '加载保洁任务失败')
  } finally {
    loading.value = false
  }
}

async function confirmComplete(row: HkTaskVO) {
  try {
    await ElMessageBox.confirm(`确认客房 ${row.roomNo} 已完成打扫？房态将变为空净。`, '完成打扫', {
      type: 'warning'
    })
  } catch {
    return
  }
  completingId.value = row.id
  try {
    await completeHkTaskApi(row.id)
    ElMessage.success('已完成打扫')
    await load()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    completingId.value = null
  }
}

onMounted(async () => {
  await loadFloors()
  await load()
})
</script>

