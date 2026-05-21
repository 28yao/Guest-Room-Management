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
        :style="{ borderColor: occupancyColor(room.status) }"
        @click="openActions(room)"
      >
        <div class="room-no">{{ room.roomNo }}</div>
        <div class="room-type">{{ room.roomTypeName }}</div>
        <div class="card-tags">
          <el-tag size="small" :color="occupancyColor(room.status)" effect="dark">
            {{ occupancyLabel(room.status) }}
          </el-tag>
          <el-tag size="small" :color="cleanColor(room.cleanStatus)" effect="plain">
            {{ cleanLabel(room.cleanStatus) }}
          </el-tag>
        </div>
        <div
          v-if="room.status !== room.occupancyStatus"
          class="card-actual"
        >
          库内 {{ occupancyLabel(room.occupancyStatus) }} · {{ cleanLabel(room.cleanStatus) }}
        </div>
        <div class="tags">
          <el-tag v-if="room.dailyTags?.includes('EXPECTED_ARRIVAL')" size="small" type="warning">预抵</el-tag>
          <el-tag v-if="room.dailyTags?.includes('EXPECTED_DEPARTURE')" size="small" type="danger">预离</el-tag>
        </div>
      </div>
    </div>

    <el-dialog v-model="actionVisible" :title="`客房 ${selected?.roomNo || ''}`" width="720px" @open="onDialogOpen">
      <p class="room-meta">
        <span>{{ selected?.roomTypeName }}</span>
        <el-tag size="small" class="meta-tag">展示 {{ occupancyLabel(selected?.status || '') }}</el-tag>
        <el-tag size="small" type="info">
          库内 {{ occupancyLabel(roomOccupancyStatus) }} · {{ cleanLabel(roomCleanStatus) }}
        </el-tag>
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
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.editable" link type="primary" @click="openEditOrder(row)">修改</el-button>
            <el-button
              v-if="row.orderType === 'RESERVATION' && canCheckIn && isResCheckInable(row.status)"
              link
              type="success"
              @click="openResCheckInFromOrder(row)"
            >
              预订入住
            </el-button>
            <el-button
              v-if="row.orderType === 'STAY' && row.status === 'IN_HOUSE' && canChangeRoom"
              link
              type="primary"
              @click="openChangeRoomFromOrder(row)"
            >
              换房
            </el-button>
            <el-button
              v-if="row.orderType === 'STAY' && row.status === 'IN_HOUSE' && canCheckout"
              link
              type="primary"
              @click="confirmCheckoutFromOrder(row)"
            >
              退房
            </el-button>
            <el-button
              v-if="row.orderType === 'STAY' && row.status === 'IN_HOUSE' && canVoidCheckout"
              link
              type="danger"
              @click="openVoidFromOrder(row)"
            >
              退款
            </el-button>
            <el-button
              v-if="row.orderType === 'RESERVATION' && canManageRes && isResCancellable(row.status)"
              link
              type="danger"
              @click="openCancelRefundFromOrder(row)"
            >
              退款
            </el-button>
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
          v-if="auth.hasPermission('stay:checkin') && canWalkIn"
          type="success"
          size="small"
          @click="openQuickWalkIn"
        >
          快速 Walk-in 入住
        </el-button>
        <span v-if="auth.hasPermission('stay:checkin') && !canWalkIn" class="hint-inline">
          仅空房/预订且净房可 Walk-in（当前库内 {{ occupancyLabel(roomOccupancyStatus) }} ·
          {{ cleanLabel(roomCleanStatus) }}）
        </span>
      </div>

      <el-divider />
      <div class="section-title">快捷操作</div>
      <div class="quick-status-row">
        <el-button
          v-if="canCheckIn && primaryConfirmableRes"
          type="success"
          size="large"
          @click="openResCheckInFromOrder(primaryConfirmableRes)"
        >
          预订入住
        </el-button>
        <el-button
          v-if="canCheckout && primaryInHouseOrder"
          type="primary"
          size="large"
          :loading="saving"
          @click="confirmCheckoutFromOrder(primaryInHouseOrder)"
        >
          退房
        </el-button>
        <el-button
          v-if="canToggleCleanDirty"
          :type="roomCleanStatus === 'DIRTY' ? 'success' : 'warning'"
          size="large"
          :loading="saving || scheduleLoading"
          @click="toggleCleanDirty"
        >
          {{ cleanDirtyToggleLabel }}
        </el-button>
        <span v-else-if="scheduleLoading" class="hint-inline">加载客房状态…</span>
        <span
          v-else-if="
            !canToggleCleanDirty &&
            !(canCheckout && primaryInHouseOrder) &&
            !(canCheckIn && primaryConfirmableRes)
          "
          class="hint-inline"
        >
          无可用快捷操作权限
        </span>
      </div>

      <el-divider />
      <div class="section-title">房态操作</div>
      <el-button
        v-if="auth.hasPermission('room:status:maintenance') && roomOccupancyStatus !== 'OUT_OF_ORDER'"
        type="warning"
        @click="openMaintenance"
      >
        设维修
      </el-button>
      <el-button
        v-if="auth.hasPermission('room:status:maintenance') && roomOccupancyStatus === 'OUT_OF_ORDER'"
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
            <el-radio label="CLEAN">净房</el-radio>
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

    <el-dialog v-model="quickResCheckInVisible" title="预订入住" width="520px">
      <el-form label-width="100px">
        <el-form-item label="预订单号">
          <span>{{ quickResCheckInOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="客人">
          <span>{{ quickResCheckInOrder?.guestName }} / {{ quickResCheckInOrder?.guestPhone }}</span>
        </el-form-item>
        <el-form-item label="入住客房">
          <span>{{ schedule?.roomNo }}（{{ schedule?.roomTypeName }}）</span>
        </el-form-item>
        <el-form-item label="入住/离店">
          <span v-if="quickResCheckInOrder">{{ formatOrderRange(quickResCheckInOrder) }}</span>
        </el-form-item>
        <el-form-item label="协议房价">
          <el-input-number v-model="quickResCheckInForm.agreedDailyRate" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="quickResCheckInForm.remark" type="textarea" />
        </el-form-item>
        <el-divider content-position="left">入住结账</el-divider>
        <el-form-item label="应付房费">
          <span class="charge-total">¥{{ quickResCheckInChargeable }}</span>
          <span class="hint-inline">（{{ quickResCheckInNights }} 晚）</span>
        </el-form-item>
        <el-form-item label="收款" required>
          <div class="pay-row">
            <el-select v-model="quickResCheckInPayMethod" style="width: 110px">
              <el-option label="现金" value="CASH" />
              <el-option label="微信" value="WECHAT" />
              <el-option label="支付宝" value="ALIPAY" />
            </el-select>
            <el-input-number v-model="quickResCheckInPayAmount" :min="0.01" :precision="2" style="width: 140px" />
            <el-button link type="primary" @click="quickResCheckInPayAmount = quickResCheckInChargeable">收齐</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickResCheckInVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitQuickResCheckIn">确认入住并结账</el-button>
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
        <el-divider content-position="left">入住结账</el-divider>
        <el-form-item label="应付房费">
          <span class="charge-total">¥{{ quickWalkChargeable }}</span>
          <span class="hint-inline">（{{ quickWalkNights }} 晚）</span>
        </el-form-item>
        <el-form-item label="收款" required>
          <div class="pay-row">
            <el-select v-model="quickWalkPayMethod" style="width: 110px">
              <el-option label="现金" value="CASH" />
              <el-option label="微信" value="WECHAT" />
              <el-option label="支付宝" value="ALIPAY" />
            </el-select>
            <el-input-number v-model="quickWalkPayAmount" :min="0.01" :precision="2" style="width: 140px" />
            <el-button link type="primary" @click="quickWalkPayAmount = quickWalkChargeable">收齐</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="quickWalkVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitQuickWalkIn">确认入住并结账</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="boardChangeVisible" title="换房" width="420px">
      <el-form label-width="90px">
        <el-form-item label="在住单">
          <span>{{ boardChangeOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="目标客房" required>
          <el-select v-model="boardChangeForm.targetRoomId" style="width: 100%" @focus="loadBoardChangeRooms">
            <el-option
              v-for="r in boardChangeRooms"
              :key="r.roomId"
              :label="`${r.roomNo} (${r.roomTypeName})`"
              :value="r.roomId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="boardChangeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitBoardChangeRoom">确认换房</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="boardVoidVisible" title="退款— 在住提前退房" width="480px">
      <el-form label-width="110px">
        <el-form-item label="在住单">
          <span>{{ boardVoidOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="计费截止日" required>
          <el-date-picker
            v-model="boardVoidForm.chargeThroughDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="refreshBoardVoidRefund"
          />
        </el-form-item>
        <el-form-item v-if="boardVoidPreview" label="应付预览">
          <span>{{ boardVoidPreview.nights }} 晚 × 房价，应付 ¥{{ boardVoidPreview.chargeable }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input-number v-model="boardVoidForm.refundAmount" :min="0" :precision="2" style="width: 100%" />
          <div class="form-hint">已按「已收 − 应付」自动填写，可手工修改</div>
        </el-form-item>
        <el-form-item label="退款方式" required>
          <el-select v-model="boardVoidForm.refundMethod" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="boardVoidForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="boardVoidVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="submitBoardVoid">确认退订</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="boardCancelResVisible" title="退款— 取消预订" width="440px">
      <el-form label-width="100px">
        <el-form-item label="预订单号">
          <span>{{ boardCancelResOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input-number v-model="boardCancelResForm.refundAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退款方式" required>
          <el-select v-model="boardCancelResForm.refundMethod" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="boardCancelResForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="boardCancelResVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="submitBoardCancelRefund">确认退订</el-button>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
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
  toggleCleanDirtyApi,
  OCCUPANCY_STATUS_LABEL,
  OCCUPANCY_STATUS_COLOR,
  CLEAN_STATUS_LABEL,
  CLEAN_STATUS_COLOR,
  type RoomBoardItem,
  type RoomScheduleVO,
  type RoomScheduleOrderVO
} from '@/api/room'
import { listRoomTypesApi, type RoomTypeVO } from '@/api/roomType'
import {
  RES_STATUS_LABEL,
  createReservationApi,
  updateReservationApi,
  assignRoomApi,
  cancelWithRefundApi,
  listAvailabilityApi
} from '@/api/reservation'
import { getCurrentShift } from '@/api/shift'
import {
  walkInCheckIn,
  checkInFromReservation,
  changeRoom,
  voidCheckout,
  checkoutStay,
  updateStayRemark,
  getStay,
  type StayVO
} from '@/api/stay'
import { computeCheckInChargeable, computeRefundPreview, computeStayNights } from '@/utils/billing'
import type { AvailableRoomVO } from '@/api/reservation'
import { combineDateTime, DEFAULT_ARRIVAL_TIME, DEFAULT_DEPARTURE_TIME, toIsoDateTime } from '@/utils/datetime'

const auth = useAuthStore()
const canCheckIn = auth.hasPermission('stay:checkin')
const canChangeRoom = auth.hasPermission('stay:change_room')
const canCheckout = auth.hasPermission('billing:checkout')
const canVoidCheckout = auth.hasPermission('billing:checkout')
const canManageRes = auth.hasPermission('reservation:manage')
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
const quickResCheckInVisible = ref(false)
const quickWalkVisible = ref(false)
const quickResCheckInOrder = ref<RoomScheduleOrderVO | null>(null)
const maintVisible = ref(false)
const endMaintVisible = ref(false)
const forceVisible = ref(false)
const boardChangeVisible = ref(false)
const boardVoidVisible = ref(false)
const boardCancelResVisible = ref(false)
const boardChangeOrder = ref<RoomScheduleOrderVO | null>(null)
const boardVoidOrder = ref<RoomScheduleOrderVO | null>(null)
const boardVoidStay = ref<StayVO | null>(null)
const boardVoidPreview = ref<{ nights: number; chargeable: number; refund: number } | null>(null)
const boardCancelResOrder = ref<RoomScheduleOrderVO | null>(null)
const boardChangeRooms = ref<AvailableRoomVO[]>([])
const boardChangeForm = ref({ targetRoomId: undefined as number | undefined })
const boardVoidForm = reactive({
  chargeThroughDate: '',
  refundAmount: undefined as number | undefined,
  refundMethod: 'CASH',
  remark: ''
})
const boardCancelResForm = reactive({
  refundAmount: 0,
  refundMethod: 'CASH',
  remark: ''
})
const saving = ref(false)

const maintForm = reactive({ reason: '', expectedRecoveryAt: '' })
const endMaintForm = reactive({ targetStatus: 'DIRTY' })
const forceForm = reactive({ targetStatus: 'VACANT', reason: '' })

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
const quickWalkPayMethod = ref('CASH')
const quickWalkPayAmount = ref(0)

const quickResCheckInForm = reactive({
  reservationId: undefined as number | undefined,
  roomId: undefined as number | undefined,
  agreedDailyRate: undefined as number | undefined,
  remark: ''
})
const quickResCheckInPayMethod = ref('CASH')
const quickResCheckInPayAmount = ref(0)

const quickWalkNights = computed(() =>
  quickWalkForm.arrivalDate && quickWalkForm.departureDate
    ? computeStayNights(quickWalkForm.arrivalDate, quickWalkForm.departureDate)
    : 0
)
const quickWalkChargeable = computed(() =>
  computeCheckInChargeable(
    Number(quickWalkForm.agreedDailyRate ?? 0),
    quickWalkForm.arrivalDate,
    quickWalkForm.departureDate
  )
)

watch(quickWalkChargeable, (v) => {
  quickWalkPayAmount.value = v
})

const quickResCheckInNights = computed(() => {
  const row = quickResCheckInOrder.value
  if (!row?.arrivalDate || !row?.departureDate) return 0
  return computeStayNights(row.arrivalDate, row.departureDate)
})
const quickResCheckInChargeable = computed(() => {
  const row = quickResCheckInOrder.value
  if (!row) return 0
  return computeCheckInChargeable(
    Number(quickResCheckInForm.agreedDailyRate ?? 0),
    row.arrivalDate,
    row.departureDate
  )
})

watch(quickResCheckInChargeable, (v) => {
  if (quickResCheckInVisible.value) {
    quickResCheckInPayAmount.value = v
  }
})

/** 查看日内在住单（用于快捷操作「退房」） */
const primaryInHouseOrder = computed(() => {
  const orders = schedule.value?.orders || []
  const found = orders.find((row) => row.orderType === 'STAY' && row.status === 'IN_HOUSE')
  return found ?? null
})

/** 查看日内已确认预订（用于快捷操作「预订入住」） */
const primaryConfirmableRes = computed(() => {
  const orders = schedule.value?.orders || []
  const found = orders.find((row) => row.orderType === 'RESERVATION' && isResCheckInable(row.status))
  return found ?? null
})

function occupancyLabel(s: string) {
  return OCCUPANCY_STATUS_LABEL[s] || s
}

function occupancyColor(s: string) {
  return OCCUPANCY_STATUS_COLOR[s] || '#dcdfe6'
}

function cleanLabel(s: string) {
  return CLEAN_STATUS_LABEL[s] || s
}

function cleanColor(s: string) {
  return CLEAN_STATUS_COLOR[s] || '#dcdfe6'
}

const roomOccupancyStatus = computed(
  () => schedule.value?.occupancyStatus || selected.value?.occupancyStatus || ''
)

const roomCleanStatus = computed(
  () => schedule.value?.cleanStatus || selected.value?.cleanStatus || 'CLEAN'
)

const canWalkIn = computed(() => {
  const occ = roomOccupancyStatus.value
  const clean = roomCleanStatus.value
  return (occ === 'VACANT' || occ === 'RESERVED') && clean === 'CLEAN'
})

const canToggleCleanDirty = computed(() =>
  auth.hasAnyPermission(['room:status:dirty', 'room:status:clean', 'room:status:force'])
)

const cleanDirtyToggleLabel = computed(() =>
  roomCleanStatus.value === 'DIRTY' ? '一键设为净房' : '一键设为脏房'
)

function currentRoomVersion(): number | undefined {
  return schedule.value?.version ?? selected.value?.version
}

function syncRoomAfterStatusChange(room: {
  status: string
  cleanStatus: string
  version: number
}) {
  if (!selected.value) return
  selected.value.occupancyStatus = room.status
  selected.value.cleanStatus = room.cleanStatus
  selected.value.version = room.version
  if (schedule.value) {
    schedule.value.occupancyStatus = room.status
    schedule.value.cleanStatus = room.cleanStatus
    schedule.value.version = room.version
  }
}

async function toggleCleanDirty() {
  if (!selected.value || !canToggleCleanDirty.value) return
  const targetLabel = roomCleanStatus.value === 'DIRTY' ? '净' : '脏'
  try {
    await ElMessageBox.confirm(`确认将保洁态切换为「${targetLabel}」？（不影响占用态）`, '保洁态切换', {
      type: 'warning'
    })
  } catch {
    return
  }
  saving.value = true
  try {
    const res = await toggleCleanDirtyApi(selected.value.id)
    syncRoomAfterStatusChange(res.data.data)
    ElMessage.success(`已切换为${cleanLabel(res.data.data.cleanStatus)}`)
    await load()
    await loadSchedule()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '切换失败')
  } finally {
    saving.value = false
  }
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
  items.value = res.data.data
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

function isResCancellable(status: string) {
  return status === 'CONFIRMED' || status === 'PENDING'
}

function isResCheckInable(status: string) {
  return status === 'CONFIRMED'
}

async function requireOpenShift(): Promise<boolean> {
  try {
    const shiftRes = await getCurrentShift()
    if (!shiftRes.data.data) {
      ElMessage.warning('请先开班后再办理退订退款')
      return false
    }
    return true
  } catch {
    ElMessage.warning('请先开班后再办理退订退款')
    return false
  }
}

function openChangeRoomFromOrder(row: RoomScheduleOrderVO) {
  boardChangeOrder.value = row
  boardChangeForm.value.targetRoomId = undefined
  boardChangeVisible.value = true
}

async function loadBoardChangeRooms() {
  if (!boardChangeOrder.value || !schedule.value) return
  const row = boardChangeOrder.value
  const res = await listAvailabilityApi({
    roomTypeId: schedule.value.roomTypeId,
    arrival: row.arrivalDate,
    departure: row.departureDate,
    arrivalAt: row.arrivalAt,
    departureAt: row.departureAt
  })
  boardChangeRooms.value = (res.data.data || []).filter((r) => r.roomId !== schedule.value?.roomId)
}

async function submitBoardChangeRoom() {
  if (!boardChangeOrder.value || !boardChangeForm.value.targetRoomId) {
    ElMessage.warning('请选择目标客房')
    return
  }
  const target = boardChangeRooms.value.find((r) => r.roomId === boardChangeForm.value.targetRoomId)
  saving.value = true
  try {
    await changeRoom(boardChangeOrder.value.orderId, {
      targetRoomId: boardChangeForm.value.targetRoomId,
      targetRoomVersion: target?.version
    })
    ElMessage.success('换房成功')
    boardChangeVisible.value = false
    await loadSchedule()
    await load()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '换房失败')
  } finally {
    saving.value = false
  }
}

async function confirmCheckoutFromOrder(row: RoomScheduleOrderVO) {
  const roomNo = schedule.value?.roomNo || ''
  try {
    await ElMessageBox.confirm(
      `确认为 ${row.guestName}（${roomNo}）办理退房？客房将置为脏房。房费已在入住时结清。`,
      '退房',
      { type: 'warning' }
    )
  } catch {
    return
  }
  saving.value = true
  try {
    await checkoutStay(row.orderId)
    ElMessage.success('已退房')
    actionVisible.value = false
    await load()
    if (schedule.value) {
      await loadSchedule()
    }
  } catch (e: unknown) {
    const err = e as { message?: string }
    if (!err.message) {
      ElMessage.error('退房失败')
    }
  } finally {
    saving.value = false
  }
}

async function openVoidFromOrder(row: RoomScheduleOrderVO) {
  boardVoidOrder.value = row
  boardVoidForm.chargeThroughDate = viewDate.value
  boardVoidForm.refundMethod = 'CASH'
  boardVoidForm.remark = ''
  boardVoidStay.value = null
  boardVoidPreview.value = null
  try {
    const res = await getStay(row.orderId)
    boardVoidStay.value = res.data.data
    refreshBoardVoidRefund()
  } catch {
    boardVoidForm.refundAmount = 0
  }
  boardVoidVisible.value = true
}

function refreshBoardVoidRefund() {
  const stay = boardVoidStay.value
  if (!stay || !boardVoidForm.chargeThroughDate) {
    boardVoidForm.refundAmount = 0
    boardVoidPreview.value = null
    return
  }
  const paid = Number(stay.folioPaidAmount ?? 0)
  const rate = Number(stay.agreedDailyRate ?? schedule.value?.rackRate ?? 0)
  const preview = computeRefundPreview(
    paid,
    rate,
    stay.arrivalDate,
    stay.departureDate,
    boardVoidForm.chargeThroughDate
  )
  boardVoidPreview.value = preview
  boardVoidForm.refundAmount = preview.refund
}

async function submitBoardVoid() {
  if (!boardVoidOrder.value || !boardVoidForm.chargeThroughDate) return
  if (!(await requireOpenShift())) return
  try {
    await ElMessageBox.confirm('确认提前退房并退款？客房将置为脏房。', '退款', { type: 'warning' })
  } catch {
    return
  }
  saving.value = true
  try {
    await voidCheckout(boardVoidOrder.value.orderId, {
      chargeThroughDate: boardVoidForm.chargeThroughDate,
      refundMethod: boardVoidForm.refundMethod,
      remark: boardVoidForm.remark,
      refundAmount: boardVoidForm.refundAmount ?? 0
    })
    ElMessage.success('已办理退款')
    boardVoidVisible.value = false
    actionVisible.value = false
    await load()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

function openCancelRefundFromOrder(row: RoomScheduleOrderVO) {
  boardCancelResOrder.value = row
  boardCancelResForm.refundAmount = 0
  boardCancelResForm.refundMethod = 'CASH'
  boardCancelResForm.remark = ''
  boardCancelResVisible.value = true
}

async function submitBoardCancelRefund() {
  if (!boardCancelResOrder.value) return
  if ((boardCancelResForm.refundAmount ?? 0) > 0 && !(await requireOpenShift())) return
  try {
    await ElMessageBox.confirm('确认取消该预订？将解除房态锁定。', '退款', { type: 'warning' })
  } catch {
    return
  }
  saving.value = true
  try {
    await cancelWithRefundApi(boardCancelResOrder.value.orderId, {
      refundAmount: boardCancelResForm.refundAmount ?? 0,
      refundMethod: boardCancelResForm.refundMethod,
      remark: boardCancelResForm.remark
    })
    ElMessage.success('预订已取消')
    boardCancelResVisible.value = false
    await loadSchedule()
    await load()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
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
  await auth.syncPermissions()
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

async function openResCheckInFromOrder(row: RoomScheduleOrderVO) {
  if (!schedule.value) return
  if (!isResCheckInable(row.status)) {
    ElMessage.warning('仅已确认预订可入住')
    return
  }
  quickResCheckInOrder.value = row
  quickResCheckInForm.reservationId = row.orderId
  quickResCheckInForm.roomId = schedule.value.roomId
  quickResCheckInForm.remark = row.remark || ''
  quickResCheckInForm.agreedDailyRate =
    row.agreedDailyRate != null
      ? Number(row.agreedDailyRate)
      : schedule.value.rackRate != null
        ? Number(schedule.value.rackRate)
        : undefined
  quickResCheckInPayMethod.value = 'CASH'
  quickResCheckInPayAmount.value = quickResCheckInChargeable.value
  try {
    const res = await listAvailabilityApi({
      roomTypeId: schedule.value.roomTypeId,
      arrival: row.arrivalDate,
      departure: row.departureDate,
      arrivalAt: toIsoDateTime(row.arrivalDate, row.arrivalAt, DEFAULT_ARRIVAL_TIME),
      departureAt: toIsoDateTime(row.departureDate, row.departureAt, DEFAULT_DEPARTURE_TIME),
      excludeReservationId: row.orderId
    })
    const rooms = res.data.data || []
    if (!rooms.some((r) => r.roomId === schedule.value!.roomId)) {
      ElMessage.warning('本房当前不可入住，请至入住页另选客房')
      return
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '校验可售房失败')
    return
  }
  quickResCheckInVisible.value = true
}

async function submitQuickResCheckIn() {
  if (!schedule.value || !quickResCheckInOrder.value) return
  if (!quickResCheckInForm.reservationId || !quickResCheckInForm.roomId) {
    ElMessage.warning('预订或客房信息不完整')
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
  if (Math.abs(quickResCheckInPayAmount.value - quickResCheckInChargeable.value) > 0.009) {
    ElMessage.warning('收款金额须等于应付房费')
    return
  }
  saving.value = true
  try {
    await checkInFromReservation({
      reservationId: quickResCheckInForm.reservationId,
      roomId: quickResCheckInForm.roomId,
      agreedDailyRate: quickResCheckInForm.agreedDailyRate,
      remark: quickResCheckInForm.remark,
      payments: [{ method: quickResCheckInPayMethod.value, amount: quickResCheckInPayAmount.value }]
    })
    ElMessage.success('预订入住成功')
    quickResCheckInVisible.value = false
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
  if (Math.abs(quickWalkPayAmount.value - quickWalkChargeable.value) > 0.009) {
    ElMessage.warning('收款金额须等于应付房费')
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
      remark: quickWalkForm.remark,
      payments: [{ method: quickWalkPayMethod.value, amount: quickWalkPayAmount.value }]
    })
    ElMessage.success('入住成功')
    quickWalkVisible.value = false
    actionVisible.value = false
    await load()
    await loadFloors()
  } catch (e: unknown) {
    const err = e as { message?: string }
    if (!err.message) {
      ElMessage.error('入住失败')
    }
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

async function submitMarkDirty(keepDialog = false) {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm('确认将该客房设为脏房？', '设为脏房', { type: 'warning' })
  } catch {
    return
  }
  saving.value = true
  try {
    const res = await markRoomDirtyApi(selected.value.id, { version: currentRoomVersion() })
    syncRoomAfterStatusChange(res.data.data)
    ElMessage.success('已设为脏房')
    await load()
    await loadFloors()
    if (keepDialog) {
      await loadSchedule()
    } else {
      actionVisible.value = false
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '设为脏房失败')
  } finally {
    saving.value = false
  }
}

async function submitMarkClean(keepDialog = false) {
  if (!selected.value) return
  try {
    await ElMessageBox.confirm('确认打扫完成，设为空净？', '设为空净', { type: 'info' })
  } catch {
    return
  }
  saving.value = true
  try {
    const res = await markRoomCleanApi(selected.value.id, { version: currentRoomVersion() })
    syncRoomAfterStatusChange(res.data.data)
    ElMessage.success('已设为空净')
    await load()
    await loadFloors()
    if (keepDialog) {
      await loadSchedule()
    } else {
      actionVisible.value = false
    }
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '设为空净失败')
  } finally {
    saving.value = false
  }
}

function openForce() {
  actionVisible.value = false
  forceForm.targetStatus = 'VACANT'
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
.card-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-top: 4px;
}
.card-actual {
  font-size: 11px;
  color: #606266;
  margin-top: 4px;
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
.quick-status-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 4px;
  align-items: center;
}
.charge-total {
  font-size: 16px;
  font-weight: 600;
  color: #e6a23c;
}
.pay-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.status-warn {
  margin-top: 8px;
}
.hint-inline {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
