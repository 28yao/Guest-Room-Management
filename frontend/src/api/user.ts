import { request } from './request'

export interface UserVO {
  id: number
  username: string
  status: number
  roleIds?: number[]
  roleNames?: string[]
  permissions?: string[]
}

export function listUsersApi() {
  return request.get<{ code: number; data: UserVO[] }>('/users')
}

export function createUserApi(data: {
  username: string
  password: string
  status?: number
  roleIds?: number[]
}) {
  return request.post<{ code: number; data: UserVO }>('/users', data)
}

export function updateUserApi(id: number, data: { password?: string; status?: number; roleIds?: number[] }) {
  return request.put<{ code: number; data: UserVO }>(`/users/${id}`, data)
}

export interface PermissionItem {
  id: number
  code: string
  name: string
  description?: string
  assigned: boolean
}

export function listUserPermissionsApi(userId: number) {
  return request.get<{ code: number; data: PermissionItem[] }>(`/users/${userId}/permissions`)
}

export function saveUserPermissionsApi(userId: number, permissionIds: number[]) {
  return request.put(`/users/${userId}/permissions`, { permissionIds })
}

export function restoreUserPermissionsDefaultApi(userId: number) {
  return request.post<{ code: number; data: PermissionItem[] }>(
    `/users/${userId}/permissions/restore-default`
  )
}

export function changeUserPasswordApi(userId: number, password: string) {
  return request.put(`/users/${userId}/password`, { password })
}

export function deleteUserApi(userId: number) {
  return request.delete(`/users/${userId}`)
}
