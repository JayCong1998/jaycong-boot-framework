import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { fileURLToPath, URL } from "node:url";
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  // 商品和认证接口统一通过网关转发。
  const target = env.GATEWAY_PROXY_TARGET || "http://localhost:8090";
  const proxy = {
    "/auth": { target, changeOrigin: true },
    "/goods": { target, changeOrigin: true },
  };
  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) },
    },
    server: { proxy },
    preview: { proxy },
  };
});
