import { request } from './request'
import type { StayVO } from './stay'

export interface FolioLineVO {
  id: number
  lineType: string
  description?: string
  quantity: number
  unitPrice: number
  amount: number
}

export interface FolioDetailVO {
  id: number
  stayOrderId: number
  totalAmount: number
  paidAmount: number
  balance: number
  status: string
  lines: FolioLineVO[]
}

export interface AddPaymentForm {
  method: string
  amount: number
}

export interface AdjustPriceForm {
  agreedDailyRate: number
}

export function getFolioByStay(stayId: number) {
  return request.get<{ data: FolioDetailVO }>(`/folios/by-stay/${stayId}`)
}

export function recalculateFolio(folioId: number) {
  return request.post<{ data: FolioDetailVO }>(`/folios/${folioId}/recalculate`)
}

export function adjustFolioPrice(folioId: number, data: AdjustPriceForm) {
  return request.post<{ data: FolioDetailVO }>(`/folios/${folioId}/adjust-price`, data)
}

export function addFolioPayment(folioId: number, data: AddPaymentForm) {
  return request.post<{ data: FolioDetailVO }>(`/folios/${folioId}/payments`, data)
}

export function checkoutStay(stayId: number) {
  return request.post<{ data: StayVO }>(`/stays/${stayId}/checkout`)
}
