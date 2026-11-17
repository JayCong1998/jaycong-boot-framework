package io.github.jaycong.admin.service;

import cn.dev33.satoken.stp.StpLogic;
import io.github.jaycong.satoken.context.AuthContext;
import io.github.jaycong.admin.exception.AdminException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 每次管理请求核验实时账号状态和角色，不使用前端角色或会话角色快照。 */
@Service
public class AdminAccessService {
    private final StpLogic sessions;
    private final AuthContext authContext;
    private final UserService users;
    private final String serviceToken;

    public AdminAccessService(StpLogic sessions, AuthContext authContext, UserService users,
            @Value("${jaycong.rpc.admin.token:}") String serviceToken) {
        this.sessions = sessions; this.authContext = authContext; this.users = users; this.serviceToken = serviceToken;
    }

    public void requireAdmin() {
        long id = authContext.requireUserId();
        if (serviceToken.isBlank()) throw new AdminException(503, "管理服务调用凭证未配置");
        try {
            var user = users.findEnabledUser(id);
            if (!"admin".equals(user.role())) throw new AdminException(403, "仅管理员可访问后台管理接口");
        } catch (AdminException exception) {
            if (exception.getStatus() == 401) {
                sessions.logout(id);
                throw new AdminException(401, "账号已失效，请重新登录");
            }
            throw exception;
        }
    }
}
