/**
 * 按权限解析登录后默认首页。
 */
export function resolveDefaultHomePath(permissions: string[]): string {
  if (permissions.includes('room:board:view')) {
    return '/rooms/board'
  }
  if (permissions.includes('hk:view')) {
    return '/housekeeping'
  }
  return '/home'
}
