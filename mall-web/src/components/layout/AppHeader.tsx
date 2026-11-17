import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Search, UserRound, ShoppingCart, LogOut } from "lucide-react";
import BrandLogo from "./BrandLogo";
import { Input } from "@/components/ui/input";
import { useCartStore, cartCount } from "@/stores/cart";
import { useAuthStore } from "@/stores/auth";
import { useNoticeStore } from "@/stores/notice";
export default function AppHeader() {
  const location = useLocation(),
    navigate = useNavigate();
  const q = new URLSearchParams(location.search).get("q") || "";
  const [search, setSearch] = useState(q);
  const count = useCartStore(cartCount),
    setOpen = useCartStore((s) => s.setOpen);
  const user = useAuthStore((s) => s.user),
    logout = useAuthStore((s) => s.logout),
    show = useNoticeStore((s) => s.show);
  useEffect(() => setSearch(q), [q]);
  async function exit() {
    try {
      await logout();
      show("已退出登录");
    } catch (error) {
      show((error as Error).message);
    }
  }
  return (
    <>
      <a href="#main-content" className="skip-link">
        跳转至主要内容
      </a>
      <header className="site-header">
        <div className="page-container header-inner">
          <BrandLogo />
          <form
            className="header-search"
            role="search"
            onSubmit={(e) => {
              e.preventDefault();
              navigate(
                search.trim()
                  ? "/?" + new URLSearchParams({ q: search.trim() })
                  : "/",
              );
            }}
          >
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="搜索商品"
              placeholder="搜索心仪好物…"
            />
            <button type="submit" aria-label="搜索">
              <Search size={21} />
            </button>
          </form>
          <nav className="header-actions" aria-label="用户导航">
            {user ? (
              <>
                <span className="user-name">
                  <UserRound size={20} />
                  {user.nickname}
                </span>
                <button
                  className="icon-button"
                  aria-label="退出登录"
                  onClick={exit}
                >
                  <LogOut size={19} />
                </button>
              </>
            ) : (
              <>
                <UserRound size={21} />
                <span className="auth-links">
                  <Link to="/login">登录</Link>
                  <span>/</span>
                  <Link to="/register">注册</Link>
                </span>
              </>
            )}
            <span className="header-divider"></span>
            <button
              className="cart-trigger icon-button"
              aria-label="打开购物车"
              onClick={() => setOpen(true)}
            >
              <ShoppingCart size={24} />
              <span className="cart-badge">{count}</span>
            </button>
          </nav>
        </div>
      </header>
    </>
  );
}
