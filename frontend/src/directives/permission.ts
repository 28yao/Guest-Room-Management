import type { App, Directive } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * 按权限码控制元素显示，无权限则移除 DOM。
 */
export const permissionDirective: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const auth = useAuthStore()
    const value = binding.value
    const codes = Array.isArray(value) ? value : [value]
    const allowed = codes.some((c) => auth.hasPermission(c))
    if (!allowed) {
      el.parentNode?.removeChild(el)
    }
  }
}

export function setupPermissionDirective(app: App) {
  app.directive('permission', permissionDirective)
}
