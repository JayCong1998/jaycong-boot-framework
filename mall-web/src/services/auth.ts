import { request } from "./http";
import type { User, LoginResponse, RegisterPayload } from "@/types";
export const authApi = {
  login: (account: string, password: string) =>
    request<LoginResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ account, password }),
    }),
  register: (payload: RegisterPayload) =>
    request<User>("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  me: () => request<User>("/auth/me"),
  logout: () => request<void>("/auth/logout", { method: "POST" }),
};
