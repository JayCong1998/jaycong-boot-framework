import { http } from './http'
import type { Result } from '@/types/auth'

export interface MemberOperation {
  id: string
  memberId: string
  operatorId: string
  operatorName: string
  operatorType: string
  source: string
  action: string
  outcome: string
  beforeValue: string | null
  afterValue: string | null
  message: string
  createTime: string
  updateTime: string
}
export interface OperationQuery {
  page: number
  size: number
  memberId?: string
  operatorId?: string
  action?: string
  operatorType?: string
  outcome?: string
  start?: string
  end?: string
}
const path = '/admin/members/operations'
export const memberOperationsApi = {
  async list(params: OperationQuery) {
    return (await http.get<Result<{ total: number; rows: MemberOperation[] }>>(path, { params })).data.data
  },
  async detail(id: string) {
    return (await http.get<Result<MemberOperation>>(`${path}/${id}`)).data.data
  },
}
