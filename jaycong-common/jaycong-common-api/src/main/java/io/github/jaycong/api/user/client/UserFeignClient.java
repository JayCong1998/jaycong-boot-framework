package io.github.jaycong.api.user.client;

import io.github.jaycong.api.user.facade.UserFacade;
import io.github.jaycong.api.user.config.UserFeignConfiguration;
import io.github.jaycong.api.user.request.UserAuthenticateRequest;
import io.github.jaycong.api.user.request.UserRegisterRequest;
import io.github.jaycong.api.user.response.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/** 由调用方显式启用；未指定 URL 时通过 user 服务名负载均衡。 */
@FeignClient(name = "user", contextId = "authUser", configuration = UserFeignConfiguration.class)
public interface UserFeignClient extends UserFacade {
    @Override
    @PostMapping("/internal/users/register")
    UserInfo register(@RequestBody UserRegisterRequest request);

    @Override
    @PostMapping("/internal/users/authenticate")
    UserInfo authenticate(@RequestBody UserAuthenticateRequest request);

    @Override
    @GetMapping("/internal/users/{id}")
    UserInfo findEnabledUser(@PathVariable("id") long id);
}
