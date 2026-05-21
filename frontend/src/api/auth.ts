import { request } from './request'

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  permissions: string[]
}

export function loginApi(data: LoginPayload) {
  return request.post<{ code: number; data: LoginResult }>('/auth/login', data)
}

export function meApi() {
  return request.get<{ code: number; data: LoginResult }>('/auth/me')
}

export function logoutApi() {
  return request.post('/auth/logout')
}
