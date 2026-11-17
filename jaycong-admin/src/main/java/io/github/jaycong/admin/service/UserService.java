package io.github.jaycong.admin.service;

import io.github.jaycong.api.user.client.UserFeignClient;
import io.github.jaycong.api.user.response.UserInfo;
import io.github.jaycong.admin.exception.AdminException;
import org.springframework.stereotype.Service;

/** 与 Auth 相同，由 Service 委托 User 客户端并校验远程响应。 */
@Service
public class UserService {
    private final UserFeignClient client;
    private final AdminRemoteService remote;

    public UserService(UserFeignClient client, AdminRemoteService remote) {
        this.client = client; this.remote = remote;
    }

    public UserInfo findEnabledUser(long id) {
        var user = remote.call(() -> client.findEnabledUser(id));
        if (!Long.toString(id).equals(user.id()))
            throw new AdminException(503, "无法核验当前账号，请稍后重试");
        return user;
    }
}
