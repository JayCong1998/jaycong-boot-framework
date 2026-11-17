package io.github.jaycong.user.facade;

import io.github.jaycong.api.user.facade.UserFacade;
import io.github.jaycong.api.user.response.UserInfo;
import io.github.jaycong.api.user.request.UserRegisterRequest;
import io.github.jaycong.api.user.request.UserAuthenticateRequest;
import io.github.jaycong.user.domain.entity.convertor.UserConvertor;
import io.github.jaycong.user.domain.service.UserManageService;
import io.github.jaycong.user.domain.service.UserReadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 内部 HTTP RPC 入口，不在网关公开路由，当前不校验服务调用凭证。
 */
@RestController
@RequestMapping("/internal/users")
public class UserFacadeServiceImpl implements UserFacade {
    private final UserManageService users;
    private final UserReadService reads;

    public UserFacadeServiceImpl(UserManageService users, UserReadService reads) {
        this.users = users;
        this.reads = reads;
    }

    @Override
    @PostMapping("/register")
    public UserInfo register(@Valid @RequestBody UserRegisterRequest request) {
        return UserConvertor.toInfo(users.register(request));
    }

    @Override
    @PostMapping("/authenticate")
    public UserInfo authenticate(@Valid @RequestBody UserAuthenticateRequest request) {
        return UserConvertor.toInfo(users.authenticate(request));
    }

    @Override
    @GetMapping("/{id}")
    public UserInfo findEnabledUser(@PathVariable long id) {
        return UserConvertor.toInfo(reads.findEnabledUser(id));
    }
}
