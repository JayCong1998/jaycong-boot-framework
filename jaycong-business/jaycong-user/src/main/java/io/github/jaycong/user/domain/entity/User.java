package io.github.jaycong.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.jaycong.core.model.BaseEntity;
import io.github.jaycong.user.UserException;
import java.time.LocalDateTime;
import lombok.Getter;

/** 用户实体同时承载账号规则与表映射，密码哈希仅供服务内部校验。 */
@TableName("sys_user")
@Getter
public class User extends BaseEntity {
    private String mobile;
    private String email;
    private String passwordHash;
    private String nickname;
    private String role;
    private Integer status;
    private LocalDateTime lastLoginTime;

    public static User register(String mobile, String email, String passwordHash, String nickname) {
        mobile = AccountNormalizer.mobile(mobile);
        email = AccountNormalizer.email(email);
        if (mobile == null && email == null) {
            throw AccountNormalizer.invalid("手机号和邮箱至少填写一项");
        }
        requireNickname(nickname);
        var user = new User();
        user.mobile = mobile;
        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname.strip();
        user.role = "user";
        user.status = 1;
        user.setLockVersion(0);
        return user;
    }

    public static String loginAccount(String account, String password) {
        AccountNormalizer.password(password);
        if (account == null || account.isBlank() || account.length() > 254) {
            throw AccountNormalizer.invalid("登录账号无效");
        }
        return account.contains("@") ? AccountNormalizer.email(account) : AccountNormalizer.mobile(account);
    }

    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }

    public void requireEnabled() {
        if (!isEnabled()) {
            throw new UserException(401, "未登录或登录已失效");
        }
    }

    public void requireMember() {
        if (!"user".equals(role)) {
            throw new UserException(404, "会员不存在");
        }
    }

    /** 只更新登录时间，避免认证流程覆盖会员资料；版本和启用状态由 SQL 再次核验。 */
    public User loginUpdate(LocalDateTime now) {
        requireEnabled();
        var update = new User();
        update.setId(getId());
        update.setLockVersion(getLockVersion());
        update.lastLoginTime = now;
        return update;
    }

    public void changeNickname(String nickname, Integer version) {
        requireMember();
        requireNickname(nickname);
        requireVersion(version);
        this.nickname = nickname.strip();
    }

    public void changeStatus(Integer status, Integer version) {
        requireMember();
        if (status == null || (status != 0 && status != 1)) {
            throw new UserException(400, "会员状态无效");
        }
        requireVersion(version);
        this.status = status;
    }

    private void requireVersion(Integer version) {
        if (version == null || version < 0) {
            throw new UserException(400, "乐观锁版本无效");
        }
        if (!version.equals(getLockVersion())) {
            throw new UserException(409, "会员信息已变化，请刷新后重试");
        }
    }

    private static void requireNickname(String nickname) {
        if (nickname == null || nickname.isBlank() || nickname.length() > 64) {
            throw new UserException(400, "昵称不能为空且最多64个字符");
        }
    }
}
