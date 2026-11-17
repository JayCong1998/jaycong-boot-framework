import { request } from "./http";
import type { GoodsPage, Product } from "@/types";
export async function fetchGoods(page: number, size: number, name: string) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (name.trim()) params.set("name", name.trim());
  const data = await request<GoodsPage>(`/goods?${params}`);
  const products: Product[] = data.rows.map((item) => ({
    id: item.id,
    name: item.name,
    price: item.price,
    image: item.image || "/images/product-placeholder.svg",
    description: item.subtitle || item.description || "",
  }));
  return { total: data.total, products };
}
