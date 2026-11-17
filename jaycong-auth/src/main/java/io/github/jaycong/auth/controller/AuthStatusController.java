package io.github.jaycong.auth.controller;

import io.github.jaycong.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用于确认服务和网关路由可达，不代表已完成身份认证。 */
@RestController
@Tag(name = "服务状态")
@RequestMapping("/auth")
public class AuthStatusController {
    @GetMapping("/status")
    @Operation(summary = "检查认证服务可达性", description = "不要求登录；不代表数据库、Redis 和下游用户服务可用。")
    public Result<String> status() {
        return Result.success("auth");
    }
}
