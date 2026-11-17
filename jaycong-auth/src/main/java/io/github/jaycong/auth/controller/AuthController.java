package io.github.jaycong.auth.controller;

import io.github.jaycong.auth.response.LoginResponse;
import io.github.jaycong.auth.response.UserView;
import io.github.jaycong.auth.service.AuthService;
import io.github.jaycong.auth.request.LoginRequest;
import io.github.jaycong.auth.request.RegisterRequest;
import io.github.jaycong.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "认证接口", description = "手机号或邮箱注册、登录及当前会话管理")
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    @Operation(summary = "注册用户", description = "手机号、邮箱至少填写一项；注册成功不自动登录。")
    public Result<UserView> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(auth.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "密码登录", description = "使用手机号或邮箱登录；tokenValue 用于 Authorization: Bearer 请求头。")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(auth.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录（兼容接口）", description = "仅撤销当前 Token，建议使用 DELETE /auth/session。", deprecated = true)
    @SecurityRequirement(name = "bearerAuth")
    public Result<Void> logout() {
        auth.logout();
        return Result.success();
    }

    /** 删除当前 Token 的会话，其他设备不受影响；旧 POST /logout 保留兼容商城。 */
    @DeleteMapping("/session")
    @Operation(summary = "删除当前登录会话", description = "仅撤销当前 Token，不影响其他设备。")
    @SecurityRequirement(name = "bearerAuth")
    public Result<Void> deleteSession() {
        auth.logout();
        return Result.success();
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前用户", description = "查询当前 Token 对应的有效用户信息。")
    @SecurityRequirement(name = "bearerAuth")
    public Result<UserView> currentUser() { return Result.success(auth.currentUser()); }
}
