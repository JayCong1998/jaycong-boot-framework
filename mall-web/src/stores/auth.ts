import { create } from "zustand";
import { authApi } from "@/services/auth";
import { ApiError } from "@/services/http";
import type { User } from "@/types";
interface AuthState {
  user: User | null;
  restoring: boolean;
  login: (account: string, password: string) => Promise<void>;
  restore: () => Promise<void>;
  logout: () => Promise<void>;
}
export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  restoring: false,
  async login(account, password) {
    const result = await authApi.login(account, password);
    sessionStorage.setItem("mall-token", result.tokenValue);
    set({ user: result.user });
  },
  async restore() {
    if (!sessionStorage.getItem("mall-token")) return;
    set({ restoring: true });
    try {
      set({ user: await authApi.me() });
    } catch (error) {
      if (error instanceof ApiError && error.status === 401)
        sessionStorage.removeItem("mall-token");
    } finally {
      set({ restoring: false });
    }
  },
  async logout() {
    try {
      await authApi.logout();
    } catch (error) {
      if (!(error instanceof ApiError && error.status === 401)) throw error;
    }
    sessionStorage.removeItem("mall-token");
    set({ user: null });
  },
}));
