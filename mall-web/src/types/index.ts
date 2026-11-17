export interface Product {
  id: string;
  name: string;
  category?: string;
  price: number;
  originalPrice?: number;
  image: string;
  description: string;
  badge?: string;
}
export interface User {
  id: string;
  nickname: string;
  mobile: string | null;
  email: string | null;
  role: string;
}
export interface LoginResponse {
  tokenName: string;
  tokenValue: string;
  timeout: number;
  user: User;
}
export interface RegisterPayload {
  mobile?: string;
  email?: string;
  password: string;
  nickname: string;
}

export interface GoodsView {
  id: string;
  goodsCode: string;
  name: string;
  subtitle: string | null;
  price: number;
  image: string | null;
  description: string | null;
}
export interface GoodsPage {
  total: number;
  rows: GoodsView[];
}
