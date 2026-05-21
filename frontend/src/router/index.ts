import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToken } from '@/api/request'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/rooms/board',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'rooms/board',
        name: 'RoomBoard',
        component: () => import('@/views/room/RoomBoard.vue'),
        meta: { title: '房态图' }
      },
      {
        path: 'room-types',
        name: 'RoomTypeManage',
        component: () => import('@/views/room/RoomTypeManage.vue'),
        meta: { title: '房型管理', permissions: ['room:type:manage'] }
      },
      {
        path: 'rooms',
        name: 'RoomManage',
        component: () => import('@/views/room/RoomManage.vue'),
        meta: { title: '客房管理', permissions: ['room:manage'] }
      },
      {
        path: 'reservations',
        name: 'ReservationList',
        component: () => import('@/views/reservation/ReservationList.vue'),
        meta: { title: '预订管理', permissions: ['reservation:manage'] }
      },
      {
        path: 'check-in',
        name: 'CheckIn',
        component: () => import('@/views/stay/CheckIn.vue'),
        meta: { title: '办理入住', permissions: ['stay:checkin'] }
      },
      {
        path: 'in-house',
        name: 'InHouseList',
        component: () => import('@/views/stay/InHouseList.vue'),
        meta: { title: '在住管理' }
      },
      {
        path: 'system/users',
        name: 'UserManage',
        component: () => import('@/views/system/UserManage.vue'),
        meta: { title: '用户管理', permissions: ['system:user:manage'] }
      },
      {
        path: 'system/roles',
        name: 'RolePermission',
        component: () => import('@/views/system/RolePermission.vue'),
        meta: { title: '角色权限', permissions: ['system:role:manage'] }
      },
      {
        path: 'system/user-permissions',
        name: 'UserPermission',
        component: () => import('@/views/system/UserPermission.vue'),
        meta: { title: '敏感权限直授', permissions: ['system:permission:grant'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  if (to.meta.public) {
    next()
    return
  }
  const token = getToken()
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  const auth = useAuthStore()
  try {
    await auth.syncPermissions()
  } catch {
    next({ path: '/login' })
    return
  }
  const required = to.meta.permissions as string[] | undefined
  if (required && required.length > 0 && !auth.hasAnyPermission(required)) {
    ElMessage.warning('无权限访问该页面')
    next({ path: '/home' })
    return
  }
  next()
})

export default router
