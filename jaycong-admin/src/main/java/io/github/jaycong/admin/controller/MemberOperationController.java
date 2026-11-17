package io.github.jaycong.admin.controller;

import io.github.jaycong.admin.request.*;
import io.github.jaycong.admin.response.*;
import io.github.jaycong.core.model.PageResult;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import io.github.jaycong.admin.service.MemberOperationService;
import io.github.jaycong.core.model.Result;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Tag(name = "会员操作记录", description = "统一由 Admin 校验管理员身份并远程调用业务服务")
@RestController
@SecurityRequirement(name = "bearerAuth")
public class MemberOperationController {
    private final MemberOperationService service;
    public MemberOperationController(MemberOperationService service) {
        this.service = service;
    }

    @GetMapping("/admin/members/operations")
    @Operation(summary = "分页查询会员操作记录")
    public Result<PageResult<MemberOperationView>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "memberId", required = false) Long memberId,
            @RequestParam(value = "operatorId", required = false) Long operatorId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "operatorType", required = false) String operatorType,
            @RequestParam(value = "outcome", required = false) String outcome,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return Result.success(service.list(page, size, memberId, operatorId, action, operatorType, outcome, start, end));
    }

    @GetMapping("/admin/members/operations/{id}")
    @Operation(summary = "查询会员操作记录详情")
    public Result<MemberOperationView> detail(@PathVariable("id") long id) {
        return Result.success(service.detail(id));
    }
}
