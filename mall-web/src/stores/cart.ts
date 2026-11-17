import { create } from "zustand";
import type { Product } from "@/types";
interface CartItem {
  product: Product;
  quantity: number;
}
function load(): CartItem[] {
  try {
    const raw = JSON.parse(localStorage.getItem("mall-cart-v2") || "[]");
    return Array.isArray(raw)
      ? raw
          .filter(
            (item) =>
              item?.product &&
              typeof item.product.id === "string" &&
              typeof item.product.name === "string" &&
              Number.isFinite(item.product.price) &&
              item.product.price >= 0 &&
              Number.isInteger(item.quantity) &&
              item.quantity > 0,
          )
          .map((item) => ({ ...item, quantity: Math.min(item.quantity, 99) }))
      : [];
  } catch {
    return [];
  }
}
interface CartState {
  items: CartItem[];
  open: boolean;
  setOpen: (open: boolean) => void;
  add: (product: Product) => void;
  change: (id: string, delta: number) => void;
  remove: (id: string) => void;
}
export const useCartStore = create<CartState>((set) => {
  function save(items: CartItem[]) {
    try {
      localStorage.setItem("mall-cart-v2", JSON.stringify(items));
    } catch {
      /* 存储不可用时保留页面状态。 */
    }
    return { items };
  }
  return {
    items: load(),
    open: false,
    setOpen: (open) => set({ open }),
    add: (product) =>
      set((state) =>
        save(
          state.items.some((item) => item.product.id === product.id)
            ? state.items.map((item) =>
                item.product.id === product.id
                  ? { product, quantity: Math.min(item.quantity + 1, 99) }
                  : item,
              )
            : [...state.items, { product, quantity: 1 }],
        ),
      ),
    change: (id, delta) =>
      set((state) =>
        save(
          state.items.map((item) =>
            item.product.id === id
              ? {
                  ...item,
                  quantity: Math.max(1, Math.min(99, item.quantity + delta)),
                }
              : item,
          ),
        ),
      ),
    remove: (id) =>
      set((state) =>
        save(state.items.filter((item) => item.product.id !== id)),
      ),
  };
});
export const cartCount = (state: CartState) =>
  state.items.reduce((n, item) => n + item.quantity, 0);
export const cartTotal = (state: CartState) =>
  state.items.reduce((n, item) => n + item.product.price * item.quantity, 0);
