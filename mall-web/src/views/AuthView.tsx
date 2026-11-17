import { Link, useLocation } from "react-router-dom";
import { ArrowLeft, Sparkles, ShieldCheck } from "lucide-react";
import AuthForm from "@/components/auth/AuthForm";
export default function AuthView() {
  const register = useLocation().pathname === "/register";
  return (
    <div className="page-container auth-page">
      <Link to="/" className="back-home">
        <ArrowLeft size={16} />
        返回商城
      </Link>
      <div className="auth-layout">
        <section className="auth-story">
          <span className="story-eyebrow">
            <Sparkles size={16} />
            美好生活，从简选开始
          </span>
          <h1>
            把喜欢的好物，
            <br />
            装进日常生活。
          </h1>
          <p>
            少一点挑选的烦恼，多一点生活的美好。
            <br />
            每一件用心甄选，都为更好的你。
          </p>
          <div className="story-image">
            <img src="/images/hero.png" alt="简洁明亮的桌面与笔记本电脑" />
          </div>
          <div className="story-bottom">
            <ShieldCheck size={18} />
            <span>品质好物 · 用心甄选 · 安心体验</span>
          </div>
        </section>
        <section className="auth-panel">
          <div className="auth-panel-heading">
            <span className="auth-kicker">
              {register ? "NICE TO MEET YOU" : "WELCOME BACK"}
            </span>
            <h2>{register ? "开启你的简选生活" : "欢迎回来"}</h2>
            <p>
              {register
                ? "注册一个账号，发现更多值得的好物"
                : "登录简选商城，遇见你的心仪好物"}
            </p>
          </div>
          <AuthForm register={register} />
        </section>
      </div>
    </div>
  );
}
