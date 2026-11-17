package io.github.jaycong.user.domain.service;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.jaycong.api.user.management.request.MemberStatusRequest;
import io.github.jaycong.api.user.management.request.MemberUpdateRequest;
import io.github.jaycong.api.user.request.UserAuthenticateRequest;
import io.github.jaycong.api.user.request.UserRegisterRequest;
import io.github.jaycong.user.UserException;
import io.github.jaycong.user.domain.entity.AccountNormalizer;
import io.github.jaycong.user.domain.entity.User;
import io.github.jaycong.user.infrastructure.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户注册、认证和会员变更；直接持久化实体，流水服务控制会员操作事务。 */
@Service
public class UserManageService {
    private final UserMapper mapper;
    private final UserReadService reads;
    private final PasswordEncoder passwords;
    private final String dummyHash;
    private final StpLogic sessions;
    private final MemberOperationManageService operations;

    public UserManageService(UserMapper mapper, UserReadService reads, PasswordEncoder passwords,
            StpLogic sessions, MemberOperationManageService operations) {
        this.mapper = mapper;
        this.reads = reads;
        this.passwords = passwords;
        this.dummyHash = passwords.encode("unmatched-dummy-password");
        this.sessions = sessions;
        this.operations = operations;
    }

    @Transactional
    public User register(UserRegisterRequest request) {
        AccountNormalizer.password(request.password());
        var user = User.register(request.mobile(), request.email(),
                passwords.encode(request.password()), request.nickname());
        try {
            mapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new UserException(409, "手机号或邮箱已被使用");
        }
        return user;
    }

    public User authenticate(UserAuthenticateRequest request) {
        String account = User.loginAccount(request.account(), request.password());
        var user = reads.findByAccount(account);
        // 未知账号仍执行哈希校验，不向调用方区分未知、禁用和密码错误。
        boolean matches = passwords.matches(request.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !user.isEnabled() || !matches) {
            throw new UserException(401, "账号或密码错误");
        }
        var update = user.loginUpdate(LocalDateTime.now());
        if (mapper.update(update, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId()).eq(User::getStatus, 1)) != 1) {
            throw new UserException(409, "用户状态已变化，请重试");
        }
        return user;
    }

    public User updateMember(long id, MemberUpdateRequest request) {
        var actor = reads.requireAdmin();
        return operations.execute(actor, id, "UPDATE_NICKNAME", log -> {
            var member = reads.getMember(id);
            String before = member.getNickname();
            member.changeNickname(request.nickname(), request.lockVersion());
            var updated = saveMember(member);
            log.recordChanges(before, updated.getNickname());
            return updated;
        }, null, null);
    }

    /** 禁用先提交数据库再撤销会话；启用前清理旧会话，失败阶段由流水独立记录。 */
    public User changeMemberStatus(long id, MemberStatusRequest request) {
        var actor = reads.requireAdmin();
        // 先确认目标是会员，不能对不存在或管理员账号执行会话清理。
        var member = reads.getMember(id);
        boolean enable = Integer.valueOf(1).equals(request.status());
        boolean disable = Integer.valueOf(0).equals(request.status());
        boolean revokeOnly = disable && member.getStatus() == 0;
        String action = enable ? "ENABLE" : revokeOnly ? "REVOKE_SESSIONS" : "DISABLE";
        return operations.execute(actor, id, action, log -> {
            var current = reads.getMember(id);
            String before = current.getStatus() == 1 ? "正常" : "禁用";
            current.changeStatus(request.status(), request.lockVersion());
            var updated = saveMember(current);
            log.recordChanges(before, updated.getStatus() == 1 ? "正常" : "禁用");
            return updated;
        }, enable ? () -> sessions.logout(id) : null, disable ? () -> sessions.logout(id) : null);
    }

    private User saveMember(User member) {
        if (mapper.update(member, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, member.getId()).eq(User::getRole, "user")) != 1) {
            throw new UserException(409, "会员信息已变化，请刷新后重试");
        }
        return reads.getMember(member.getId());
    }
}
