import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Eye,
  EyeOff,
  LoaderCircle,
  ArrowRight,
  Mail,
  Smartphone,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuthStore } from "@/stores/auth";
import { useNoticeStore } from "@/stores/notice";
import { authApi } from "@/services/auth";
export default function AuthForm({ register }: { register: boolean }) {
  const navigate = useNavigate(),
    login = useAuthStore((s) => s.login),
    show = useNoticeStore((s) => s.show);
  const [form, setForm] = useState({
    account: "",
    password: "",
    confirm: "",
    nickname: "",
  });
  const [method, setMethod] = useState<"mobile" | "email">("mobile"),
    [visible, setVisible] = useState(false),
    [pending, setPending] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({}),
    [serverError, setServerError] = useState("");
  const submitting = useRef(false);
  useEffect(() => {
    setForm((f) => ({ ...f, password: "", confirm: "" }));
    setServerError("");
    setErrors({});
  }, [register]);
  function field(key: keyof typeof form) {
    return {
      value: form[key],
      onChange: (e: React.ChangeEvent<HTMLInputElement>) =>
        setForm((f) => ({ ...f, [key]: e.target.value })),
    };
  }
  function validate() {
    const errors: Record<string, string> = {},
      account = form.account.trim();
    setServerError("");
    if (!account) errors.account = "请输入手机号或邮箱";
    else {
      const mobile = account.replace(/^(?:\+86|0086)/, "").replace(/[ -]/g, "");
      const validMobile = /^1[3-9]\d{9}$/.test(mobile),
        validEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(account);
      if (
        !(register
          ? method === "mobile"
            ? validMobile
            : validEmail
          : validMobile || validEmail)
      )
        errors.account = "请输入有效的手机号或邮箱";
    }
    if (!form.password) errors.password = "请输入密码";
    else if (
      register &&
      (form.password.length < 8 ||
        form.password.length > 64 ||
        new TextEncoder().encode(form.password).length > 72)
    )
      errors.password = "密码须为 8–64 个字符，UTF-8 编码不超过 72 字节";
    if (register && !form.nickname.trim()) errors.nickname = "请输入昵称";
    if (register && form.confirm !== form.password)
      errors.confirm = "两次输入的密码不一致";
    setErrors(errors);
    return !Object.keys(errors).length;
  }
  async function submit() {
    if (submitting.current || !validate()) return;
    submitting.current = true;
    setPending(true);
    try {
      if (register) {
        await authApi.register({
          [method]: form.account.trim(),
          nickname: form.nickname.trim(),
          password: form.password,
        });
        show("注册成功，请使用新账号登录");
        navigate("/login", { replace: true });
      } else {
        await login(form.account.trim(), form.password);
        show("欢迎回来，" + useAuthStore.getState().user?.nickname);
        navigate("/", { replace: true });
      }
    } catch (error) {
      setServerError((error as Error).message);
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }
  return (
    <form
      className="auth-form"
      noValidate
      onSubmit={(e) => {
        e.preventDefault();
        void submit();
      }}
    >
      {register && (
        <div className="register-method" aria-label="注册方式">
          <button
            type="button"
            className={method === "mobile" ? "active" : ""}
            aria-pressed={method === "mobile"}
            onClick={() => {
              setMethod("mobile");
              setForm((f) => ({ ...f, account: "" }));
            }}
          >
            <Smartphone size={16} />
            手机号注册
          </button>
          <button
            type="button"
            className={method === "email" ? "active" : ""}
            aria-pressed={method === "email"}
            onClick={() => {
              setMethod("email");
              setForm((f) => ({ ...f, account: "" }));
            }}
          >
            <Mail size={16} />
            邮箱注册
          </button>
        </div>
      )}
      {register && (
        <div className="form-field">
          <label htmlFor="nickname">昵称</label>
          <Input
            id="nickname"
            {...field("nickname")}
            placeholder="怎么称呼你？"
            autoComplete="nickname"
            maxLength={64}
            aria-invalid={!!errors.nickname}
            aria-describedby="nickname-error"
          />
          {errors.nickname && (
            <p id="nickname-error" className="field-error">
              {errors.nickname}
            </p>
          )}
        </div>
      )}
      <div className="form-field">
        <label htmlFor="account">
          {register
            ? method === "mobile"
              ? "手机号"
              : "邮箱"
            : "手机号 / 邮箱"}
        </label>
        <Input
          id="account"
          {...field("account")}
          type={register && method === "email" ? "email" : "text"}
          placeholder={
            register
              ? method === "mobile"
                ? "请输入手机号"
                : "请输入邮箱地址"
              : "请输入手机号或邮箱"
          }
          autoComplete="username"
          maxLength={254}
          aria-invalid={!!errors.account}
          aria-describedby="account-error"
        />
        {errors.account && (
          <p id="account-error" className="field-error">
            {errors.account}
          </p>
        )}
      </div>
      <div className="form-field">
        <label htmlFor="password">密码</label>
        <div className="password-input">
          <Input
            id="password"
            {...field("password")}
            type={visible ? "text" : "password"}
            placeholder={register ? "设置 8–64 位密码" : "请输入密码"}
            autoComplete={register ? "new-password" : "current-password"}
            aria-invalid={!!errors.password}
            aria-describedby="password-error"
          />
          <button
            type="button"
            aria-label={visible ? "隐藏密码" : "显示密码"}
            aria-pressed={visible}
            onClick={() => setVisible((v) => !v)}
          >
            {visible ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
        {errors.password && (
          <p id="password-error" className="field-error">
            {errors.password}
          </p>
        )}
      </div>
      {register && (
        <div className="form-field">
          <label htmlFor="confirm">确认密码</label>
          <Input
            id="confirm"
            {...field("confirm")}
            type={visible ? "text" : "password"}
            placeholder="请再次输入密码"
            autoComplete="new-password"
            aria-invalid={!!errors.confirm}
            aria-describedby="confirm-error"
          />
          {errors.confirm && (
            <p id="confirm-error" className="field-error">
              {errors.confirm}
            </p>
          )}
        </div>
      )}
      {serverError && (
        <p className="server-error" role="alert">
          {serverError}
        </p>
      )}
      <Button className="auth-submit" type="submit" disabled={pending}>
        {pending && <LoaderCircle className="animate-spin" size={18} />}
        {pending ? "正在提交…" : register ? "创建账号" : "登录"}
        {!pending && <ArrowRight size={18} />}
      </Button>
      <p className="auth-switch">
        {register ? "已有账号？" : "还没有账号？"}
        <Link to={register ? "/login" : "/register"}>
          {register ? "立即登录" : "免费注册"}
        </Link>
      </p>
    </form>
  );
}
