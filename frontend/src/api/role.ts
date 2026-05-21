import { request } from './request'
import type { PermissionItem } from './user'

export interface RoleVO {
  id: number
  code: string
  name: string
  description?: string
}

export function listRolesApi() {
  return request.get<{ code: number; data: RoleVO[] }>('/roles')
}

export function listRolePermissionsApi(roleId: number) {
  return request.get<{ code: number; data: PermissionItem[] }>(`/roles/${roleId}/permissions`)
}

export function saveRolePermissionsApi(roleId: number, permissionIds: number[]) {
  return request.put(`/roles/${roleId}/permissions`, { permissionIds })
}

export function restoreRolePermissionsDefaultApi(roleId: number) {
  return request.post<{ code: number; data: PermissionItem[] }>(
    `/roles/${roleId}/permissions/restore-default`
  )
}
