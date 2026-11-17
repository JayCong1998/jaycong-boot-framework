export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}
export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = sessionStorage.getItem("mall-token");
  const headers = new Headers(options.headers);
  if (options.body) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), 12000);
  try {
    const response = await fetch(path, {
      ...options,
      headers,
      signal: controller.signal,
    });
    const result = await response.json().catch(() => null);
    if (!response.ok || result?.code !== 0)
      throw new ApiError(
        result?.message || "服务暂不可用，请稍后重试",
        response.status,
      );
    return result.data as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new Error(
      error instanceof DOMException && error.name === "AbortError"
        ? "请求超时，请稍后重试"
        : "无法连接服务，请检查网络或确认认证服务已启动",
    );
  } finally {
    window.clearTimeout(timeout);
  }
}
