package io.github.jaycong.api.user.management.facade;

import io.github.jaycong.api.user.management.request.*;
import io.github.jaycong.api.user.management.response.*;
import io.github.jaycong.core.model.PageResult;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/** 仅供 Admin 调用的内部管理契约；服务凭证与登录会话均须校验。 */
public interface MemberOperationManagementFacade {
    @GetMapping("/internal/admin/members/operations")
    PageResult<MemberOperationView> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "memberId", required = false) Long memberId,
            @RequestParam(value = "operatorId", required = false) Long operatorId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "operatorType", required = false) String operatorType,
            @RequestParam(value = "outcome", required = false) String outcome,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    );

    @GetMapping("/internal/admin/members/operations/{id}")
    MemberOperationView detail(@PathVariable("id") long id);

}
