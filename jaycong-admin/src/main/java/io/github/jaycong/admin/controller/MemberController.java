package io.github.jaycong.admin.controller;

import io.github.jaycong.admin.request.*;
import io.github.jaycong.admin.response.*;
import io.github.jaycong.core.model.PageResult;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import io.github.jaycong.admin.service.MemberService;
import io.github.jaycong.core.model.Result;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Tag(name = "会员管理", description = "统一由 Admin 校验管理员身份并远程调用业务服务")
@RestController
@SecurityRequirement(name = "bearerAuth")
public class MemberController {
    private final MemberService service;
    public MemberController(MemberService service) {
        this.service = service;
    }

    @GetMapping("/admin/members")
    @Operation(summary = "分页查询会员管理")
    public Result<PageResult<MemberView>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "createTimeStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createTimeStart,
            @RequestParam(value = "createTimeEnd", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createTimeEnd
    ) {
        return Result.success(service.list(page, size, keyword, id, status, createTimeStart, createTimeEnd));
    }

    @GetMapping("/admin/members/{id}")
    @Operation(summary = "查询会员管理详情")
    public Result<MemberView> detail(@PathVariable("id") long id) {
        return Result.success(service.detail(id));
    }

    @PutMapping("/admin/members/{id}")
    @Operation(summary = "修改资料（提交 lockVersion）")
    public Result<MemberView> update(
            @PathVariable("id") long id,
            @Valid @RequestBody MemberUpdateRequest request
    ) {
        return Result.success(service.update(id, request));
    }

    @PutMapping("/admin/members/{id}/status")
    @Operation(summary = "修改状态（提交 lockVersion）")
    public Result<MemberView> status(
            @PathVariable("id") long id,
            @Valid @RequestBody MemberStatusRequest request
    ) {
        return Result.success(service.status(id, request));
    }
}
