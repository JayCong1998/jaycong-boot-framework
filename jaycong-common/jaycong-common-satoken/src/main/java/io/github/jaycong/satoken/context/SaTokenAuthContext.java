package io.github.jaycong.satoken.context;

import cn.dev33.satoken.exception.InvalidContextException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.SaJsonConvertException;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.github.jaycong.api.user.client.UserFeignClient;
import io.github.jaycong.api.user.response.UserInfo;
import io.github.jaycong.core.exception.BusinessException;
import io.github.jaycong.satoken.exception.AuthContextUnavailableException;
import io.github.jaycong.satoken.model.LoginUser;
import io.github.jaycong.satoken.session.AuthSessionKeys;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;

/** 会话读取委托 StpUtil，角色鉴权实时查询 User；不保存额外用户状态。 */
public class SaTokenAuthContext implements AuthContext {
    private final ObjectProvider<UserFeignClient> users;
    private final ObjectMapper json;

    public SaTokenAuthContext(ObjectProvider<UserFeignClient> users, ObjectMapper json) {
        this.users = users;
        this.json = json;
    }

    @Override
    public Optional<LoginUser> currentUser() {
        return isLogin() ? Optional.of(requireUser()) : Optional.empty();
    }

    @Override
    public LoginUser requireUser() {
        long userId = requireUserId();
        try {
            // LoginUser 通过 SaJsonType 声明可由 Sa-Token JSON 安全还原。
            LoginUser user = StpUtil.getTokenSession().getModel(AuthSessionKeys.LOGIN_USER, LoginUser.class);
            if (user == null || user.userId() != userId || user.nickname() == null
                    || user.role() == null || user.role().isBlank()) {
                throw notLogin("登录资料已失效，请重新登录");
            }
            return user;
        } catch (InvalidContextException | SaJsonConvertException | ClassCastException exception) {
            throw notLogin("登录资料已失效，请重新登录");
        } catch (DataAccessException exception) {
            throw new AuthContextUnavailableException(exception);
        }
    }

    @Override
    public long requireUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (InvalidContextException exception) {
            throw notLogin("当前调用没有登录上下文");
        } catch (DataAccessException exception) {
            throw new AuthContextUnavailableException(exception);
        }
    }

    @Override
    public long requireRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        long userId = requireUserId();
        var user = enabledUser(userId);
        if (!role.equals(user.role())) {
            throw new BusinessException(403, "admin".equals(role)
                    ? "仅管理员可执行此操作" : "当前账号无权执行此操作");
        }
        return userId;
    }

    private UserInfo enabledUser(long userId) {
        // 客户端只在鉴权时解析，不让仅使用会话读取的服务强制创建 Feign 客户端。
        var client = users.getIfAvailable();
        if (client == null) {
            throw new BusinessException(503, "用户身份核验服务未配置");
        }
        try {
            var user = client.findEnabledUser(userId);
            if (user == null || !Long.toString(userId).equals(user.id())
                    || user.role() == null || user.role().isBlank()) {
                throw new BusinessException(503, "无法核验当前账号，请稍后重试");
            }
            return user;
        } catch (FeignException exception) {
            // 服务凭证错误或非标准响应不能当作账号失效，避免误撤销其他有效会话。
            if (exception.status() == 401 && disabledUser(exception.contentUTF8())) {
                revokeSessions(userId);
                throw new BusinessException(401, "账号已失效，请重新登录");
            }
            throw new BusinessException(503, "用户服务暂不可用，请稍后重试");
        }
    }

    private boolean disabledUser(String body) {
        try {
            var response = json.readTree(body);
            return response != null && response.path("code").asInt(-1) == 401;
        } catch (JsonProcessingException exception) {
            return false;
        }
    }

    private void revokeSessions(long userId) {
        try {
            StpUtil.logout(userId);
        } catch (DataAccessException exception) {
            throw new AuthContextUnavailableException(exception);
        }
    }

    @Override
    public boolean isLogin() {
        try {
            return StpUtil.isLogin();
        } catch (InvalidContextException exception) {
            return false;
        } catch (DataAccessException exception) {
            throw new AuthContextUnavailableException(exception);
        }
    }

    private NotLoginException notLogin(String message) {
        return new NotLoginException(message, StpUtil.getLoginType(), NotLoginException.INVALID_TOKEN);
    }
}
