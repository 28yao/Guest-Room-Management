import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
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
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '工作台' }
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
  if (!auth.username) {
    try {
      await auth.fetchMe()
    } catch {
      next({ path: '/login' })
      return
    }
  }
  const required = to.meta.permissions as string[] | undefined
  if (required && required.length > 0 && !auth.hasAnyPermission(required)) {
    next({ path: '/home' })
    return
  }
  next()
})

export default router
