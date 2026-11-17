package io.github.jaycong.api.user.facade;

import io.github.jaycong.api.user.request.UserAuthenticateRequest;
import io.github.jaycong.api.user.request.UserRegisterRequest;
import io.github.jaycong.api.user.response.UserInfo;

/** 用户远程能力；仅供可信服务调用，不返回密码哈希或数据库实体。 */
public interface UserFacade {
    /** 注册普通用户，不创建登录会话。 */
    UserInfo register(UserRegisterRequest request);
    /** 校验凭证并更新最后登录时间，失败通过 HTTP 业务状态表达。 */
    UserInfo authenticate(UserAuthenticateRequest request);
    /** 获取当前仍有效的用户，用于会话签发后的复查和当前用户查询。 */
    UserInfo findEnabledUser(long id);
}
