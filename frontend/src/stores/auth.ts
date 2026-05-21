import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, meApi, logoutApi } from '@/api/auth'
import { clearToken, getToken, setToken } from '@/api/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const userId = ref<number | null>(null)
  const username = ref('')
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  function hasPermission(code: string): boolean {
    return permissions.value.includes(code)
  }

  function hasAnyPermission(codes: string[]): boolean {
    return codes.some((c) => permissions.value.includes(c))
  }

  async function login(usernameInput: string, password: string) {
    const res = await loginApi({ username: usernameInput, password })
    const data = res.data.data
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    permissions.value = data.permissions || []
    setToken(data.token)
  }

  async function fetchMe() {
    const res = await meApi()
    const data = res.data.data
    userId.value = data.userId
    username.value = data.username
    permissions.value = data.permissions || []
  }

  /** 从服务端同步最新权限（角色/直授变更后无需重新登录） */
  async function syncPermissions() {
    if (!getToken()) {
      return
    }
    await fetchMe()
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = null
      userId.value = null
      username.value = ''
      permissions.value = []
      clearToken()
    }
  }

  async function restoreSession() {
    if (!getToken()) {
      return false
    }
    token.value = getToken()
    await fetchMe()
    return true
  }

  return {
    token,
    userId,
    username,
    permissions,
    isLoggedIn,
    hasPermission,
    hasAnyPermission,
    login,
    fetchMe,
    syncPermissions,
    logout,
    restoreSession
  }
})
