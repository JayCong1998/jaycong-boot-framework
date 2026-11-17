package io.github.jaycong.satoken.session;

/** 各 Servlet 服务共用的登录域及 Token-Session 数据契约。 */
public final class AuthSessionKeys {
    public static final String LOGIN_TYPE = "jaycong-auth";
    public static final String LOGIN_USER = "jaycong.login-user.v1";

    private AuthSessionKeys() {
    }
}
