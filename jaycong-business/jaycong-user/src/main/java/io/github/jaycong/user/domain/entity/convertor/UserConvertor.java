package io.github.jaycong.user.domain.entity.convertor;

import io.github.jaycong.user.domain.entity.User;
import io.github.jaycong.api.user.response.UserInfo;
import io.github.jaycong.api.user.management.response.MemberView;

/** 集中处理认证资料和脱敏的会员展示，不向外暴露实体及密码哈希。 */
public final class UserConvertor {
    private UserConvertor() { }

    public static UserInfo toInfo(User user) {
        return new UserInfo(user.getId().toString(), user.getMobile(), user.getEmail(), user.getNickname(), user.getRole());
    }

    public static MemberView toMemberView(User user) {
        String mobile = user.getMobile();
        String email = user.getEmail();
        if (mobile != null) mobile = mobile.length() > 7
                ? mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4) : "****";
        if (email != null) email = "***" + email.substring(email.indexOf('@'));
        return new MemberView(user.getId().toString(), user.getNickname(), mobile, email,
                user.getStatus(), user.getCreateTime(), user.getUpdateTime(),
                user.getLastLoginTime(), user.getLockVersion());
    }
}
