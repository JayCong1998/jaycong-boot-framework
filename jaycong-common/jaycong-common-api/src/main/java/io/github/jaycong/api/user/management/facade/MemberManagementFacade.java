package io.github.jaycong.api.user.management.facade;

import io.github.jaycong.api.user.management.request.*;
import io.github.jaycong.api.user.management.response.*;
import io.github.jaycong.core.model.PageResult;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/** 仅供 Admin 调用的内部管理契约；服务凭证与登录会话均须校验。 */
public interface MemberManagementFacade {
    @GetMapping("/internal/admin/members")
    PageResult<MemberView> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "createTimeStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createTimeStart,
            @RequestParam(value = "createTimeEnd", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createTimeEnd
    );

    @GetMapping("/internal/admin/members/{id}")
    MemberView detail(@PathVariable("id") long id);

    @PutMapping("/internal/admin/members/{id}")
    MemberView update(@PathVariable("id") long id, @Valid @RequestBody MemberUpdateRequest request);

    @PutMapping("/internal/admin/members/{id}/status")
    MemberView status(@PathVariable("id") long id, @Valid @RequestBody MemberStatusRequest request);

}
