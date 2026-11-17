import { http } from './http'
import type { Result } from '@/types/auth'

export interface Member {
  id: string
  nickname: string
  mobile: string | null
  email: string | null
  status: number
  createTime: string
  updateTime: string
  lastLoginTime: string | null
  lockVersion: number
}
export interface MemberQuery {
  page: number
  size: number
  keyword?: string
  id?: string
  status?: number
  createTimeStart?: string
  createTimeEnd?: string
}
const path = '/admin/members'
export const membersApi = {
  async list(params: MemberQuery) {
    return (await http.get<Result<{ total: number; rows: Member[] }>>(path, { params })).data.data
  },
  async detail(id: string) {
    return (await http.get<Result<Member>>(`${path}/${id}`)).data.data
  },
  async update(member: Member, nickname: string) {
    return (await http.put<Result<Member>>(`${path}/${member.id}`, {
      nickname, lockVersion: member.lockVersion,
    })).data.data
  },
  async status(member: Member, status: number) {
    return (await http.put<Result<Member>>(`${path}/${member.id}/status`, {
      status, lockVersion: member.lockVersion,
    })).data.data
  },
}
