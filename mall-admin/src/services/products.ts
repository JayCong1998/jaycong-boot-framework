import { http } from './http'
import type { Result } from '@/types/auth'

export interface Category {
  value: string
  label: string
}
export interface Product {
  id: string
  goodsCode: string
  name: string
  subtitle: string
  category: string | null
  coverUrl: string
  description: string
  price: number
  originalPrice: number | null
  stock: number
  stockWarning: number
  sortOrder: number
  status: number
  lockVersion: number
  createTime: string
  updateTime: string
}
export interface ProductInput {
  goodsCode: string
  name: string
  subtitle: string
  category: string
  coverUrl: string
  description: string
  price: number
  originalPrice: number | null
  stockWarning: number
  sortOrder: number
  lockVersion?: number
}
export interface ProductQuery {
  page: number
  size: number
  name?: string
  goodsCode?: string
  category?: string
  status?: number
  lowStock?: boolean
}
const path = '/admin/goods'
const categories = '/admin/goods-categories'
export const productsApi = {
  async list(params: ProductQuery) {
    return (await http.get<Result<{ total: number; rows: Product[] }>>(path, { params })).data.data
  },
  async detail(id: string) {
    return (await http.get<Result<Product>>(`${path}/${id}`)).data.data
  },
  async create(input: ProductInput) {
    return (await http.post<Result<Product>>(path, input)).data.data
  },
  async update(id: string, input: ProductInput) {
    return (await http.put<Result<Product>>(`${path}/${id}`, input)).data.data
  },
  async status(product: Product, status: number) {
    return (await http.put<Result<Product>>(`${path}/${product.id}/status`, {
      status, lockVersion: product.lockVersion,
    })).data.data
  },
  async stock(product: Product, quantity: number) {
    return (await http.put<Result<Product>>(`${path}/${product.id}/stock`, {
      quantity, lockVersion: product.lockVersion,
    })).data.data
  },
  async remove(product: Product) {
    await http.delete(`${path}/${product.id}`, { params: { lockVersion: product.lockVersion } })
  },
  async categories() {
    return (await http.get<Result<Category[]>>(categories)).data.data
  },
}
