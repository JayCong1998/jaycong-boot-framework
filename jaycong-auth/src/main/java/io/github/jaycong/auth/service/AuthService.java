package io.github.jaycong.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import io.github.jaycong.api.user.request.UserAuthenticateRequest;
import io.github.jaycong.api.user.request.UserRegisterRequest;
import io.github.jaycong.auth.request.LoginRequest;
import io.github.jaycong.auth.request.RegisterRequest;
import io.github.jaycong.auth.response.LoginResponse;
import io.github.jaycong.auth.response.UserView;
import io.github.jaycong.auth.exception.AuthException;
import io.github.jaycong.satoken.context.AuthContext;
import io.github.jaycong.satoken.exception.AuthContextUnavailableException;
import io.github.jaycong.satoken.model.LoginUser;
import io.github.jaycong.satoken.session.AuthSessionKeys;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/** Auth 仅编排远程用户能力和 Redis 会话，不访问用户表或处理密码哈希。 */
@Service
public class AuthService {
    private final UserService users;
    private final AuthContext authContext;

    public AuthService(UserService users, AuthContext authContext) {
        this.users = users;
        this.authContext = authContext;
    }

    public UserView register(RegisterRequest request) {
        return UserView.from(users.register(new UserRegisterRequest(
                request.mobile(), request.email(), request.password(), request.nickname())));
    }

    public LoginResponse login(LoginRequest request) {
        var user = users.authenticate(new UserAuthenticateRequest(request.account(), request.password()));
        long id = Long.parseLong(user.id());
        try {
            StpUtil.login(id);
            String token = StpUtil.getTokenValue();
            // 复查和资料写入成功后才向客户端返回 Token；失败时撤销本次会话。
            try {
                var latest = users.findEnabledUser(id);
                StpUtil.getTokenSession().set(AuthSessionKeys.LOGIN_USER,
                        new LoginUser(id, latest.nickname(), latest.role()));
                return new LoginResponse(StpUtil.getTokenName(), token,
                        StpUtil.getTokenTimeout(), UserView.from(latest));
            } catch (RuntimeException exception) {
                try {
                    StpUtil.logoutByTokenValue(token);
                } catch (RuntimeException cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
                throw exception;
            }
        } catch (DataAccessException exception) {
            throw new AuthContextUnavailableException(exception);
        }
    }

    public UserView currentUser() {
        long id = authContext.requireUser().userId();
        try {
            return UserView.from(users.findEnabledUser(id));
        } catch (AuthException exception) {
            if (exception.getStatus() == 401) {
                StpUtil.logout(id);
            }
            throw exception;
        }
    }

    public void logout() {
        authContext.requireUserId();
        StpUtil.logoutByTokenValue(StpUtil.getTokenValue());
    }
}
