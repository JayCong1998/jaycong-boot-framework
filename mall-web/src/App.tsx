import { lazy, Suspense, useEffect, useState } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { CheckCircle2 } from "lucide-react";
import AppHeader from "@/components/layout/AppHeader";
import AppFooter from "@/components/layout/AppFooter";
import CartSheet from "@/components/cart/CartSheet";
import { useAuthStore } from "@/stores/auth";
import { useNoticeStore } from "@/stores/notice";
const HomeView = lazy(() => import("@/views/HomeView"));
const AuthView = lazy(() => import("@/views/AuthView"));
function Notice() {
  const message = useNoticeStore((s) => s.message);
  const [display, setDisplay] = useState(message),
    [shown, setShown] = useState(false);
  useEffect(() => {
    if (message) {
      setDisplay(message);
      const frame = requestAnimationFrame(() => setShown(true));
      return () => cancelAnimationFrame(frame);
    }
    setShown(false);
    const timer = setTimeout(() => setDisplay(""), 200);
    return () => clearTimeout(timer);
  }, [message]);
  return display ? (
    <div
      className={
        "toast-message toast-enter-active" + (shown ? "" : " toast-enter-from")
      }
      role="status"
    >
      <CheckCircle2 size={19} />
      {display}
    </div>
  ) : null;
}
export default function App() {
  const location = useLocation(),
    restore = useAuthStore((s) => s.restore);
  useEffect(() => {
    void restore();
  }, [restore]);
  useEffect(() => {
    document.title =
      location.pathname === "/login"
        ? "登录 · 简选商城"
        : location.pathname === "/register"
          ? "注册 · 简选商城"
          : "简选商城 · 让生活更简单";
    window.scrollTo({ top: 0 });
  }, [location.pathname, location.search, location.key]);
  return (
    <div className="app-shell">
      <AppHeader />
      <main id="main-content">
        <Suspense fallback={null}>
          <Routes>
            <Route path="/" element={<HomeView />} />
            <Route path="/login" element={<AuthView />} />
            <Route path="/register" element={<AuthView />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </main>
      <AppFooter />
      <CartSheet />
      <Notice />
    </div>
  );
}
